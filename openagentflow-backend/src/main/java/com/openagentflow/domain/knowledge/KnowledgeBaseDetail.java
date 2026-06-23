package com.openagentflow.domain.knowledge;

import java.util.List;

/**
 * 知识库详情对象。
 */
public class KnowledgeBaseDetail extends KnowledgeBaseSummary {

    /** 知识库文档列表。 */
    private List<KnowledgeDocumentSummary> documents;

    /** 最近分片列表。 */
    private List<KnowledgeChunkSummary> chunks;

    public List<KnowledgeDocumentSummary> getDocuments() {
        return documents;
    }

    public void setDocuments(List<KnowledgeDocumentSummary> documents) {
        this.documents = documents;
    }

    public List<KnowledgeChunkSummary> getChunks() {
        return chunks;
    }

    public void setChunks(List<KnowledgeChunkSummary> chunks) {
        this.chunks = chunks;
    }
}
