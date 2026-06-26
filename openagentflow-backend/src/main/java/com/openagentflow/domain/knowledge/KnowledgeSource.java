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

    /** 高亮后的引用文本，命中的关键词会用 mark 标签包裹。 */
    private String highlightedQuoteText;

    /** 相似度得分。 */
    private Double score;

    /** 向量相似度得分。 */
    private Double vectorScore;

    /** 关键词命中得分。 */
    private Double keywordScore;

    /** 重排后的最终得分。 */
    private Double rerankScore;

    /** 命中原因说明。 */
    private String matchReason;

    /** 重排原因说明，用于展示分数如何形成。 */
    private String rankReason;

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

    public String getHighlightedQuoteText() {
        return highlightedQuoteText;
    }

    public void setHighlightedQuoteText(String highlightedQuoteText) {
        this.highlightedQuoteText = highlightedQuoteText;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getVectorScore() {
        return vectorScore;
    }

    public void setVectorScore(Double vectorScore) {
        this.vectorScore = vectorScore;
    }

    public Double getKeywordScore() {
        return keywordScore;
    }

    public void setKeywordScore(Double keywordScore) {
        this.keywordScore = keywordScore;
    }

    public Double getRerankScore() {
        return rerankScore;
    }

    public void setRerankScore(Double rerankScore) {
        this.rerankScore = rerankScore;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }

    public String getRankReason() {
        return rankReason;
    }

    public void setRankReason(String rankReason) {
        this.rankReason = rankReason;
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
