package com.openagentflow.domain.knowledge;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识文档摘要对象。
 */
public class KnowledgeDocumentSummary {

    /** 文档主键 ID。 */
    private String id;

    /** 知识库 ID。 */
    private String kbId;

    /** 文档名称。 */
    private String docName;

    /** 文档类型。 */
    private String docType;

    /** 文件扩展名。 */
    private String fileExt;

    /** 文件大小。 */
    private Long fileSize;

    /** 文件哈希。 */
    private String fileHash;

    /** 解析状态：processing、parsed、failed。 */
    private String parseStatus;

    /** 解析或处理失败原因。 */
    private String parseError;

    /** 当前处理阶段编码。 */
    private String processStage;

    /** 当前处理阶段中文名称。 */
    private String processStageLabel;

    /** 处理进度百分比。 */
    private Integer progressPercent;

    /** 最近一条处理提示。 */
    private String lastMessage;

    /** 异步任务ID。 */
    private String asyncTaskId;

    /** 是否使用本地兜底向量。 */
    private Boolean embeddingFallbackUsed;

    /** Embedding 接口类型，例如 multimodal。 */
    private String embeddingApi;

    /** Embedding 模型编码，豆包场景为接入点 ID。 */
    private String embeddingModelCode;

    /** Embedding 模型展示名称。 */
    private String embeddingModelName;

    /** 向量维度。 */
    private Integer embeddingDimension;

    /** Milvus 是否已全部同步成功。 */
    private Boolean milvusSynced;

    /** 文档处理日志。 */
    private List<String> processLogs;

    /** 分片数量。 */
    private Integer chunkCount;

    /** 向量数量。 */
    private Integer embeddingCount;

    /** 上传时间。 */
    private LocalDateTime uploadedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getFileExt() {
        return fileExt;
    }

    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getParseError() {
        return parseError;
    }

    public void setParseError(String parseError) {
        this.parseError = parseError;
    }

    public String getProcessStage() {
        return processStage;
    }

    public void setProcessStage(String processStage) {
        this.processStage = processStage;
    }

    public String getProcessStageLabel() {
        return processStageLabel;
    }

    public void setProcessStageLabel(String processStageLabel) {
        this.processStageLabel = processStageLabel;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getAsyncTaskId() {
        return asyncTaskId;
    }

    public void setAsyncTaskId(String asyncTaskId) {
        this.asyncTaskId = asyncTaskId;
    }

    public Boolean getEmbeddingFallbackUsed() {
        return embeddingFallbackUsed;
    }

    public void setEmbeddingFallbackUsed(Boolean embeddingFallbackUsed) {
        this.embeddingFallbackUsed = embeddingFallbackUsed;
    }

    public String getEmbeddingApi() {
        return embeddingApi;
    }

    public void setEmbeddingApi(String embeddingApi) {
        this.embeddingApi = embeddingApi;
    }

    public String getEmbeddingModelCode() {
        return embeddingModelCode;
    }

    public void setEmbeddingModelCode(String embeddingModelCode) {
        this.embeddingModelCode = embeddingModelCode;
    }

    public String getEmbeddingModelName() {
        return embeddingModelName;
    }

    public void setEmbeddingModelName(String embeddingModelName) {
        this.embeddingModelName = embeddingModelName;
    }

    public Integer getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(Integer embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }

    public Boolean getMilvusSynced() {
        return milvusSynced;
    }

    public void setMilvusSynced(Boolean milvusSynced) {
        this.milvusSynced = milvusSynced;
    }

    public List<String> getProcessLogs() {
        return processLogs;
    }

    public void setProcessLogs(List<String> processLogs) {
        this.processLogs = processLogs;
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

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
