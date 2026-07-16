package com.openagentflow.service;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * 文档流水线可靠性规则。
 *
 * <p>集中维护稳定ID、Embedding基数和Fan-in屏障，防止并发流水线再次出现静默丢分片。</p>
 */
public final class DocumentPipelineReliability {

    private DocumentPipelineReliability() { }

    /** 根据全局位置、父级和内容生成稳定且跨父分片唯一的ID。 */
    public static String stableChunkId(String documentId,
                                       int chunkNo,
                                       String level,
                                       Integer ordinal,
                                       Integer parentOrdinal,
                                       int startOffset,
                                       String content) {
        String contentHash = DigestUtils.md5DigestAsHex((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
        String seed = documentId + ":" + chunkNo + ":" + level + ":" + ordinal + ":" + parentOrdinal
                + ":" + startOffset + ":" + contentHash;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** 校验模型返回向量数必须与输入文本数严格相等。 */
    public static void requireVectorCardinality(int expected, List<?> vectors, String stage) {
        int actual = vectors == null ? 0 : vectors.size();
        if (actual != expected) {
            throw new IllegalStateException(stage + "向量数量不一致：expected=" + expected + ", actual=" + actual);
        }
    }

    /** 只有预期节点全部创建且全部成功时，Fan-in阶段才允许继续。 */
    public static boolean stageComplete(long total, long success, int expected) {
        return expected > 0 && total == expected && success == expected;
    }

    /** 校验当前Worker属于文档最新流水线代际。 */
    public static void requireCurrentPipeline(String currentRootTaskId,
                                              long currentGeneration,
                                              String workerRootTaskId,
                                              long workerGeneration) {
        if (currentRootTaskId == null || currentRootTaskId.isBlank()
                || !currentRootTaskId.equals(workerRootTaskId) || currentGeneration != workerGeneration) {
            throw new IllegalStateException("文档流水线代际已变化，拒绝旧Worker写入：current="
                    + currentRootTaskId + "/" + currentGeneration + ", worker="
                    + workerRootTaskId + "/" + workerGeneration);
        }
    }

    /** 校验最终分片与向量数量，禁止部分成功被误标为全部完成。 */
    public static void requireFinalCardinality(long expectedChunks,
                                               long actualChunks,
                                               long expectedEmbeddings,
                                               long actualEmbeddings) {
        if (expectedChunks <= 0 || expectedEmbeddings <= 0
                || expectedChunks != actualChunks || expectedEmbeddings != actualEmbeddings) {
            throw new IllegalStateException("文档流水线最终数量不一致：expectedChunks=" + expectedChunks
                    + ", actualChunks=" + actualChunks + ", expectedEmbeddings=" + expectedEmbeddings
                    + ", actualEmbeddings=" + actualEmbeddings);
        }
    }
}
