package com.openagentflow.service;

import com.openagentflow.entity.AgentEntity;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.OafWorkspaceMapper;
import com.openagentflow.security.WorkspaceContextHolder;
import com.openagentflow.support.MySqlContainerIntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多租户 SQL 层隔离集成测试。
 *
 * <p>验证 MyBatis-Plus 租户拦截器（{@code TenantLineInnerInterceptor}）对纳管表的强制隔离：
 * 查询/更新/删除自动追加 {@code workspace_id} 条件、插入自动补列。核心不变量：</p>
 * <ol>
 *   <li><b>防静默穿透</b>：跨工作空间按主键、列表、更新、删除一律不命中；</li>
 *   <li><b>无上下文跳过</b>：后台任务线程（无请求上下文）拦截器直接放行，隔离靠显式字段；</li>
 *   <li><b>白名单边界</b>：非纳管表（如 oaf_workspace）不追加租户条件。</li>
 * </ol>
 */
@SpringBootTest
class TenantIsolationIT extends MySqlContainerIntegrationTestSupport {

    /** 测试用两个相互独立的工作空间。 */
    private static final String WORKSPACE_A = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String WORKSPACE_B = "bbbbbbbb-0000-0000-0000-000000000001";

    @Autowired private AgentMapper agentMapper;
    @Autowired private OafWorkspaceMapper oafWorkspaceMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 共享数据库：清空 agent（级联清理引用表），避免列表断言受历史数据干扰。
        jdbcTemplate.update("DELETE FROM agent");
    }

    @AfterEach
    void tearDown() {
        WorkspaceContextHolder.clear();
    }

    /** 跨工作空间按主键读取被拦截：租户 A 读不到租户 B 的 Agent（防静默穿透）。 */
    @Test
    void shouldNotReadAcrossWorkspacesByPrimaryKey() {
        String agentId = insertAgent(WORKSPACE_A, "it_agent_a", "租户A的Agent");

        WorkspaceContextHolder.bind(WORKSPACE_B);
        assertThat(agentMapper.selectById(agentId)).isNull();

        WorkspaceContextHolder.bind(WORKSPACE_A);
        assertThat(agentMapper.selectById(agentId)).isNotNull();
    }

    /** 插入时拦截器自动补齐 workspace_id，业务代码无需手动赋值。 */
    @Test
    void shouldAutoInjectWorkspaceIdOnInsert() {
        String agentId = insertAgent(WORKSPACE_A, "it_agent_inject", "自动注入空间");

        // 用原生 SQL 读回（绕过 MyBatis 拦截器），验证拦截器确实写入了 workspace_id。
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT workspace_id FROM agent WHERE id = ?", agentId);
        assertThat(String.valueOf(row.get("workspace_id"))).isEqualTo(WORKSPACE_A);
    }

    /** 列表查询按当前绑定空间自动过滤：A 的列表不包含 B 的数据，反之亦然。 */
    @Test
    void shouldFilterListQueryByBoundWorkspace() {
        insertAgent(WORKSPACE_A, "it_agent_a", "租户A的Agent");
        insertAgent(WORKSPACE_B, "it_agent_b", "租户B的Agent");

        WorkspaceContextHolder.bind(WORKSPACE_A);
        List<AgentEntity> agentsOfA = agentMapper.selectList(null);
        assertThat(agentsOfA).extracting(AgentEntity::getAgentCode)
                .contains("it_agent_a")
                .doesNotContain("it_agent_b");

        WorkspaceContextHolder.bind(WORKSPACE_B);
        List<AgentEntity> agentsOfB = agentMapper.selectList(null);
        assertThat(agentsOfB).extracting(AgentEntity::getAgentCode)
                .contains("it_agent_b")
                .doesNotContain("it_agent_a");
    }

    /** 后台任务线程（无工作空间上下文）拦截器直接跳过，隔离靠显式字段——设计契约。 */
    @Test
    void shouldSkipInterceptorWhenNoWorkspaceContext() {
        String agentId = insertAgent(WORKSPACE_A, "it_agent_ctx", "后台任务可见");
        WorkspaceContextHolder.clear();

        // 无上下文时拦截器 ignoreTable 返回 true，不追加条件，原生 ID 仍可读（由任务代码自行显式过滤）。
        assertThat(agentMapper.selectById(agentId)).isNotNull();
    }

    /** 非纳管表（oaf_workspace 本身）不追加租户条件：绑定任意空间都能读到默认工作空间。 */
    @Test
    void shouldNotScopeNonGovernedTable() {
        WorkspaceContextHolder.bind(WORKSPACE_B);
        // V011 种子数据里的默认工作空间，不属于 WORKSPACE_B，但表不在纳管白名单 → 不受过滤。
        assertThat(oafWorkspaceMapper.selectById("90000000-0000-0000-0000-000000000101")).isNotNull();
    }

    /** 更新/删除同样带租户条件：跨空间不命中，同空间生效。 */
    @Test
    void shouldScopeUpdateAndDeleteByWorkspace() {
        String agentId = insertAgent(WORKSPACE_A, "it_agent_ud", "更新删除隔离");

        WorkspaceContextHolder.bind(WORKSPACE_B);
        assertThat(updateAgentName(agentId, "跨空间改名")).isZero();

        WorkspaceContextHolder.bind(WORKSPACE_A);
        assertThat(updateAgentName(agentId, "同空间改名")).isEqualTo(1);
        assertThat(agentMapper.selectById(agentId).getAgentName()).isEqualTo("同空间改名");

        WorkspaceContextHolder.bind(WORKSPACE_B);
        assertThat(agentMapper.deleteById(agentId)).isZero();

        WorkspaceContextHolder.bind(WORKSPACE_A);
        assertThat(agentMapper.deleteById(agentId)).isEqualTo(1);
        assertThat(agentMapper.selectById(agentId)).isNull();
    }

    /** 在指定空间上下文下插入一条 Agent，返回其主键。 */
    private String insertAgent(String workspaceId, String code, String name) {
        WorkspaceContextHolder.bind(workspaceId);
        AgentEntity entity = new AgentEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setAgentCode(code);
        entity.setAgentName(name);
        entity.setCategory("测试");
        entity.setAgentType("chat");
        entity.setModelParams("{}");
        agentMapper.insert(entity);
        return entity.getId();
    }

    private int updateAgentName(String agentId, String name) {
        AgentEntity patch = new AgentEntity();
        patch.setId(agentId);
        patch.setAgentName(name);
        return agentMapper.updateById(patch);
    }
}