package com.openagentflow.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PDF中文文本解析回归测试。
 */
class DocumentParseServicePdfTests {

    /**
     * 使用外部指定的真实PDF检查提取结果，未传路径时在普通CI中跳过。
     */
    @Test
    void shouldExtractReadablePdfText() {
        String pdfPath = System.getProperty("oaf.test.pdf", "");
        Assumptions.assumeTrue(!pdfPath.isBlank() && Files.exists(Path.of(pdfPath)));

        String text = new DocumentParseService().parse(Path.of(pdfPath), "pdf");

        assertFalse(text.isBlank());
        assertTrue(text.length() > 100, "PDF应提取到足够的可读文本");
        long replacementCount = text.codePoints().filter(codePoint -> codePoint == 0xFFFD).count();
        assertTrue(replacementCount * 100D / text.codePointCount(0, text.length()) < 2D,
                "PDF文本中的Unicode替换字符比例不应超过2% ");
        System.out.println("PDF_TEXT_LENGTH=" + text.length());
        System.out.println("PDF_TEXT_PREVIEW=" + text.substring(0, Math.min(500, text.length())).replaceAll("\\s+", " "));
    }
}
