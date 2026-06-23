package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openagentflow.domain.DashboardOverview;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.KnowledgeBaseEntity;
import com.openagentflow.entity.McpServerEntity;
import com.openagentflow.entity.RuntimeRunEntity;
import com.openagentflow.entity.ToolDefinitionEntity;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.KnowledgeBaseMapper;
import com.openagentflow.mapper.McpServerMapper;
import com.openagentflow.mapper.RuntimeRunMapper;
import com.openagentflow.mapper.ToolDefinitionMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 首页概览服务。
 */
@Service
public class DashboardService {

    /** Agent Mapper。 */
    private final AgentMapper agentMapper;

    /** 知识库 Mapper。 */
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /** 工具定义 Mapper。 */
    private final ToolDefinitionMapper toolDefinitionMapper;

    /** MCP 服务 Mapper。 */
    private final McpServerMapper mcpServerMapper;

    /** 运行记录 Mapper。 */
    private final RuntimeRunMapper runtimeRunMapper;

    public DashboardService(AgentMapper agentMapper,
                            KnowledgeBaseMapper knowledgeBaseMapper,
                            ToolDefinitionMapper toolDefinitionMapper,
                            McpServerMapper mcpServerMapper,
                            RuntimeRunMapper runtimeRunMapper) {
        this.agentMapper = agentMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.toolDefinitionMapper = toolDefinitionMapper;
        this.mcpServerMapper = mcpServerMapper;
        this.runtimeRunMapper = runtimeRunMapper;
    }

    /**
     * 查询首页概览数据。
     *
     * @return 首页概览对象
     */
    public DashboardOverview getOverview() {
        DashboardOverview overview = new DashboardOverview();
        // 首页只读取轻量级聚合数据，避免影响后续 Agent 调试和 RAG 检索接口。
        overview.setAgentCount(agentMapper.selectCount(new LambdaQueryWrapper<AgentEntity>().isNull(AgentEntity::getDeletedAt)));
        overview.setKnowledgeBaseCount(knowledgeBaseMapper.selectCount(new LambdaQueryWrapper<KnowledgeBaseEntity>().isNull(KnowledgeBaseEntity::getDeletedAt)));
        overview.setToolCount(toolDefinitionMapper.selectCount(new LambdaQueryWrapper<ToolDefinitionEntity>().isNull(ToolDefinitionEntity::getDeletedAt)));
        overview.setMcpServerCount(mcpServerMapper.selectCount(new LambdaQueryWrapper<McpServerEntity>().isNull(McpServerEntity::getDeletedAt)));
        overview.setTodayRunCount(queryTodayRunCount());
        overview.setTodayCost(queryTodayCost());
        return overview;
    }

    /**
     * 查询今日运行次数。
     *
     * @return 今日运行次数
     */
    private Long queryTodayRunCount() {
        LocalDate today = LocalDate.now();
        // 使用时间范围而不是 date(created_at)，这样数据库可以命中 created_at 索引。
        return runtimeRunMapper.selectCount(new LambdaQueryWrapper<RuntimeRunEntity>()
                .ge(RuntimeRunEntity::getCreatedAt, today.atStartOfDay())
                .lt(RuntimeRunEntity::getCreatedAt, today.plusDays(1).atStartOfDay()));
    }

    /**
     * 查询今日模型调用成本。
     *
     * @return 今日成本
     */
    private Double queryTodayCost() {
        LocalDate today = LocalDate.now();
        // MyBatis-Plus 基础 Mapper 没有直接 sum 方法，基础框架阶段先在内存聚合今日少量运行数据。
        BigDecimal cost = runtimeRunMapper.selectList(new LambdaQueryWrapper<RuntimeRunEntity>()
                        .select(RuntimeRunEntity::getTotalCost)
                        .ge(RuntimeRunEntity::getCreatedAt, today.atStartOfDay())
                        .lt(RuntimeRunEntity::getCreatedAt, today.plusDays(1).atStartOfDay()))
                .stream()
                .map(RuntimeRunEntity::getTotalCost)
                .filter(item -> item != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cost.doubleValue();
    }
}
