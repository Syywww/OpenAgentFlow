package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.mcp.McpDtos;
import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.entity.McpCapabilityEntity;
import com.openagentflow.entity.McpConnectionTestEntity;
import com.openagentflow.entity.McpDiscoveryTaskEntity;
import com.openagentflow.entity.McpServerEntity;
import com.openagentflow.entity.ToolDefinitionEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.McpCapabilityMapper;
import com.openagentflow.mapper.McpConnectionTestMapper;
import com.openagentflow.mapper.McpDiscoveryTaskMapper;
import com.openagentflow.mapper.McpServerMapper;
import com.openagentflow.mapper.ToolDefinitionMapper;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * MCP Server 应用服务。
 */
@Service
public class McpServerService implements DistributedTaskHandler {

    /** MCP Server Mapper。 */
    private final McpServerMapper mcpServerMapper;

    /** MCP 能力 Mapper。 */
    private final McpCapabilityMapper mcpCapabilityMapper;

    /** MCP 连接测试 Mapper。 */
    private final McpConnectionTestMapper mcpConnectionTestMapper;

    /** MCP 发现任务 Mapper。 */
    private final McpDiscoveryTaskMapper mcpDiscoveryTaskMapper;

    /** 工具定义 Mapper，用于把 MCP tool 同步进工具中心。 */
    private final ToolDefinitionMapper toolDefinitionMapper;

    /** MCP 客户端服务。 */
    private final McpClientService mcpClientService;

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    /** 统一异步任务中心。 */
    private final AsyncTaskService asyncTaskService;

    /** Kafka 任务工具类。 */
    private final KafkaTaskClient kafkaTaskClient;

    public McpServerService(McpServerMapper mcpServerMapper,
                            McpCapabilityMapper mcpCapabilityMapper,
                            McpConnectionTestMapper mcpConnectionTestMapper,
                            McpDiscoveryTaskMapper mcpDiscoveryTaskMapper,
                            ToolDefinitionMapper toolDefinitionMapper,
                            McpClientService mcpClientService,
                            ObjectMapper objectMapper,
                            AsyncTaskService asyncTaskService,
                            KafkaTaskClient kafkaTaskClient) {
        this.mcpServerMapper = mcpServerMapper;
        this.mcpCapabilityMapper = mcpCapabilityMapper;
        this.mcpConnectionTestMapper = mcpConnectionTestMapper;
        this.mcpDiscoveryTaskMapper = mcpDiscoveryTaskMapper;
        this.toolDefinitionMapper = toolDefinitionMapper;
        this.mcpClientService = mcpClientService;
        this.objectMapper = objectMapper;
        this.asyncTaskService = asyncTaskService;
        this.kafkaTaskClient = kafkaTaskClient;
    }

    /**
     * 查询 MCP Server 列表。
     *
     * @return MCP Server 摘要列表
     */
    public List<McpDtos.ServerSummary> listServers() {
        return mcpServerMapper.selectList(new LambdaQueryWrapper<McpServerEntity>()
                        .isNull(McpServerEntity::getDeletedAt)
                        .orderByDesc(McpServerEntity::getUpdatedAt)
                        .last("limit 100"))
                .stream()
                .filter(this::canView)
                .map(this::toSummary)
                .toList();
    }

    /**
     * 查询 MCP Server 详情。
     *
     * @param id MCP Server ID
     * @return MCP Server 详情
     */
    public McpDtos.ServerDetail getServer(String id) {
        McpServerEntity entity = requireServer(id);
        if (!canView(entity)) {
            throw new BusinessException("MCP_FORBIDDEN", "没有访问该 MCP Server 的权限");
        }
        McpDtos.ServerDetail detail = toDetail(entity);
        detail.setCapabilities(listCapabilities(id));
        detail.setLastTest(latestTest(id));
        return detail;
    }

    /**
     * 创建 MCP Server。
     *
     * @param request 创建请求
     * @return 创建后的详情
     */
    @Transactional(rollbackFor = Exception.class)
    public McpDtos.ServerDetail createServer(McpDtos.ServerRequest request) {
        String userId = currentUserIdOrThrow();
        McpServerEntity entity = new McpServerEntity();
        entity.setId(newId());
        fillServer(entity, request, true);
        entity.setOwnerUserId(userId);
        entity.setCreatedBy(userId);
        entity.setVersion(0L);
        mcpServerMapper.insert(entity);
        return getServer(entity.getId());
    }

    /**
     * 更新 MCP Server。
     *
     * @param id MCP Server ID
     * @param request 更新请求
     * @return 更新后的详情
     */
    @Transactional(rollbackFor = Exception.class)
    public McpDtos.ServerDetail updateServer(String id, McpDtos.ServerRequest request) {
        McpServerEntity entity = requireServer(id);
        assertCanManage(entity);
        fillServer(entity, request, false);
        entity.setVersion(entity.getVersion() == null ? 1L : entity.getVersion() + 1);
        mcpServerMapper.updateById(entity);
        return getServer(id);
    }

    /**
     * 软删除 MCP Server，并停用它同步出来的 MCP 工具。
     *
     * @param id MCP Server ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteServer(String id) {
        McpServerEntity entity = requireServer(id);
        assertCanManage(entity);
        entity.setStatus("deleted");
        entity.setDeletedAt(LocalDateTime.now());
        mcpServerMapper.updateById(entity);

        // 删除 Server 后不物理删除工具，保留历史日志可追溯，但统一停用避免继续调用。
        List<ToolDefinitionEntity> tools = toolDefinitionMapper.selectList(new LambdaQueryWrapper<ToolDefinitionEntity>()
                .eq(ToolDefinitionEntity::getMcpServerId, id)
                .isNull(ToolDefinitionEntity::getDeletedAt));
        for (ToolDefinitionEntity tool : tools) {
            tool.setEnabled(false);
            tool.setStatus("disabled");
            toolDefinitionMapper.updateById(tool);
        }
    }

    /**
     * 测试 MCP Server 连接。
     *
     * @param id MCP Server ID
     * @return 连接测试结果
     */
    @Transactional(rollbackFor = Exception.class)
    public McpDtos.ConnectionTestResult testServer(String id) {
        McpServerEntity entity = requireServer(id);
        assertCanManage(entity);
        McpClientService.McpConnectionResult result = mcpClientService.testConnection(entity);

        McpConnectionTestEntity test = new McpConnectionTestEntity();
        test.setId(newId());
        test.setServerId(id);
        test.setSuccess(Boolean.TRUE.equals(result.getSuccess()));
        test.setLatencyMs(result.getLatencyMs());
        test.setToolsCount(result.getToolsCount());
        test.setPromptsCount(result.getPromptsCount());
        test.setResourcesCount(result.getResourcesCount());
        test.setRequestPayload(toJson(Map.of("transportType", entity.getTransportType(), "endpointUrl", safeText(entity.getEndpointUrl()))));
        test.setResponsePayload(validJsonOrDefault(result.getResponsePayload(), "{}"));
        test.setErrorMessage(result.getErrorMessage());
        test.setTestedBy(currentUserId());
        test.setCreatedAt(LocalDateTime.now());
        mcpConnectionTestMapper.insert(test);

        // 连接成功即可认为 Server 运行中；失败时保留配置并标记 error，便于前端提示。
        entity.setStatus(Boolean.TRUE.equals(result.getSuccess()) ? "running" : "error");
        entity.setLastHeartbeatAt(Boolean.TRUE.equals(result.getSuccess()) ? LocalDateTime.now() : entity.getLastHeartbeatAt());
        mcpServerMapper.updateById(entity);
        return toConnectionTestResult(test);
    }

    /**
     * 发现 MCP 能力并同步工具中心。
     *
     * @param id MCP Server ID
     * @return 发现结果
     */
    @Transactional(rollbackFor = Exception.class)
    public McpDtos.DiscoveryResult discoverServer(String id) {
        McpServerEntity entity = requireServer(id);
        assertCanManage(entity);
        McpDiscoveryTaskEntity task = new McpDiscoveryTaskEntity();
        task.setId(newId());
        task.setServerId(id);
        task.setStatus("pending");
        task.setDiscoveredTools(0);
        task.setDiscoveredPrompts(0);
        task.setDiscoveredResources(0);
        task.setCreatedAt(LocalDateTime.now());
        mcpDiscoveryTaskMapper.insert(task);
        AsyncTaskEntity asyncTask = asyncTaskService.createTask(
                "发现 MCP 能力：" + entity.getServerName(),
                "MCP_DISCOVERY",
                "mcp_server",
                entity.getId(),
                "mcp_discovery_task",
                task.getId(),
                null,
                Map.of("serverId", entity.getId(), "discoveryTaskId", task.getId()));
        try {
            kafkaTaskClient.publish(asyncTask);
        } catch (Exception exception) {
            asyncTaskService.appendLog(asyncTask.getId(), "warn", "enqueue_failed",
                    "Kafka 首次投递失败，补偿调度器将自动重试", Map.of("error", exception.getMessage()), 0);
        }
        return toDiscoveryResult(task, List.of(), null);
    }

    /**
     * 返回 Kafka 任务类型。
     */
    @Override
    public String taskType() {
        return "MCP_DISCOVERY";
    }

    /**
     * 在 Kafka Worker 中发现 MCP 能力并同步工具中心。
     */
    @Override
    public Map<String, Object> executeDistributedTask(AsyncTaskEntity asyncTask) {
        McpDiscoveryTaskEntity task = mcpDiscoveryTaskMapper.selectById(asyncTask.getSourceId());
        if (task == null) {
            throw new BusinessException("MCP_DISCOVERY_TASK_NOT_FOUND", "MCP 发现任务不存在");
        }
        McpServerEntity entity = requireServer(task.getServerId());
        task.setStatus("running");
        task.setStartedAt(LocalDateTime.now());
        task.setFinishedAt(null);
        task.setErrorMessage(null);
        mcpDiscoveryTaskMapper.updateById(task);
        try {
            McpClientService.McpDiscoveryPayload payload = mcpClientService.discover(entity);
            List<McpCapabilityEntity> saved = saveCapabilities(entity, payload);
            syncTools(entity, payload.getTools());

            task.setStatus("success");
            task.setDiscoveredTools(payload.getTools().size());
            task.setDiscoveredPrompts(payload.getPrompts().size());
            task.setDiscoveredResources(payload.getResources().size());
            task.setFinishedAt(LocalDateTime.now());
            mcpDiscoveryTaskMapper.updateById(task);

            entity.setStatus("running");
            entity.setLastHeartbeatAt(LocalDateTime.now());
            mcpServerMapper.updateById(entity);
            return Map.of(
                    "serverId", entity.getId(),
                    "discoveryTaskId", task.getId(),
                    "toolsCount", payload.getTools().size(),
                    "promptsCount", payload.getPrompts().size(),
                    "resourcesCount", payload.getResources().size(),
                    "capabilityCount", saved.size());
        } catch (Exception exception) {
            task.setStatus("failed");
            task.setErrorMessage(exception.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            mcpDiscoveryTaskMapper.updateById(task);
            entity.setStatus("error");
            mcpServerMapper.updateById(entity);
            throw new IllegalStateException("MCP 能力发现失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 查询 MCP Server 已发现能力。
     *
     * @param serverId MCP Server ID
     * @return 能力摘要列表
     */
    public List<McpDtos.CapabilitySummary> listCapabilities(String serverId) {
        McpServerEntity server = requireServer(serverId);
        if (!canView(server)) {
            throw new BusinessException("MCP_FORBIDDEN", "没有访问该 MCP Server 的权限");
        }
        return mcpCapabilityMapper.selectList(new LambdaQueryWrapper<McpCapabilityEntity>()
                        .eq(McpCapabilityEntity::getServerId, serverId)
                        .orderByAsc(McpCapabilityEntity::getCapabilityType)
                        .orderByAsc(McpCapabilityEntity::getCapabilityName))
                .stream()
                .map(this::toCapabilitySummary)
                .toList();
    }

    /**
     * 保存发现到的 MCP 能力。
     *
     * @param server MCP Server 实体
     * @param payload 发现载荷
     * @return 保存后的能力实体
     */
    private List<McpCapabilityEntity> saveCapabilities(McpServerEntity server, McpClientService.McpDiscoveryPayload payload) {
        mcpCapabilityMapper.delete(new LambdaQueryWrapper<McpCapabilityEntity>()
                .eq(McpCapabilityEntity::getServerId, server.getId()));
        List<McpCapabilityEntity> saved = new ArrayList<>();
        List<McpClientService.McpCapabilitySpec> specs = new ArrayList<>();
        specs.addAll(payload.getTools());
        specs.addAll(payload.getPrompts());
        specs.addAll(payload.getResources());
        for (McpClientService.McpCapabilitySpec spec : specs) {
            McpCapabilityEntity capability = new McpCapabilityEntity();
            capability.setId(newId());
            capability.setServerId(server.getId());
            capability.setCapabilityType(spec.getType());
            capability.setCapabilityName(spec.getName());
            capability.setDescription(spec.getDescription());
            capability.setSchemaJson(validJsonOrDefault(spec.getSchemaJson(), "{}"));
            capability.setMetadata(validJsonOrDefault(spec.getMetadata(), "{}"));
            capability.setRiskLevel(spec.getRiskLevel());
            capability.setEnabled(!"high".equalsIgnoreCase(spec.getRiskLevel()));
            capability.setDiscoveredAt(LocalDateTime.now());
            mcpCapabilityMapper.insert(capability);
            saved.add(capability);
        }
        return saved;
    }

    /**
     * 将 MCP tools 同步成工具中心工具。
     *
     * @param server MCP Server 实体
     * @param tools MCP tool 规范列表
     */
    private void syncTools(McpServerEntity server, List<McpClientService.McpCapabilitySpec> tools) {
        for (McpClientService.McpCapabilitySpec spec : tools) {
            ToolDefinitionEntity tool = findSyncedTool(server.getId(), spec.getName());
            boolean create = tool == null;
            if (create) {
                tool = new ToolDefinitionEntity();
                tool.setId(newId());
                tool.setToolCode(uniqueToolCode(slugify("mcp_" + server.getServerCode() + "_" + spec.getName()), null));
                tool.setOwnerUserId(server.getOwnerUserId());
                tool.setCreatedBy(server.getCreatedBy());
                tool.setSourceType("mcp");
                tool.setVersion(0L);
            } else {
                tool.setVersion(tool.getVersion() == null ? 1L : tool.getVersion() + 1);
            }

            boolean highRisk = "high".equalsIgnoreCase(spec.getRiskLevel());
            tool.setToolName(server.getServerName() + " / " + spec.getName());
            tool.setToolType("MCP");
            tool.setDescription(spec.getDescription());
            tool.setRequestMethod("MCP");
            tool.setEndpointUrl(StringUtils.hasText(server.getEndpointUrl()) ? server.getEndpointUrl() : server.getCommand());
            tool.setAuthType(server.getAuthType());
            tool.setAuthConfig("{}");
            tool.setHeaders("{}");
            tool.setRequestSchema(validJsonOrDefault(spec.getSchemaJson(), "{\"type\":\"object\",\"properties\":{}}"));
            tool.setResponseSchema("{\"type\":\"object\"}");
            tool.setTimeoutMs(30000);
            tool.setRetryCount(0);
            tool.setRiskLevel(spec.getRiskLevel());
            tool.setRequireConfirm(highRisk);
            tool.setEnabled(create ? !highRisk : (!highRisk && Boolean.TRUE.equals(tool.getEnabled())));
            tool.setStatus(Boolean.TRUE.equals(tool.getEnabled()) ? "active" : "disabled");
            tool.setMcpServerId(server.getId());
            tool.setMcpToolName(spec.getName());

            if (create) {
                toolDefinitionMapper.insert(tool);
            } else {
                toolDefinitionMapper.updateById(tool);
            }
        }
    }

    /**
     * 查找已同步的 MCP 工具。
     *
     * @param serverId MCP Server ID
     * @param toolName MCP tool 名称
     * @return 工具定义实体
     */
    private ToolDefinitionEntity findSyncedTool(String serverId, String toolName) {
        return toolDefinitionMapper.selectOne(new LambdaQueryWrapper<ToolDefinitionEntity>()
                .eq(ToolDefinitionEntity::getMcpServerId, serverId)
                .eq(ToolDefinitionEntity::getMcpToolName, toolName)
                .isNull(ToolDefinitionEntity::getDeletedAt)
                .last("limit 1"));
    }

    /**
     * 填充 MCP Server 实体。
     *
     * @param entity MCP Server 实体
     * @param request 保存请求
     * @param create 是否创建场景
     */
    private void fillServer(McpServerEntity entity, McpDtos.ServerRequest request, boolean create) {
        String code = StringUtils.hasText(request.getServerCode()) ? request.getServerCode().trim() : slugify(request.getServerName());
        entity.setServerCode(create ? uniqueServerCode(code) : code);
        entity.setServerName(request.getServerName().trim());
        entity.setDescription(request.getDescription());
        entity.setTransportType(safeText(request.getTransportType()).toLowerCase(Locale.ROOT));
        entity.setCommand(request.getCommand());
        entity.setArgs(validJsonOrDefault(request.getArgs(), "[]"));
        entity.setEndpointUrl(request.getEndpointUrl());
        entity.setAuthType(StringUtils.hasText(request.getAuthType()) ? request.getAuthType() : "none");
        entity.setAuthConfig(validJsonOrDefault(request.getAuthConfig(), "{}"));
        entity.setEnvVars(validJsonOrDefault(request.getEnvVars(), "{}"));
        entity.setAllowedPaths(validJsonOrDefault(request.getAllowedPaths(), "[]"));
        entity.setRiskPolicy(validJsonOrDefault(request.getRiskPolicy(), "{\"highRiskDefault\":\"disabled_and_confirm\"}"));
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "stopped");
    }

    /**
     * 转换 Server 摘要。
     *
     * @param entity MCP Server 实体
     * @return Server 摘要
     */
    private McpDtos.ServerSummary toSummary(McpServerEntity entity) {
        McpDtos.ServerSummary summary = new McpDtos.ServerSummary();
        summary.setId(entity.getId());
        summary.setServerCode(entity.getServerCode());
        summary.setServerName(entity.getServerName());
        summary.setDescription(entity.getDescription());
        summary.setTransportType(entity.getTransportType());
        summary.setCommand(entity.getCommand());
        summary.setEndpointUrl(entity.getEndpointUrl());
        summary.setAuthType(entity.getAuthType());
        summary.setStatus(entity.getStatus());
        summary.setLastHeartbeatAt(entity.getLastHeartbeatAt());
        summary.setToolsCount(countCapabilities(entity.getId(), "tool"));
        summary.setPromptsCount(countCapabilities(entity.getId(), "prompt"));
        summary.setResourcesCount(countCapabilities(entity.getId(), "resource"));
        summary.setCanManage(canManage(entity));
        return summary;
    }

    /**
     * 转换 Server 详情。
     *
     * @param entity MCP Server 实体
     * @return Server 详情
     */
    private McpDtos.ServerDetail toDetail(McpServerEntity entity) {
        McpDtos.ServerDetail detail = new McpDtos.ServerDetail();
        McpDtos.ServerSummary summary = toSummary(entity);
        detail.setId(summary.getId());
        detail.setServerCode(summary.getServerCode());
        detail.setServerName(summary.getServerName());
        detail.setDescription(summary.getDescription());
        detail.setTransportType(summary.getTransportType());
        detail.setCommand(summary.getCommand());
        detail.setEndpointUrl(summary.getEndpointUrl());
        detail.setAuthType(summary.getAuthType());
        detail.setStatus(summary.getStatus());
        detail.setLastHeartbeatAt(summary.getLastHeartbeatAt());
        detail.setToolsCount(summary.getToolsCount());
        detail.setPromptsCount(summary.getPromptsCount());
        detail.setResourcesCount(summary.getResourcesCount());
        detail.setCanManage(summary.getCanManage());
        detail.setArgs(entity.getArgs());
        detail.setAuthConfig(entity.getAuthConfig());
        detail.setEnvVars(entity.getEnvVars());
        detail.setAllowedPaths(entity.getAllowedPaths());
        detail.setRiskPolicy(entity.getRiskPolicy());
        return detail;
    }

    /**
     * 转换 MCP 能力摘要。
     *
     * @param entity MCP 能力实体
     * @return 能力摘要
     */
    private McpDtos.CapabilitySummary toCapabilitySummary(McpCapabilityEntity entity) {
        McpDtos.CapabilitySummary summary = new McpDtos.CapabilitySummary();
        summary.setId(entity.getId());
        summary.setServerId(entity.getServerId());
        summary.setCapabilityType(entity.getCapabilityType());
        summary.setCapabilityName(entity.getCapabilityName());
        summary.setDescription(entity.getDescription());
        summary.setSchemaJson(entity.getSchemaJson());
        summary.setMetadata(entity.getMetadata());
        summary.setEnabled(entity.getEnabled());
        summary.setRiskLevel(entity.getRiskLevel());
        summary.setRiskLabel(riskLabel(entity.getRiskLevel()));
        summary.setDiscoveredAt(entity.getDiscoveredAt());
        return summary;
    }

    /**
     * 转换连接测试结果。
     *
     * @param entity 测试实体
     * @return 前端测试结果
     */
    private McpDtos.ConnectionTestResult toConnectionTestResult(McpConnectionTestEntity entity) {
        if (entity == null) {
            return null;
        }
        McpDtos.ConnectionTestResult result = new McpDtos.ConnectionTestResult();
        result.setSuccess(entity.getSuccess());
        result.setLatencyMs(entity.getLatencyMs());
        result.setToolsCount(entity.getToolsCount());
        result.setPromptsCount(entity.getPromptsCount());
        result.setResourcesCount(entity.getResourcesCount());
        result.setResponsePayload(entity.getResponsePayload());
        result.setErrorMessage(entity.getErrorMessage());
        result.setCreatedAt(entity.getCreatedAt());
        return result;
    }

    /**
     * 转换发现结果。
     *
     * @param task 发现任务
     * @param capabilities 已保存能力
     * @param errorMessage 错误信息
     * @return 发现结果
     */
    private McpDtos.DiscoveryResult toDiscoveryResult(McpDiscoveryTaskEntity task,
                                                      List<McpCapabilityEntity> capabilities,
                                                      String errorMessage) {
        McpDtos.DiscoveryResult result = new McpDtos.DiscoveryResult();
        result.setTaskId(task.getId());
        result.setStatus(task.getStatus());
        result.setToolsCount(task.getDiscoveredTools());
        result.setPromptsCount(task.getDiscoveredPrompts());
        result.setResourcesCount(task.getDiscoveredResources());
        result.setErrorMessage(errorMessage);
        result.setCapabilities(capabilities.stream().map(this::toCapabilitySummary).toList());
        return result;
    }

    /**
     * 查询最近一次连接测试。
     *
     * @param serverId MCP Server ID
     * @return 最近测试结果
     */
    private McpDtos.ConnectionTestResult latestTest(String serverId) {
        McpConnectionTestEntity entity = mcpConnectionTestMapper.selectOne(new LambdaQueryWrapper<McpConnectionTestEntity>()
                .eq(McpConnectionTestEntity::getServerId, serverId)
                .orderByDesc(McpConnectionTestEntity::getCreatedAt)
                .last("limit 1"));
        return toConnectionTestResult(entity);
    }

    /**
     * 统计指定类型能力数量。
     *
     * @param serverId MCP Server ID
     * @param type 能力类型
     * @return 数量
     */
    private Integer countCapabilities(String serverId, String type) {
        Long count = mcpCapabilityMapper.selectCount(new LambdaQueryWrapper<McpCapabilityEntity>()
                .eq(McpCapabilityEntity::getServerId, serverId)
                .eq(McpCapabilityEntity::getCapabilityType, type));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 查询 MCP Server 实体。
     *
     * @param id MCP Server ID
     * @return MCP Server 实体
     */
    private McpServerEntity requireServer(String id) {
        McpServerEntity entity = mcpServerMapper.selectById(id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new BusinessException("MCP_SERVER_NOT_FOUND", "MCP Server 不存在");
        }
        return entity;
    }

    /**
     * 校验当前用户可管理 MCP Server。
     *
     * @param entity MCP Server 实体
     */
    private void assertCanManage(McpServerEntity entity) {
        if (!canManage(entity)) {
            throw new BusinessException("MCP_FORBIDDEN", "没有管理该 MCP Server 的权限");
        }
    }

    /**
     * 判断当前用户是否可查看 MCP Server。
     *
     * @param entity MCP Server 实体
     * @return 是否可查看
     */
    private boolean canView(McpServerEntity entity) {
        return entity != null && (isSystemManager() || !StringUtils.hasText(entity.getOwnerUserId()) || entity.getOwnerUserId().equals(currentUserId()));
    }

    /**
     * 判断当前用户是否可管理 MCP Server。
     *
     * @param entity MCP Server 实体
     * @return 是否可管理
     */
    private boolean canManage(McpServerEntity entity) {
        return entity != null && (isSystemManager() || (StringUtils.hasText(entity.getOwnerUserId()) && entity.getOwnerUserId().equals(currentUserId())));
    }

    /**
     * 判断是否系统管理员。
     *
     * @return 是否管理员
     */
    private boolean isSystemManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> List.of("ROLE_super_admin", "ROLE_admin", "tool:manage", "mcp:manage").contains(authority));
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 当前用户 ID
     */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }

    /**
     * 获取当前用户 ID，未登录时抛出异常。
     *
     * @return 当前用户 ID
     */
    private String currentUserIdOrThrow() {
        String userId = currentUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("UNAUTHORIZED", "请先登录");
        }
        return userId;
    }

    /**
     * 生成唯一 Server 编码。
     *
     * @param baseCode 基础编码
     * @return 唯一编码
     */
    private String uniqueServerCode(String baseCode) {
        String normalized = StringUtils.hasText(baseCode) ? baseCode : "mcp_server";
        String candidate = normalized;
        int suffix = 1;
        while (mcpServerMapper.selectCount(new LambdaQueryWrapper<McpServerEntity>()
                .eq(McpServerEntity::getServerCode, candidate)) > 0) {
            candidate = normalized + "_" + suffix++;
        }
        return candidate;
    }

    /**
     * 生成唯一工具编码。
     *
     * @param baseCode 基础编码
     * @param currentId 当前工具 ID，更新时可排除自身
     * @return 唯一工具编码
     */
    private String uniqueToolCode(String baseCode, String currentId) {
        String normalized = StringUtils.hasText(baseCode) ? baseCode : "mcp_tool";
        if (normalized.length() > 100) {
            normalized = normalized.substring(0, 100);
        }
        String candidate = normalized;
        int suffix = 1;
        while (existsToolCode(candidate, currentId)) {
            candidate = normalized + "_" + suffix++;
        }
        return candidate;
    }

    /**
     * 判断工具编码是否已存在。
     *
     * @param code 工具编码
     * @param currentId 当前工具 ID
     * @return 是否存在
     */
    private boolean existsToolCode(String code, String currentId) {
        ToolDefinitionEntity entity = toolDefinitionMapper.selectOne(new LambdaQueryWrapper<ToolDefinitionEntity>()
                .eq(ToolDefinitionEntity::getToolCode, code)
                .last("limit 1"));
        return entity != null && (currentId == null || !currentId.equals(entity.getId()));
    }

    /**
     * 将名称转换成编码。
     *
     * @param text 原始文本
     * @return 编码
     */
    private String slugify(String text) {
        String cleaned = text == null ? "mcp_server" : text.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_\\u4e00-\\u9fa5]+", "_")
                .replaceAll("^_|_$", "");
        return StringUtils.hasText(cleaned) ? cleaned : "mcp_server";
    }

    /**
     * 风险等级中文标签。
     *
     * @param riskLevel 风险等级
     * @return 中文标签
     */
    private String riskLabel(String riskLevel) {
        if ("high".equalsIgnoreCase(riskLevel)) {
            return "高风险";
        }
        if ("medium".equalsIgnoreCase(riskLevel)) {
            return "中风险";
        }
        return "低风险";
    }

    /**
     * 校验 JSON 字符串。
     *
     * @param json 原始 JSON
     * @param fallback 默认 JSON
     * @return 有效 JSON
     */
    private String validJsonOrDefault(String json, String fallback) {
        try {
            if (!StringUtils.hasText(json)) {
                return fallback;
            }
            objectMapper.readTree(json);
            return json;
        } catch (Exception exception) {
            return fallback;
        }
    }

    /**
     * 解析 JSON Map。
     *
     * @param json JSON 字符串
     * @return Map
     */
    private Map<String, Object> parseMap(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception exception) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 安全文本。
     *
     * @param text 原始文本
     * @return 非空文本
     */
    private String safeText(String text) {
        return text == null ? "" : text;
    }

    /**
     * 生成 UUID。
     *
     * @return UUID 字符串
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 转换 JSON。
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
