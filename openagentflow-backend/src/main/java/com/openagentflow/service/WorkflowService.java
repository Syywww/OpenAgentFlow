package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.workflow.WorkflowDtos;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.AgentWorkflowBindingEntity;
import com.openagentflow.entity.WorkflowDefinitionEntity;
import com.openagentflow.entity.WorkflowEdgeEntity;
import com.openagentflow.entity.WorkflowNodeEntity;
import com.openagentflow.entity.WorkflowVersionEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.AgentWorkflowBindingMapper;
import com.openagentflow.mapper.WorkflowDefinitionMapper;
import com.openagentflow.mapper.WorkflowEdgeMapper;
import com.openagentflow.mapper.WorkflowNodeMapper;
import com.openagentflow.mapper.WorkflowVersionMapper;
import com.openagentflow.security.WorkspaceContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 工作流定义、发布版本和 Agent 绑定服务。
 */
@Service
public class WorkflowService {

    /** 工作流定义 Mapper。 */
    private final WorkflowDefinitionMapper workflowDefinitionMapper;

    /** 工作流节点 Mapper。 */
    private final WorkflowNodeMapper workflowNodeMapper;

    /** 工作流连线 Mapper。 */
    private final WorkflowEdgeMapper workflowEdgeMapper;

    /** 工作流版本 Mapper。 */
    private final WorkflowVersionMapper workflowVersionMapper;

    /** Agent 工作流绑定 Mapper。 */
    private final AgentWorkflowBindingMapper agentWorkflowBindingMapper;

    /** Agent Mapper，用于绑定校验。 */
    private final AgentMapper agentMapper;

    /** Agent 权限服务，用于复用 Agent 的查看和管理权限。 */
    private final AgentAccessService agentAccessService;

    /** 工作空间治理服务。 */
    private final WorkspaceGovernanceService workspaceGovernanceService;

    /** 资源访问控制服务，用于资源级 ACL 授权判定。 */
    private final ResourceAclService resourceAclService;

    /** JDBC 工具，用于轻量统计。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    /** 发布质量门禁服务。 */
    private final ReleaseGateService releaseGateService;

    /** 列表 data_scope 过滤组件。 */
    private final DataScopeListFilter dataScopeListFilter;

    public WorkflowService(WorkflowDefinitionMapper workflowDefinitionMapper,
                           WorkflowNodeMapper workflowNodeMapper,
                           WorkflowEdgeMapper workflowEdgeMapper,
                           WorkflowVersionMapper workflowVersionMapper,
                           AgentWorkflowBindingMapper agentWorkflowBindingMapper,
                           AgentMapper agentMapper,
                           AgentAccessService agentAccessService,
                           WorkspaceGovernanceService workspaceGovernanceService,
                           ResourceAclService resourceAclService,
                           JdbcTemplate jdbcTemplate,
                           ObjectMapper objectMapper,
                           ReleaseGateService releaseGateService,
                           DataScopeListFilter dataScopeListFilter) {
        this.workflowDefinitionMapper = workflowDefinitionMapper;
        this.workflowNodeMapper = workflowNodeMapper;
        this.workflowEdgeMapper = workflowEdgeMapper;
        this.workflowVersionMapper = workflowVersionMapper;
        this.agentWorkflowBindingMapper = agentWorkflowBindingMapper;
        this.agentMapper = agentMapper;
        this.agentAccessService = agentAccessService;
        this.workspaceGovernanceService = workspaceGovernanceService;
        this.resourceAclService = resourceAclService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.releaseGateService = releaseGateService;
        this.dataScopeListFilter = dataScopeListFilter;
    }

    /**
     * 查询当前用户可见的工作流列表。
     *
     * @return 工作流摘要列表
     */
    public List<WorkflowDtos.Summary> listWorkflows() {
        LambdaQueryWrapper<WorkflowDefinitionEntity> wrapper = new LambdaQueryWrapper<WorkflowDefinitionEntity>()
                .isNull(WorkflowDefinitionEntity::getDeletedAt)
                .orderByDesc(WorkflowDefinitionEntity::getUpdatedAt)
                .last("limit 200");
        // data_scope 下沉为列表 SQL 过滤；内存 canView 兜底，防 SQL 与内存语义漂移。
        DataScopeListFilter.ListFilter filter = dataScopeListFilter.buildListVisibilityFilter(
                WorkspaceContextHolder.current(), agentAccessService.currentUserId(), "workflow");
        if (filter != null && filter.requiresFilter()) {
            wrapper.apply(filter.sql(), filter.args().toArray());
        }
        return workflowDefinitionMapper.selectList(wrapper)
                .stream()
                .filter(this::canView)
                .map(this::toSummary)
                .toList();
    }

    /**
     * 查询工作流详情。
     *
     * @param id 工作流 ID
     * @return 工作流详情
     */
    public WorkflowDtos.Detail getWorkflow(String id) {
        WorkflowDefinitionEntity entity = requireWorkflow(id);
        assertCanView(entity);
        WorkflowDtos.Detail detail = new WorkflowDtos.Detail();
        copySummary(toSummary(entity), detail);
        detail.setGraphJson(parseJson(entity.getGraphJson(), Map.of()));
        detail.setVariableSchema(parseJson(entity.getVariableSchema(), Map.of()));
        detail.setNodes(listNodeDtos(id));
        detail.setEdges(listEdgeDtos(id));
        detail.setVersions(listVersions(id));
        return detail;
    }

    /**
     * 创建工作流。
     *
     * @param request 保存请求
     * @return 创建后的详情
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDtos.Detail createWorkflow(WorkflowDtos.Request request) {
        String userId = currentUserIdOrThrow();
        WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
        entity.setId(newId());
        fillWorkflow(entity, request, true);
        entity.setOwnerUserId(userId);
        entity.setCreatedBy(userId);
        entity.setWorkspaceId(workspaceGovernanceService.attachResource(request.getWorkspaceId(), "workflow", entity.getId(), userId));
        entity.setVersion(0L);
        workflowDefinitionMapper.insert(entity);
        saveGraph(entity.getId(), request);
        return getWorkflow(entity.getId());
    }

    /**
     * 更新工作流并替换节点、连线快照。
     *
     * @param id 工作流 ID
     * @param request 保存请求
     * @return 更新后的详情
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDtos.Detail updateWorkflow(String id, WorkflowDtos.Request request) {
        WorkflowDefinitionEntity entity = requireWorkflow(id);
        assertCanManage(entity);
        fillWorkflow(entity, request, false);
        entity.setVersion(entity.getVersion() == null ? 1L : entity.getVersion() + 1);
        workflowDefinitionMapper.updateById(entity);
        workflowNodeMapper.delete(new LambdaQueryWrapper<WorkflowNodeEntity>().eq(WorkflowNodeEntity::getWorkflowId, id));
        workflowEdgeMapper.delete(new LambdaQueryWrapper<WorkflowEdgeEntity>().eq(WorkflowEdgeEntity::getWorkflowId, id));
        saveGraph(id, request);
        return getWorkflow(id);
    }

    /**
     * 软删除工作流。
     *
     * @param id 工作流 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkflow(String id) {
        WorkflowDefinitionEntity entity = requireWorkflow(id);
        assertCanManage(entity);
        entity.setStatus("deleted");
        entity.setDeletedAt(LocalDateTime.now());
        workflowDefinitionMapper.updateById(entity);
    }

    /**
     * 发布工作流版本。
     *
     * @param id 工作流 ID
     * @param request 发布请求
     * @return 发布后的工作流详情
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDtos.Detail publishWorkflow(String id, WorkflowDtos.PublishRequest request) {
        WorkflowDefinitionEntity entity = requireWorkflow(id);
        assertCanManage(entity);
        String versionNo = request != null && StringUtils.hasText(request.getVersionNo())
                ? request.getVersionNo()
                : "v" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        releaseGateService.assertCanRelease("workflow", entity.getId(), entity.getWorkspaceId(), versionNo);

        WorkflowVersionEntity version = new WorkflowVersionEntity();
        version.setId(newId());
        version.setWorkflowId(entity.getId());
        version.setVersionNo(versionNo);
        version.setGraphJson(entity.getGraphJson());
        version.setVariableSchema(entity.getVariableSchema());
        version.setPublishEnv(request == null || !StringUtils.hasText(request.getPublishEnv()) ? "dev" : request.getPublishEnv());
        version.setPublishNote(request == null ? "" : request.getPublishNote());
        version.setStatus("published");
        version.setCreatedBy(currentUserId());
        workflowVersionMapper.insert(version);

        entity.setStatus("published");
        entity.setPublishedVersion(versionNo);
        workflowDefinitionMapper.updateById(entity);
        return getWorkflow(id);
    }

    /**
     * 查询 Agent 已绑定的工作流。
     *
     * @param agentId Agent ID
     * @return 绑定列表
     */
    public List<WorkflowDtos.BindingSummary> listAgentWorkflowBindings(String agentId) {
        AgentEntity agent = requireAgent(agentId);
        agentAccessService.assertCanView(agent);
        return agentWorkflowBindingMapper.selectList(new LambdaQueryWrapper<AgentWorkflowBindingEntity>()
                        .eq(AgentWorkflowBindingEntity::getAgentId, agentId)
                        .eq(AgentWorkflowBindingEntity::getEnabled, true))
                .stream()
                .map(this::toBindingSummary)
                .toList();
    }

    /**
     * 保存 Agent 与工作流绑定。
     *
     * @param agentId Agent ID
     * @param request 绑定请求
     * @return 保存后的绑定列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<WorkflowDtos.BindingSummary> saveAgentWorkflowBindings(String agentId, WorkflowDtos.BindingRequest request) {
        AgentEntity agent = requireAgent(agentId);
        agentAccessService.assertCanManage(agent);
        agentWorkflowBindingMapper.delete(new LambdaQueryWrapper<AgentWorkflowBindingEntity>()
                .eq(AgentWorkflowBindingEntity::getAgentId, agentId));
        Set<String> workflowIds = new LinkedHashSet<>(request == null || request.getWorkflowIds() == null ? List.of() : request.getWorkflowIds());
        for (String workflowId : workflowIds) {
            WorkflowDefinitionEntity workflow = requireWorkflow(workflowId);
            assertCanView(workflow);
            AgentWorkflowBindingEntity binding = new AgentWorkflowBindingEntity();
            binding.setAgentId(agentId);
            binding.setWorkflowId(workflowId);
            binding.setTriggerMode(request == null || !StringUtils.hasText(request.getTriggerMode()) ? "agent_run" : request.getTriggerMode());
            binding.setEnabled(true);
            agentWorkflowBindingMapper.insert(binding);
        }
        return listAgentWorkflowBindings(agentId);
    }

    /**
     * 查找 Agent 运行时优先触发的工作流。
     *
     * @param agentId Agent ID
     * @return 工作流定义；没有绑定时返回 null
     */
    public WorkflowDefinitionEntity findEnabledWorkflowForAgent(String agentId) {
        if (!StringUtils.hasText(agentId)) {
            return null;
        }
        List<AgentWorkflowBindingEntity> bindings = agentWorkflowBindingMapper.selectList(new LambdaQueryWrapper<AgentWorkflowBindingEntity>()
                .eq(AgentWorkflowBindingEntity::getAgentId, agentId)
                .eq(AgentWorkflowBindingEntity::getEnabled, true)
                .orderByDesc(AgentWorkflowBindingEntity::getCreatedAt));
        for (AgentWorkflowBindingEntity binding : bindings) {
            WorkflowDefinitionEntity workflow = workflowDefinitionMapper.selectById(binding.getWorkflowId());
            if (workflow != null
                    && workflow.getDeletedAt() == null
                    && !"disabled".equalsIgnoreCase(workflow.getStatus())
                    && !"deleted".equalsIgnoreCase(workflow.getStatus())) {
                return workflow;
            }
        }
        return null;
    }

    /**
     * 查询工作流实体。
     *
     * @param id 工作流 ID
     * @return 工作流实体
     */
    public WorkflowDefinitionEntity requireWorkflow(String id) {
        WorkflowDefinitionEntity entity = workflowDefinitionMapper.selectById(id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new BusinessException("WORKFLOW_NOT_FOUND", "工作流不存在");
        }
        return entity;
    }

    /**
     * 判断当前用户是否可查看工作流。
     *
     * @param entity 工作流实体
     * @return 是否可查看
     */
    public boolean canView(WorkflowDefinitionEntity entity) {
        if (entity == null || entity.getDeletedAt() != null) {
            return false;
        }
        return resourceAclService.currentUserHasAcl(entity.getWorkspaceId(), "workflow", entity.getId(),
                List.of("owner", "write", "run", "read"))
                || workspaceGovernanceService.canViewResource(
                "workflow",
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getOwnerUserId(),
                entity.getCreatedBy(),
                entity.getVisibility());
    }

    /**
     * 判断当前用户是否可管理工作流。
     *
     * @param entity 工作流实体
     * @return 是否可管理
     */
    public boolean canManage(WorkflowDefinitionEntity entity) {
        if (entity == null || entity.getDeletedAt() != null) {
            return false;
        }
        return resourceAclService.currentUserHasAcl(entity.getWorkspaceId(), "workflow", entity.getId(),
                List.of("owner", "write"))
                || workspaceGovernanceService.canManageResource("workflow", entity.getWorkspaceId(), entity.getOwnerUserId(), entity.getCreatedBy());
    }

    /**
     * 获取节点 DTO 列表。
     *
     * @param workflowId 工作流 ID
     * @return 节点 DTO 列表
     */
    public List<WorkflowDtos.NodeDto> listNodeDtos(String workflowId) {
        return workflowNodeMapper.selectList(new LambdaQueryWrapper<WorkflowNodeEntity>()
                        .eq(WorkflowNodeEntity::getWorkflowId, workflowId)
                        .orderByAsc(WorkflowNodeEntity::getCreatedAt))
                .stream()
                .map(this::toNodeDto)
                .toList();
    }

    /**
     * 获取连线 DTO 列表。
     *
     * @param workflowId 工作流 ID
     * @return 连线 DTO 列表
     */
    public List<WorkflowDtos.EdgeDto> listEdgeDtos(String workflowId) {
        return workflowEdgeMapper.selectList(new LambdaQueryWrapper<WorkflowEdgeEntity>()
                        .eq(WorkflowEdgeEntity::getWorkflowId, workflowId)
                        .orderByAsc(WorkflowEdgeEntity::getCreatedAt))
                .stream()
                .map(this::toEdgeDto)
                .toList();
    }

    /**
     * 校验工作流查看权限。
     *
     * @param entity 工作流实体
     */
    private void assertCanView(WorkflowDefinitionEntity entity) {
        if (!canView(entity)) {
            throw new BusinessException("WORKFLOW_FORBIDDEN", "没有访问该工作流的权限");
        }
    }

    /**
     * 校验工作流管理权限。
     *
     * @param entity 工作流实体
     */
    private void assertCanManage(WorkflowDefinitionEntity entity) {
        if (!canManage(entity)) {
            throw new BusinessException("WORKFLOW_FORBIDDEN", "没有管理该工作流的权限");
        }
    }

    /**
     * 填充工作流定义实体。
     *
     * @param entity 工作流实体
     * @param request 保存请求
     * @param create 是否创建场景
     */
    private void fillWorkflow(WorkflowDefinitionEntity entity, WorkflowDtos.Request request, boolean create) {
        if (request == null || !StringUtils.hasText(request.getWorkflowName())) {
            throw new BusinessException("WORKFLOW_NAME_EMPTY", "工作流名称不能为空");
        }
        String code = StringUtils.hasText(request.getWorkflowCode()) ? request.getWorkflowCode().trim() : slugify(request.getWorkflowName());
        entity.setWorkflowCode(create ? uniqueWorkflowCode(code) : code);
        entity.setWorkflowName(request.getWorkflowName().trim());
        entity.setDescription(request.getDescription());
        entity.setWorkflowType(StringUtils.hasText(request.getWorkflowType()) ? request.getWorkflowType() : "agent_workflow");
        if (!create && StringUtils.hasText(request.getWorkspaceId())) {
            entity.setWorkspaceId(workspaceGovernanceService.attachResource(request.getWorkspaceId(), "workflow", entity.getId(), entity.getOwnerUserId()));
        }
        entity.setGraphJson(toJson(request.getGraphJson() == null ? buildGraph(request) : request.getGraphJson()));
        entity.setVariableSchema(toJson(request.getVariableSchema() == null ? Map.of() : request.getVariableSchema()));
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "draft");
        entity.setVisibility(StringUtils.hasText(request.getVisibility()) ? request.getVisibility() : "private");
    }

    /**
     * 保存节点和连线快照。
     *
     * @param workflowId 工作流 ID
     * @param request 保存请求
     */
    private void saveGraph(String workflowId, WorkflowDtos.Request request) {
        // 前端显式传入空数组时保留空画布；只有旧调用未传节点字段时才补默认最小链路。
        List<WorkflowDtos.NodeDto> nodes = request.getNodes() == null ? defaultNodes() : request.getNodes();
        List<WorkflowDtos.EdgeDto> edges = request.getEdges() == null ? defaultEdges() : request.getEdges();
        for (WorkflowDtos.NodeDto dto : nodes) {
            WorkflowNodeEntity node = new WorkflowNodeEntity();
            node.setId(newId());
            node.setWorkflowId(workflowId);
            node.setNodeKey(StringUtils.hasText(dto.getNodeKey()) ? dto.getNodeKey() : newId());
            node.setNodeName(StringUtils.hasText(dto.getNodeName()) ? dto.getNodeName() : node.getNodeKey());
            node.setNodeType(StringUtils.hasText(dto.getNodeType()) ? dto.getNodeType().toUpperCase(Locale.ROOT) : "LLM");
            node.setPositionX(BigDecimal.valueOf(dto.getPositionX() == null ? 0D : dto.getPositionX()));
            node.setPositionY(BigDecimal.valueOf(dto.getPositionY() == null ? 0D : dto.getPositionY()));
            node.setConfigJson(toJson(dto.getConfigJson() == null ? Map.of() : dto.getConfigJson()));
            node.setInputSchema(toJson(dto.getInputSchema() == null ? Map.of() : dto.getInputSchema()));
            node.setOutputSchema(toJson(dto.getOutputSchema() == null ? Map.of() : dto.getOutputSchema()));
            node.setRetryPolicy(toJson(dto.getRetryPolicy() == null ? Map.of() : dto.getRetryPolicy()));
            node.setEnabled(dto.getEnabled() == null || dto.getEnabled());
            workflowNodeMapper.insert(node);
        }
        for (WorkflowDtos.EdgeDto dto : edges) {
            WorkflowEdgeEntity edge = new WorkflowEdgeEntity();
            edge.setId(newId());
            edge.setWorkflowId(workflowId);
            edge.setEdgeKey(StringUtils.hasText(dto.getEdgeKey()) ? dto.getEdgeKey() : "edge_" + newId());
            edge.setSourceNodeKey(dto.getSourceNodeKey());
            edge.setTargetNodeKey(dto.getTargetNodeKey());
            edge.setConditionExpr(dto.getConditionExpr());
            edge.setLabel(dto.getLabel());
            edge.setMetadata(toJson(dto.getMetadata() == null ? Map.of() : dto.getMetadata()));
            workflowEdgeMapper.insert(edge);
        }
    }

    /**
     * 根据节点和连线构造画布 JSON。
     *
     * @param request 保存请求
     * @return 画布 JSON 对象
     */
    private Map<String, Object> buildGraph(WorkflowDtos.Request request) {
        return Map.of(
                "nodes", request.getNodes() == null ? defaultNodes() : request.getNodes(),
                "edges", request.getEdges() == null ? defaultEdges() : request.getEdges()
        );
    }

    /**
     * 默认节点，保证新建工作流可以直接运行最小链路。
     *
     * @return 默认节点列表
     */
    private List<WorkflowDtos.NodeDto> defaultNodes() {
        List<WorkflowDtos.NodeDto> nodes = new ArrayList<>();
        nodes.add(node("start", "开始", "START", 40D, 100D, Map.of()));
        nodes.add(node("llm", "LLM 生成", "LLM", 280D, 100D, Map.of("promptTemplate", "{{input}}")));
        nodes.add(node("end", "结束", "END", 520D, 100D, Map.of()));
        return nodes;
    }

    /**
     * 默认连线。
     *
     * @return 默认连线列表
     */
    private List<WorkflowDtos.EdgeDto> defaultEdges() {
        WorkflowDtos.EdgeDto first = edge("e_start_llm", "start", "llm", "");
        WorkflowDtos.EdgeDto second = edge("e_llm_end", "llm", "end", "");
        return List.of(first, second);
    }

    /**
     * 构造默认节点。
     */
    private WorkflowDtos.NodeDto node(String key, String name, String type, Double x, Double y, Map<String, Object> config) {
        WorkflowDtos.NodeDto node = new WorkflowDtos.NodeDto();
        node.setNodeKey(key);
        node.setNodeName(name);
        node.setNodeType(type);
        node.setPositionX(x);
        node.setPositionY(y);
        node.setConfigJson(config);
        node.setEnabled(true);
        return node;
    }

    /**
     * 构造默认连线。
     */
    private WorkflowDtos.EdgeDto edge(String key, String source, String target, String label) {
        WorkflowDtos.EdgeDto edge = new WorkflowDtos.EdgeDto();
        edge.setEdgeKey(key);
        edge.setSourceNodeKey(source);
        edge.setTargetNodeKey(target);
        edge.setLabel(label);
        return edge;
    }

    /**
     * 查询发布版本列表。
     *
     * @param workflowId 工作流 ID
     * @return 版本摘要
     */
    private List<WorkflowDtos.VersionSummary> listVersions(String workflowId) {
        return workflowVersionMapper.selectList(new LambdaQueryWrapper<WorkflowVersionEntity>()
                        .eq(WorkflowVersionEntity::getWorkflowId, workflowId)
                        .orderByDesc(WorkflowVersionEntity::getCreatedAt)
                        .last("limit 20"))
                .stream()
                .map(this::toVersionSummary)
                .toList();
    }

    /**
     * 转换摘要。
     *
     * @param entity 工作流实体
     * @return 工作流摘要
     */
    private WorkflowDtos.Summary toSummary(WorkflowDefinitionEntity entity) {
        WorkflowDtos.Summary summary = new WorkflowDtos.Summary();
        summary.setId(entity.getId());
        summary.setWorkflowCode(entity.getWorkflowCode());
        summary.setWorkflowName(entity.getWorkflowName());
        summary.setDescription(entity.getDescription());
        summary.setWorkflowType(entity.getWorkflowType());
        summary.setWorkspaceId(entity.getWorkspaceId());
        summary.setWorkspaceName(findWorkspaceName(entity.getWorkspaceId()));
        summary.setStatus(entity.getStatus());
        summary.setStatusLabel(statusLabel(entity.getStatus()));
        summary.setPublishedVersion(entity.getPublishedVersion());
        summary.setVisibility(entity.getVisibility());
        summary.setOwnerUserId(entity.getOwnerUserId());
        summary.setNodeCount(countNodes(entity.getId()));
        summary.setCanManage(canManage(entity));
        summary.setCreatedAt(entity.getCreatedAt());
        summary.setUpdatedAt(entity.getUpdatedAt());
        return summary;
    }

    /**
     * 拷贝摘要字段。
     */
    private void copySummary(WorkflowDtos.Summary source, WorkflowDtos.Summary target) {
        target.setId(source.getId());
        target.setWorkflowCode(source.getWorkflowCode());
        target.setWorkflowName(source.getWorkflowName());
        target.setDescription(source.getDescription());
        target.setWorkflowType(source.getWorkflowType());
        target.setWorkspaceId(source.getWorkspaceId());
        target.setWorkspaceName(source.getWorkspaceName());
        target.setStatus(source.getStatus());
        target.setStatusLabel(source.getStatusLabel());
        target.setPublishedVersion(source.getPublishedVersion());
        target.setVisibility(source.getVisibility());
        target.setOwnerUserId(source.getOwnerUserId());
        target.setNodeCount(source.getNodeCount());
        target.setCanManage(source.getCanManage());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    /**
     * 转换节点 DTO。
     */
    private WorkflowDtos.NodeDto toNodeDto(WorkflowNodeEntity entity) {
        WorkflowDtos.NodeDto dto = new WorkflowDtos.NodeDto();
        dto.setId(entity.getId());
        dto.setNodeKey(entity.getNodeKey());
        dto.setNodeName(entity.getNodeName());
        dto.setNodeType(entity.getNodeType());
        dto.setPositionX(entity.getPositionX() == null ? 0D : entity.getPositionX().doubleValue());
        dto.setPositionY(entity.getPositionY() == null ? 0D : entity.getPositionY().doubleValue());
        dto.setConfigJson(parseJson(entity.getConfigJson(), Map.of()));
        dto.setInputSchema(parseJson(entity.getInputSchema(), Map.of()));
        dto.setOutputSchema(parseJson(entity.getOutputSchema(), Map.of()));
        dto.setRetryPolicy(parseJson(entity.getRetryPolicy(), Map.of()));
        dto.setEnabled(entity.getEnabled());
        return dto;
    }

    /**
     * 转换连线 DTO。
     */
    private WorkflowDtos.EdgeDto toEdgeDto(WorkflowEdgeEntity entity) {
        WorkflowDtos.EdgeDto dto = new WorkflowDtos.EdgeDto();
        dto.setId(entity.getId());
        dto.setEdgeKey(entity.getEdgeKey());
        dto.setSourceNodeKey(entity.getSourceNodeKey());
        dto.setTargetNodeKey(entity.getTargetNodeKey());
        dto.setConditionExpr(entity.getConditionExpr());
        dto.setLabel(entity.getLabel());
        dto.setMetadata(parseJson(entity.getMetadata(), Map.of()));
        return dto;
    }

    /**
     * 转换版本摘要。
     */
    private WorkflowDtos.VersionSummary toVersionSummary(WorkflowVersionEntity entity) {
        WorkflowDtos.VersionSummary summary = new WorkflowDtos.VersionSummary();
        summary.setId(entity.getId());
        summary.setVersionNo(entity.getVersionNo());
        summary.setPublishEnv(entity.getPublishEnv());
        summary.setPublishNote(entity.getPublishNote());
        summary.setStatus(entity.getStatus());
        summary.setCreatedAt(entity.getCreatedAt());
        return summary;
    }

    /**
     * 转换绑定摘要。
     */
    private WorkflowDtos.BindingSummary toBindingSummary(AgentWorkflowBindingEntity binding) {
        WorkflowDefinitionEntity workflow = workflowDefinitionMapper.selectById(binding.getWorkflowId());
        WorkflowDtos.BindingSummary summary = new WorkflowDtos.BindingSummary();
        summary.setAgentId(binding.getAgentId());
        summary.setWorkflowId(binding.getWorkflowId());
        summary.setWorkflowName(workflow == null ? "" : workflow.getWorkflowName());
        summary.setWorkflowCode(workflow == null ? "" : workflow.getWorkflowCode());
        summary.setTriggerMode(binding.getTriggerMode());
        summary.setEnabled(binding.getEnabled());
        return summary;
    }

    /**
     * 查询 Agent 实体。
     */
    private AgentEntity requireAgent(String id) {
        AgentEntity entity = agentMapper.selectById(id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new BusinessException("AGENT_NOT_FOUND", "Agent 不存在");
        }
        return entity;
    }

    /**
     * 工作流状态中文标签。
     */
    private String statusLabel(String status) {
        if ("published".equalsIgnoreCase(status)) {
            return "已发布";
        }
        if ("draft".equalsIgnoreCase(status)) {
            return "草稿";
        }
        if ("disabled".equalsIgnoreCase(status)) {
            return "已停用";
        }
        if ("deleted".equalsIgnoreCase(status)) {
            return "已删除";
        }
        return StringUtils.hasText(status) ? status : "未知";
    }

    /**
     * 统计节点数量。
     */
    private Integer countNodes(String workflowId) {
        Number count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM workflow_node WHERE workflow_id = ?", Number.class, workflowId);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 生成唯一工作流编码。
     */
    private String uniqueWorkflowCode(String baseCode) {
        String normalized = StringUtils.hasText(baseCode) ? baseCode : "workflow";
        String candidate = normalized;
        int suffix = 1;
        while (workflowDefinitionMapper.selectCount(new LambdaQueryWrapper<WorkflowDefinitionEntity>()
                .eq(WorkflowDefinitionEntity::getWorkflowCode, candidate)) > 0) {
            candidate = normalized + "-" + suffix++;
        }
        return candidate;
    }

    /**
     * 将名称转换为保守编码。
     */
    private String slugify(String text) {
        String cleaned = text == null ? "workflow" : text.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-|-$", "");
        return StringUtils.hasText(cleaned) ? cleaned : "workflow";
    }

    /**
     * 查询工作空间展示名称。
     *
     * @param workspaceId 工作空间 ID
     * @return 工作空间名称
     */
    private String findWorkspaceName(String workspaceId) {
        if (!StringUtils.hasText(workspaceId)) {
            return "";
        }
        List<String> names = jdbcTemplate.queryForList(
                "SELECT workspace_name FROM oaf_workspace WHERE id = ? LIMIT 1",
                String.class,
                workspaceId);
        return names.isEmpty() ? "" : names.get(0);
    }

    /**
     * 判断当前用户是否系统管理员。
     */
    private boolean isSystemManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> List.of("ROLE_super_admin", "ROLE_admin", "workflow:manage", "agent:manage").contains(authority));
    }

    /**
     * 获取当前用户 ID。
     */
    private String currentUserId() {
        return agentAccessService.currentUserId();
    }

    /**
     * 获取当前用户 ID，未登录时抛出异常。
     */
    private String currentUserIdOrThrow() {
        String userId = currentUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("UNAUTHORIZED", "请先登录");
        }
        return userId;
    }

    /**
     * 解析 JSON 字符串。
     */
    private Object parseJson(String json, Object fallback) {
        try {
            if (!StringUtils.hasText(json)) {
                return fallback;
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception exception) {
            return fallback;
        }
    }

    /**
     * 转换 JSON 字符串。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    /**
     * 生成 UUID 主键。
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }
}
