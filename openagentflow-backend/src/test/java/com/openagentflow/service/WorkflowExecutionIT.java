package com.openagentflow.service;

import com.openagentflow.domain.workflow.WorkflowDtos;
import com.openagentflow.security.AuthUserDetails;
import com.openagentflow.security.AuthUserDetailsService;
import com.openagentflow.security.WorkspaceContextHolder;
import com.openagentflow.support.MySqlContainerIntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.assertThat;

/** 工作流执行引擎集成测试。 */
@SpringBootTest
class WorkflowExecutionIT extends MySqlContainerIntegrationTestSupport {

    @Autowired private WorkflowExecutionService workflowExecutionService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AuthUserDetailsService userDetailsService;

    private String workspaceId;
    private String userId;
    private String workflowId;

    @BeforeEach
    void setUpContext() {
        // 1. 查 admin 用户 + 默认空间（照抄 PermissionGovernanceServiceTests）
        userId = jdbcTemplate.queryForObject(
                "SELECT id FROM iam_user WHERE username='admin' LIMIT 1", String.class);
        workspaceId = jdbcTemplate.queryForObject(
                "SELECT id FROM oaf_workspace WHERE status='enabled' ORDER BY default_flag DESC LIMIT 1", String.class);
        AuthUserDetails details = userDetailsService.loadUserById(userId);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities()));
        WorkspaceContextHolder.bind(workspaceId);

        // 2. 插入 workflow_definition + workflow_node + workflow_edge
        insertWorkflow();
    }

    private void insertWorkflow() {
        // 固定节点ID，便于 edge 关联；out/end 是 Java 关键字，不能当变量名，故用 outId/endId
        String wfId = "10000000-0000-0000-0000-000000000010";
        String startId = "10000000-0000-0000-0000-000000000011";
        String condId = "10000000-0000-0000-0000-000000000012";
        String outId = "10000000-0000-0000-0000-000000000013";
        String endId = "10000000-0000-0000-0000-000000000014";

        this.workflowId = wfId;

        jdbcTemplate.update("""
            INSERT INTO workflow_definition
            (id, workflow_code, workflow_name, workflow_type, workspace_id,
            status, visibility, owner_user_id, created_by, version, graph_json, variable_schema)
            VALUES (?, ?, '执行测试工作流', 'agent_flow', ?,
            'draft', 'private', ?, ?, 0, '{}', '{}')
            """, wfId, "wf_exec_it_" + System.currentTimeMillis(), workspaceId, userId, userId);

        // START 节点
        jdbcTemplate.update("""
            INSERT INTO workflow_node
              (id, workflow_id, node_key, node_name, node_type, config_json,
                input_schema, output_schema, retry_policy, enabled)
            VALUES (?, ?, 'start', '开始', 'START', '{}','{}','{}','{}', 1)
            """, startId, wfId);

        // CONDITION 节点：configJson 用 Java 字符串传参（含单引号），
        // 避免 SQL 字符串字面量把 '' 转义成 ' 导致 JSON 损坏。
        // 表达式用路径 key `input`（matches() 不做模板渲染，{{input}} 无法解析）。
        String condConfigJson = "{\"conditionExpr\":\"input != ''\"}";
        jdbcTemplate.update("""
            INSERT INTO workflow_node
              (id, workflow_id, node_key, node_name, node_type, config_json,
                input_schema, output_schema, retry_policy, enabled)
            VALUES (?, ?, 'cond', '条件', 'CONDITION', ?, '{}','{}','{}', 1)
            """, condId, wfId, condConfigJson);

        // OUTPUT 节点
        jdbcTemplate.update("""
            INSERT INTO workflow_node
              (id, workflow_id, node_key, node_name, node_type, config_json,
                input_schema, output_schema, retry_policy, enabled)
            VALUES (?, ?, 'out', '输出', 'OUTPUT', '{"outputTemplate":"处理完成: {{input}}"}','{}','{}','{}', 1)
            """, outId, wfId);

        // END 节点
        jdbcTemplate.update("""
            INSERT INTO workflow_node
              (id, workflow_id, node_key, node_name, node_type, config_json,
                input_schema, output_schema, retry_policy, enabled)
            VALUES (?, ?, 'end', '结束', 'END', '{}','{}','{}','{}', 1)
            """, endId, wfId);

        // START → CONDITION
        jdbcTemplate.update("""
            INSERT INTO workflow_edge
              (id, workflow_id, edge_key, source_node_key, target_node_key, metadata)
            VALUES (?, ?, 'e1', 'start', 'cond', '{}')
            """, "10000000-0000-0000-0000-000000000021", wfId);

        // CONDITION → OUTPUT
        jdbcTemplate.update("""
            INSERT INTO workflow_edge
              (id, workflow_id, edge_key, source_node_key, target_node_key, metadata)
            VALUES (?, ?, 'e2', 'cond', 'out', '{}')
            """, "10000000-0000-0000-0000-000000000022", wfId);

        // OUTPUT → END
        jdbcTemplate.update("""
            INSERT INTO workflow_edge
              (id, workflow_id, edge_key, source_node_key, target_node_key, metadata)
            VALUES (?, ?, 'e3', 'out', 'end', '{}')
            """, "10000000-0000-0000-0000-000000000023", wfId);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        WorkspaceContextHolder.clear();
    }

    /** START→CONDITION→OUTPUT→END 链路应成功执行并输出模板渲染结果。 */
    @Test
    void shouldExecuteLinearWorkflowAndProduceOutput() {
        // 1. 构造请求
        WorkflowDtos.RunRequest request = new WorkflowDtos.RunRequest();
        request.setInput("你好");
        request.setMaxSteps(20);

        // 2. 执行
        WorkflowDtos.RunResult result = workflowExecutionService.runWorkflow(workflowId, request, "test");

        // 3. 断言
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getOutputText()).isEqualTo("处理完成: 你好");
    }
}
