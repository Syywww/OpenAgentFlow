package com.openagentflow.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识文本切片服务。
 */
@Service
public class KnowledgeChunkingService {

    /**
     * 按固定窗口切分文本。
     *
     * @param text 原始文本
     * @param chunkSize 切片大小
     * @param overlap 重叠长度
     * @return 切片文本列表
     */
    public List<String> split(String text, Integer chunkSize, Integer overlap) {
        int size = chunkSize == null || chunkSize < 100 ? 512 : chunkSize;
        int overlapSize = overlap == null || overlap < 0 ? 64 : Math.min(overlap, size / 2);
        List<String> chunks = new ArrayList<>();
        int cursor = 0;
        while (cursor < text.length()) {
            int end = Math.min(cursor + size, text.length());
            int naturalEnd = findNaturalBoundary(text, cursor, end);
            String chunk = text.substring(cursor, naturalEnd).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (naturalEnd >= text.length()) {
                break;
            }
            cursor = Math.max(naturalEnd - overlapSize, cursor + 1);
        }
        return chunks;
    }

    /**
     * 估算 Token 数，中文场景用字符数折算即可满足调试台展示。
     *
     * @param text 分片文本
     * @return Token 估算值
     */
    public int estimateTokens(String text) {
        return text == null ? 0 : Math.max(1, (int) Math.ceil(text.length() / 1.8));
    }

    /**
     * 尽量在段落、句号或换行处结束切片。
     *
     * @param text 原始文本
     * @param start 开始位置
     * @param hardEnd 最大结束位置
     * @return 自然结束位置
     */
    private int findNaturalBoundary(String text, int start, int hardEnd) {
        int searchFrom = Math.max(start + 120, hardEnd - 160);
        int boundary = -1;
        for (int i = hardEnd - 1; i >= searchFrom; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '。' || c == '！' || c == '？' || c == '.' || c == ';') {
                boundary = i + 1;
                break;
            }
        }
        return boundary > start ? boundary : hardEnd;
    }
}
