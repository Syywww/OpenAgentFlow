package com.openagentflow.domain.delivery;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 交付验收中心 DTO 集合。
 */
public final class DeliveryAcceptanceDtos {

    private DeliveryAcceptanceDtos() {
    }

    /**
     * 交付验收总览。
     */
    public static class Overview {

        /** 当前交付状态：ready、warning、failed。 */
        private String overallStatus;

        /** 交付评分，范围 0 到 100。 */
        private BigDecimal score;

        /** 通过项数量。 */
        private Integer passedCount;

        /** 警告项数量。 */
        private Integer warningCount;

        /** 失败项数量。 */
        private Integer failedCount;

        /** 核心模块数量。 */
        private Integer moduleCount;

        /** 环境组件数量。 */
        private Integer componentCount;

        /** 最近报告时间。 */
        private LocalDateTime latestReportAt;

        /** 当前报告编码。 */
        private String latestReportCode;

        /** 关键指标。 */
        private Map<String, Object> metrics;

        /** 交付清单。 */
        private Manifest manifest;

        public String getOverallStatus() {
            return overallStatus;
        }

        public void setOverallStatus(String overallStatus) {
            this.overallStatus = overallStatus;
        }

        public BigDecimal getScore() {
            return score;
        }

        public void setScore(BigDecimal score) {
            this.score = score;
        }

        public Integer getPassedCount() {
            return passedCount;
        }

        public void setPassedCount(Integer passedCount) {
            this.passedCount = passedCount;
        }

        public Integer getWarningCount() {
            return warningCount;
        }

        public void setWarningCount(Integer warningCount) {
            this.warningCount = warningCount;
        }

        public Integer getFailedCount() {
            return failedCount;
        }

        public void setFailedCount(Integer failedCount) {
            this.failedCount = failedCount;
        }

        public Integer getModuleCount() {
            return moduleCount;
        }

        public void setModuleCount(Integer moduleCount) {
            this.moduleCount = moduleCount;
        }

        public Integer getComponentCount() {
            return componentCount;
        }

        public void setComponentCount(Integer componentCount) {
            this.componentCount = componentCount;
        }

        public LocalDateTime getLatestReportAt() {
            return latestReportAt;
        }

        public void setLatestReportAt(LocalDateTime latestReportAt) {
            this.latestReportAt = latestReportAt;
        }

        public String getLatestReportCode() {
            return latestReportCode;
        }

        public void setLatestReportCode(String latestReportCode) {
            this.latestReportCode = latestReportCode;
        }

        public Map<String, Object> getMetrics() {
            return metrics;
        }

        public void setMetrics(Map<String, Object> metrics) {
            this.metrics = metrics;
        }

        public Manifest getManifest() {
            return manifest;
        }

        public void setManifest(Manifest manifest) {
            this.manifest = manifest;
        }
    }

    /**
     * 验收检查项。
     */
    public static class CheckItem {

        /** 检查项编码。 */
        private String checkCode;

        /** 检查项名称。 */
        private String checkName;

        /** 检查分类。 */
        private String category;

        /** 状态：passed、warning、failed。 */
        private String status;

        /** 检查说明。 */
        private String message;

        /** 修复建议。 */
        private String suggestion;

        /** 是否阻断交付。 */
        private Boolean blocking;

        /** 当前值。 */
        private Object actualValue;

        /** 期望值。 */
        private Object expectedValue;

        /** 详情。 */
        private Map<String, Object> detail;

        public String getCheckCode() {
            return checkCode;
        }

        public void setCheckCode(String checkCode) {
            this.checkCode = checkCode;
        }

        public String getCheckName() {
            return checkName;
        }

        public void setCheckName(String checkName) {
            this.checkName = checkName;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }

        public Boolean getBlocking() {
            return blocking;
        }

        public void setBlocking(Boolean blocking) {
            this.blocking = blocking;
        }

        public Object getActualValue() {
            return actualValue;
        }

        public void setActualValue(Object actualValue) {
            this.actualValue = actualValue;
        }

        public Object getExpectedValue() {
            return expectedValue;
        }

        public void setExpectedValue(Object expectedValue) {
            this.expectedValue = expectedValue;
        }

        public Map<String, Object> getDetail() {
            return detail;
        }

        public void setDetail(Map<String, Object> detail) {
            this.detail = detail;
        }
    }

    /**
     * 风险提示。
     */
    public static class RiskItem {

        /** 风险级别：high、medium、low。 */
        private String riskLevel;

        /** 风险标题。 */
        private String title;

        /** 风险说明。 */
        private String description;

        /** 处置建议。 */
        private String suggestion;

        /** 来源检查项编码。 */
        private String sourceCheckCode;

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }

        public String getSourceCheckCode() {
            return sourceCheckCode;
        }

        public void setSourceCheckCode(String sourceCheckCode) {
            this.sourceCheckCode = sourceCheckCode;
        }
    }

    /**
     * 交付清单。
     */
    public static class Manifest {

        /** 应用名称。 */
        private String appName;

        /** 后端版本。 */
        private String backendVersion;

        /** 前端版本。 */
        private String frontendVersion;

        /** Java 版本。 */
        private String javaVersion;

        /** 数据库名称。 */
        private String databaseName;

        /** 最新 SQL 脚本。 */
        private String latestSqlVersion;

        /** 数据组件。 */
        private Map<String, Object> dataComponents;

        /** 模块数量。 */
        private Map<String, Object> moduleCounts;

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public String getBackendVersion() {
            return backendVersion;
        }

        public void setBackendVersion(String backendVersion) {
            this.backendVersion = backendVersion;
        }

        public String getFrontendVersion() {
            return frontendVersion;
        }

        public void setFrontendVersion(String frontendVersion) {
            this.frontendVersion = frontendVersion;
        }

        public String getJavaVersion() {
            return javaVersion;
        }

        public void setJavaVersion(String javaVersion) {
            this.javaVersion = javaVersion;
        }

        public String getDatabaseName() {
            return databaseName;
        }

        public void setDatabaseName(String databaseName) {
            this.databaseName = databaseName;
        }

        public String getLatestSqlVersion() {
            return latestSqlVersion;
        }

        public void setLatestSqlVersion(String latestSqlVersion) {
            this.latestSqlVersion = latestSqlVersion;
        }

        public Map<String, Object> getDataComponents() {
            return dataComponents;
        }

        public void setDataComponents(Map<String, Object> dataComponents) {
            this.dataComponents = dataComponents;
        }

        public Map<String, Object> getModuleCounts() {
            return moduleCounts;
        }

        public void setModuleCounts(Map<String, Object> moduleCounts) {
            this.moduleCounts = moduleCounts;
        }
    }

    /**
     * 交付报告摘要。
     */
    public static class ReportSummary {

        /** 报告 ID。 */
        private String id;

        /** 报告编码。 */
        private String reportCode;

        /** 报告名称。 */
        private String reportName;

        /** 总体状态。 */
        private String overallStatus;

        /** 交付评分。 */
        private BigDecimal score;

        /** 通过项数量。 */
        private Integer passedCount;

        /** 警告项数量。 */
        private Integer warningCount;

        /** 失败项数量。 */
        private Integer failedCount;

        /** 创建时间。 */
        private LocalDateTime createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getReportCode() {
            return reportCode;
        }

        public void setReportCode(String reportCode) {
            this.reportCode = reportCode;
        }

        public String getReportName() {
            return reportName;
        }

        public void setReportName(String reportName) {
            this.reportName = reportName;
        }

        public String getOverallStatus() {
            return overallStatus;
        }

        public void setOverallStatus(String overallStatus) {
            this.overallStatus = overallStatus;
        }

        public BigDecimal getScore() {
            return score;
        }

        public void setScore(BigDecimal score) {
            this.score = score;
        }

        public Integer getPassedCount() {
            return passedCount;
        }

        public void setPassedCount(Integer passedCount) {
            this.passedCount = passedCount;
        }

        public Integer getWarningCount() {
            return warningCount;
        }

        public void setWarningCount(Integer warningCount) {
            this.warningCount = warningCount;
        }

        public Integer getFailedCount() {
            return failedCount;
        }

        public void setFailedCount(Integer failedCount) {
            this.failedCount = failedCount;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * 交付报告详情。
     */
    public static class ReportDetail extends ReportSummary {

        /** 总览快照。 */
        private Overview overview;

        /** 检查项快照。 */
        private List<CheckItem> checks;

        /** 风险快照。 */
        private List<RiskItem> risks;

        /** 清单快照。 */
        private Manifest manifest;

        public Overview getOverview() {
            return overview;
        }

        public void setOverview(Overview overview) {
            this.overview = overview;
        }

        public List<CheckItem> getChecks() {
            return checks;
        }

        public void setChecks(List<CheckItem> checks) {
            this.checks = checks;
        }

        public List<RiskItem> getRisks() {
            return risks;
        }

        public void setRisks(List<RiskItem> risks) {
            this.risks = risks;
        }

        public Manifest getManifest() {
            return manifest;
        }

        public void setManifest(Manifest manifest) {
            this.manifest = manifest;
        }
    }
}
