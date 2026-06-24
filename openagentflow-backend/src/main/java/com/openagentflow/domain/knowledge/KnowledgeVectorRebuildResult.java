package com.openagentflow.domain.knowledge;

/**
 * 知识库向量重建结果。
 */
public class KnowledgeVectorRebuildResult {

    /** 知识库 ID。 */
    private String kbId;

    /** 知识库名称。 */
    private String kbName;

    /** 待重建分片数量。 */
    private Integer chunkCount;

    /** 是否已进入异步任务队列。 */
    private Boolean asyncAccepted;

    /** 异步任务 ID。 */
    private String asyncTaskId;

    /** 提示消息。 */
    private String message;

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public Boolean getAsyncAccepted() {
        return asyncAccepted;
    }

    public void setAsyncAccepted(Boolean asyncAccepted) {
        this.asyncAccepted = asyncAccepted;
    }

    public String getAsyncTaskId() {
        return asyncTaskId;
    }

    public void setAsyncTaskId(String asyncTaskId) {
        this.asyncTaskId = asyncTaskId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
