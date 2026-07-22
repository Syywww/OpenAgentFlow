package com.openagentflow.service;

import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.domain.template.TemplateDtos;
import com.openagentflow.security.AuthUserDetails;
import com.openagentflow.security.AuthUserDetailsService;
import com.openagentflow.security.WorkspaceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 解决方案模板完整安装链路测试。 */
@SpringBootTest(properties = {
        "openagentflow.async-task.consumer-enabled=false",
        "openagentflow.observability.otlp-enabled=false"
})
@Transactional
class TemplateInstallServiceTests {

    /** 安装服务。 */ @Autowired private TemplateInstallService installService;
    /** 用户加载服务。 */ @Autowired private AuthUserDetailsService userDetailsService;
    /** 数据库工具。 */ @Autowired private JdbcTemplate jdbcTemplate;

    /** 测试用户ID。 */ private String userId;
    /** 测试工作空间ID。 */ private String workspaceId;

    /** 建立管理员和工作空间上下文。 */
    @BeforeEach
    void setUpContext() {
        userId = jdbcTemplate.queryForObject("SELECT id FROM iam_user WHERE username='admin' LIMIT 1", String.class);
        workspaceId = jdbcTemplate.queryForObject("SELECT id FROM oaf_workspace WHERE status='enabled' LIMIT 1", String.class);
        AuthUserDetails details = userDetailsService.loadUserById(userId);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities()));
        WorkspaceContextHolder.bind(workspaceId);
    }

    /** 清理线程上下文。 */
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        WorkspaceContextHolder.clear();
    }

    /** 公开种子模板必须复制成工作空间独立Agent，并写入安装资源映射。 */
    @Test
    void shouldInstallSeedSolutionAsIndependentAgentCopy() {
        String installId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO agent_template_install
                  (id,template_id,workspace_id,template_version_id,install_status,progress_percent,current_stage,
                   name_prefix,model_mapping,credentials_ready,installed_by,install_config,installed_manifest)
                VALUES (?,'74000000-0000-0000-0000-000000000001',?,
                        '74100000-0000-0000-0000-000000000001','pending',0,'accepted','测试-',JSON_OBJECT(),0,?,JSON_OBJECT(),JSON_OBJECT())
                """, installId, workspaceId, userId);
        AsyncTaskEntity task = new AsyncTaskEntity();
        task.setId(UUID.randomUUID().toString());
        task.setRequestPayload("{\"installId\":\"" + installId + "\"}");

        Map<String, Object> result = installService.executeInstall(task);

        String agentId = String.valueOf(result.get("targetAgentId"));
        assertThat(agentId).isNotBlank();
        assertThat(jdbcTemplate.queryForObject("SELECT agent_name FROM agent WHERE id=?", String.class, agentId))
                .startsWith("测试-");
        assertThat(jdbcTemplate.queryForObject("SELECT install_status FROM agent_template_install WHERE id=?", String.class, installId))
                .isEqualTo("success");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM agent_template_install_resource WHERE install_id=?", Long.class, installId))
                .isEqualTo(10L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT resource_type) FROM agent_template_install_resource WHERE install_id=?", Long.class, installId))
                .isEqualTo(10L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM agent_tool_binding WHERE agent_id=?", Long.class, agentId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM agent_knowledge_binding WHERE agent_id=?", Long.class, agentId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM agent_workflow_binding WHERE agent_id=?", Long.class, agentId)).isEqualTo(1L);
        String workflowId = jdbcTemplate.queryForObject("SELECT target_resource_id FROM agent_template_install_resource WHERE install_id=? AND resource_type='workflow'", String.class, installId);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM workflow_node WHERE workflow_id=?", Long.class, workflowId)).isGreaterThan(0L);
    }

    /** 从页面提交安装时必须成功创建安装实例和Kafka异步任务。 */
    @Test
    void shouldCreateAsynchronousTemplateInstallFromRequest() {
        TemplateDtos.InstallRequest request = new TemplateDtos.InstallRequest();
        request.templateVersionId = "74100000-0000-0000-0000-000000000001";
        request.workspaceId = workspaceId;
        request.namePrefix = "接口测试-";
        request.idempotencyKey = "template-install-test-" + UUID.randomUUID();

        TemplateDtos.InstallSummary result = installService.install(
                "74000000-0000-0000-0000-000000000001", request);

        assertThat(result.id).isNotBlank();
        assertThat(result.installStatus).isEqualTo("pending");
        assertThat(result.installTaskId).isNotBlank();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM async_task WHERE id=?", Long.class, result.installTaskId)).isEqualTo(1L);
    }
}
