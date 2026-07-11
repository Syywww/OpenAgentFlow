package com.openagentflow.service;

import com.openagentflow.exception.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

/**
 * 文档解析服务。
 */
@Service
public class DocumentParseService {

    /** HTML 标签匹配表达式。 */
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    /**
     * 根据扩展名解析上传文件内容。
     *
     * @param bytes 文件字节
     * @param fileExt 文件扩展名
     * @return 可用于切片和向量化的纯文本
     */
    public String parse(byte[] bytes, String fileExt) {
        return parse(new ByteArrayInputStream(bytes), fileExt);
    }

    /**
     * 从 Worker 临时文件解析内容，避免对象存储文件整体进入 JVM 堆内存。
     *
     * @param path 临时文件路径
     * @param fileExt 文件扩展名
     * @return 可切片纯文本
     */
    public String parse(Path path, String fileExt) {
        if ("pdf".equalsIgnoreCase(fileExt)) {
            return normalizeAndValidate(parsePdf(path));
        }
        try (InputStream input = Files.newInputStream(path)) {
            return parse(input, fileExt);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("DOCUMENT_PARSE_FAILED", "文档流式解析失败：" + exception.getMessage());
        }
    }

    private String parse(InputStream input, String fileExt) {
        String ext = fileExt == null ? "" : fileExt.toLowerCase(Locale.ROOT);
        String text = switch (ext) {
            case "txt", "md", "markdown", "csv", "json", "log", "yml", "yaml" -> readUtf8(input);
            case "html", "htm" -> stripHtml(readUtf8(input));
            case "docx" -> parseDocx(input);
            case "xlsx", "xlsm" -> parseZipXmlText(input, "xl/worksheets/", "xl/sharedStrings.xml");
            case "pptx" -> parseZipXmlText(input, "ppt/slides/", "");
            case "pdf" -> parsePdf(readAll(input));
            default -> readUtf8(input);
        };
        return normalizeAndValidate(text);
    }

    /**
     * 提取 DOCX 中 word/document.xml 的文本。
     *
     * @param bytes DOCX 文件字节
     * @return 纯文本
     */
    private String parseDocx(InputStream input) {
        StringBuilder builder = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(input)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!"word/document.xml".equals(entry.getName())) {
                    continue;
                }
                String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                String text = xml.replace("</w:p>", "\n")
                        .replaceAll("<[^>]+>", "")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'");
                builder.append(text);
            }
        } catch (Exception exception) {
            throw new BusinessException("DOCUMENT_PARSE_FAILED", "DOCX 解析失败：" + exception.getMessage());
        }
        return builder.toString();
    }

    /**
     * 从 Office Open XML 压缩包中提取文本，用于 Excel/PPTX 的基础解析。
     *
     * @param bytes 文件字节
     * @param entryPrefix 需要读取的条目前缀
     * @param extraEntry 额外读取的共享字符串条目
     * @return 提取出的文本
     */
    private String parseZipXmlText(InputStream input, String entryPrefix, String extraEntry) {
        StringBuilder builder = new StringBuilder();
        List<String> sharedStrings = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(input)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (StringUtils.hasText(extraEntry) && extraEntry.equals(name)) {
                    sharedStrings.addAll(extractXmlTextNodes(new String(zip.readAllBytes(), StandardCharsets.UTF_8)));
                    continue;
                }
                if (!name.startsWith(entryPrefix) || !name.endsWith(".xml")) {
                    continue;
                }
                String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                List<String> texts = extractXmlTextNodes(xml);
                if (texts.isEmpty() && !sharedStrings.isEmpty()) {
                    texts = resolveSheetSharedStringIndexes(xml, sharedStrings);
                }
                if (!texts.isEmpty()) {
                    builder.append(String.join(" ", texts)).append("\n\n");
                }
            }
        } catch (Exception exception) {
            throw new BusinessException("DOCUMENT_PARSE_FAILED", "Office 文档解析失败：" + exception.getMessage());
        }
        return builder.toString();
    }

    /**
     * 提取 XML 中常见文本节点。
     *
     * @param xml XML 内容
     * @return 文本列表
     */
    private List<String> extractXmlTextNodes(String xml) {
        List<String> values = new ArrayList<>();
        java.util.regex.Matcher matcher = Pattern.compile("<(?:a:)?t[^>]*>(.*?)</(?:a:)?t>", Pattern.DOTALL).matcher(xml);
        while (matcher.find()) {
            String value = unescapeXml(matcher.group(1).replaceAll("<[^>]+>", "")).trim();
            if (StringUtils.hasText(value)) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * 解析 Excel 工作表里的 sharedString 索引。
     *
     * @param xml 工作表 XML
     * @param sharedStrings 共享字符串
     * @return 工作表文本
     */
    private List<String> resolveSheetSharedStringIndexes(String xml, List<String> sharedStrings) {
        List<String> values = new ArrayList<>();
        java.util.regex.Matcher matcher = Pattern.compile("<c[^>]*t=\"s\"[^>]*>\\s*<v>(\\d+)</v>\\s*</c>", Pattern.DOTALL).matcher(xml);
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            if (index >= 0 && index < sharedStrings.size()) {
                values.add(sharedStrings.get(index));
            }
        }
        return values;
    }

    /**
     * 从Worker临时文件解析PDF，PDFBox使用临时文件缓存控制大文档堆内存占用。
     *
     * @param path PDF临时文件
     * @return 按阅读顺序提取的Unicode文本
     */
    private String parsePdf(Path path) {
        try (PDDocument document = Loader.loadPDF(path.toFile(), IOUtils.createTempFileOnlyStreamCache())) {
            return extractPdfText(document);
        } catch (InvalidPasswordException exception) {
            throw new BusinessException("PDF_PASSWORD_REQUIRED", "PDF已加密，请解除密码后重新上传");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("PDF_PARSE_FAILED", "PDF文本解析失败：" + exception.getMessage());
        }
    }

    /**
     * 解析内存中的PDF字节，兼容旧版直接传字节调用入口。
     *
     * @param bytes PDF文件字节
     * @return Unicode文本
     */
    private String parsePdf(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return extractPdfText(document);
        } catch (InvalidPasswordException exception) {
            throw new BusinessException("PDF_PASSWORD_REQUIRED", "PDF已加密，请解除密码后重新上传");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("PDF_PARSE_FAILED", "PDF文本解析失败：" + exception.getMessage());
        }
    }

    /**
     * 按页面和文字位置提取PDF文本，并识别扫描件或字体映射缺失场景。
     *
     * @param document PDFBox文档对象
     * @return 提取文本
     */
    private String extractPdfText(PDDocument document) throws Exception {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setPageEnd("\n\n");
        String text = stripper.getText(document);
        if (!StringUtils.hasText(text) || visibleCharacterCount(text) < Math.max(8, document.getNumberOfPages() * 2)) {
            throw new BusinessException("PDF_OCR_REQUIRED", "PDF没有可提取的文本层，可能是扫描件，请先执行OCR后重新上传");
        }
        if (replacementCharacterRate(text) > 0.02D) {
            throw new BusinessException("PDF_TEXT_QUALITY_LOW", "PDF字体缺少Unicode映射，文本提取质量过低，请转换为可搜索PDF后重新上传");
        }
        return text;
    }

    /** 统计非空白可见字符数量。 */
    private long visibleCharacterCount(String text) {
        return text.codePoints().filter(codePoint -> !Character.isWhitespace(codePoint) && !Character.isISOControl(codePoint)).count();
    }

    /** 计算Unicode替换字符占比，用于识别字体映射失败。 */
    private double replacementCharacterRate(String text) {
        long visible = Math.max(1L, visibleCharacterCount(text));
        long replacements = text.codePoints().filter(codePoint -> codePoint == 0xFFFD).count();
        return replacements * 1D / visible;
    }

    private String readUtf8(InputStream input) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[16 * 1024];
            int length;
            while ((length = reader.read(buffer)) >= 0) {
                builder.append(buffer, 0, length);
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new BusinessException("DOCUMENT_PARSE_FAILED", "文本流式解析失败：" + exception.getMessage());
        }
    }

    private byte[] readAll(InputStream input) {
        try {
            return input.readAllBytes();
        } catch (Exception exception) {
            throw new BusinessException("DOCUMENT_PARSE_FAILED", "二进制文档读取失败：" + exception.getMessage());
        }
    }

    /**
     * 去除 HTML 标签并保留段落间隔。
     *
     * @param html HTML 文本
     * @return 纯文本
     */
    private String stripHtml(String html) {
        return HTML_TAG.matcher(html.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("(?i)</p>", "\n")).replaceAll(" ");
    }

    /**
     * 还原 XML 实体。
     *
     * @param text XML 文本
     * @return 普通文本
     */
    private String unescapeXml(String text) {
        return text == null ? "" : text
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    /**
     * 规整空白字符，避免切片出现大量空行。
     *
     * @param text 原始文本
     * @return 规整后的文本
     */
    private String normalize(String text) {
        return text == null ? "" : text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /** 统一规整文本并检查空内容。 */
    private String normalizeAndValidate(String text) {
        String normalized = normalize(text);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("DOCUMENT_PARSE_EMPTY", "文档解析后没有可用文本");
        }
        return normalized;
    }
}
