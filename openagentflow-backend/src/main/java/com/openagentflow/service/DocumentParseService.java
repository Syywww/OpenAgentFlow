package com.openagentflow.service;

import com.openagentflow.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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
