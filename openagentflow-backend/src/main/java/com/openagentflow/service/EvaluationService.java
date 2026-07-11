package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.chat.ChatCompletionRequest;
import com.openagentflow.domain.chat.ChatCompletionResponse;
import com.openagentflow.domain.chat.ChatMessage;
import com.openagentflow.domain.chat.ChatRunContext;
import com.openagentflow.domain.chat.LlmCallResult;
import com.openagentflow.domain.eval.EvaluationDtos;
import com.openagentflow.domain.knowledge.KnowledgeSource;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.entity.EvalDatasetEntity;
import com.openagentflow.entity.EvalMetricEntity;
import com.openagentflow.entity.EvalReportEntity;
import com.openagentflow.entity.EvalSampleEntity;
import com.openagentflow.entity.EvalScoreEntity;
import com.openagentflow.entity.EvalTaskEntity;
import com.openagentflow.entity.EvalTaskRunEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelProviderEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.EvalDatasetMapper;
import com.openagentflow.mapper.EvalMetricMapper;
import com.openagentflow.mapper.EvalReportMapper;
import com.openagentflow.mapper.EvalSampleMapper;
import com.openagentflow.mapper.EvalScoreMapper;
import com.openagentflow.mapper.EvalTaskMapper;
import com.openagentflow.mapper.EvalTaskRunMapper;
import com.openagentflow.mapper.ModelConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 模型评测业务服务。
 *
 * <p>负责评测集 CRUD、样本导入、批量调用 Agent、指标评分、结果聚合和 Trace 关联。</p>
 */
@Service
public class EvaluationService implements DistributedTaskHandler {

    /** 默认单次评测最多执行样本数，避免误操作一次性消耗过多模型额度。 */
    private static final int DEFAULT_MAX_SAMPLES = 50;

    /** 规则评分通过阈值。 */
    private static final BigDecimal PASS_SCORE = BigDecimal.valueOf(60);

    /** 评测集 Mapper。 */
    private final EvalDatasetMapper evalDatasetMapper;

    /** 评测样本 Mapper。 */
    private final EvalSampleMapper evalSampleMapper;

    /** 评测指标 Mapper。 */
    private final EvalMetricMapper evalMetricMapper;

    /** 评测任务 Mapper。 */
    private final EvalTaskMapper evalTaskMapper;

    /** 评测运行 Mapper。 */
    private final EvalTaskRunMapper evalTaskRunMapper;

    /** 评测得分 Mapper。 */
    private final EvalScoreMapper evalScoreMapper;

    /** 评测报告 Mapper。 */
    private final EvalReportMapper evalReportMapper;

    /** Agent Mapper，用于校验 Agent 和补充展示名称。 */
    private final AgentMapper agentMapper;

    /** 模型配置 Mapper，用于选择和展示模型。 */
    private final ModelConfigMapper modelConfigMapper;

    /** Agent 资源权限服务，复用已有资源级权限规则。 */
    private final AgentAccessService agentAccessService;

    /** Agent 运行服务，评测时通过它调用真实 Agent 并写入运行 Trace。 */
    private final AgentService agentService;

    /** 模型服务，用于加载 Judge 模型、服务商和 API Key。 */
    private final ModelProviderService modelProviderService;

    /** OpenAI-compatible 客户端，用于真实执行 LLM-as-Judge 评分。 */
    private final OpenAiCompatibleClient openAiCompatibleClient;

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    /** 统一异步任务中心。 */
    private final AsyncTaskService asyncTaskService;

    /** Kafka 任务工具类。 */
    private final KafkaTaskClient kafkaTaskClient;

    public EvaluationService(EvalDatasetMapper evalDatasetMapper,
                             EvalSampleMapper evalSampleMapper,
                             EvalMetricMapper evalMetricMapper,
                             EvalTaskMapper evalTaskMapper,
                             EvalTaskRunMapper evalTaskRunMapper,
                             EvalScoreMapper evalScoreMapper,
                             EvalReportMapper evalReportMapper,
                             AgentMapper agentMapper,
                             ModelConfigMapper modelConfigMapper,
                             AgentAccessService agentAccessService,
                             AgentService agentService,
                             ModelProviderService modelProviderService,
                             OpenAiCompatibleClient openAiCompatibleClient,
                             ObjectMapper objectMapper,
                             AsyncTaskService asyncTaskService,
                             KafkaTaskClient kafkaTaskClient) {
        this.evalDatasetMapper = evalDatasetMapper;
        this.evalSampleMapper = evalSampleMapper;
        this.evalMetricMapper = evalMetricMapper;
        this.evalTaskMapper = evalTaskMapper;
        this.evalTaskRunMapper = evalTaskRunMapper;
        this.evalScoreMapper = evalScoreMapper;
        this.evalReportMapper = evalReportMapper;
        this.agentMapper = agentMapper;
        this.modelConfigMapper = modelConfigMapper;
        this.agentAccessService = agentAccessService;
        this.agentService = agentService;
        this.modelProviderService = modelProviderService;
        this.openAiCompatibleClient = openAiCompatibleClient;
        this.objectMapper = objectMapper;
        this.asyncTaskService = asyncTaskService;
        this.kafkaTaskClient = kafkaTaskClient;
    }

    /**
     * 查询当前用户可见的评测集列表。
     *
     * @return 评测集摘要列表
     */
    public List<EvaluationDtos.DatasetSummary> listDatasets() {
        return evalDatasetMapper.selectList(new LambdaQueryWrapper<EvalDatasetEntity>()
                        .isNull(EvalDatasetEntity::getDeletedAt)
                        .orderByDesc(EvalDatasetEntity::getUpdatedAt)
                        .last("limit 200"))
                .stream()
                .filter(this::canViewDataset)
                .map(this::toDatasetSummary)
                .toList();
    }

    /**
     * 获取评测集详情，包含样本和最近任务。
     *
     * @param id 评测集 ID
     * @return 评测集详情
     */
    public EvaluationDtos.DatasetDetail getDataset(String id) {
        EvalDatasetEntity entity = requireDataset(id);
        assertCanViewDataset(entity);
        EvaluationDtos.DatasetDetail detail = new EvaluationDtos.DatasetDetail();
        fillDatasetSummary(detail, entity);
        detail.setSamples(listSamples(entity.getId()).stream().map(this::toSampleSummary).toList());
        detail.setRecentTasks(evalTaskMapper.selectList(new LambdaQueryWrapper<EvalTaskEntity>()
                        .eq(EvalTaskEntity::getDatasetId, entity.getId())
                        .orderByDesc(EvalTaskEntity::getCreatedAt)
                        .last("limit 10"))
                .stream()
                .map(this::toTaskSummary)
                .toList());
        return detail;
    }

    /**
     * 创建评测集。
     *
     * @param request 保存请求
     * @return 创建后的评测集详情
     */
    @Transactional(rollbackFor = Exception.class)
    public EvaluationDtos.DatasetDetail createDataset(EvaluationDtos.DatasetRequest request) {
        String userId = currentUserIdOrThrow();
        EvalDatasetEntity entity = new EvalDatasetEntity();
        entity.setId(newId());
        entity.setDatasetCode(StringUtils.hasText(request.getDatasetCode())
                ? request.getDatasetCode().trim()
                : uniqueDatasetCode("eval_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())));
        fillDataset(entity, request);
        entity.setOwnerUserId(userId);
        entity.setCreatedBy(userId);
        entity.setVersion(0L);
        evalDatasetMapper.insert(entity);
        return getDataset(entity.getId());
    }

    /**
     * 更新评测集。
     *
     * @param id 评测集 ID
     * @param request 保存请求
     * @return 更新后的评测集详情
     */
    @Transactional(rollbackFor = Exception.class)
    public EvaluationDtos.DatasetDetail updateDataset(String id, EvaluationDtos.DatasetRequest request) {
        EvalDatasetEntity entity = requireDataset(id);
        assertCanManageDataset(entity);
        fillDataset(entity, request);
        evalDatasetMapper.updateById(entity);
        return getDataset(entity.getId());
    }

    /**
     * 软删除评测集。
     *
     * @param id 评测集 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDataset(String id) {
        EvalDatasetEntity entity = requireDataset(id);
        assertCanManageDataset(entity);
        entity.setStatus("deleted");
        entity.setDeletedAt(LocalDateTime.now());
        evalDatasetMapper.updateById(entity);
    }

    /**
     * 导入评测样本。
     *
     * @param datasetId 评测集 ID
     * @param request 导入请求
     * @return 导入后的评测集详情
     */
    @Transactional(rollbackFor = Exception.class)
    public EvaluationDtos.DatasetDetail importSamples(String datasetId, EvaluationDtos.SampleImportRequest request) {
        EvalDatasetEntity dataset = requireDataset(datasetId);
        assertCanManageDataset(dataset);
        if (request.getSamples() == null || request.getSamples().isEmpty()) {
            throw new BusinessException("EVAL_SAMPLE_EMPTY", "导入样本不能为空");
        }
        if (Boolean.TRUE.equals(request.getReplaceExisting())) {
            // 评测样本表没有 deleted_at 字段，这里按评测集维度物理替换。
            evalSampleMapper.delete(new LambdaQueryWrapper<EvalSampleEntity>()
                    .eq(EvalSampleEntity::getDatasetId, dataset.getId()));
        }
        int nextNo = nextSampleNo(dataset.getId());
        for (EvaluationDtos.SampleRequest sampleRequest : request.getSamples()) {
            EvalSampleEntity sample = new EvalSampleEntity();
            sample.setId(newId());
            sample.setDatasetId(dataset.getId());
            sample.setSampleNo(sampleRequest.getSampleNo() != null ? sampleRequest.getSampleNo() : nextNo++);
            sample.setQuestion(sampleRequest.getQuestion());
            sample.setExpectedAnswer(sampleRequest.getExpectedAnswer());
            sample.setReferenceContext(sampleRequest.getReferenceContext());
            sample.setScoringPoints(normalizeJsonArrayText(sampleRequest.getScoringPoints()));
            sample.setMetadata(normalizeJsonObjectText(sampleRequest.getMetadata()));
            sample.setStatus(StringUtils.hasText(sampleRequest.getStatus()) ? sampleRequest.getStatus() : "enabled");
            evalSampleMapper.insert(sample);
        }
        return getDataset(dataset.getId());
    }

    /**
     * 创建评测任务并投递 Kafka 异步执行。
     *
     * @param request 运行请求
     * @return 任务详情
     */
    public EvaluationDtos.TaskDetail runTask(EvaluationDtos.RunTaskRequest request) {
        ensureDefaultMetrics();
        EvalDatasetEntity dataset = requireDataset(request.getDatasetId());
        assertCanViewDataset(dataset);
        AgentEntity agent = requireAgent(request.getAgentId());
        agentAccessService.assertCanView(agent);

        List<EvalSampleEntity> samples = listRunnableSamples(dataset.getId(), request.getMaxSamples());
        if (samples.isEmpty()) {
            throw new BusinessException("EVAL_SAMPLE_EMPTY", "当前评测集没有可运行样本");
        }
        List<String> modelIds = resolveModelIds(request, agent);
        if (modelIds.isEmpty()) {
            throw new BusinessException("EVAL_MODEL_EMPTY", "请先为 Agent 绑定模型或选择基线模型");
        }

        EvalTaskEntity task = createTaskEntity(request, samples.size() * modelIds.size());
        evalTaskMapper.insert(task);
        AsyncTaskEntity asyncTask = asyncTaskService.createTask(
                "批量评测：" + task.getTaskName(),
                "EVALUATION_RUN",
                "eval_task",
                task.getId(),
                "eval_task",
                task.getId(),
                null,
                Map.of("evalTaskId", task.getId(), "datasetId", task.getDatasetId(), "totalRuns", task.getTotalSamples()));
        try {
            kafkaTaskClient.publish(asyncTask);
        } catch (Exception exception) {
            asyncTaskService.appendLog(asyncTask.getId(), "warn", "enqueue_failed",
                    "Kafka 首次投递失败，补偿调度器将自动重试", Map.of("error", exception.getMessage()), 0);
        }
        return getTask(task.getId());
    }

    /**
     * 返回 Kafka 任务类型。
     */
    @Override
    public String taskType() {
        return "EVALUATION_RUN";
    }

    /**
     * 在 Kafka Worker 中批量执行评测样本。
     */
    @Override
    public Map<String, Object> executeDistributedTask(AsyncTaskEntity asyncTask) {
        EvalTaskEntity task = requireTask(asyncTask.getSourceId());
        EvaluationDtos.RunTaskRequest request = restoreRunTaskRequest(task);
        try {
            if (value(asyncTask.getRetryCount()) > 0) {
                // 自动重试前清理上次不完整结果，确保任务重放后结果唯一。
                jdbcDeleteEvaluationRuns(task.getId());
            }
            task.setStatus("running");
            task.setStartedAt(LocalDateTime.now());
            task.setFinishedAt(null);
            task.setFinishedSamples(0);
            evalTaskMapper.updateById(task);

            List<EvalSampleEntity> samples = listRunnableSamples(task.getDatasetId(), request.getMaxSamples());
            AgentEntity agent = requireAgent(task.getAgentId());
            List<String> modelIds = resolveModelIds(request, agent);
            int total = samples.size() * modelIds.size();
            int finished = 0;
            for (EvalSampleEntity sample : samples) {
                for (String modelId : modelIds) {
                    if (asyncTaskService.isCancelRequested(asyncTask.getId())) {
                        task.setStatus("canceled");
                        task.setFinishedAt(LocalDateTime.now());
                        evalTaskMapper.updateById(task);
                        asyncTaskService.markCanceled(asyncTask.getId(), "用户取消批量评测任务");
                        return Map.of("canceled", true, "finishedRuns", finished, "totalRuns", total);
                    }
                    runSingleSample(task, sample, modelId, request);
                    finished++;
                    task.setFinishedSamples(finished);
                    evalTaskMapper.updateById(task);
                    int progress = total == 0 ? 100 : Math.min(99, (int) (finished * 100D / total));
                    asyncTaskService.updateProgress(asyncTask.getId(), "evaluating",
                            "已完成评测 " + finished + " / " + total, progress,
                            Map.of("finishedRuns", finished, "totalRuns", total));
                }
            }
            task.setStatus("success");
            task.setFinishedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            evalTaskMapper.updateById(task);
            upsertReport(task);
            return Map.of("evalTaskId", task.getId(), "finishedRuns", finished, "totalRuns", total);
        } catch (Exception exception) {
            task.setStatus("failed");
            task.setFinishedAt(LocalDateTime.now());
            evalTaskMapper.updateById(task);
            throw new IllegalStateException("批量评测执行失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 查询评测任务列表。
     *
     * @return 任务摘要列表
     */
    public List<EvaluationDtos.TaskSummary> listTasks() {
        return evalTaskMapper.selectList(new LambdaQueryWrapper<EvalTaskEntity>()
                        .orderByDesc(EvalTaskEntity::getCreatedAt)
                        .last("limit 100"))
                .stream()
                .filter(this::canViewTask)
                .map(this::toTaskSummary)
                .toList();
    }

    /**
     * 获取评测任务详情。
     *
     * @param id 任务 ID
     * @return 任务详情
     */
    public EvaluationDtos.TaskDetail getTask(String id) {
        EvalTaskEntity task = requireTask(id);
        assertCanViewTask(task);
        EvaluationDtos.TaskDetail detail = new EvaluationDtos.TaskDetail();
        fillTaskSummary(detail, task);
        detail.setRuns(listRuns(task.getId()).stream().map(this::toTaskRunSummary).toList());
        detail.setSummary(buildTaskSummaryMap(task));
        detail.setModelCompare(buildModelCompare(task));
        return detail;
    }

    /**
     * 运行单条样本并保存评分。
     *
     * @param task 评测任务
     * @param sample 评测样本
     * @param modelId 模型 ID
     * @param request 运行请求
     */
    private void runSingleSample(EvalTaskEntity task,
                                 EvalSampleEntity sample,
                                 String modelId,
                                 EvaluationDtos.RunTaskRequest request) {
        EvalTaskRunEntity run = new EvalTaskRunEntity();
        run.setId(newId());
        run.setTaskId(task.getId());
        run.setSampleId(sample.getId());
        run.setModelId(modelId);
        run.setStatus("running");
        evalTaskRunMapper.insert(run);

        try {
            ChatCompletionRequest chatRequest = new ChatCompletionRequest();
            chatRequest.setAgentId(task.getAgentId());
            chatRequest.setModelId(modelId);
            chatRequest.setInput(buildEvalInput(sample, request));
            chatRequest.setTemperature(request.getTemperature());
            chatRequest.setMaxTokens(request.getMaxTokens());
            ChatCompletionResponse response = agentService.runAgent(task.getAgentId(), chatRequest);

            // AgentService 已经写入 runtime_run / runtime_trace_step，这里只保存 runId 作为评测追溯入口。
            run.setRunId(response.getRunId());
            run.setAnswerText(response.getContent());
            run.setLatencyMs(response.getLatencyMs());
            run.setTokenCount(response.getTotalTokens());
            run.setStatus("success".equalsIgnoreCase(response.getStatus()) ? "success" : "failed");
            run.setErrorMessage(response.getErrorMessage());
            evalTaskRunMapper.updateById(run);
            int judgeTokens = saveScores(run, sample, response, request);
            if (judgeTokens > 0) {
                run.setTokenCount((run.getTokenCount() == null ? 0 : run.getTokenCount()) + judgeTokens);
                run.setUpdatedAt(LocalDateTime.now());
                evalTaskRunMapper.updateById(run);
            }
        } catch (Exception ex) {
            // 单条样本失败不阻断整个批次，失败原因会进入评测结果和 Trace 列表。
            run.setStatus("failed");
            run.setErrorMessage(ex.getMessage());
            run.setUpdatedAt(LocalDateTime.now());
            evalTaskRunMapper.updateById(run);
            saveFailedScores(run, ex.getMessage());
        }
    }

    /**
     * 保存规则评分结果。
     *
     * @param run 样本运行记录
     * @param sample 评测样本
     * @param response Agent 响应
     */
    private int saveScores(EvalTaskRunEntity run,
                           EvalSampleEntity sample,
                           ChatCompletionResponse response,
                           EvaluationDtos.RunTaskRequest request) {
        Map<String, EvalMetricEntity> metrics = metricsByCode();
        String answer = response.getContent() == null ? "" : response.getContent();
        List<String> points = scoringPoints(sample);
        double accuracy = coverage(answer, points);
        double relevance = relevance(answer, sample);
        double completeness = completeness(answer, points, sample.getExpectedAnswer());
        double citation = citationCorrectness(answer, sample, response.getSources());
        double toolSuccess = toolSuccessRate(response.getToolResults());
        double hallucinationControl = hallucinationControl(answer, sample, response.getSources(), accuracy, citation);
        JudgeResult judge = judgeEnabled(request) ? runLlmJudge(run, sample, response, request) : JudgeResult.disabled();

        insertScore(run.getId(), metrics.get("llm_judge_overall"), judge.scoreOr("overallScore", overallRuleScore(accuracy, relevance, completeness, hallucinationControl)), judge.detailWithFallback(
                "metric", "overallScore",
                "fallbackScore", overallRuleScore(accuracy, relevance, completeness, hallucinationControl)));
        insertScore(run.getId(), metrics.get("accuracy"), judge.scoreOr("accuracy", accuracy), judge.detailWithFallback(
                "metric", "accuracy",
                "scoringPoints", points,
                "expectedAnswer", sample.getExpectedAnswer()));
        insertScore(run.getId(), metrics.get("relevance"), judge.scoreOr("relevance", relevance), judge.detailWithFallback(
                "metric", "relevance",
                "question", sample.getQuestion(),
                "referenceContext", sample.getReferenceContext()));
        insertScore(run.getId(), metrics.get("completeness"), judge.scoreOr("completeness", completeness), judge.detailWithFallback(
                "metric", "completeness",
                "expectedAnswer", sample.getExpectedAnswer(),
                "coveredPoints", coveredPoints(answer, points)));
        insertScore(run.getId(), metrics.get("hallucination_control"), judge.scoreOr("hallucinationControl", hallucinationControl), judge.detailWithFallback(
                "metric", "hallucinationControl",
                "referenceAvailable", StringUtils.hasText(sample.getReferenceContext()),
                "citationCount", response.getSources() == null ? 0 : response.getSources().size()));
        insertScore(run.getId(), metrics.get("citation_correctness"), citation, mapOf(
                "sourceCount", response.getSources() == null ? 0 : response.getSources().size()));
        insertScore(run.getId(), metrics.get("tool_success"), toolSuccess, mapOf(
                "toolCallCount", response.getToolResults() == null ? 0 : response.getToolResults().size()));
        return judge.totalTokens();
    }

    /**
     * 保存失败样本的兜底评分。
     *
     * @param run 样本运行记录
     * @param errorMessage 错误消息
     */
    private void saveFailedScores(EvalTaskRunEntity run, String errorMessage) {
        Map<String, EvalMetricEntity> metrics = metricsByCode();
        for (EvalMetricEntity metric : metrics.values()) {
            insertScore(run.getId(), metric, 0, mapOf("errorMessage", errorMessage));
        }
    }

    /**
     * 写入一条指标得分。
     *
     * @param taskRunId 样本运行 ID
     * @param metric 指标
     * @param score 分数
     * @param detail 评分明细
     */
    private void insertScore(String taskRunId, EvalMetricEntity metric, double score, Map<String, Object> detail) {
        if (metric == null) {
            return;
        }
        EvalScoreEntity entity = new EvalScoreEntity();
        entity.setId(newId());
        entity.setTaskRunId(taskRunId);
        entity.setMetricId(metric.getId());
        entity.setScore(score(score));
        entity.setPassed(entity.getScore().compareTo(PASS_SCORE) >= 0);
        entity.setJudgeType(String.valueOf(detail.getOrDefault("judgeType", "rule")));
        entity.setJudgeDetail(toJson(detail));
        entity.setJudgedBy(String.valueOf(detail.getOrDefault("judgedBy", "system-rule")));
        evalScoreMapper.insert(entity);
    }

    /**
     * 调用真实模型执行 LLM-as-Judge 打分。
     *
     * @param run 样本运行结果
     * @param sample 评测样本
     * @param response Agent 回答
     * @param request 评测任务请求
     * @return Judge 结果，失败时返回可兜底对象
     */
    private JudgeResult runLlmJudge(EvalTaskRunEntity run,
                                    EvalSampleEntity sample,
                                    ChatCompletionResponse response,
                                    EvaluationDtos.RunTaskRequest request) {
        String judgeModelId = resolveJudgeModelId(request, run.getModelId());
        if (!StringUtils.hasText(judgeModelId)) {
            return JudgeResult.failed("未找到可用 Judge 模型", "", "", 0, 0, "");
        }
        try {
            ModelConfigEntity model = modelProviderService.requireModel(judgeModelId);
            ModelProviderEntity provider = modelProviderService.requireProviderByModel(model);
            ChatRunContext context = new ChatRunContext();
            context.setModel(model);
            context.setProvider(provider);
            context.setApiKey(modelProviderService.findApiKeyValue(provider.getId()));
            context.setMessages(List.of(
                    new ChatMessage("system", judgeSystemPrompt(request)),
                    new ChatMessage("user", judgeUserPrompt(sample, response))
            ));
            LlmCallResult result = openAiCompatibleClient.complete(context, 0D, 900);
            JudgeResult judge = parseJudgeResult(result.getContent(), model);
            judge.setLatencyMs(result.getLatencyMs() == null ? 0 : result.getLatencyMs());
            judge.setTotalTokens(result.getTotalTokens() == null ? 0 : result.getTotalTokens());
            judge.setRawOutput(result.getContent());
            return judge;
        } catch (Exception exception) {
            return JudgeResult.failed(exception.getMessage(), judgeModelId, modelName(judgeModelId), 0, 0, "");
        }
    }

    /**
     * 解析 Judge 模型返回的 JSON，兼容模型包裹 markdown 代码块的情况。
     *
     * @param raw 模型原始输出
     * @param model Judge 模型
     * @return Judge 结构化结果
     */
    private JudgeResult parseJudgeResult(String raw, ModelConfigEntity model) {
        String json = extractJsonObject(raw);
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, Object> detail = new LinkedHashMap<>();
        try {
            JsonNode node = objectMapper.readTree(json);
            scores.put("overallScore", clampJudgeScore(node.path("overallScore").asDouble(node.path("overall").asDouble(0))));
            scores.put("accuracy", clampJudgeScore(node.path("accuracy").asDouble(0)));
            scores.put("relevance", clampJudgeScore(node.path("relevance").asDouble(0)));
            scores.put("completeness", clampJudgeScore(node.path("completeness").asDouble(0)));
            scores.put("hallucinationControl", clampJudgeScore(node.path("hallucinationControl").asDouble(node.path("faithfulness").asDouble(0))));
            detail.put("reason", node.path("reason").asText(""));
            detail.put("strengths", node.path("strengths").isMissingNode() ? List.of() : node.path("strengths"));
            detail.put("risks", node.path("risks").isMissingNode() ? List.of() : node.path("risks"));
            detail.put("dimensions", node);
        } catch (Exception exception) {
            return JudgeResult.failed("Judge 返回不是合法 JSON：" + exception.getMessage(),
                    model.getId(), model.getModelName(), 0, 0, raw);
        }
        return JudgeResult.success(model.getId(), model.getModelName(), scores, detail);
    }

    /**
     * 组装 Judge 系统提示词，要求模型只输出 JSON。
     *
     * @param request 评测任务请求
     * @return 系统提示词
     */
    private String judgeSystemPrompt(EvaluationDtos.RunTaskRequest request) {
        if (StringUtils.hasText(request.getJudgePrompt())) {
            return request.getJudgePrompt();
        }
        return """
                你是严格、稳定、可复核的 AI 评测裁判。请根据用户问题、标准答案、评分点、参考上下文、模型回答、引用来源和工具调用结果进行打分。
                输出必须是一个 JSON 对象，不要输出 markdown，不要输出解释性前后缀。
                字段要求：
                overallScore: 0到100的综合分。
                accuracy: 0到100，答案是否符合标准答案和评分点。
                relevance: 0到100，答案是否紧扣问题和参考上下文。
                completeness: 0到100，答案是否完整覆盖关键要点。
                hallucinationControl: 0到100，100表示没有幻觉，0表示严重编造。
                reason: 简短中文评分理由。
                strengths: 字符串数组，列出优点。
                risks: 字符串数组，列出风险或扣分原因。
                """;
    }

    /**
     * 组装 Judge 用户输入，提供足够上下文让裁判模型做稳定评分。
     *
     * @param sample 评测样本
     * @param response Agent 回答
     * @return 用户消息
     */
    private String judgeUserPrompt(EvalSampleEntity sample, ChatCompletionResponse response) {
        return """
                【用户问题】
                %s

                【标准答案】
                %s

                【评分点】
                %s

                【参考上下文】
                %s

                【模型回答】
                %s

                【引用来源】
                %s

                【工具调用结果】
                %s
                """.formatted(
                nullToBlank(sample.getQuestion()),
                nullToBlank(sample.getExpectedAnswer()),
                toJson(scoringPoints(sample)),
                nullToBlank(sample.getReferenceContext()),
                nullToBlank(response.getContent()),
                sourceSummary(response.getSources()),
                toJson(response.getToolResults() == null ? List.of() : response.getToolResults()));
    }

    /**
     * 选择 Judge 模型，不显式配置时复用当前被评测模型。
     *
     * @param request 评测请求
     * @param runModelId 当前样本运行模型
     * @return Judge 模型 ID
     */
    private String resolveJudgeModelId(EvaluationDtos.RunTaskRequest request, String runModelId) {
        if (StringUtils.hasText(request.getJudgeModelId())) {
            return request.getJudgeModelId().trim();
        }
        if (StringUtils.hasText(runModelId)) {
            return runModelId;
        }
        AgentEntity agent = agentMapper.selectById(request.getAgentId());
        return agent == null ? "" : agent.getModelId();
    }

    /**
     * 判断本次评测是否启用 Judge。
     *
     * @param request 评测请求
     * @return 是否启用
     */
    private boolean judgeEnabled(EvaluationDtos.RunTaskRequest request) {
        return request.getJudgeEnabled() == null || Boolean.TRUE.equals(request.getJudgeEnabled());
    }

    /**
     * 将引用来源压缩成适合 Judge 阅读的文本。
     *
     * @param sources 引用来源
     * @return 引用摘要
     */
    private String sourceSummary(List<KnowledgeSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return "[]";
        }
        return toJson(sources.stream()
                .limit(6)
                .map(source -> mapOf(
                        "documentName", source.getDocumentName(),
                        "chunkNo", source.getChunkNo(),
                        "score", source.getScore(),
                        "quoteText", source.getQuoteText()))
                .toList());
    }

    /**
     * 从模型输出中抽取第一个 JSON 对象。
     *
     * @param raw 原始输出
     * @return JSON 文本
     */
    private String extractJsonObject(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "{}";
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw.trim();
    }

    /**
     * 限制 Judge 分数范围。
     *
     * @param value 原始分
     * @return 0 到 100 的分数
     */
    private double clampJudgeScore(double value) {
        return Math.max(0, Math.min(100, value));
    }

    /**
     * 规则评分综合分，用于 Judge 关闭或失败时兜底。
     *
     * @param accuracy 准确率
     * @param relevance 相关性
     * @param completeness 完整性
     * @param hallucinationControl 幻觉控制
     * @return 综合分
     */
    private double overallRuleScore(double accuracy, double relevance, double completeness, double hallucinationControl) {
        return (accuracy + relevance + completeness + hallucinationControl) / 4D;
    }

    /**
     * 创建任务实体。
     *
     * @param request 运行请求
     * @param totalRuns 总运行条数
     * @return 任务实体
     */
    private EvalTaskEntity createTaskEntity(EvaluationDtos.RunTaskRequest request, int totalRuns) {
        EvalTaskEntity task = new EvalTaskEntity();
        task.setId(newId());
        // 任务编码追加短随机后缀，避免多人或连续点击在同一秒内创建任务时撞唯一索引。
        task.setTaskCode("eval_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + "_" + UUID.randomUUID().toString().substring(0, 8));
        task.setTaskName(request.getTaskName());
        task.setDatasetId(request.getDatasetId());
        task.setAgentId(request.getAgentId());
        task.setBaselineModelId(request.getBaselineModelId());
        task.setCompareModelIds(toJson(request.getCompareModelIds() == null ? List.of() : request.getCompareModelIds()));
        task.setEvalConfig(toJson(evalConfigMap(request)));
        task.setStatus("pending");
        task.setTotalSamples(totalRuns);
        task.setFinishedSamples(0);
        task.setCreatedBy(currentUserIdOrThrow());
        task.setStartedAt(null);
        return task;
    }

    /**
     * 从评测任务快照恢复 Kafka Worker 所需的运行请求。
     */
    private EvaluationDtos.RunTaskRequest restoreRunTaskRequest(EvalTaskEntity task) {
        try {
            Map<String, Object> config = objectMapper.readValue(task.getEvalConfig(), new TypeReference<>() {
            });
            config.put("evalConfig", config.remove("extra"));
            EvaluationDtos.RunTaskRequest request = objectMapper.convertValue(config, EvaluationDtos.RunTaskRequest.class);
            request.setTaskName(task.getTaskName());
            request.setDatasetId(task.getDatasetId());
            request.setAgentId(task.getAgentId());
            request.setBaselineModelId(task.getBaselineModelId());
            request.setCompareModelIds(objectMapper.readValue(task.getCompareModelIds(), new TypeReference<>() {
            }));
            return request;
        } catch (Exception exception) {
            throw new IllegalStateException("评测任务配置快照解析失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 删除评测任务上一次未完整执行的运行与评分数据。
     */
    private void jdbcDeleteEvaluationRuns(String taskId) {
        List<String> runIds = evalTaskRunMapper.selectList(new LambdaQueryWrapper<EvalTaskRunEntity>()
                        .eq(EvalTaskRunEntity::getTaskId, taskId))
                .stream().map(EvalTaskRunEntity::getId).toList();
        if (!runIds.isEmpty()) {
            evalScoreMapper.delete(new LambdaQueryWrapper<EvalScoreEntity>()
                    .in(EvalScoreEntity::getTaskRunId, runIds));
        }
        evalTaskRunMapper.delete(new LambdaQueryWrapper<EvalTaskRunEntity>()
                .eq(EvalTaskRunEntity::getTaskId, taskId));
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 组装评测输入，Prompt 对比文本会作为额外评测指令拼接到问题前。
     *
     * @param sample 评测样本
     * @param request 运行请求
     * @return Agent 输入
     */
    private String buildEvalInput(EvalSampleEntity sample, EvaluationDtos.RunTaskRequest request) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(request.getPromptVariantText())) {
            builder.append("请按以下评测 Prompt 策略回答：\n")
                    .append(request.getPromptVariantText().trim())
                    .append("\n\n");
        }
        builder.append(sample.getQuestion());
        return builder.toString();
    }

    /**
     * 生成评测任务报告快照。
     *
     * @param task 评测任务
     */
    private void upsertReport(EvalTaskEntity task) {
        EvalReportEntity report = evalReportMapper.selectOne(new LambdaQueryWrapper<EvalReportEntity>()
                .eq(EvalReportEntity::getTaskId, task.getId())
                .last("limit 1"));
        if (report == null) {
            report = new EvalReportEntity();
            report.setId(newId());
            report.setTaskId(task.getId());
            report.setReportName(task.getTaskName() + " 报告");
            report.setCreatedBy(task.getCreatedBy());
        }
        report.setSummary(toJson(buildTaskSummaryMap(task)));
        report.setModelCompare(toJson(buildModelCompare(task)));
        if (report.getCreatedAt() == null) {
            evalReportMapper.insert(report);
        } else {
            evalReportMapper.updateById(report);
        }
    }

    /**
     * 构建任务整体指标汇总。
     *
     * @param task 评测任务
     * @return 汇总 Map
     */
    private Map<String, Object> buildTaskSummaryMap(EvalTaskEntity task) {
        List<EvalTaskRunEntity> runs = listRuns(task.getId());
        List<EvalScoreEntity> scores = listScores(runs);
        Map<String, BigDecimal> metricScores = averageScoresByMetric(scores);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("overallScore", overallScore(metricScores));
        summary.put("judgeScore", metricScores.getOrDefault("llm_judge_overall", BigDecimal.ZERO));
        summary.put("accuracy", metricScores.getOrDefault("accuracy", BigDecimal.ZERO));
        summary.put("relevance", metricScores.getOrDefault("relevance", BigDecimal.ZERO));
        summary.put("completeness", metricScores.getOrDefault("completeness", BigDecimal.ZERO));
        summary.put("hallucinationRate", BigDecimal.valueOf(100).subtract(metricScores.getOrDefault("hallucination_control", BigDecimal.ZERO)));
        summary.put("citationCorrectness", metricScores.getOrDefault("citation_correctness", BigDecimal.ZERO));
        summary.put("toolSuccessRate", metricScores.getOrDefault("tool_success", BigDecimal.ZERO));
        summary.put("successRate", successRate(runs));
        summary.put("averageLatencyMs", averageLatency(runs));
        summary.put("totalTokens", totalTokens(runs));
        summary.put("runCount", runs.size());
        return summary;
    }

    /**
     * 构建模型维度对比结果。
     *
     * @param task 评测任务
     * @return 模型对比列表
     */
    private List<Map<String, Object>> buildModelCompare(EvalTaskEntity task) {
        return listRuns(task.getId()).stream()
                .collect(Collectors.groupingBy(EvalTaskRunEntity::getModelId, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<EvalTaskRunEntity> runs = entry.getValue();
                    Map<String, BigDecimal> metricScores = averageScoresByMetric(listScores(runs));
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("modelId", entry.getKey());
                    row.put("modelName", modelName(entry.getKey()));
                    row.put("overallScore", overallScore(metricScores));
                    row.put("judgeScore", metricScores.getOrDefault("llm_judge_overall", BigDecimal.ZERO));
                    row.put("accuracy", metricScores.getOrDefault("accuracy", BigDecimal.ZERO));
                    row.put("relevance", metricScores.getOrDefault("relevance", BigDecimal.ZERO));
                    row.put("completeness", metricScores.getOrDefault("completeness", BigDecimal.ZERO));
                    row.put("hallucinationRate", BigDecimal.valueOf(100).subtract(metricScores.getOrDefault("hallucination_control", BigDecimal.ZERO)));
                    row.put("citationCorrectness", metricScores.getOrDefault("citation_correctness", BigDecimal.ZERO));
                    row.put("toolSuccessRate", metricScores.getOrDefault("tool_success", BigDecimal.ZERO));
                    row.put("averageLatencyMs", averageLatency(runs));
                    row.put("totalTokens", totalTokens(runs));
                    row.put("runCount", runs.size());
                    return row;
                })
                .toList();
    }

    /**
     * 计算整体分，优先使用效果类指标，幻觉率以控制分参与平均。
     *
     * @param metricScores 指标均分
     * @return 综合分
     */
    private BigDecimal overallScore(Map<String, BigDecimal> metricScores) {
        if (metricScores.containsKey("llm_judge_overall")) {
            return metricScores.get("llm_judge_overall");
        }
        List<String> keys = List.of("accuracy", "relevance", "completeness", "hallucination_control", "citation_correctness", "tool_success");
        List<BigDecimal> values = keys.stream()
                .map(metricScores::get)
                .filter(value -> value != null)
                .toList();
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * 根据样本和 Agent 默认模型解析本次要运行的模型列表。
     *
     * @param request 运行请求
     * @param agent Agent 实体
     * @return 去重后的模型 ID 列表
     */
    private List<String> resolveModelIds(EvaluationDtos.RunTaskRequest request, AgentEntity agent) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (StringUtils.hasText(request.getBaselineModelId())) {
            ids.add(request.getBaselineModelId());
        } else if (StringUtils.hasText(agent.getModelId())) {
            ids.add(agent.getModelId());
            request.setBaselineModelId(agent.getModelId());
        }
        if (request.getCompareModelIds() != null) {
            request.getCompareModelIds().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(ids::add);
        }
        return ids.stream().filter(this::modelExists).toList();
    }

    /**
     * 组装评测配置快照。
     *
     * @param request 运行请求
     * @return 配置 Map
     */
    private Map<String, Object> evalConfigMap(EvaluationDtos.RunTaskRequest request) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("promptStrategy", request.getPromptStrategy());
        config.put("promptVariantText", request.getPromptVariantText());
        config.put("knowledgeStrategy", request.getKnowledgeStrategy());
        config.put("temperature", request.getTemperature());
        config.put("maxTokens", request.getMaxTokens());
        config.put("maxSamples", request.getMaxSamples());
        config.put("judgeEnabled", judgeEnabled(request));
        config.put("judgeModelId", request.getJudgeModelId());
        config.put("judgePrompt", request.getJudgePrompt());
        config.put("extra", request.getEvalConfig() == null ? Map.of() : request.getEvalConfig());
        return config;
    }

    /**
     * 确保系统默认指标已存在。
     */
    private void ensureDefaultMetrics() {
        addMetricIfAbsent("accuracy", "准确率", "quality", "标准答案和评分点覆盖率");
        addMetricIfAbsent("relevance", "相关性", "quality", "回答与问题及参考上下文的相关程度");
        addMetricIfAbsent("completeness", "完整性", "quality", "回答是否覆盖关键评分点");
        addMetricIfAbsent("hallucination_control", "幻觉控制", "risk", "回答相对标准答案和引用来源的可信程度");
        addMetricIfAbsent("llm_judge_overall", "LLM Judge 综合分", "llm_as_judge", "裁判模型给出的综合质量分");
        addMetricIfAbsent("citation_correctness", "引用正确率", "rag", "回答是否具备可追溯知识库引用");
        addMetricIfAbsent("tool_success", "工具调用成功率", "tool", "工具调用是否成功完成");
    }

    /**
     * 不存在时新增指标。
     *
     * @param code 指标编码
     * @param name 指标名称
     * @param type 指标类型
     * @param description 指标说明
     */
    private void addMetricIfAbsent(String code, String name, String type, String description) {
        Long count = evalMetricMapper.selectCount(new LambdaQueryWrapper<EvalMetricEntity>()
                .eq(EvalMetricEntity::getMetricCode, code));
        if (count != null && count > 0) {
            return;
        }
        EvalMetricEntity metric = new EvalMetricEntity();
        metric.setId(newId());
        metric.setMetricCode(code);
        metric.setMetricName(name);
        metric.setMetricType(type);
        metric.setDescription(description);
        metric.setConfigJson("{}");
        metric.setEnabled(true);
        evalMetricMapper.insert(metric);
    }

    /**
     * 计算答案对评分点的覆盖率。
     *
     * @param answer 回答
     * @param points 评分点
     * @return 百分制分数
     */
    private double coverage(String answer, List<String> points) {
        if (!StringUtils.hasText(answer)) {
            return 0;
        }
        if (points.isEmpty()) {
            return 60;
        }
        long hit = points.stream().filter(point -> containsNormalized(answer, point)).count();
        return (hit * 100.0) / points.size();
    }

    /**
     * 计算相关性分数。
     *
     * @param answer 回答
     * @param sample 样本
     * @return 百分制分数
     */
    private double relevance(String answer, EvalSampleEntity sample) {
        if (!StringUtils.hasText(answer)) {
            return 0;
        }
        Set<String> anchors = keywords(sample.getQuestion() + " " + nullToBlank(sample.getReferenceContext()));
        if (anchors.isEmpty()) {
            return 70;
        }
        long hit = anchors.stream().filter(anchor -> containsNormalized(answer, anchor)).count();
        return Math.min(100, 40 + (hit * 60.0 / anchors.size()));
    }

    /**
     * 计算完整性分数。
     *
     * @param answer 回答
     * @param points 评分点
     * @param expectedAnswer 标准答案
     * @return 百分制分数
     */
    private double completeness(String answer, List<String> points, String expectedAnswer) {
        if (!StringUtils.hasText(answer)) {
            return 0;
        }
        if (!points.isEmpty()) {
            return coverage(answer, points);
        }
        return StringUtils.hasText(expectedAnswer) && containsNormalized(answer, expectedAnswer) ? 90 : 60;
    }

    /**
     * 计算引用正确率。
     *
     * @param answer 回答
     * @param sample 样本
     * @param sources 引用来源
     * @return 百分制分数
     */
    private double citationCorrectness(String answer, EvalSampleEntity sample, List<KnowledgeSource> sources) {
        if (!StringUtils.hasText(sample.getReferenceContext())) {
            return sources == null || sources.isEmpty() ? 100 : 80;
        }
        if (sources == null || sources.isEmpty()) {
            return 0;
        }
        return StringUtils.hasText(answer) ? 85 : 20;
    }

    /**
     * 计算工具调用成功率。
     *
     * @param toolResults 工具调用结果
     * @return 百分制分数
     */
    private double toolSuccessRate(List<Map<String, Object>> toolResults) {
        if (toolResults == null || toolResults.isEmpty()) {
            return 100;
        }
        long success = toolResults.stream().filter(result -> {
            Object status = result.get("status");
            Object successValue = result.get("success");
            String raw = String.valueOf(status == null ? successValue : status).toLowerCase(Locale.ROOT);
            return raw.contains("success") || "true".equals(raw) || "200".equals(raw);
        }).count();
        return success * 100.0 / toolResults.size();
    }

    /**
     * 计算幻觉控制分，前端展示幻觉率时会使用 100 - 该分值。
     *
     * @param answer 回答
     * @param sample 样本
     * @param sources 引用来源
     * @param accuracy 准确率
     * @param citation 引用正确率
     * @return 百分制分数
     */
    private double hallucinationControl(String answer,
                                        EvalSampleEntity sample,
                                        List<KnowledgeSource> sources,
                                        double accuracy,
                                        double citation) {
        if (!StringUtils.hasText(answer)) {
            return 0;
        }
        double base = Math.max(accuracy, StringUtils.hasText(sample.getExpectedAnswer()) ? 40 : 70);
        if (StringUtils.hasText(sample.getReferenceContext()) && (sources == null || sources.isEmpty())) {
            base -= 25;
        }
        return Math.max(0, Math.min(100, (base * 0.75) + (citation * 0.25)));
    }

    /**
     * 提取已覆盖的评分点。
     *
     * @param answer 回答
     * @param points 评分点
     * @return 已命中的评分点列表
     */
    private List<String> coveredPoints(String answer, List<String> points) {
        return points.stream().filter(point -> containsNormalized(answer, point)).toList();
    }

    /**
     * 将评分点文本解析为列表。
     *
     * @param sample 样本
     * @return 评分点列表
     */
    private List<String> scoringPoints(EvalSampleEntity sample) {
        List<String> points = parseStringList(sample.getScoringPoints());
        if (!points.isEmpty()) {
            return points;
        }
        if (StringUtils.hasText(sample.getExpectedAnswer())) {
            return List.of(sample.getExpectedAnswer());
        }
        return List.of();
    }

    /**
     * 解析字符串列表 JSON，失败时按换行和逗号拆分。
     *
     * @param raw 原始文本
     * @return 字符串列表
     */
    private List<String> parseStringList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {
            }).stream().filter(StringUtils::hasText).map(String::trim).toList();
        } catch (Exception ignored) {
            return splitText(raw);
        }
    }

    /**
     * 文本拆词，兼顾中文短语、英文和数字。
     *
     * @param text 输入文本
     * @return 关键词集合
     */
    private Set<String> keywords(String text) {
        return splitText(text).stream()
                .filter(word -> word.length() >= 2)
                .limit(20)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 按常见标点拆分文本。
     *
     * @param text 输入文本
     * @return 拆分后的文本片段
     */
    private List<String> splitText(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String normalized = text.replaceAll("[\\[\\]\"{}]", " ");
        String[] parts = normalized.split("[,，。；;、\\n\\r\\t]+");
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            String value = part.trim();
            if (StringUtils.hasText(value)) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * 归一化后判断答案是否包含目标片段。
     *
     * @param answer 答案
     * @param target 目标片段
     * @return 是否命中
     */
    private boolean containsNormalized(String answer, String target) {
        if (!StringUtils.hasText(answer) || !StringUtils.hasText(target)) {
            return false;
        }
        String answerText = normalizeForMatch(answer);
        String targetText = normalizeForMatch(target);
        return answerText.contains(targetText) || targetText.contains(answerText);
    }

    /**
     * 归一化匹配文本。
     *
     * @param value 原始文本
     * @return 归一化文本
     */
    private String normalizeForMatch(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replaceAll("[，。！？；：、,.!?;:()（）\\[\\]【】\"'`]", "");
    }

    /**
     * 查询可运行样本。
     *
     * @param datasetId 评测集 ID
     * @param maxSamples 最大样本数
     * @return 样本列表
     */
    private List<EvalSampleEntity> listRunnableSamples(String datasetId, Integer maxSamples) {
        int limit = maxSamples != null && maxSamples > 0 ? Math.min(maxSamples, 500) : DEFAULT_MAX_SAMPLES;
        return evalSampleMapper.selectList(new LambdaQueryWrapper<EvalSampleEntity>()
                .eq(EvalSampleEntity::getDatasetId, datasetId)
                .ne(EvalSampleEntity::getStatus, "disabled")
                .orderByAsc(EvalSampleEntity::getSampleNo)
                .last("limit " + limit));
    }

    /**
     * 查询评测集样本。
     *
     * @param datasetId 评测集 ID
     * @return 样本列表
     */
    private List<EvalSampleEntity> listSamples(String datasetId) {
        return evalSampleMapper.selectList(new LambdaQueryWrapper<EvalSampleEntity>()
                .eq(EvalSampleEntity::getDatasetId, datasetId)
                .orderByAsc(EvalSampleEntity::getSampleNo));
    }

    /**
     * 查询任务运行列表。
     *
     * @param taskId 任务 ID
     * @return 运行列表
     */
    private List<EvalTaskRunEntity> listRuns(String taskId) {
        return evalTaskRunMapper.selectList(new LambdaQueryWrapper<EvalTaskRunEntity>()
                .eq(EvalTaskRunEntity::getTaskId, taskId)
                .orderByAsc(EvalTaskRunEntity::getCreatedAt));
    }

    /**
     * 查询多个运行的得分。
     *
     * @param runs 运行列表
     * @return 得分列表
     */
    private List<EvalScoreEntity> listScores(List<EvalTaskRunEntity> runs) {
        if (runs.isEmpty()) {
            return List.of();
        }
        return evalScoreMapper.selectList(new LambdaQueryWrapper<EvalScoreEntity>()
                .in(EvalScoreEntity::getTaskRunId, runs.stream().map(EvalTaskRunEntity::getId).toList()));
    }

    /**
     * 按指标编码计算均分。
     *
     * @param scores 得分列表
     * @return 指标均分
     */
    private Map<String, BigDecimal> averageScoresByMetric(List<EvalScoreEntity> scores) {
        Map<String, String> metricCodes = metricsById();
        return scores.stream()
                .filter(score -> score.getScore() != null)
                .collect(Collectors.groupingBy(score -> metricCodes.getOrDefault(score.getMetricId(), score.getMetricId())))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> average(entry.getValue().stream().map(EvalScoreEntity::getScore).toList()),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    /**
     * 填充评测集实体字段。
     *
     * @param entity 评测集实体
     * @param request 保存请求
     */
    private void fillDataset(EvalDatasetEntity entity, EvaluationDtos.DatasetRequest request) {
        entity.setDatasetName(request.getDatasetName());
        entity.setDescription(request.getDescription());
        entity.setDomain(request.getDomain());
        entity.setTags(normalizeJsonArrayText(request.getTags()));
        entity.setVisibility(StringUtils.hasText(request.getVisibility()) ? request.getVisibility() : "private");
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "active");
    }

    /**
     * 转换评测集摘要。
     *
     * @param entity 评测集实体
     * @return 摘要
     */
    private EvaluationDtos.DatasetSummary toDatasetSummary(EvalDatasetEntity entity) {
        EvaluationDtos.DatasetSummary summary = new EvaluationDtos.DatasetSummary();
        fillDatasetSummary(summary, entity);
        return summary;
    }

    /**
     * 填充评测集摘要字段。
     *
     * @param summary 摘要 DTO
     * @param entity 评测集实体
     */
    private void fillDatasetSummary(EvaluationDtos.DatasetSummary summary, EvalDatasetEntity entity) {
        summary.setId(entity.getId());
        summary.setDatasetCode(entity.getDatasetCode());
        summary.setDatasetName(entity.getDatasetName());
        summary.setDescription(entity.getDescription());
        summary.setDomain(entity.getDomain());
        summary.setTags(entity.getTags());
        summary.setVisibility(entity.getVisibility());
        summary.setStatus(entity.getStatus());
        summary.setOwnerUserId(entity.getOwnerUserId());
        summary.setCanManage(canManageDataset(entity));
        summary.setSampleCount(countSamples(entity.getId()));
        summary.setTaskCount(countTasks(entity.getId()));
        summary.setCreatedAt(entity.getCreatedAt());
        summary.setUpdatedAt(entity.getUpdatedAt());
    }

    /**
     * 转换样本摘要。
     *
     * @param entity 样本实体
     * @return 样本摘要
     */
    private EvaluationDtos.SampleSummary toSampleSummary(EvalSampleEntity entity) {
        EvaluationDtos.SampleSummary summary = new EvaluationDtos.SampleSummary();
        summary.setId(entity.getId());
        summary.setDatasetId(entity.getDatasetId());
        summary.setSampleNo(entity.getSampleNo());
        summary.setQuestion(entity.getQuestion());
        summary.setExpectedAnswer(entity.getExpectedAnswer());
        summary.setReferenceContext(entity.getReferenceContext());
        summary.setScoringPoints(entity.getScoringPoints());
        summary.setMetadata(entity.getMetadata());
        summary.setStatus(entity.getStatus());
        return summary;
    }

    /**
     * 转换任务摘要。
     *
     * @param entity 任务实体
     * @return 任务摘要
     */
    private EvaluationDtos.TaskSummary toTaskSummary(EvalTaskEntity entity) {
        EvaluationDtos.TaskSummary summary = new EvaluationDtos.TaskSummary();
        fillTaskSummary(summary, entity);
        return summary;
    }

    /**
     * 填充任务摘要字段。
     *
     * @param summary 摘要 DTO
     * @param entity 任务实体
     */
    private void fillTaskSummary(EvaluationDtos.TaskSummary summary, EvalTaskEntity entity) {
        summary.setId(entity.getId());
        summary.setTaskCode(entity.getTaskCode());
        summary.setTaskName(entity.getTaskName());
        summary.setDatasetId(entity.getDatasetId());
        summary.setDatasetName(datasetName(entity.getDatasetId()));
        summary.setAgentId(entity.getAgentId());
        summary.setAgentName(agentName(entity.getAgentId()));
        summary.setBaselineModelId(entity.getBaselineModelId());
        summary.setBaselineModelName(modelName(entity.getBaselineModelId()));
        summary.setCompareModelIds(entity.getCompareModelIds());
        summary.setEvalConfig(entity.getEvalConfig());
        summary.setStatus(entity.getStatus());
        summary.setTotalSamples(entity.getTotalSamples());
        summary.setFinishedSamples(entity.getFinishedSamples());
        Map<String, Object> aggregate = buildTaskSummaryMap(entity);
        summary.setOverallScore(asBigDecimal(aggregate.get("overallScore")));
        summary.setSuccessRate(asBigDecimal(aggregate.get("successRate")));
        summary.setTotalTokens((Integer) aggregate.get("totalTokens"));
        summary.setAverageLatencyMs((Integer) aggregate.get("averageLatencyMs"));
        summary.setCreatedAt(entity.getCreatedAt());
        summary.setStartedAt(entity.getStartedAt());
        summary.setFinishedAt(entity.getFinishedAt());
    }

    /**
     * 转换样本运行摘要。
     *
     * @param run 运行实体
     * @return 运行摘要
     */
    private EvaluationDtos.TaskRunSummary toTaskRunSummary(EvalTaskRunEntity run) {
        EvaluationDtos.TaskRunSummary summary = new EvaluationDtos.TaskRunSummary();
        EvalSampleEntity sample = evalSampleMapper.selectById(run.getSampleId());
        summary.setId(run.getId());
        summary.setTaskId(run.getTaskId());
        summary.setSampleId(run.getSampleId());
        summary.setSampleNo(sample == null ? null : sample.getSampleNo());
        summary.setQuestion(sample == null ? null : sample.getQuestion());
        summary.setExpectedAnswer(sample == null ? null : sample.getExpectedAnswer());
        summary.setModelId(run.getModelId());
        summary.setModelName(modelName(run.getModelId()));
        summary.setRunId(run.getRunId());
        summary.setAnswerText(run.getAnswerText());
        summary.setStatus(run.getStatus());
        summary.setLatencyMs(run.getLatencyMs());
        summary.setTokenCount(run.getTokenCount());
        summary.setErrorMessage(run.getErrorMessage());
        summary.setScores(evalScoreMapper.selectList(new LambdaQueryWrapper<EvalScoreEntity>()
                        .eq(EvalScoreEntity::getTaskRunId, run.getId()))
                .stream()
                .map(this::toScoreSummary)
                .toList());
        return summary;
    }

    /**
     * 转换评分摘要。
     *
     * @param entity 评分实体
     * @return 评分摘要
     */
    private EvaluationDtos.ScoreSummary toScoreSummary(EvalScoreEntity entity) {
        EvalMetricEntity metric = evalMetricMapper.selectById(entity.getMetricId());
        EvaluationDtos.ScoreSummary summary = new EvaluationDtos.ScoreSummary();
        summary.setMetricId(entity.getMetricId());
        summary.setMetricCode(metric == null ? entity.getMetricId() : metric.getMetricCode());
        summary.setMetricName(metric == null ? entity.getMetricId() : metric.getMetricName());
        summary.setScore(entity.getScore());
        summary.setPassed(entity.getPassed());
        summary.setJudgeType(entity.getJudgeType());
        summary.setJudgeDetail(entity.getJudgeDetail());
        return summary;
    }

    /**
     * 判断当前用户是否可见评测集。
     *
     * @param dataset 评测集
     * @return 是否可见
     */
    private boolean canViewDataset(EvalDatasetEntity dataset) {
        if (dataset == null || dataset.getDeletedAt() != null) {
            return false;
        }
        return "public".equalsIgnoreCase(dataset.getVisibility()) || canManageDataset(dataset);
    }

    /**
     * 判断当前用户是否可管理评测集。
     *
     * @param dataset 评测集
     * @return 是否可管理
     */
    private boolean canManageDataset(EvalDatasetEntity dataset) {
        String userId = agentAccessService.currentUserId();
        return StringUtils.hasText(userId)
                && (userId.equals(dataset.getOwnerUserId()) || userId.equals(dataset.getCreatedBy()));
    }

    /**
     * 校验当前用户可查看评测集。
     *
     * @param dataset 评测集
     */
    private void assertCanViewDataset(EvalDatasetEntity dataset) {
        if (!canViewDataset(dataset)) {
            throw new BusinessException("EVAL_FORBIDDEN", "没有访问该评测集的权限");
        }
    }

    /**
     * 校验当前用户可管理评测集。
     *
     * @param dataset 评测集
     */
    private void assertCanManageDataset(EvalDatasetEntity dataset) {
        if (!canManageDataset(dataset)) {
            throw new BusinessException("EVAL_FORBIDDEN", "没有管理该评测集的权限");
        }
    }

    /**
     * 判断任务是否可见。
     *
     * @param task 评测任务
     * @return 是否可见
     */
    private boolean canViewTask(EvalTaskEntity task) {
        EvalDatasetEntity dataset = evalDatasetMapper.selectById(task.getDatasetId());
        return canViewDataset(dataset);
    }

    /**
     * 校验任务可见性。
     *
     * @param task 评测任务
     */
    private void assertCanViewTask(EvalTaskEntity task) {
        if (!canViewTask(task)) {
            throw new BusinessException("EVAL_FORBIDDEN", "没有访问该评测任务的权限");
        }
    }

    /**
     * 获取评测集实体。
     *
     * @param id 评测集 ID
     * @return 评测集实体
     */
    private EvalDatasetEntity requireDataset(String id) {
        EvalDatasetEntity entity = evalDatasetMapper.selectById(id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new BusinessException("EVAL_DATASET_NOT_FOUND", "评测集不存在");
        }
        return entity;
    }

    /**
     * 获取任务实体。
     *
     * @param id 任务 ID
     * @return 任务实体
     */
    private EvalTaskEntity requireTask(String id) {
        EvalTaskEntity entity = evalTaskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("EVAL_TASK_NOT_FOUND", "评测任务不存在");
        }
        return entity;
    }

    /**
     * 获取 Agent 实体。
     *
     * @param id Agent ID
     * @return Agent 实体
     */
    private AgentEntity requireAgent(String id) {
        AgentEntity entity = agentMapper.selectById(id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new BusinessException("AGENT_NOT_FOUND", "Agent 不存在");
        }
        return entity;
    }

    /**
     * 当前登录用户 ID。
     *
     * @return 用户 ID
     */
    private String currentUserIdOrThrow() {
        String userId = agentAccessService.currentUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("UNAUTHORIZED", "请先登录");
        }
        return userId;
    }

    /**
     * 计算下一个样本序号。
     *
     * @param datasetId 评测集 ID
     * @return 下一个序号
     */
    private int nextSampleNo(String datasetId) {
        return listSamples(datasetId).stream()
                .map(EvalSampleEntity::getSampleNo)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
    }

    /**
     * 样本数量。
     *
     * @param datasetId 评测集 ID
     * @return 数量
     */
    private int countSamples(String datasetId) {
        Long count = evalSampleMapper.selectCount(new LambdaQueryWrapper<EvalSampleEntity>()
                .eq(EvalSampleEntity::getDatasetId, datasetId));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 任务数量。
     *
     * @param datasetId 评测集 ID
     * @return 数量
     */
    private int countTasks(String datasetId) {
        Long count = evalTaskMapper.selectCount(new LambdaQueryWrapper<EvalTaskEntity>()
                .eq(EvalTaskEntity::getDatasetId, datasetId));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 生成唯一评测集编码。
     *
     * @param base 基础编码
     * @return 唯一编码
     */
    private String uniqueDatasetCode(String base) {
        String code = base;
        int index = 1;
        while (evalDatasetMapper.selectCount(new LambdaQueryWrapper<EvalDatasetEntity>()
                .eq(EvalDatasetEntity::getDatasetCode, code)) > 0) {
            code = base + "_" + index++;
        }
        return code;
    }

    /**
     * 判断模型是否存在。
     *
     * @param modelId 模型 ID
     * @return 是否存在
     */
    private boolean modelExists(String modelId) {
        return StringUtils.hasText(modelId) && modelConfigMapper.selectById(modelId) != null;
    }

    /**
     * 获取模型名称。
     *
     * @param modelId 模型 ID
     * @return 模型名称
     */
    private String modelName(String modelId) {
        if (!StringUtils.hasText(modelId)) {
            return "";
        }
        ModelConfigEntity model = modelConfigMapper.selectById(modelId);
        return model == null ? modelId : model.getModelName();
    }

    /**
     * 获取 Agent 名称。
     *
     * @param agentId Agent ID
     * @return Agent 名称
     */
    private String agentName(String agentId) {
        AgentEntity agent = agentMapper.selectById(agentId);
        return agent == null ? agentId : agent.getAgentName();
    }

    /**
     * 获取评测集名称。
     *
     * @param datasetId 评测集 ID
     * @return 评测集名称
     */
    private String datasetName(String datasetId) {
        EvalDatasetEntity dataset = evalDatasetMapper.selectById(datasetId);
        return dataset == null ? datasetId : dataset.getDatasetName();
    }

    /**
     * 指标按编码索引。
     *
     * @return 指标 Map
     */
    private Map<String, EvalMetricEntity> metricsByCode() {
        return evalMetricMapper.selectList(new LambdaQueryWrapper<EvalMetricEntity>()
                        .eq(EvalMetricEntity::getEnabled, true))
                .stream()
                .collect(Collectors.toMap(EvalMetricEntity::getMetricCode, metric -> metric, (left, right) -> left));
    }

    /**
     * 指标 ID 到编码映射。
     *
     * @return 映射 Map
     */
    private Map<String, String> metricsById() {
        return evalMetricMapper.selectList(new LambdaQueryWrapper<EvalMetricEntity>()
                        .eq(EvalMetricEntity::getEnabled, true))
                .stream()
                .collect(Collectors.toMap(EvalMetricEntity::getId, EvalMetricEntity::getMetricCode, (left, right) -> left));
    }

    /**
     * 成功率。
     *
     * @param runs 运行列表
     * @return 成功率
     */
    private BigDecimal successRate(List<EvalTaskRunEntity> runs) {
        if (runs.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long success = runs.stream().filter(run -> "success".equalsIgnoreCase(run.getStatus())).count();
        return BigDecimal.valueOf(success * 100.0 / runs.size()).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 平均耗时。
     *
     * @param runs 运行列表
     * @return 平均毫秒
     */
    private int averageLatency(List<EvalTaskRunEntity> runs) {
        return (int) Math.round(runs.stream()
                .filter(run -> run.getLatencyMs() != null)
                .mapToInt(EvalTaskRunEntity::getLatencyMs)
                .average()
                .orElse(0));
    }

    /**
     * Token 总量。
     *
     * @param runs 运行列表
     * @return Token 总量
     */
    private int totalTokens(List<EvalTaskRunEntity> runs) {
        return runs.stream()
                .filter(run -> run.getTokenCount() != null)
                .mapToInt(EvalTaskRunEntity::getTokenCount)
                .sum();
    }

    /**
     * 平均值。
     *
     * @param values 分数列表
     * @return 平均分
     */
    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * 百分制分数格式化。
     *
     * @param value 原始分
     * @return BigDecimal 分数
     */
    private BigDecimal score(double value) {
        return BigDecimal.valueOf(Math.max(0, Math.min(100, value))).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 对象转 BigDecimal。
     *
     * @param value 原始值
     * @return BigDecimal
     */
    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 标准化 JSON 数组文本。
     *
     * @param raw 原始文本
     * @return JSON 数组文本
     */
    private String normalizeJsonArrayText(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "[]";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed;
        }
        return toJson(splitText(trimmed));
    }

    /**
     * 标准化 JSON 对象文本。
     *
     * @param raw 原始文本
     * @return JSON 对象文本
     */
    private String normalizeJsonObjectText(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "{}";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        return toJson(mapOf("text", trimmed));
    }

    /**
     * 构建可包含 null 的 Map。
     *
     * @param values 键值交替数组
     * @return Map
     */
    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    /**
     * 空字符串兜底。
     *
     * @param value 原始字符串
     * @return 非 null 字符串
     */
    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    /**
     * 序列化 JSON。
     *
     * @param value 对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    /**
     * 生成主键。
     *
     * @return UUID 字符串
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * LLM-as-Judge 结构化结果。
     */
    private static class JudgeResult {

        /** Judge 是否真实成功返回评分。 */
        private final boolean success;

        /** Judge 模型 ID。 */
        private final String modelId;

        /** Judge 模型名称。 */
        private final String modelName;

        /** 各维度得分。 */
        private final Map<String, Double> scores;

        /** 评分理由和维度详情。 */
        private final Map<String, Object> detail;

        /** 失败原因。 */
        private final String errorMessage;

        /** Judge 耗时。 */
        private int latencyMs;

        /** Judge 消耗 Token。 */
        private int totalTokens;

        /** Judge 原始输出。 */
        private String rawOutput;

        private JudgeResult(boolean success,
                            String modelId,
                            String modelName,
                            Map<String, Double> scores,
                            Map<String, Object> detail,
                            String errorMessage) {
            this.success = success;
            this.modelId = modelId;
            this.modelName = modelName;
            this.scores = scores == null ? Map.of() : scores;
            this.detail = detail == null ? Map.of() : detail;
            this.errorMessage = errorMessage;
        }

        private static JudgeResult success(String modelId, String modelName, Map<String, Double> scores, Map<String, Object> detail) {
            return new JudgeResult(true, modelId, modelName, scores, detail, "");
        }

        private static JudgeResult failed(String errorMessage, String modelId, String modelName, int latencyMs, int totalTokens, String rawOutput) {
            JudgeResult result = new JudgeResult(false, modelId, modelName, Map.of(), Map.of(), errorMessage);
            result.setLatencyMs(latencyMs);
            result.setTotalTokens(totalTokens);
            result.setRawOutput(rawOutput);
            return result;
        }

        private static JudgeResult disabled() {
            return failed("Judge 已关闭，使用规则评分", "", "", 0, 0, "");
        }

        private double scoreOr(String key, double fallback) {
            return success && scores.containsKey(key) ? scores.get(key) : fallback;
        }

        private Map<String, Object> detailWithFallback(Object... values) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("judgeType", success ? "llm_as_judge" : "rule");
            map.put("judgedBy", success ? modelId : "system-rule");
            map.put("judgeSuccess", success);
            map.put("judgeModelId", modelId);
            map.put("judgeModelName", modelName);
            map.put("judgeLatencyMs", latencyMs);
            map.put("judgeTotalTokens", totalTokens);
            map.put("judgeRawOutput", rawOutput);
            map.put("judgeErrorMessage", errorMessage);
            map.put("judgeDetail", detail);
            for (int index = 0; index + 1 < values.length; index += 2) {
                map.put(String.valueOf(values[index]), values[index + 1]);
            }
            return map;
        }

        private int totalTokens() {
            return totalTokens;
        }

        private void setLatencyMs(int latencyMs) {
            this.latencyMs = latencyMs;
        }

        private void setTotalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
        }

        private void setRawOutput(String rawOutput) {
            this.rawOutput = rawOutput;
        }
    }
}
