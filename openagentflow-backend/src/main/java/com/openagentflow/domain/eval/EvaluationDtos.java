package com.openagentflow.domain.eval;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 模型评测模块 DTO 集合。
 */
public final class EvaluationDtos {

    private EvaluationDtos() {
    }

    /**
     * 评测数据集保存请求。
     */
    public static class DatasetRequest {

        /** 数据集编码，不填时后端自动生成。 */
        private String datasetCode;

        /** 数据集名称。 */
        @NotBlank(message = "评测集名称不能为空")
        private String datasetName;

        /** 数据集描述。 */
        private String description;

        /** 业务领域。 */
        private String domain;

        /** 标签 JSON 数组或普通标签文本。 */
        private String tags;

        /** 可见性：private 或 public。 */
        private String visibility;

        /** 数据集状态。 */
        private String status;

        public String getDatasetCode() {
            return datasetCode;
        }

        public void setDatasetCode(String datasetCode) {
            this.datasetCode = datasetCode;
        }

        public String getDatasetName() {
            return datasetName;
        }

        public void setDatasetName(String datasetName) {
            this.datasetName = datasetName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }

        public String getVisibility() {
            return visibility;
        }

        public void setVisibility(String visibility) {
            this.visibility = visibility;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * 评测数据集摘要。
     */
    public static class DatasetSummary {

        /** 数据集 ID。 */
        private String id;

        /** 数据集编码。 */
        private String datasetCode;

        /** 数据集名称。 */
        private String datasetName;

        /** 数据集描述。 */
        private String description;

        /** 业务领域。 */
        private String domain;

        /** 标签 JSON。 */
        private String tags;

        /** 可见性。 */
        private String visibility;

        /** 状态。 */
        private String status;

        /** 样本数量。 */
        private Integer sampleCount;

        /** 任务数量。 */
        private Integer taskCount;

        /** 所有者用户 ID。 */
        private String ownerUserId;

        /** 当前用户是否可管理。 */
        private Boolean canManage;

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

        public String getDatasetCode() {
            return datasetCode;
        }

        public void setDatasetCode(String datasetCode) {
            this.datasetCode = datasetCode;
        }

        public String getDatasetName() {
            return datasetName;
        }

        public void setDatasetName(String datasetName) {
            this.datasetName = datasetName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }

        public String getVisibility() {
            return visibility;
        }

        public void setVisibility(String visibility) {
            this.visibility = visibility;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getSampleCount() {
            return sampleCount;
        }

        public void setSampleCount(Integer sampleCount) {
            this.sampleCount = sampleCount;
        }

        public Integer getTaskCount() {
            return taskCount;
        }

        public void setTaskCount(Integer taskCount) {
            this.taskCount = taskCount;
        }

        public String getOwnerUserId() {
            return ownerUserId;
        }

        public void setOwnerUserId(String ownerUserId) {
            this.ownerUserId = ownerUserId;
        }

        public Boolean getCanManage() {
            return canManage;
        }

        public void setCanManage(Boolean canManage) {
            this.canManage = canManage;
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
     * 评测数据集详情。
     */
    public static class DatasetDetail extends DatasetSummary {

        /** 样本列表。 */
        private List<SampleSummary> samples;

        /** 最近任务列表。 */
        private List<TaskSummary> recentTasks;

        public List<SampleSummary> getSamples() {
            return samples;
        }

        public void setSamples(List<SampleSummary> samples) {
            this.samples = samples;
        }

        public List<TaskSummary> getRecentTasks() {
            return recentTasks;
        }

        public void setRecentTasks(List<TaskSummary> recentTasks) {
            this.recentTasks = recentTasks;
        }
    }

    /**
     * 样本导入请求。
     */
    public static class SampleImportRequest {

        /** 是否替换已有样本。 */
        private Boolean replaceExisting;

        /** 样本列表。 */
        private List<SampleRequest> samples;

        public Boolean getReplaceExisting() {
            return replaceExisting;
        }

        public void setReplaceExisting(Boolean replaceExisting) {
            this.replaceExisting = replaceExisting;
        }

        public List<SampleRequest> getSamples() {
            return samples;
        }

        public void setSamples(List<SampleRequest> samples) {
            this.samples = samples;
        }
    }

    /**
     * 样本保存请求。
     */
    public static class SampleRequest {

        /** 样本序号，不填时按导入顺序自动生成。 */
        private Integer sampleNo;

        /** 问题文本。 */
        @NotBlank(message = "样本问题不能为空")
        private String question;

        /** 标准答案。 */
        private String expectedAnswer;

        /** 参考上下文。 */
        private String referenceContext;

        /** 评分点 JSON 数组或换行文本。 */
        private String scoringPoints;

        /** 元数据 JSON。 */
        private String metadata;

        /** 样本状态。 */
        private String status;

        public Integer getSampleNo() {
            return sampleNo;
        }

        public void setSampleNo(Integer sampleNo) {
            this.sampleNo = sampleNo;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getExpectedAnswer() {
            return expectedAnswer;
        }

        public void setExpectedAnswer(String expectedAnswer) {
            this.expectedAnswer = expectedAnswer;
        }

        public String getReferenceContext() {
            return referenceContext;
        }

        public void setReferenceContext(String referenceContext) {
            this.referenceContext = referenceContext;
        }

        public String getScoringPoints() {
            return scoringPoints;
        }

        public void setScoringPoints(String scoringPoints) {
            this.scoringPoints = scoringPoints;
        }

        public String getMetadata() {
            return metadata;
        }

        public void setMetadata(String metadata) {
            this.metadata = metadata;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * 样本摘要。
     */
    public static class SampleSummary {

        /** 样本 ID。 */
        private String id;

        /** 数据集 ID。 */
        private String datasetId;

        /** 样本序号。 */
        private Integer sampleNo;

        /** 问题文本。 */
        private String question;

        /** 标准答案。 */
        private String expectedAnswer;

        /** 参考上下文。 */
        private String referenceContext;

        /** 评分点 JSON。 */
        private String scoringPoints;

        /** 元数据 JSON。 */
        private String metadata;

        /** 状态。 */
        private String status;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDatasetId() {
            return datasetId;
        }

        public void setDatasetId(String datasetId) {
            this.datasetId = datasetId;
        }

        public Integer getSampleNo() {
            return sampleNo;
        }

        public void setSampleNo(Integer sampleNo) {
            this.sampleNo = sampleNo;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getExpectedAnswer() {
            return expectedAnswer;
        }

        public void setExpectedAnswer(String expectedAnswer) {
            this.expectedAnswer = expectedAnswer;
        }

        public String getReferenceContext() {
            return referenceContext;
        }

        public void setReferenceContext(String referenceContext) {
            this.referenceContext = referenceContext;
        }

        public String getScoringPoints() {
            return scoringPoints;
        }

        public void setScoringPoints(String scoringPoints) {
            this.scoringPoints = scoringPoints;
        }

        public String getMetadata() {
            return metadata;
        }

        public void setMetadata(String metadata) {
            this.metadata = metadata;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * 创建并运行评测任务请求。
     */
    public static class RunTaskRequest {

        /** 任务名称。 */
        @NotBlank(message = "评测任务名称不能为空")
        private String taskName;

        /** 数据集 ID。 */
        @NotBlank(message = "评测集不能为空")
        private String datasetId;

        /** Agent ID；与工作流ID至少填写一个。 */
        private String agentId;

        /** 工作流ID；填写后评测真实工作流输出。 */
        private String workflowId;

        /** 基线模型 ID。 */
        private String baselineModelId;

        /** 对比模型 ID 列表。 */
        private List<String> compareModelIds;

        /** Prompt 策略名称。 */
        private String promptStrategy;

        /** 评测或Judge使用的Prompt模板ID。 */
        private String promptTemplateId;

        /** 锁定的Prompt版本ID。 */
        private String promptVersionId;

        /** Prompt绑定模式。 */
        private String promptBindingMode;

        /** Prompt 补充文本，会拼接进评测输入用于 A/B 对比。 */
        private String promptVariantText;

        /** 知识库切片策略名称，仅用于对比维度记录。 */
        private String knowledgeStrategy;

        /** 温度参数。 */
        private Double temperature;

        /** 最大输出 Token。 */
        private Integer maxTokens;

        /** 最大样本数，避免误跑过大数据集。 */
        private Integer maxSamples;

        /** 是否启用 LLM-as-Judge，默认启用。 */
        private Boolean judgeEnabled;

        /** Judge 模型 ID，不填时优先复用当前评测模型。 */
        private String judgeModelId;

        /** 自定义 Judge Prompt，不填时使用内置 JSON 打分模板。 */
        private String judgePrompt;

        /** 额外评测配置。 */
        private Map<String, Object> evalConfig;

        public String getTaskName() {
            return taskName;
        }

        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }

        public String getDatasetId() {
            return datasetId;
        }

        public void setDatasetId(String datasetId) {
            this.datasetId = datasetId;
        }

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getWorkflowId() {
            return workflowId;
        }

        public void setWorkflowId(String workflowId) {
            this.workflowId = workflowId;
        }

        public String getBaselineModelId() {
            return baselineModelId;
        }

        public void setBaselineModelId(String baselineModelId) {
            this.baselineModelId = baselineModelId;
        }

        public List<String> getCompareModelIds() {
            return compareModelIds;
        }

        public void setCompareModelIds(List<String> compareModelIds) {
            this.compareModelIds = compareModelIds;
        }

        public String getPromptStrategy() {
            return promptStrategy;
        }

        public void setPromptStrategy(String promptStrategy) {
            this.promptStrategy = promptStrategy;
        }

        public String getPromptTemplateId() { return promptTemplateId; }
        public void setPromptTemplateId(String promptTemplateId) { this.promptTemplateId = promptTemplateId; }
        public String getPromptVersionId() { return promptVersionId; }
        public void setPromptVersionId(String promptVersionId) { this.promptVersionId = promptVersionId; }
        public String getPromptBindingMode() { return promptBindingMode; }
        public void setPromptBindingMode(String promptBindingMode) { this.promptBindingMode = promptBindingMode; }

        public String getPromptVariantText() {
            return promptVariantText;
        }

        public void setPromptVariantText(String promptVariantText) {
            this.promptVariantText = promptVariantText;
        }

        public String getKnowledgeStrategy() {
            return knowledgeStrategy;
        }

        public void setKnowledgeStrategy(String knowledgeStrategy) {
            this.knowledgeStrategy = knowledgeStrategy;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public Integer getMaxSamples() {
            return maxSamples;
        }

        public void setMaxSamples(Integer maxSamples) {
            this.maxSamples = maxSamples;
        }

        public Boolean getJudgeEnabled() {
            return judgeEnabled;
        }

        public void setJudgeEnabled(Boolean judgeEnabled) {
            this.judgeEnabled = judgeEnabled;
        }

        public String getJudgeModelId() {
            return judgeModelId;
        }

        public void setJudgeModelId(String judgeModelId) {
            this.judgeModelId = judgeModelId;
        }

        public String getJudgePrompt() {
            return judgePrompt;
        }

        public void setJudgePrompt(String judgePrompt) {
            this.judgePrompt = judgePrompt;
        }

        public Map<String, Object> getEvalConfig() {
            return evalConfig;
        }

        public void setEvalConfig(Map<String, Object> evalConfig) {
            this.evalConfig = evalConfig;
        }
    }

    /**
     * 评测任务摘要。
     */
    public static class TaskSummary {

        /** 任务 ID。 */
        private String id;

        /** 任务编码。 */
        private String taskCode;

        /** 任务名称。 */
        private String taskName;

        /** 数据集 ID。 */
        private String datasetId;

        /** 数据集名称。 */
        private String datasetName;

        /** Agent ID。 */
        private String agentId;

        /** Agent 名称。 */
        private String agentName;

        /** 工作流 ID。 */
        private String workflowId;

        /** 工作流名称。 */
        private String workflowName;

        /** 基线模型 ID。 */
        private String baselineModelId;

        /** 基线模型名称。 */
        private String baselineModelName;

        /** 对比模型 ID JSON。 */
        private String compareModelIds;

        /** 评测配置 JSON。 */
        private String evalConfig;

        /** 任务状态。 */
        private String status;

        /** 样本总数。 */
        private Integer totalSamples;

        /** 已完成样本数。 */
        private Integer finishedSamples;

        /** 综合得分。 */
        private BigDecimal overallScore;

        /** 成功率。 */
        private BigDecimal successRate;

        /** 总 Token。 */
        private Integer totalTokens;

        /** 平均耗时。 */
        private Integer averageLatencyMs;

        /** 创建时间。 */
        private LocalDateTime createdAt;

        /** 开始时间。 */
        private LocalDateTime startedAt;

        /** 完成时间。 */
        private LocalDateTime finishedAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTaskCode() {
            return taskCode;
        }

        public void setTaskCode(String taskCode) {
            this.taskCode = taskCode;
        }

        public String getTaskName() {
            return taskName;
        }

        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }

        public String getDatasetId() {
            return datasetId;
        }

        public void setDatasetId(String datasetId) {
            this.datasetId = datasetId;
        }

        public String getDatasetName() {
            return datasetName;
        }

        public void setDatasetName(String datasetName) {
            this.datasetName = datasetName;
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

        public String getWorkflowId() {
            return workflowId;
        }

        public void setWorkflowId(String workflowId) {
            this.workflowId = workflowId;
        }

        public String getWorkflowName() {
            return workflowName;
        }

        public void setWorkflowName(String workflowName) {
            this.workflowName = workflowName;
        }

        public String getBaselineModelId() {
            return baselineModelId;
        }

        public void setBaselineModelId(String baselineModelId) {
            this.baselineModelId = baselineModelId;
        }

        public String getBaselineModelName() {
            return baselineModelName;
        }

        public void setBaselineModelName(String baselineModelName) {
            this.baselineModelName = baselineModelName;
        }

        public String getCompareModelIds() {
            return compareModelIds;
        }

        public void setCompareModelIds(String compareModelIds) {
            this.compareModelIds = compareModelIds;
        }

        public String getEvalConfig() {
            return evalConfig;
        }

        public void setEvalConfig(String evalConfig) {
            this.evalConfig = evalConfig;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getTotalSamples() {
            return totalSamples;
        }

        public void setTotalSamples(Integer totalSamples) {
            this.totalSamples = totalSamples;
        }

        public Integer getFinishedSamples() {
            return finishedSamples;
        }

        public void setFinishedSamples(Integer finishedSamples) {
            this.finishedSamples = finishedSamples;
        }

        public BigDecimal getOverallScore() {
            return overallScore;
        }

        public void setOverallScore(BigDecimal overallScore) {
            this.overallScore = overallScore;
        }

        public BigDecimal getSuccessRate() {
            return successRate;
        }

        public void setSuccessRate(BigDecimal successRate) {
            this.successRate = successRate;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }

        public Integer getAverageLatencyMs() {
            return averageLatencyMs;
        }

        public void setAverageLatencyMs(Integer averageLatencyMs) {
            this.averageLatencyMs = averageLatencyMs;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
        }

        public LocalDateTime getFinishedAt() {
            return finishedAt;
        }

        public void setFinishedAt(LocalDateTime finishedAt) {
            this.finishedAt = finishedAt;
        }
    }

    /**
     * 评测任务详情。
     */
    public static class TaskDetail extends TaskSummary {

        /** 指标汇总。 */
        private Map<String, Object> summary;

        /** 模型维度对比。 */
        private List<Map<String, Object>> modelCompare;

        /** 样本运行结果。 */
        private List<TaskRunSummary> runs;

        public Map<String, Object> getSummary() {
            return summary;
        }

        public void setSummary(Map<String, Object> summary) {
            this.summary = summary;
        }

        public List<Map<String, Object>> getModelCompare() {
            return modelCompare;
        }

        public void setModelCompare(List<Map<String, Object>> modelCompare) {
            this.modelCompare = modelCompare;
        }

        public List<TaskRunSummary> getRuns() {
            return runs;
        }

        public void setRuns(List<TaskRunSummary> runs) {
            this.runs = runs;
        }
    }

    /**
     * 样本运行摘要。
     */
    public static class TaskRunSummary {

        /** 样本运行 ID。 */
        private String id;

        /** 评测任务 ID，用于前端把样本运行归属到具体评测任务。 */
        private String taskId;

        /** 样本 ID。 */
        private String sampleId;

        /** 样本序号。 */
        private Integer sampleNo;

        /** 问题文本。 */
        private String question;

        /** 标准答案。 */
        private String expectedAnswer;

        /** 模型 ID。 */
        private String modelId;

        /** 模型名称。 */
        private String modelName;

        /** Runtime Run ID，可跳转 Trace。 */
        private String runId;

        /** 模型回答。 */
        private String answerText;

        /** 运行状态。 */
        private String status;

        /** 耗时毫秒。 */
        private Integer latencyMs;

        /** Token 数量。 */
        private Integer tokenCount;

        /** 错误信息。 */
        private String errorMessage;

        /** 指标得分列表。 */
        private List<ScoreSummary> scores;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getSampleId() {
            return sampleId;
        }

        public void setSampleId(String sampleId) {
            this.sampleId = sampleId;
        }

        public Integer getSampleNo() {
            return sampleNo;
        }

        public void setSampleNo(Integer sampleNo) {
            this.sampleNo = sampleNo;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getExpectedAnswer() {
            return expectedAnswer;
        }

        public void setExpectedAnswer(String expectedAnswer) {
            this.expectedAnswer = expectedAnswer;
        }

        public String getModelId() {
            return modelId;
        }

        public void setModelId(String modelId) {
            this.modelId = modelId;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public String getRunId() {
            return runId;
        }

        public void setRunId(String runId) {
            this.runId = runId;
        }

        public String getAnswerText() {
            return answerText;
        }

        public void setAnswerText(String answerText) {
            this.answerText = answerText;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Integer latencyMs) {
            this.latencyMs = latencyMs;
        }

        public Integer getTokenCount() {
            return tokenCount;
        }

        public void setTokenCount(Integer tokenCount) {
            this.tokenCount = tokenCount;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public List<ScoreSummary> getScores() {
            return scores;
        }

        public void setScores(List<ScoreSummary> scores) {
            this.scores = scores;
        }
    }

    /**
     * 指标得分摘要。
     */
    public static class ScoreSummary {

        /** 指标编码。 */
        private String metricCode;

        /** 指标名称。 */
        private String metricName;

        /** 指标得分。 */
        private BigDecimal score;

        /** 是否通过。 */
        private Boolean passed;

        /** 裁判类型。 */
        private String judgeType;

        /** 裁判详情 JSON。 */
        private String judgeDetail;

        /** 指标 ID，用于定位评分记录对应的评测指标。 */
        private String metricId;

        public String getMetricId() {
            return metricId;
        }

        public void setMetricId(String metricId) {
            this.metricId = metricId;
        }

        public String getMetricCode() {
            return metricCode;
        }

        public void setMetricCode(String metricCode) {
            this.metricCode = metricCode;
        }

        public String getMetricName() {
            return metricName;
        }

        public void setMetricName(String metricName) {
            this.metricName = metricName;
        }

        public BigDecimal getScore() {
            return score;
        }

        public void setScore(BigDecimal score) {
            this.score = score;
        }

        public Boolean getPassed() {
            return passed;
        }

        public void setPassed(Boolean passed) {
            this.passed = passed;
        }

        public String getJudgeType() {
            return judgeType;
        }

        public void setJudgeType(String judgeType) {
            this.judgeType = judgeType;
        }

        public String getJudgeDetail() {
            return judgeDetail;
        }

        public void setJudgeDetail(String judgeDetail) {
            this.judgeDetail = judgeDetail;
        }
    }
}
