package com.openagentflow.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 知识文本切片服务。
 */
@Service
public class KnowledgeChunkingService {

    /**
     * 根据策略切分文本。
     *
     * <p>fixed_size 保持旧逻辑；recursive / structure / markdown 会优先按标题、段落、列表和句子边界切分，
     * 超长内容再回退到固定窗口，避免把一段完整语义硬切断。</p>
     *
     * @param text 原始文本
     * @param strategy 切片策略
     * @param chunkSize 切片大小
     * @param overlap 重叠长度
     * @return 切片文本列表
     */
    public List<String> split(String text, String strategy, Integer chunkSize, Integer overlap) {
        String normalizedStrategy = strategy == null ? "" : strategy.trim().toLowerCase(Locale.ROOT);
        if ("parent_child".equals(normalizedStrategy)) {
            return splitSegments(text, strategy, chunkSize, overlap).stream()
                    .filter(ChunkSegment::embeddingEnabled)
                    .map(ChunkSegment::content)
                    .toList();
        }
        if (List.of("recursive", "recursive_structure", "structure", "markdown").contains(normalizedStrategy)) {
            return splitRecursive(text, chunkSize, overlap);
        }
        return splitFixed(text, chunkSize, overlap);
    }

    /**
     * 生成带元数据的切片段落。
     *
     * <p>parent_child 策略会同时生成父分片和子分片：父分片不向量化，用于回答上下文；子分片向量化，用于精准召回。</p>
     *
     * @param text 原始文本
     * @param strategy 切片策略
     * @param chunkSize 子分片大小
     * @param overlap 重叠长度
     * @return 带元数据的切片段落
     */
    public List<ChunkSegment> splitSegments(String text, String strategy, Integer chunkSize, Integer overlap) {
        String normalizedStrategy = strategy == null ? "" : strategy.trim().toLowerCase(Locale.ROOT);
        if ("parent_child".equals(normalizedStrategy)) {
            return splitParentChild(text, chunkSize, overlap);
        }
        List<String> chunks = split(text, strategy, chunkSize, overlap);
        List<ChunkSegment> segments = new ArrayList<>();
        int cursor = 0;
        for (int index = 0; index < chunks.size(); index++) {
            String content = chunks.get(index);
            int start = findOffset(text, content, cursor);
            int end = start < 0 ? 0 : start + content.length();
            cursor = Math.max(cursor, end);
            String sectionTitle = extractSectionTitle(content);
            segments.add(new ChunkSegment(content, "child", null, index + 1, sectionTitle, sectionTitle, index + 1, start, end, true));
        }
        return segments;
    }

    /**
     * 按固定窗口切分文本，保留旧调用入口兼容历史代码。
     *
     * @param text 原始文本
     * @param chunkSize 切片大小
     * @param overlap 重叠长度
     * @return 切片文本列表
     */
    public List<String> split(String text, Integer chunkSize, Integer overlap) {
        return splitFixed(text, chunkSize, overlap);
    }

    /**
     * 按固定窗口切分文本。
     *
     * @param text 原始文本
     * @param chunkSize 切片大小
     * @param overlap 重叠长度
     * @return 切片文本列表
     */
    private List<String> splitFixed(String text, Integer chunkSize, Integer overlap) {
        int size = chunkSize == null || chunkSize < 100 ? 512 : chunkSize;
        int overlapSize = overlap == null || overlap < 0 ? 64 : Math.min(overlap, size / 2);
        List<String> chunks = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return chunks;
        }
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
     * 递归结构化切片。
     *
     * <p>先按文档结构得到候选单元，再把候选单元打包到目标大小；单元过长时继续按句子切，
     * 句子仍过长时才使用固定窗口兜底。</p>
     *
     * @param text 原始文本
     * @param chunkSize 切片大小
     * @param overlap 重叠长度
     * @return 切片文本列表
     */
    private List<String> splitRecursive(String text, Integer chunkSize, Integer overlap) {
        int size = chunkSize == null || chunkSize < 100 ? 512 : chunkSize;
        int overlapSize = overlap == null || overlap < 0 ? 64 : Math.min(overlap, size / 2);
        List<String> chunks = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return chunks;
        }
        List<String> units = new ArrayList<>();
        for (String unit : structuralUnits(text)) {
            if (unit.length() <= size) {
                units.add(unit);
            } else {
                units.addAll(splitLongUnit(unit, size, overlapSize));
            }
        }
        StringBuilder current = new StringBuilder();
        for (String unit : units) {
            if (!StringUtils.hasText(unit)) {
                continue;
            }
            if (current.isEmpty()) {
                current.append(unit.trim());
                continue;
            }
            int nextLength = current.length() + 2 + unit.length();
            if (nextLength <= size) {
                current.append("\n\n").append(unit.trim());
                continue;
            }
            addChunk(chunks, current.toString());
            String tail = overlapTail(current.toString(), overlapSize);
            current.setLength(0);
            if (StringUtils.hasText(tail)) {
                current.append(tail).append("\n\n");
            }
            current.append(unit.trim());
        }
        addChunk(chunks, current.toString());
        return chunks;
    }

    /**
     * Parent-Child 切片：父分片承载上下文，子分片承载召回。
     *
     * @param text 原始文本
     * @param chunkSize 子分片大小
     * @param overlap 重叠长度
     * @return 父子分片列表
     */
    private List<ChunkSegment> splitParentChild(String text, Integer chunkSize, Integer overlap) {
        int childSize = chunkSize == null || chunkSize < 100 ? 512 : chunkSize;
        int parentSize = Math.max(childSize * 3, 1200);
        int parentOverlap = Math.min(Math.max(overlap == null ? 128 : overlap * 2, 96), parentSize / 3);
        List<String> parents = splitRecursive(text, parentSize, parentOverlap);
        List<ChunkSegment> segments = new ArrayList<>();
        int parentCursor = 0;
        for (int parentIndex = 0; parentIndex < parents.size(); parentIndex++) {
            String parent = parents.get(parentIndex);
            int parentStart = findOffset(text, parent, parentCursor);
            int parentEnd = parentStart < 0 ? 0 : parentStart + parent.length();
            parentCursor = Math.max(parentCursor, parentEnd);
            String sectionTitle = extractSectionTitle(parent);
            int parentOrdinal = parentIndex + 1;
            segments.add(new ChunkSegment(parent, "parent", null, parentOrdinal, sectionTitle, sectionTitle, parentOrdinal, parentStart, parentEnd, false));
            List<String> children = splitRecursive(parent, childSize, overlap);
            int childCursor = 0;
            for (int childIndex = 0; childIndex < children.size(); childIndex++) {
                String child = children.get(childIndex);
                int localStart = findOffset(parent, child, childCursor);
                int childStart = parentStart < 0 || localStart < 0 ? 0 : parentStart + localStart;
                int childEnd = childStart + child.length();
                childCursor = Math.max(childCursor, localStart < 0 ? childCursor : localStart + child.length());
                segments.add(new ChunkSegment(child, "child", parentOrdinal, childIndex + 1, sectionTitle, sectionTitle, childIndex + 1, childStart, childEnd, true));
            }
        }
        return segments;
    }

    /**
     * 把文档切成结构单元，标题、空行和列表边界会形成更自然的候选块。
     *
     * @param text 原始文本
     * @return 结构候选块
     */
    private List<String> structuralUnits(String text) {
        List<String> units = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : text.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed)) {
                flushUnit(units, current);
                continue;
            }
            if (isHeading(trimmed) || isListItem(trimmed)) {
                flushUnit(units, current);
            }
            if (!current.isEmpty()) {
                current.append('\n');
            }
            current.append(trimmed);
        }
        flushUnit(units, current);
        return units;
    }

    /**
     * 将超长结构单元继续按句子切分。
     *
     * @param unit 超长结构单元
     * @param size 切片大小
     * @param overlapSize 重叠长度
     * @return 已控制长度的候选块
     */
    private List<String> splitLongUnit(String unit, int size, int overlapSize) {
        List<String> result = new ArrayList<>();
        List<String> sentences = sentenceUnits(unit);
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (sentence.length() > size) {
                addChunk(result, current.toString());
                current.setLength(0);
                result.addAll(splitFixed(sentence, size, overlapSize));
                continue;
            }
            if (current.isEmpty()) {
                current.append(sentence);
                continue;
            }
            if (current.length() + sentence.length() <= size) {
                current.append(sentence);
            } else {
                addChunk(result, current.toString());
                String tail = overlapTail(current.toString(), overlapSize);
                current.setLength(0);
                if (StringUtils.hasText(tail)) {
                    current.append(tail);
                }
                current.append(sentence);
            }
        }
        addChunk(result, current.toString());
        return result;
    }

    /**
     * 按句号、问号、感叹号、分号和换行得到句子单元。
     *
     * @param text 文本
     * @return 句子列表
     */
    private List<String> sentenceUnits(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            current.append(c);
            if (isSentenceBoundary(c)) {
                addChunk(sentences, current.toString());
                current.setLength(0);
            }
        }
        addChunk(sentences, current.toString());
        return sentences;
    }

    /**
     * 判断是否 Markdown 或常见中文标题。
     *
     * @param line 文本行
     * @return 是否标题行
     */
    private boolean isHeading(String line) {
        return line.matches("^#{1,6}\\s+.+")
                || line.matches("^第[一二三四五六七八九十百千万0-9]+[章节部分].*")
                || line.matches("^[0-9]+(\\.[0-9]+)*[、.．]\\s*.+");
    }

    /**
     * 判断是否列表项。
     *
     * @param line 文本行
     * @return 是否列表项
     */
    private boolean isListItem(String line) {
        return line.matches("^[-*+]\\s+.+")
                || line.matches("^[0-9]+[).、]\\s*.+")
                || line.matches("^[（(][0-9一二三四五六七八九十]+[）)]\\s*.+");
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
            if (isSentenceBoundary(c)) {
                boundary = i + 1;
                break;
            }
        }
        return boundary > start ? boundary : hardEnd;
    }

    /**
     * 判断是否句子或段落边界。
     *
     * @param c 字符
     * @return 是否边界字符
     */
    private boolean isSentenceBoundary(char c) {
        return c == '\n' || c == '。' || c == '！' || c == '？' || c == '.' || c == ';' || c == '；';
    }

    /**
     * 输出一个非空分片。
     *
     * @param chunks 分片集合
     * @param text 候选文本
     */
    private void addChunk(List<String> chunks, String text) {
        String chunk = text == null ? "" : text.trim();
        if (!chunk.isBlank()) {
            chunks.add(chunk);
        }
    }

    /**
     * 输出结构单元并清空缓冲区。
     *
     * @param units 结构单元集合
     * @param current 当前缓冲区
     */
    private void flushUnit(List<String> units, StringBuilder current) {
        addChunk(units, current.toString());
        current.setLength(0);
    }

    /**
     * 从上一个分片尾部抽取重叠上下文。
     *
     * @param text 上一个分片
     * @param overlapSize 重叠长度
     * @return 尾部上下文
     */
    private String overlapTail(String text, int overlapSize) {
        if (!StringUtils.hasText(text) || overlapSize <= 0) {
            return "";
        }
        int start = Math.max(0, text.length() - overlapSize);
        return text.substring(start).trim();
    }

    /**
     * 在原文中查找分片偏移。
     *
     * @param text 原文
     * @param chunk 分片
     * @param from 起始查找位置
     * @return 起始偏移，找不到返回 -1
     */
    private int findOffset(String text, String chunk, int from) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(chunk)) {
            return -1;
        }
        int start = text.indexOf(chunk, Math.max(0, from));
        return start >= 0 ? start : text.indexOf(chunk);
    }

    /**
     * 提取分片中的第一个标题作为章节标题。
     *
     * @param text 分片文本
     * @return 章节标题
     */
    private String extractSectionTitle(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        for (String line : text.split("\\n")) {
            String trimmed = line.trim();
            if (isHeading(trimmed)) {
                return trimmed.replaceFirst("^#{1,6}\\s+", "");
            }
        }
        String firstLine = text.split("\\n")[0].trim();
        return firstLine.length() > 80 ? firstLine.substring(0, 80) : firstLine;
    }

    /**
     * 带元数据的知识分片。
     *
     * @param content 分片内容
     * @param level 分片层级，parent/child
     * @param parentOrdinal 父分片临时序号
     * @param ordinal 当前层级序号
     * @param sectionTitle 章节标题
     * @param sectionPath 章节路径
     * @param paragraphNo 段落序号
     * @param startOffset 起始偏移
     * @param endOffset 结束偏移
     * @param embeddingEnabled 是否需要向量化
     */
    public record ChunkSegment(String content,
                               String level,
                               Integer parentOrdinal,
                               Integer ordinal,
                               String sectionTitle,
                               String sectionPath,
                               Integer paragraphNo,
                               Integer startOffset,
                               Integer endOffset,
                               boolean embeddingEnabled) {
    }
}
