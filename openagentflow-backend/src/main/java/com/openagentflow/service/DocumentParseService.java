package com.openagentflow.service;

import com.openagentflow.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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
        String ext = fileExt == null ? "" : fileExt.toLowerCase(Locale.ROOT);
        String text = switch (ext) {
            case "txt", "md", "markdown", "csv", "json", "log", "yml", "yaml" -> new String(bytes, StandardCharsets.UTF_8);
            case "html", "htm" -> stripHtml(new String(bytes, StandardCharsets.UTF_8));
            case "docx" -> parseDocx(bytes);
            case "xlsx", "xlsm" -> parseZipXmlText(bytes, "xl/worksheets/", "xl/sharedStrings.xml");
            case "pptx" -> parseZipXmlText(bytes, "ppt/slides/", "");
            case "pdf" -> parsePdfFallback(bytes);
            default -> new String(bytes, StandardCharsets.UTF_8);
        };
        text = normalize(text);
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("DOCUMENT_PARSE_EMPTY", "文档解析后没有可用文本");
        }
        return text;
    }

    /**
     * 提取 DOCX 中 word/document.xml 的文本。
     *
     * @param bytes DOCX 文件字节
     * @return 纯文本
     */
    private String parseDocx(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
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
    private String parseZipXmlText(byte[] bytes, String entryPrefix, String extraEntry) {
        StringBuilder builder = new StringBuilder();
        List<String> sharedStrings = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
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
     * PDF 基础兜底提取。
     *
     * @param bytes PDF 文件字节
     * @return 尽力提取出的可读文本
     */
    private String parsePdfFallback(byte[] bytes) {
        String raw = new String(bytes, StandardCharsets.ISO_8859_1);
        return raw.replaceAll("[^\\x20-\\x7E\\u4e00-\\u9fa5\\r\\n]+", " ");
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
}
