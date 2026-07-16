package com.openagentflow.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 文档流水线关键可靠性规则单元测试。 */
class DocumentPipelineReliabilityTests {

    /** 不同父分片下局部序号相同的子分片不能发生ID碰撞。 */
    @Test
    void stableChunkIdShouldRemainUniqueAcrossParents() {
        String first = DocumentPipelineReliability.stableChunkId("doc-1", 2, "child", 1, 0, 0, "相同开头");
        String second = DocumentPipelineReliability.stableChunkId("doc-1", 7, "child", 1, 1, 0, "相同开头");
        assertNotEquals(first, second);
        assertEquals(first, DocumentPipelineReliability.stableChunkId("doc-1", 2, "child", 1, 0, 0, "相同开头"));
    }

    /** 百余个分片必须全部生成唯一稳定ID。 */
    @Test
    void stableChunkIdShouldBeUniqueForLargeDocument() {
        Set<String> ids = new HashSet<>();
        for (int chunkNo = 1; chunkNo <= 500; chunkNo++) {
            ids.add(DocumentPipelineReliability.stableChunkId("doc-large", chunkNo, "child",
                    chunkNo % 4, chunkNo / 5, chunkNo % 8, "内容-" + chunkNo));
        }
        assertEquals(500, ids.size());
    }

    /** 模型部分返回必须失败，不能继续持久化。 */
    @Test
    void embeddingCardinalityShouldRejectPartialResponse() {
        assertThrows(IllegalStateException.class,
                () -> DocumentPipelineReliability.requireVectorCardinality(16, List.of(1, 2, 3, 4), "Embedding分片"));
        DocumentPipelineReliability.requireVectorCardinality(4, List.of(1, 2, 3, 4), "Embedding分片");
    }

    /** Fan-in必须同时满足节点已创建完整和全部成功。 */
    @Test
    void stageBarrierShouldRequireExpectedShardCount() {
        assertFalse(DocumentPipelineReliability.stageComplete(1, 1, 7));
        assertFalse(DocumentPipelineReliability.stageComplete(7, 6, 7));
        assertTrue(DocumentPipelineReliability.stageComplete(7, 7, 7));
    }

    /** 旧代际Worker必须被拒绝，避免覆盖新一轮重新解析的数据。 */
    @Test
    void pipelineFenceShouldRejectStaleRootTask() {
        assertThrows(IllegalStateException.class,
                () -> DocumentPipelineReliability.requireCurrentPipeline("root-new", 3, "root-old", 2));
        assertThrows(IllegalStateException.class,
                () -> DocumentPipelineReliability.requireCurrentPipeline("root-new", 3, "root-new", 2));
        DocumentPipelineReliability.requireCurrentPipeline("root-new", 3, "root-new", 3);
    }

    /** 最终收口必须同时核对分片总数和已同步向量总数。 */
    @Test
    void finalCardinalityShouldRejectPartialPersistence() {
        assertThrows(IllegalStateException.class,
                () -> DocumentPipelineReliability.requireFinalCardinality(120, 119, 100, 100));
        assertThrows(IllegalStateException.class,
                () -> DocumentPipelineReliability.requireFinalCardinality(120, 120, 100, 99));
        DocumentPipelineReliability.requireFinalCardinality(120, 120, 100, 100);
    }
}
