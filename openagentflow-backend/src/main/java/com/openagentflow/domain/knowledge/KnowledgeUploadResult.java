package com.openagentflow.domain.knowledge;

/**
 * 知识文档上传处理结果。
 */
public class KnowledgeUploadResult {

    /** 文档摘要。 */
    private KnowledgeDocumentSummary document;

    /** 新增分片数量。 */
    private Integer chunkCount;

    /** 新增向量数量。 */
    private Integer embeddingCount;

    /** Milvus 同步是否全部成功。 */
    private Boolean milvusSynced;

    /** 处理提示或兜底说明。 */
    private String message;

    /** 是否已进入后台处理。 */
    private Boolean asyncAccepted;

    public KnowledgeDocumentSummary getDocument() {
        return document;
    }

    public void setDocument(KnowledgeDocumentSummary document) {
        this.document = document;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public Integer getEmbeddingCount() {
        return embeddingCount;
    }

    public void setEmbeddingCount(Integer embeddingCount) {
        this.embeddingCount = embeddingCount;
    }

    public Boolean getMilvusSynced() {
        return milvusSynced;
    }

    public void setMilvusSynced(Boolean milvusSynced) {
        this.milvusSynced = milvusSynced;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getAsyncAccepted() {
        return asyncAccepted;
    }

    public void setAsyncAccepted(Boolean asyncAccepted) {
        this.asyncAccepted = asyncAccepted;
    }
}
