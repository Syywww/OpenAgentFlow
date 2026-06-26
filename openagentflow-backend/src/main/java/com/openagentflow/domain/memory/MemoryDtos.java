package com.openagentflow.domain.memory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Memory 记忆中心数据传输对象集合。
 */
public class MemoryDtos {

    /**
     * 记忆中心概览。
     */
    public static class Overview {
        /** 当前可见记忆总数。 */
        private long totalCount;
        /** 短期会话记忆数量。 */
        private long shortTermCount;
        /** 长期记忆数量。 */
        private long longTermCount;
        /** 任务记忆数量。 */
        private long taskCount;
        /** 向量记忆数量。 */
        private long vectorCount;
        /** 已过期记忆数量。 */
        private long expiredCount;
        /** 待同步向量数量。 */
        private long pendingSyncCount;

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }

        public long getShortTermCount() {
            return shortTermCount;
        }

        public void setShortTermCount(long shortTermCount) {
            this.shortTermCount = shortTermCount;
        }

        public long getLongTermCount() {
            return longTermCount;
        }

        public void setLongTermCount(long longTermCount) {
            this.longTermCount = longTermCount;
        }

        public long getTaskCount() {
            return taskCount;
        }

        public void setTaskCount(long taskCount) {
            this.taskCount = taskCount;
        }

        public long getVectorCount() {
            return vectorCount;
        }

        public void setVectorCount(long vectorCount) {
            this.vectorCount = vectorCount;
        }

        public long getExpiredCount() {
            return expiredCount;
        }

        public void setExpiredCount(long expiredCount) {
            this.expiredCount = expiredCount;
        }

        public long getPendingSyncCount() {
            return pendingSyncCount;
        }

        public void setPendingSyncCount(long pendingSyncCount) {
            this.pendingSyncCount = pendingSyncCount;
        }
    }

    /**
     * 记忆摘要。
     */
    public static class Summary {
        /** 记忆 ID。 */
        private String id;
        /** Agent ID。 */
        private String agentId;
        /** Agent 名称。 */
        private String agentName;
        /** 用户 ID。 */
        private String userId;
        /** 会话 ID。 */
        private String sessionId;
        /** 记忆类型。 */
        private String memoryType;
        /** 记忆密钥。 */
        private String memoryKey;
        /** 记忆文本。 */
        private String memoryText;
        /** 结构化记忆值 JSON。 */
        private String memoryValue;
        /** 同步状态。 */
        private String syncStatus;
        /** 重要度得分。 */
        private BigDecimal importanceScore;
        /** 过期时间。 */
        private LocalDateTime expiredAt;
        /** 记忆状态。 */
        private String status;
        /** 可见范围。 */
        private String privacyScope;
        /** 来源运行 ID。 */
        private String sourceRunId;
        /** 标签 JSON。 */
        private String tagsJson;
        /** 命中次数。 */
        private Integer hitCount;
        /** 最后命中时间。 */
        private LocalDateTime lastAccessedAt;
        /** 创建时间。 */
        private LocalDateTime createdAt;
        /** 更新时间。 */
        private LocalDateTime updatedAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getAgentName() {
            return agentName;
        }

        public void setAgentName(String agentName) {
            this.agentName = agentName;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getMemoryType() {
            return memoryType;
        }

        public void setMemoryType(String memoryType) {
            this.memoryType = memoryType;
        }

        public String getMemoryKey() {
            return memoryKey;
        }

        public void setMemoryKey(String memoryKey) {
            this.memoryKey = memoryKey;
        }

        public String getMemoryText() {
            return memoryText;
        }

        public void setMemoryText(String memoryText) {
            this.memoryText = memoryText;
        }

        public String getMemoryValue() {
            return memoryValue;
        }

        public void setMemoryValue(String memoryValue) {
            this.memoryValue = memoryValue;
        }

        public String getSyncStatus() {
            return syncStatus;
        }

        public void setSyncStatus(String syncStatus) {
            this.syncStatus = syncStatus;
        }

        public BigDecimal getImportanceScore() {
            return importanceScore;
        }

        public void setImportanceScore(BigDecimal importanceScore) {
            this.importanceScore = importanceScore;
        }

        public LocalDateTime getExpiredAt() {
            return expiredAt;
        }

        public void setExpiredAt(LocalDateTime expiredAt) {
            this.expiredAt = expiredAt;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getPrivacyScope() {
            return privacyScope;
        }

        public void setPrivacyScope(String privacyScope) {
            this.privacyScope = privacyScope;
        }

        public String getSourceRunId() {
            return sourceRunId;
        }

        public void setSourceRunId(String sourceRunId) {
            this.sourceRunId = sourceRunId;
        }

        public String getTagsJson() {
            return tagsJson;
        }

        public void setTagsJson(String tagsJson) {
            this.tagsJson = tagsJson;
        }

        public Integer getHitCount() {
            return hitCount;
        }

        public void setHitCount(Integer hitCount) {
            this.hitCount = hitCount;
        }

        public LocalDateTime getLastAccessedAt() {
            return lastAccessedAt;
        }

        public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
            this.lastAccessedAt = lastAccessedAt;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    /**
     * 记忆保存请求。
     */
    public static class SaveRequest {
        /** Agent ID。 */
        private String agentId;
        /** 会话 ID。 */
        private String sessionId;
        /** 记忆类型。 */
        private String memoryType;
        /** 记忆密钥。 */
        private String memoryKey;
        /** 记忆文本。 */
        private String memoryText;
        /** 结构化记忆值 JSON。 */
        private String memoryValue;
        /** 重要度得分。 */
        private BigDecimal importanceScore;
        /** 过期时间。 */
        private LocalDateTime expiredAt;
        /** 记忆状态。 */
        private String status;
        /** 可见范围。 */
        private String privacyScope;
        /** 标签 JSON。 */
        private String tagsJson;

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getMemoryType() {
            return memoryType;
        }

        public void setMemoryType(String memoryType) {
            this.memoryType = memoryType;
        }

        public String getMemoryKey() {
            return memoryKey;
        }

        public void setMemoryKey(String memoryKey) {
            this.memoryKey = memoryKey;
        }

        public String getMemoryText() {
            return memoryText;
        }

        public void setMemoryText(String memoryText) {
            this.memoryText = memoryText;
        }

        public String getMemoryValue() {
            return memoryValue;
        }

        public void setMemoryValue(String memoryValue) {
            this.memoryValue = memoryValue;
        }

        public BigDecimal getImportanceScore() {
            return importanceScore;
        }

        public void setImportanceScore(BigDecimal importanceScore) {
            this.importanceScore = importanceScore;
        }

        public LocalDateTime getExpiredAt() {
            return expiredAt;
        }

        public void setExpiredAt(LocalDateTime expiredAt) {
            this.expiredAt = expiredAt;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getPrivacyScope() {
            return privacyScope;
        }

        public void setPrivacyScope(String privacyScope) {
            this.privacyScope = privacyScope;
        }

        public String getTagsJson() {
            return tagsJson;
        }

        public void setTagsJson(String tagsJson) {
            this.tagsJson = tagsJson;
        }
    }

    /**
     * 记忆召回请求。
     */
    public static class RecallRequest {
        /** Agent ID。 */
        private String agentId;
        /** 会话 ID。 */
        private String sessionId;
        /** 查询文本。 */
        private String query;
        /** 返回条数。 */
        private Integer limit;

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }
    }

    /**
     * 记忆召回结果。
     */
    public static class RecallItem {
        /** 记忆 ID。 */
        private String id;
        /** Agent ID。 */
        private String agentId;
        /** Agent 名称。 */
        private String agentName;
        /** 记忆类型。 */
        private String memoryType;
        /** 记忆文本。 */
        private String memoryText;
        /** 召回得分。 */
        private double score;
        /** 重要度得分。 */
        private BigDecimal importanceScore;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getAgentName() {
            return agentName;
        }

        public void setAgentName(String agentName) {
            this.agentName = agentName;
        }

        public String getMemoryType() {
            return memoryType;
        }

        public void setMemoryType(String memoryType) {
            this.memoryType = memoryType;
        }

        public String getMemoryText() {
            return memoryText;
        }

        public void setMemoryText(String memoryText) {
            this.memoryText = memoryText;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public BigDecimal getImportanceScore() {
            return importanceScore;
        }

        public void setImportanceScore(BigDecimal importanceScore) {
            this.importanceScore = importanceScore;
        }
    }

    /**
     * 记忆清理结果。
     */
    public static class CleanupResult {
        /** 归档的过期记忆数量。 */
        private int archivedExpiredCount;
        /** 删除的低价值记忆数量。 */
        private int deletedLowValueCount;
        /** 处理提示。 */
        private List<String> messages = new ArrayList<>();

        public int getArchivedExpiredCount() {
            return archivedExpiredCount;
        }

        public void setArchivedExpiredCount(int archivedExpiredCount) {
            this.archivedExpiredCount = archivedExpiredCount;
        }

        public int getDeletedLowValueCount() {
            return deletedLowValueCount;
        }

        public void setDeletedLowValueCount(int deletedLowValueCount) {
            this.deletedLowValueCount = deletedLowValueCount;
        }

        public List<String> getMessages() {
            return messages;
        }

        public void setMessages(List<String> messages) {
            this.messages = messages;
        }
    }
}
