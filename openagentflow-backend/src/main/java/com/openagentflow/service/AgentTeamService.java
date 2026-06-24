package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.agent.AgentTeamDtos;
import com.openagentflow.domain.chat.ChatCompletionRequest;
import com.openagentflow.domain.chat.ChatCompletionResponse;
import com.openagentflow.entity.AgentCollaborationRunEntity;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.AgentTeamEntity;
import com.openagentflow.entity.AgentTeamMemberEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.RuntimeRunEntity;
import com.openagentflow.entity.RuntimeTraceStepEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AgentCollaborationRunMapper;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.AgentTeamMapper;
import com.openagentflow.mapper.AgentTeamMemberMapper;
import com.openagentflow.mapper.ModelConfigMapper;
import com.openagentflow.mapper.RuntimeRunMapper;
import com.openagentflow.mapper.RuntimeTraceStepMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 多 Agent 协作团队服务。
 */
@Service
public class AgentTeamService {

    /** 协作团队 Mapper。 */
    private final AgentTeamMapper agentTeamMapper;
    /** 协作团队成员 Mapper。 */
    private final AgentTeamMemberMapper agentTeamMemberMapper;
    /** 协作运行 Mapper。 */
    private final AgentCollaborationRunMapper collaborationRunMapper;
    /** Agent Mapper，用于校验成员和展示名称。 */
    private final AgentMapper agentMapper;
    /** 模型配置 Mapper，用于展示成员模型名称。 */
    private final ModelConfigMapper modelConfigMapper;
    /** 顶层运行记录 Mapper。 */
    private final RuntimeRunMapper runtimeRunMapper;
    /** Trace 步骤 Mapper。 */
    private final RuntimeTraceStepMapper traceStepMapper;
    /** Agent 访问控制服务。 */
    private final AgentAccessService agentAccessService;
    /** Agent 运行服务，复用已有 RAG、Tool Calling、Workflow 能力。 */
    private final AgentService agentService;
    /** JDBC 工具，用于成员替换和统计查询。 */
    private final JdbcTemplate jdbcTemplate;
    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    public AgentTeamService(AgentTeamMapper agentTeamMapper,
                            AgentTeamMemberMapper agentTeamMemberMapper,
                            AgentCollaborationRunMapper collaborationRunMapper,
                            AgentMapper agentMapper,
                            ModelConfigMapper modelConfigMapper,
                            RuntimeRunMapper runtimeRunMapper,
                            RuntimeTraceStepMapper traceStepMapper,
                            AgentAccessService agentAccessService,
                            AgentService agentService,
                            JdbcTemplate jdbcTemplate,
                            ObjectMapper objectMapper) {
        this.agentTeamMapper = agentTeamMapper;
        this.agentTeamMemberMapper = agentTeamMemberMapper;
        this.collaborationRunMapper = collaborationRunMapper;
        this.agentMapper = agentMapper;
        this.modelConfigMapper = modelConfigMapper;
        this.runtimeRunMapper = runtimeRunMapper;
        this.traceStepMapper = traceStepMapper;
        this.agentAccessService = agentAccessService;
        this.agentService = agentService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询当前用户可见的协作团队列表。
     *
     * @return 协作团队摘要列表
     */
    public List<AgentTeamDtos.TeamSummary> listTeams() {
        return agentTeamMapper.selectList(new LambdaQueryWrapper<AgentTeamEntity>()
                        .ne(AgentTeamEntity::getStatus, "deleted")
                        .orderByDesc(AgentTeamEntity::getUpdatedAt)
                        .last("limit 200"))
                .stream()
                .filter(this::canView)
                .map(this::toSummary)
                .toList();
    }

    /**
     * 查询协作团队详情。
     *
     * @param id 团队 ID
     * @return 协作团队详情
     */
    public AgentTeamDtos.TeamDetail getTeam(String id) {
        AgentTeamEntity entity = requireTeam(id);
        assertCanView(entity);
        return toDetail(entity);
    }

    /**
     * 创建协作团队。
     *
     * @param request 保存请求
     * @return 创建后的团队详情
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentTeamDtos.TeamDetail createTeam(AgentTeamDtos.TeamRequest request) {
        String userId = currentUserIdOrThrow();
        AgentTeamEntity entity = new AgentTeamEntity();
        entity.setId(newId());
        fillTeam(entity, request, true);
        entity.setOwnerUserId(userId);
        entity.setCreatedBy(userId);
        agentTeamMapper.insert(entity);
        replaceMembers(entity.getId(), request.getMembers());
        return getTeam(entity.getId());
    }

    /**
     * 更新协作团队。
     *
     * @param id 团队 ID
     * @param request 保存请求
     * @return 更新后的团队详情
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentTeamDtos.TeamDetail updateTeam(String id, AgentTeamDtos.TeamRequest request) {
        AgentTeamEntity entity = requireTeam(id);
        assertCanManage(entity);
        fillTeam(entity, request, false);
        agentTeamMapper.updateById(entity);
        replaceMembers(entity.getId(), request.getMembers());
        return getTeam(entity.getId());
    }

    /**
     * 发布协作团队。
     *
     * @param id 团队 ID
     * @return 发布后的团队详情
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentTeamDtos.TeamDetail publishTeam(String id) {
        AgentTeamEntity entity = requireTeam(id);
        assertCanManage(entity);
        if (enabledMembers(entity.getId()).isEmpty()) {
            throw new BusinessException("AGENT_TEAM_MEMBER_EMPTY", "请至少配置一个启用的团队成员");
        }
        entity.setStatus("published");
        agentTeamMapper.updateById(entity);
        return getTeam(entity.getId());
    }

    /**
     * 删除协作团队。
     *
     * @param id 团队 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTeam(String id) {
        AgentTeamEntity entity = requireTeam(id);
        assertCanManage(entity);
        entity.setStatus("deleted");
        agentTeamMapper.updateById(entity);
    }

    /**
     * 运行一次多 Agent 协作。
     *
     * @param id 团队 ID
     * @param request 运行请求
     * @return 协作运行结果
     */
    public AgentTeamDtos.RunResult runTeam(String id, AgentTeamDtos.RunRequest request) {
        AgentTeamEntity team = requireTeam(id);
        assertCanView(team);
        if (!StringUtils.hasText(request == null ? null : request.getObjective())) {
            throw new BusinessException("AGENT_TEAM_OBJECTIVE_REQUIRED", "协作目标不能为空");
        }
        List<AgentTeamMemberEntity> members = enabledMembers(team.getId());
        if (members.isEmpty()) {
            throw new BusinessException("AGENT_TEAM_MEMBER_EMPTY", "请先配置启用的团队成员");
        }
        members.forEach(member -> agentAccessService.assertCanView(requireAgent(member.getAgentId())));

        LocalDateTime startedAt = LocalDateTime.now();
        RuntimeRunEntity runtimeRun = createRuntimeRun(team, request, startedAt);
        AgentCollaborationRunEntity collaborationRun = createCollaborationRun(team, runtimeRun, request, startedAt);
        AgentTeamDtos.RunResult result = initialRunResult(team, collaborationRun, runtimeRun, request);
        List<AgentTeamDtos.StepResult> steps = new ArrayList<>();

        try {
            // 根据协作模式生成可执行步骤，并逐步调用已有 Agent 运行链路。
            String finalResult = executeByMode(team, request, members, runtimeRun, steps);
            result.setFinalResult(finalResult);
            result.setStatus("SUCCESS");
            result.setTotalTokens(sumTokens(steps));
            result.setLatencyMs(toLatency(startedAt, LocalDateTime.now()));
            result.setSteps(steps);
            updateRuntimeSuccess(runtimeRun, result, startedAt);
            updateCollaborationSuccess(collaborationRun, result, steps);
            return result;
        } catch (Exception exception) {
            String message = safeText(exception.getMessage());
            result.setStatus("FAILED");
            result.setErrorMessage(message);
            result.setFinalResult(lastOutput(steps));
            result.setTotalTokens(sumTokens(steps));
            result.setLatencyMs(toLatency(startedAt, LocalDateTime.now()));
            result.setSteps(steps);
            updateRuntimeFailure(runtimeRun, result, message, startedAt);
            updateCollaborationFailure(collaborationRun, result, steps, message);
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException("AGENT_TEAM_RUN_FAILED", "多 Agent 协作运行失败：" + message);
        }
    }

    /**
     * 按协作模式执行成员 Agent。
     *
     * @param team 团队实体
     * @param request 运行请求
     * @param members 启用成员
     * @param runtimeRun 顶层运行记录
     * @param steps 步骤结果容器
     * @return 最终输出
     */
    private String executeByMode(AgentTeamEntity team,
                                 AgentTeamDtos.RunRequest request,
                                 List<AgentTeamMemberEntity> members,
                                 RuntimeRunEntity runtimeRun,
                                 List<AgentTeamDtos.StepResult> steps) {
        String mode = normalizeMode(team.getCollaborationMode());
        List<AgentTeamMemberEntity> sortedMembers = sortedMembers(members);
        AgentTeamMemberEntity coordinator = findCoordinator(team, sortedMembers);
        String context = "";

        if ("parallel".equals(mode)) {
            for (AgentTeamMemberEntity member : sortedMembers) {
                callMember(member, "并行处理", request.getObjective(), request, runtimeRun, steps, false);
            }
            return combineOutputs(request.getObjective(), steps);
        }

        if ("reviewer".equals(mode)) {
            List<AgentTeamMemberEntity> primaryMembers = sortedMembers.stream().filter(member -> !isReviewer(member)).toList();
            List<AgentTeamMemberEntity> reviewers = sortedMembers.stream().filter(this::isReviewer).toList();
            context = executeSequential(primaryMembers.isEmpty() ? sortedMembers : primaryMembers, request, runtimeRun, steps, context, false);
            if (!reviewers.isEmpty()) {
                context = executeSequential(reviewers, request, runtimeRun, steps, context, true);
            }
            return lastOutput(steps);
        }

        if ("supervisor".equals(mode) && coordinator != null) {
            AgentTeamDtos.StepResult plan = callMember(coordinator, "主控规划", request.getObjective(), request, runtimeRun, steps, false);
            context = safeText(plan.getOutput());
            List<AgentTeamMemberEntity> workers = sortedMembers.stream()
                    .filter(member -> !member.getAgentId().equals(coordinator.getAgentId()))
                    .toList();
            context = executeSequential(workers.isEmpty() ? sortedMembers : workers, request, runtimeRun, steps, context, false);
            callMember(coordinator, "主控汇总", buildReviewInput(request.getObjective(), context), request, runtimeRun, steps, true);
            return lastOutput(steps);
        }

        if ("router".equals(mode) && coordinator != null) {
            AgentTeamDtos.StepResult route = callMember(coordinator, "路由决策", request.getObjective(), request, runtimeRun, steps, false);
            context = safeText(route.getOutput());
            List<AgentTeamMemberEntity> workers = sortedMembers.stream()
                    .filter(member -> !member.getAgentId().equals(coordinator.getAgentId()))
                    .toList();
            context = executeSequential(workers.isEmpty() ? sortedMembers : workers, request, runtimeRun, steps, context, false);
            return StringUtils.hasText(context) ? context : lastOutput(steps);
        }

        return executeSequential(sortedMembers, request, runtimeRun, steps, context, false);
    }

    /**
     * 顺序执行成员列表。
     *
     * @param members 成员列表
     * @param request 运行请求
     * @param runtimeRun 顶层运行记录
     * @param steps 步骤结果容器
     * @param previousContext 上一步输出
     * @param reviewMode 是否为复核阶段
     * @return 最后一步输出
     */
    private String executeSequential(List<AgentTeamMemberEntity> members,
                                     AgentTeamDtos.RunRequest request,
                                     RuntimeRunEntity runtimeRun,
                                     List<AgentTeamDtos.StepResult> steps,
                                     String previousContext,
                                     boolean reviewMode) {
        String context = safeText(previousContext);
        for (AgentTeamMemberEntity member : members) {
            String input = reviewMode
                    ? buildReviewInput(request.getObjective(), context)
                    : buildMemberInput(request.getObjective(), context, member);
            AgentTeamDtos.StepResult step = callMember(member, reviewMode ? "复核审阅" : "顺序协作", input, request, runtimeRun, steps, true);
            context = safeText(step.getOutput());
        }
        return context;
    }

    /**
     * 调用单个成员 Agent，并写入顶层 Trace Step。
     *
     * @param member 成员配置
     * @param phase 阶段名称
     * @param input 成员输入
     * @param request 运行请求
     * @param runtimeRun 顶层运行记录
     * @param steps 步骤结果容器
     * @param carryContext 是否携带上下文
     * @return 步骤结果
     */
    private AgentTeamDtos.StepResult callMember(AgentTeamMemberEntity member,
                                                String phase,
                                                String input,
                                                AgentTeamDtos.RunRequest request,
                                                RuntimeRunEntity runtimeRun,
                                                List<AgentTeamDtos.StepResult> steps,
                                                boolean carryContext) {
        AgentEntity agent = requireAgent(member.getAgentId());
        LocalDateTime startedAt = LocalDateTime.now();
        RuntimeTraceStepEntity traceStep = createTraceStep(runtimeRun, member, agent, phase, input, startedAt);
        AgentTeamDtos.StepResult step = initialStepResult(traceStep, member, agent, phase, input);
        try {
            // 复用 AgentService.runAgent，成员 Agent 原有模型、RAG、工具和工作流能力都会按既有逻辑执行。
            ChatCompletionRequest chatRequest = new ChatCompletionRequest();
            chatRequest.setAgentId(agent.getId());
            chatRequest.setModelId(agent.getModelId());
            chatRequest.setInput(input);
            ChatCompletionResponse response = agentService.runAgent(agent.getId(), chatRequest);
            LocalDateTime finishedAt = LocalDateTime.now();
            step.setStatus("SUCCESS");
            step.setOutput(safeText(response.getContent()));
            step.setChildRunId(response.getRunId());
            step.setTotalTokens(response.getTotalTokens() == null ? 0 : response.getTotalTokens());
            step.setLatencyMs(response.getLatencyMs() == null ? toLatency(startedAt, finishedAt) : response.getLatencyMs());
            updateTraceStepSuccess(traceStep, step, response, finishedAt, carryContext);
            steps.add(step);
            return step;
        } catch (Exception exception) {
            LocalDateTime finishedAt = LocalDateTime.now();
            String message = safeText(exception.getMessage());
            step.setStatus("FAILED");
            step.setErrorMessage(message);
            step.setLatencyMs(toLatency(startedAt, finishedAt));
            updateTraceStepFailure(traceStep, step, message, finishedAt);
            steps.add(step);
            if (Boolean.TRUE.equals(request.getContinueOnError())) {
                return step;
            }
            throw exception;
        }
    }

    /**
     * 填充团队实体。
     *
     * @param entity 团队实体
     * @param request 保存请求
     * @param create 是否创建场景
     */
    private void fillTeam(AgentTeamEntity entity, AgentTeamDtos.TeamRequest request, boolean create) {
        if (request == null || !StringUtils.hasText(request.getTeamName())) {
            throw new BusinessException("AGENT_TEAM_NAME_REQUIRED", "团队名称不能为空");
        }
        String code = StringUtils.hasText(request.getTeamCode()) ? request.getTeamCode().trim() : slugify(request.getTeamName());
        entity.setTeamCode(create ? uniqueTeamCode(code) : code);
        entity.setTeamName(request.getTeamName().trim());
        entity.setDescription(request.getDescription());
        entity.setCollaborationMode(normalizeMode(request.getCollaborationMode()));
        entity.setCoordinatorAgentId(request.getCoordinatorAgentId());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "draft");
    }

    /**
     * 替换团队成员配置。
     *
     * @param teamId 团队 ID
     * @param members 新成员列表
     */
    private void replaceMembers(String teamId, List<AgentTeamDtos.MemberRequest> members) {
        jdbcTemplate.update("DELETE FROM agent_team_member WHERE team_id = ?", teamId);
        if (members == null || members.isEmpty()) {
            return;
        }
        int index = 0;
        for (AgentTeamDtos.MemberRequest item : members) {
            if (!StringUtils.hasText(item.getAgentId())) {
                continue;
            }
            AgentEntity agent = requireAgent(item.getAgentId());
            agentAccessService.assertCanView(agent);
            AgentTeamMemberEntity member = new AgentTeamMemberEntity();
            member.setTeamId(teamId);
            member.setAgentId(agent.getId());
            member.setMemberRole(StringUtils.hasText(item.getMemberRole()) ? item.getMemberRole() : "worker");
            member.setHandoffPolicy(StringUtils.hasText(item.getHandoffPolicy()) ? item.getHandoffPolicy() : "{}");
            member.setSortOrder(item.getSortOrder() == null ? index * 10 : item.getSortOrder());
            member.setEnabled(item.getEnabled() == null || item.getEnabled());
            agentTeamMemberMapper.insert(member);
            index++;
        }
    }

    /**
     * 创建顶层运行记录。
     *
     * @param team 团队实体
     * @param request 运行请求
     * @param startedAt 开始时间
     * @return 运行记录
     */
    private RuntimeRunEntity createRuntimeRun(AgentTeamEntity team, AgentTeamDtos.RunRequest request, LocalDateTime startedAt) {
        RuntimeRunEntity run = new RuntimeRunEntity();
        run.setId(newId());
        run.setRunNo("TEAM-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(startedAt) + "-" + newId().substring(0, 8));
        run.setRunType("AGENT_TEAM");
        run.setUserId(agentAccessService.currentUserId());
        run.setInputText(request.getObjective());
        run.setInputPayload(toJson(Map.of(
                "teamId", team.getId(),
                "teamName", safeText(team.getTeamName()),
                "collaborationMode", safeText(team.getCollaborationMode()),
                "sharedContext", request.getSharedContext() == null ? Map.of() : request.getSharedContext()
        )));
        run.setStatus("RUNNING");
        run.setTotalTokens(0);
        run.setTotalCost(BigDecimal.ZERO);
        run.setStartedAt(startedAt);
        run.setCreatedAt(startedAt);
        run.setMetadata(toJson(Map.of("teamId", team.getId(), "teamCode", safeText(team.getTeamCode()))));
        runtimeRunMapper.insert(run);
        return run;
    }

    /**
     * 创建协作运行记录。
     *
     * @param team 团队实体
     * @param runtimeRun 顶层运行记录
     * @param request 运行请求
     * @param startedAt 开始时间
     * @return 协作运行记录
     */
    private AgentCollaborationRunEntity createCollaborationRun(AgentTeamEntity team,
                                                               RuntimeRunEntity runtimeRun,
                                                               AgentTeamDtos.RunRequest request,
                                                               LocalDateTime startedAt) {
        AgentCollaborationRunEntity run = new AgentCollaborationRunEntity();
        run.setId(newId());
        run.setTeamId(team.getId());
        run.setRunId(runtimeRun.getId());
        run.setObjective(request.getObjective());
        run.setSharedContext(toJson(request.getSharedContext() == null ? Map.of() : request.getSharedContext()));
        run.setStatus("RUNNING");
        run.setStartedAt(startedAt);
        collaborationRunMapper.insert(run);
        return run;
    }

    /**
     * 创建运行 Trace Step。
     *
     * @param runtimeRun 顶层运行记录
     * @param member 成员配置
     * @param agent Agent 实体
     * @param phase 协作阶段
     * @param input 输入内容
     * @param startedAt 开始时间
     * @return Trace Step
     */
    private RuntimeTraceStepEntity createTraceStep(RuntimeRunEntity runtimeRun,
                                                   AgentTeamMemberEntity member,
                                                   AgentEntity agent,
                                                   String phase,
                                                   String input,
                                                   LocalDateTime startedAt) {
        RuntimeTraceStepEntity step = new RuntimeTraceStepEntity();
        step.setId(newId());
        step.setRunId(runtimeRun.getId());
        step.setStepKey("agent_team_" + (stepsKeySuffix(member)));
        step.setStepName(phase + " / " + safeText(agent.getAgentName()));
        step.setStepType("AGENT_TEAM_MEMBER");
        step.setStatus("RUNNING");
        step.setInputPayload(toJson(Map.of(
                "agentId", agent.getId(),
                "agentName", safeText(agent.getAgentName()),
                "memberRole", safeText(member.getMemberRole()),
                "input", safeText(input)
        )));
        step.setPromptText(input);
        step.setModelId(agent.getModelId());
        step.setTokenUsage("{}");
        step.setCostAmount(BigDecimal.ZERO);
        step.setStartedAt(startedAt);
        step.setCreatedAt(startedAt);
        traceStepMapper.insert(step);
        return step;
    }

    /**
     * 更新成功的 Trace Step。
     *
     * @param traceStep Trace Step
     * @param step 前端步骤结果
     * @param response Agent 响应
     * @param finishedAt 完成时间
     * @param carryContext 是否携带上下文
     */
    private void updateTraceStepSuccess(RuntimeTraceStepEntity traceStep,
                                        AgentTeamDtos.StepResult step,
                                        ChatCompletionResponse response,
                                        LocalDateTime finishedAt,
                                        boolean carryContext) {
        traceStep.setStatus("SUCCESS");
        traceStep.setOutputPayload(toJson(Map.of(
                "output", safeText(step.getOutput()),
                "childRunId", safeText(step.getChildRunId()),
                "sources", response.getSources() == null ? List.of() : response.getSources(),
                "toolResults", response.getToolResults() == null ? List.of() : response.getToolResults(),
                "carryContext", carryContext
        )));
        traceStep.setTokenUsage(toJson(Map.of(
                "promptTokens", response.getPromptTokens() == null ? 0 : response.getPromptTokens(),
                "completionTokens", response.getCompletionTokens() == null ? 0 : response.getCompletionTokens(),
                "totalTokens", step.getTotalTokens() == null ? 0 : step.getTotalTokens()
        )));
        traceStep.setLatencyMs(step.getLatencyMs());
        traceStep.setFinishedAt(finishedAt);
        traceStepMapper.updateById(traceStep);
    }

    /**
     * 更新失败的 Trace Step。
     *
     * @param traceStep Trace Step
     * @param step 前端步骤结果
     * @param message 错误信息
     * @param finishedAt 完成时间
     */
    private void updateTraceStepFailure(RuntimeTraceStepEntity traceStep,
                                        AgentTeamDtos.StepResult step,
                                        String message,
                                        LocalDateTime finishedAt) {
        traceStep.setStatus("FAILED");
        traceStep.setOutputPayload(toJson(Map.of("error", safeText(message))));
        traceStep.setErrorMessage(message);
        traceStep.setLatencyMs(step.getLatencyMs());
        traceStep.setFinishedAt(finishedAt);
        traceStepMapper.updateById(traceStep);
    }

    /**
     * 更新成功的顶层运行记录。
     *
     * @param runtimeRun 顶层运行记录
     * @param result 协作结果
     * @param startedAt 开始时间
     */
    private void updateRuntimeSuccess(RuntimeRunEntity runtimeRun, AgentTeamDtos.RunResult result, LocalDateTime startedAt) {
        runtimeRun.setStatus("SUCCESS");
        runtimeRun.setOutputText(result.getFinalResult());
        runtimeRun.setOutputPayload(toJson(Map.of("steps", result.getSteps())));
        runtimeRun.setTotalTokens(result.getTotalTokens());
        runtimeRun.setPromptTokens(0);
        runtimeRun.setCompletionTokens(result.getTotalTokens());
        runtimeRun.setTotalCost(BigDecimal.ZERO);
        runtimeRun.setLatencyMs(result.getLatencyMs());
        runtimeRun.setFinishedAt(startedAt.plus(Duration.ofMillis(result.getLatencyMs() == null ? 0 : result.getLatencyMs())));
        runtimeRunMapper.updateById(runtimeRun);
    }

    /**
     * 更新失败的顶层运行记录。
     *
     * @param runtimeRun 顶层运行记录
     * @param result 协作结果
     * @param message 错误信息
     * @param startedAt 开始时间
     */
    private void updateRuntimeFailure(RuntimeRunEntity runtimeRun, AgentTeamDtos.RunResult result, String message, LocalDateTime startedAt) {
        runtimeRun.setStatus("FAILED");
        runtimeRun.setOutputText(result.getFinalResult());
        runtimeRun.setOutputPayload(toJson(Map.of("steps", result.getSteps())));
        runtimeRun.setTotalTokens(result.getTotalTokens());
        runtimeRun.setTotalCost(BigDecimal.ZERO);
        runtimeRun.setLatencyMs(result.getLatencyMs());
        runtimeRun.setErrorMessage(message);
        runtimeRun.setFinishedAt(startedAt.plus(Duration.ofMillis(result.getLatencyMs() == null ? 0 : result.getLatencyMs())));
        runtimeRunMapper.updateById(runtimeRun);
    }

    /**
     * 更新成功的协作运行记录。
     *
     * @param collaborationRun 协作运行记录
     * @param result 协作结果
     * @param steps 步骤结果
     */
    private void updateCollaborationSuccess(AgentCollaborationRunEntity collaborationRun,
                                            AgentTeamDtos.RunResult result,
                                            List<AgentTeamDtos.StepResult> steps) {
        collaborationRun.setFinalResult(result.getFinalResult());
        collaborationRun.setStatus("SUCCESS");
        collaborationRun.setSharedContext(toJson(Map.of("steps", steps)));
        collaborationRun.setFinishedAt(LocalDateTime.now());
        collaborationRunMapper.updateById(collaborationRun);
    }

    /**
     * 更新失败的协作运行记录。
     *
     * @param collaborationRun 协作运行记录
     * @param result 协作结果
     * @param steps 步骤结果
     * @param message 错误信息
     */
    private void updateCollaborationFailure(AgentCollaborationRunEntity collaborationRun,
                                            AgentTeamDtos.RunResult result,
                                            List<AgentTeamDtos.StepResult> steps,
                                            String message) {
        collaborationRun.setFinalResult(result.getFinalResult());
        collaborationRun.setStatus("FAILED");
        collaborationRun.setSharedContext(toJson(Map.of("steps", steps, "error", safeText(message))));
        collaborationRun.setFinishedAt(LocalDateTime.now());
        collaborationRunMapper.updateById(collaborationRun);
    }

    /**
     * 转换团队摘要。
     *
     * @param entity 团队实体
     * @return 团队摘要
     */
    private AgentTeamDtos.TeamSummary toSummary(AgentTeamEntity entity) {
        AgentTeamDtos.TeamSummary item = new AgentTeamDtos.TeamSummary();
        item.setId(entity.getId());
        item.setTeamCode(entity.getTeamCode());
        item.setTeamName(entity.getTeamName());
        item.setDescription(entity.getDescription());
        item.setCollaborationMode(entity.getCollaborationMode());
        item.setCollaborationModeLabel(modeLabel(entity.getCollaborationMode()));
        item.setCoordinatorAgentId(entity.getCoordinatorAgentId());
        item.setCoordinatorAgentName(agentName(entity.getCoordinatorAgentId()));
        item.setStatus(entity.getStatus());
        item.setStatusLabel(statusLabel(entity.getStatus()));
        item.setMemberCount(countMembers(entity.getId()));
        item.setRuns7d(countRuns(entity.getId(), false));
        item.setSuccess7d(countRuns(entity.getId(), true));
        item.setOwnerUserId(entity.getOwnerUserId());
        item.setCanManage(canManage(entity));
        item.setCreatedAt(entity.getCreatedAt());
        item.setUpdatedAt(entity.getUpdatedAt());
        return item;
    }

    /**
     * 转换团队详情。
     *
     * @param entity 团队实体
     * @return 团队详情
     */
    private AgentTeamDtos.TeamDetail toDetail(AgentTeamEntity entity) {
        AgentTeamDtos.TeamSummary summary = toSummary(entity);
        AgentTeamDtos.TeamDetail detail = new AgentTeamDtos.TeamDetail();
        detail.setId(summary.getId());
        detail.setTeamCode(summary.getTeamCode());
        detail.setTeamName(summary.getTeamName());
        detail.setDescription(summary.getDescription());
        detail.setCollaborationMode(summary.getCollaborationMode());
        detail.setCollaborationModeLabel(summary.getCollaborationModeLabel());
        detail.setCoordinatorAgentId(summary.getCoordinatorAgentId());
        detail.setCoordinatorAgentName(summary.getCoordinatorAgentName());
        detail.setStatus(summary.getStatus());
        detail.setStatusLabel(summary.getStatusLabel());
        detail.setMemberCount(summary.getMemberCount());
        detail.setRuns7d(summary.getRuns7d());
        detail.setSuccess7d(summary.getSuccess7d());
        detail.setOwnerUserId(summary.getOwnerUserId());
        detail.setCanManage(summary.getCanManage());
        detail.setCreatedAt(summary.getCreatedAt());
        detail.setUpdatedAt(summary.getUpdatedAt());
        detail.setMembers(listMembers(entity.getId()));
        detail.setRecentRuns(listRecentRuns(entity.getId()));
        return detail;
    }

    /**
     * 查询团队最近 10 次协作运行。
     *
     * @param teamId 团队 ID
     * @return 运行历史摘要
     */
    private List<AgentTeamDtos.RunHistoryItem> listRecentRuns(String teamId) {
        return jdbcTemplate.query("""
                        SELECT cr.id AS collaboration_run_id,
                               cr.run_id AS runtime_run_id,
                               cr.objective,
                               cr.final_result,
                               cr.status,
                               rr.total_tokens,
                               rr.latency_ms,
                               cr.started_at,
                               cr.finished_at
                        FROM agent_collaboration_run cr
                        LEFT JOIN runtime_run rr ON rr.id = cr.run_id
                        WHERE cr.team_id = ?
                        ORDER BY cr.started_at DESC
                        LIMIT 10
                        """,
                (rs, rowNum) -> {
                    AgentTeamDtos.RunHistoryItem item = new AgentTeamDtos.RunHistoryItem();
                    item.setCollaborationRunId(rs.getString("collaboration_run_id"));
                    item.setRuntimeRunId(rs.getString("runtime_run_id"));
                    item.setObjective(rs.getString("objective"));
                    item.setFinalResult(rs.getString("final_result"));
                    item.setStatus(rs.getString("status"));
                    item.setTotalTokens(rs.getObject("total_tokens", Integer.class));
                    item.setLatencyMs(rs.getObject("latency_ms", Integer.class));
                    item.setStartedAt(toLocalDateTime(rs.getTimestamp("started_at")));
                    item.setFinishedAt(toLocalDateTime(rs.getTimestamp("finished_at")));
                    return item;
                },
                teamId);
    }

    /**
     * 查询团队成员摘要。
     *
     * @param teamId 团队 ID
     * @return 成员摘要列表
     */
    private List<AgentTeamDtos.MemberSummary> listMembers(String teamId) {
        return agentTeamMemberMapper.selectList(new LambdaQueryWrapper<AgentTeamMemberEntity>()
                        .eq(AgentTeamMemberEntity::getTeamId, teamId)
                        .orderByAsc(AgentTeamMemberEntity::getSortOrder))
                .stream()
                .map(this::toMemberSummary)
                .toList();
    }

    /**
     * 转换成员摘要。
     *
     * @param member 成员实体
     * @return 成员摘要
     */
    private AgentTeamDtos.MemberSummary toMemberSummary(AgentTeamMemberEntity member) {
        AgentEntity agent = agentMapper.selectById(member.getAgentId());
        AgentTeamDtos.MemberSummary item = new AgentTeamDtos.MemberSummary();
        item.setTeamId(member.getTeamId());
        item.setAgentId(member.getAgentId());
        item.setAgentName(agent == null ? "" : safeText(agent.getAgentName()));
        item.setAgentType(agent == null ? "" : safeText(agent.getAgentType()));
        item.setModelName(agent == null ? "" : modelName(agent.getModelId()));
        item.setMemberRole(member.getMemberRole());
        item.setHandoffPolicy(member.getHandoffPolicy());
        item.setSortOrder(member.getSortOrder());
        item.setEnabled(Boolean.TRUE.equals(member.getEnabled()));
        item.setCanRun(agent != null && agentAccessService.canView(agent));
        return item;
    }

    /**
     * 初始化协作运行响应。
     *
     * @param team 团队实体
     * @param collaborationRun 协作运行记录
     * @param runtimeRun 顶层运行记录
     * @param request 运行请求
     * @return 协作运行响应
     */
    private AgentTeamDtos.RunResult initialRunResult(AgentTeamEntity team,
                                                     AgentCollaborationRunEntity collaborationRun,
                                                     RuntimeRunEntity runtimeRun,
                                                     AgentTeamDtos.RunRequest request) {
        AgentTeamDtos.RunResult result = new AgentTeamDtos.RunResult();
        result.setCollaborationRunId(collaborationRun.getId());
        result.setRuntimeRunId(runtimeRun.getId());
        result.setTeamId(team.getId());
        result.setTeamName(team.getTeamName());
        result.setObjective(request.getObjective());
        result.setStatus("RUNNING");
        result.setTotalTokens(0);
        result.setLatencyMs(0);
        return result;
    }

    /**
     * 初始化步骤响应。
     *
     * @param traceStep Trace Step
     * @param member 成员实体
     * @param agent Agent 实体
     * @param phase 阶段名称
     * @param input 输入内容
     * @return 步骤响应
     */
    private AgentTeamDtos.StepResult initialStepResult(RuntimeTraceStepEntity traceStep,
                                                       AgentTeamMemberEntity member,
                                                       AgentEntity agent,
                                                       String phase,
                                                       String input) {
        AgentTeamDtos.StepResult step = new AgentTeamDtos.StepResult();
        step.setTraceStepId(traceStep.getId());
        step.setAgentId(agent.getId());
        step.setAgentName(agent.getAgentName());
        step.setMemberRole(member.getMemberRole());
        step.setStepName(phase);
        step.setInput(input);
        step.setStatus("RUNNING");
        step.setTotalTokens(0);
        step.setLatencyMs(0);
        return step;
    }

    /**
     * 校验团队存在。
     *
     * @param id 团队 ID
     * @return 团队实体
     */
    private AgentTeamEntity requireTeam(String id) {
        AgentTeamEntity entity = agentTeamMapper.selectById(id);
        if (entity == null || "deleted".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException("AGENT_TEAM_NOT_FOUND", "协作团队不存在");
        }
        return entity;
    }

    /**
     * 校验 Agent 存在。
     *
     * @param id Agent ID
     * @return Agent 实体
     */
    private AgentEntity requireAgent(String id) {
        AgentEntity agent = agentMapper.selectById(id);
        if (agent == null || agent.getDeletedAt() != null) {
            throw new BusinessException("AGENT_NOT_FOUND", "成员 Agent 不存在");
        }
        return agent;
    }

    /**
     * 查询启用成员。
     *
     * @param teamId 团队 ID
     * @return 启用成员列表
     */
    private List<AgentTeamMemberEntity> enabledMembers(String teamId) {
        return agentTeamMemberMapper.selectList(new LambdaQueryWrapper<AgentTeamMemberEntity>()
                .eq(AgentTeamMemberEntity::getTeamId, teamId)
                .eq(AgentTeamMemberEntity::getEnabled, true)
                .orderByAsc(AgentTeamMemberEntity::getSortOrder));
    }

    /**
     * 当前用户是否可查看团队。
     *
     * @param entity 团队实体
     * @return 是否可查看
     */
    private boolean canView(AgentTeamEntity entity) {
        if (entity == null || "deleted".equalsIgnoreCase(entity.getStatus())) {
            return false;
        }
        return canManage(entity)
                || "published".equalsIgnoreCase(entity.getStatus())
                || hasAuthority("agent-team:view")
                || hasAuthority("agent-team:manage");
    }

    /**
     * 当前用户是否可管理团队。
     *
     * @param entity 团队实体
     * @return 是否可管理
     */
    private boolean canManage(AgentTeamEntity entity) {
        String userId = agentAccessService.currentUserId();
        return hasAuthority("ROLE_super_admin")
                || hasAuthority("ROLE_admin")
                || hasAuthority("agent-team:manage")
                || (StringUtils.hasText(userId) && (userId.equals(entity.getOwnerUserId()) || userId.equals(entity.getCreatedBy())));
    }

    /**
     * 校验当前用户可查看。
     *
     * @param entity 团队实体
     */
    private void assertCanView(AgentTeamEntity entity) {
        if (!canView(entity)) {
            throw new BusinessException("AGENT_TEAM_FORBIDDEN", "没有访问该协作团队的权限");
        }
    }

    /**
     * 校验当前用户可管理。
     *
     * @param entity 团队实体
     */
    private void assertCanManage(AgentTeamEntity entity) {
        if (!canManage(entity)) {
            throw new BusinessException("AGENT_TEAM_FORBIDDEN", "没有管理该协作团队的权限");
        }
    }

    /**
     * 判断当前用户是否具备指定权限。
     *
     * @param authority 权限标识
     * @return 是否具备权限
     */
    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(authority::equals);
    }

    /**
     * 当前登录用户 ID，不存在时抛出异常。
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
     * 查询团队成员数量。
     *
     * @param teamId 团队 ID
     * @return 成员数量
     */
    private Integer countMembers(String teamId) {
        Long count = agentTeamMemberMapper.selectCount(new LambdaQueryWrapper<AgentTeamMemberEntity>()
                .eq(AgentTeamMemberEntity::getTeamId, teamId));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 查询近 7 天协作运行次数。
     *
     * @param teamId 团队 ID
     * @param successOnly 是否只统计成功
     * @return 运行次数
     */
    private Integer countRuns(String teamId, boolean successOnly) {
        String sql = "SELECT COUNT(1) FROM agent_collaboration_run WHERE team_id = ? AND started_at >= DATE_SUB(NOW(3), INTERVAL 7 DAY)"
                + (successOnly ? " AND status = 'SUCCESS'" : "");
        Number count = jdbcTemplate.queryForObject(sql, Number.class, teamId);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 成员排序。
     *
     * @param members 成员列表
     * @return 排序后的成员
     */
    private List<AgentTeamMemberEntity> sortedMembers(List<AgentTeamMemberEntity> members) {
        return members.stream()
                .sorted(Comparator.comparing(member -> member.getSortOrder() == null ? 0 : member.getSortOrder()))
                .toList();
    }

    /**
     * 查找主控成员。
     *
     * @param team 团队实体
     * @param members 成员列表
     * @return 主控成员
     */
    private AgentTeamMemberEntity findCoordinator(AgentTeamEntity team, List<AgentTeamMemberEntity> members) {
        if (StringUtils.hasText(team.getCoordinatorAgentId())) {
            return members.stream()
                    .filter(member -> team.getCoordinatorAgentId().equals(member.getAgentId()))
                    .findFirst()
                    .orElse(null);
        }
        return members.stream()
                .filter(member -> "coordinator".equalsIgnoreCase(member.getMemberRole()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断成员是否是复核角色。
     *
     * @param member 成员实体
     * @return 是否复核角色
     */
    private boolean isReviewer(AgentTeamMemberEntity member) {
        String role = safeText(member.getMemberRole()).toLowerCase();
        return role.contains("review") || role.contains("audit") || role.contains("qa") || role.contains("复核");
    }

    /**
     * 构建成员输入。
     *
     * @param objective 协作目标
     * @param previousContext 前序上下文
     * @param member 成员配置
     * @return 成员输入
     */
    private String buildMemberInput(String objective, String previousContext, AgentTeamMemberEntity member) {
        return "协作目标：\n" + objective
                + "\n\n你的成员职责：" + safeText(member.getMemberRole())
                + "\n\n前序成员输出：\n" + (StringUtils.hasText(previousContext) ? previousContext : "暂无")
                + "\n\n交接策略：\n" + safeText(member.getHandoffPolicy())
                + "\n\n请基于你的职责给出可交付的阶段结果。";
    }

    /**
     * 构建复核输入。
     *
     * @param objective 协作目标
     * @param context 已有结果
     * @return 复核输入
     */
    private String buildReviewInput(String objective, String context) {
        return "协作目标：\n" + objective
                + "\n\n已有协作结果：\n" + (StringUtils.hasText(context) ? context : "暂无")
                + "\n\n请进行质量复核、补充遗漏，并输出最终建议。";
    }

    /**
     * 合并并行成员输出。
     *
     * @param objective 协作目标
     * @param steps 步骤列表
     * @return 合并文本
     */
    private String combineOutputs(String objective, List<AgentTeamDtos.StepResult> steps) {
        StringBuilder builder = new StringBuilder("协作目标：").append(objective).append("\n\n成员输出汇总：\n");
        for (AgentTeamDtos.StepResult step : steps) {
            builder.append("\n[").append(step.getAgentName()).append(" / ").append(step.getMemberRole()).append("]\n")
                    .append(safeText(step.getOutput())).append("\n");
        }
        return builder.toString();
    }

    /**
     * 统计步骤 Token。
     *
     * @param steps 步骤列表
     * @return 总 Token
     */
    private Integer sumTokens(List<AgentTeamDtos.StepResult> steps) {
        return steps.stream().mapToInt(step -> step.getTotalTokens() == null ? 0 : step.getTotalTokens()).sum();
    }

    /**
     * 获取最后一个非空输出。
     *
     * @param steps 步骤列表
     * @return 最后输出
     */
    private String lastOutput(List<AgentTeamDtos.StepResult> steps) {
        for (int index = steps.size() - 1; index >= 0; index--) {
            String output = steps.get(index).getOutput();
            if (StringUtils.hasText(output)) {
                return output;
            }
        }
        return "";
    }

    /**
     * 计算耗时。
     *
     * @param startedAt 开始时间
     * @param finishedAt 完成时间
     * @return 耗时毫秒
     */
    private Integer toLatency(LocalDateTime startedAt, LocalDateTime finishedAt) {
        return Math.toIntExact(Math.max(0, Duration.between(startedAt, finishedAt).toMillis()));
    }

    /**
     * 查询 Agent 名称。
     *
     * @param agentId Agent ID
     * @return Agent 名称
     */
    private String agentName(String agentId) {
        if (!StringUtils.hasText(agentId)) {
            return "";
        }
        AgentEntity agent = agentMapper.selectById(agentId);
        return agent == null ? "" : safeText(agent.getAgentName());
    }

    /**
     * 查询模型名称。
     *
     * @param modelId 模型 ID
     * @return 模型名称
     */
    private String modelName(String modelId) {
        if (!StringUtils.hasText(modelId)) {
            return "";
        }
        ModelConfigEntity model = modelConfigMapper.selectById(modelId);
        return model == null ? "" : safeText(model.getModelName());
    }

    /**
     * 协作模式归一化。
     *
     * @param mode 原始模式
     * @return 归一化模式
     */
    private String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return "sequential";
        }
        String value = mode.trim().toLowerCase();
        if (List.of("sequential", "parallel", "router", "supervisor", "reviewer").contains(value)) {
            return value;
        }
        return "sequential";
    }

    /**
     * 协作模式中文标签。
     *
     * @param mode 模式编码
     * @return 中文标签
     */
    private String modeLabel(String mode) {
        return switch (normalizeMode(mode)) {
            case "parallel" -> "并行协作";
            case "router" -> "路由分派";
            case "supervisor" -> "主控规划";
            case "reviewer" -> "产出复核";
            default -> "顺序协作";
        };
    }

    /**
     * 团队状态中文标签。
     *
     * @param status 状态编码
     * @return 中文标签
     */
    private String statusLabel(String status) {
        if ("published".equalsIgnoreCase(status)) {
            return "已发布";
        }
        if ("disabled".equalsIgnoreCase(status)) {
            return "已停用";
        }
        if ("deleted".equalsIgnoreCase(status)) {
            return "已删除";
        }
        return "草稿";
    }

    /**
     * 生成唯一团队编码。
     *
     * @param baseCode 基础编码
     * @return 唯一编码
     */
    private String uniqueTeamCode(String baseCode) {
        String normalized = StringUtils.hasText(baseCode) ? baseCode : "agent-team";
        String candidate = normalized;
        int suffix = 1;
        while (agentTeamMapper.selectCount(new LambdaQueryWrapper<AgentTeamEntity>()
                .eq(AgentTeamEntity::getTeamCode, candidate)) > 0) {
            candidate = normalized + "-" + suffix++;
        }
        return candidate;
    }

    /**
     * 将名称转成保守编码。
     *
     * @param text 名称文本
     * @return 编码文本
     */
    private String slugify(String text) {
        String cleaned = text == null ? "agent-team" : text.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-|-$", "");
        return StringUtils.hasText(cleaned) ? cleaned : "agent-team";
    }

    /**
     * 生成成员步骤键后缀。
     *
     * @param member 成员实体
     * @return 步骤键后缀
     */
    private String stepsKeySuffix(AgentTeamMemberEntity member) {
        return safeText(member.getAgentId()).replace("-", "").substring(0, Math.min(12, safeText(member.getAgentId()).replace("-", "").length()))
                + "_" + (member.getSortOrder() == null ? 0 : member.getSortOrder());
    }

    /**
     * 生成 UUID 主键。
     *
     * @return UUID 字符串
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 空文本兜底。
     *
     * @param text 原始文本
     * @return 非空文本
     */
    private String safeText(String text) {
        return text == null ? "" : text;
    }

    /**
     * 将 JDBC 时间戳转换为本地时间。
     *
     * @param timestamp JDBC 时间戳
     * @return 本地时间，空值返回 null
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * 转换 JSON 字符串。
     *
     * @param value 任意对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }
}
