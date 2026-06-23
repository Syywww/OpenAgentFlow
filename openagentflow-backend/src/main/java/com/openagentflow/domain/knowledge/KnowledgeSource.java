package com.openagentflow.domain.knowledge;

/**
 * RAG 引用来源对象。
 */
public class KnowledgeSource {

    /** 知识库 ID。 */
    private String kbId;

    /** 知识库名称。 */
    private String kbName;

    /** 文档 ID。 */
    private String documentId;

    /** 文档名称。 */
    private String documentName;

    /** 分片 ID。 */
    private String chunkId;

    /** 分片序号。 */
    private Integer chunkNo;

    /** 引用文本。 */
    private String quoteText;

    /** 相似度得分。 */
    private Double score;

    /** 页码。 */
    private Integer pageNo;

    /** 检索日志 ID。 */
    private String retrievalLogId;

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

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public Integer getChunkNo() {
        return chunkNo;
    }

    public void setChunkNo(Integer chunkNo) {
        this.chunkNo = chunkNo;
    }

    public String getQuoteText() {
        return quoteText;
    }

    public void setQuoteText(String quoteText) {
        this.quoteText = quoteText;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public String getRetrievalLogId() {
        return retrievalLogId;
    }

    public void setRetrievalLogId(String retrievalLogId) {
        this.retrievalLogId = retrievalLogId;
    }
}
