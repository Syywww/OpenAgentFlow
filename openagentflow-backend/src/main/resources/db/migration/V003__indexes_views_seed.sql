USE openagentflow;

CREATE INDEX idx_iam_user_department ON iam_user(department_id);
CREATE INDEX idx_iam_user_status ON iam_user(status);
CREATE INDEX idx_iam_login_user_time ON iam_login_log(user_id, created_at);
CREATE INDEX idx_iam_acl_resource ON iam_resource_acl(resource_type, resource_id);

CREATE INDEX idx_model_config_provider_type ON model_config(provider_id, model_type, status);
CREATE INDEX idx_model_connectivity_created ON model_connectivity_test(created_at);

CREATE INDEX idx_prompt_template_type_status ON prompt_template(prompt_type, status);
CREATE INDEX idx_agent_status_type ON agent(status, agent_type);
CREATE INDEX idx_agent_owner ON agent(owner_user_id);
CREATE INDEX idx_agent_session_agent_user ON agent_session(agent_id, user_id, updated_at);
CREATE INDEX idx_agent_message_session_time ON agent_message(session_id, created_at);
CREATE INDEX idx_agent_memory_lookup ON agent_memory(agent_id, user_id, memory_type);
CREATE INDEX idx_agent_memory_external_vector ON agent_memory(external_vector_id);

CREATE INDEX idx_kb_status_owner ON knowledge_base(status, owner_user_id);
CREATE INDEX idx_doc_kb_status ON knowledge_document(kb_id, parse_status);
CREATE INDEX idx_chunk_kb_doc ON knowledge_chunk(kb_id, document_id);
CREATE FULLTEXT INDEX ft_chunk_content ON knowledge_chunk(content);
CREATE INDEX idx_embedding_kb ON knowledge_embedding(kb_id);
CREATE INDEX idx_embedding_external_vector ON knowledge_embedding(external_vector_id);
CREATE INDEX idx_retrieval_log_run_time ON knowledge_retrieval_log(run_id, created_at);
CREATE INDEX idx_citation_message ON knowledge_source_citation(message_id);

CREATE INDEX idx_tool_type_risk_status ON tool_definition(tool_type, risk_level, status);
CREATE INDEX idx_tool_invocation_tool_time ON tool_invocation_log(tool_id, created_at);
CREATE INDEX idx_tool_invocation_run ON tool_invocation_log(run_id);
CREATE INDEX idx_mcp_server_status ON mcp_server(status);
CREATE INDEX idx_mcp_capability_server_type ON mcp_capability(server_id, capability_type, enabled);

CREATE INDEX idx_workflow_status_owner ON workflow_definition(status, owner_user_id);
CREATE INDEX idx_workflow_node_workflow_type ON workflow_node(workflow_id, node_type);
CREATE INDEX idx_workflow_run_status_time ON workflow_run(status, created_at);
CREATE INDEX idx_workflow_step_run_run ON workflow_step_run(workflow_run_id, created_at);
CREATE INDEX idx_workflow_human_task_assignee ON workflow_human_task(assignee_user_id, status, created_at);

CREATE INDEX idx_runtime_run_type_status_time ON runtime_run(run_type, status, started_at);
CREATE INDEX idx_runtime_run_agent_time ON runtime_run(agent_id, started_at);
CREATE INDEX idx_runtime_run_workflow_time ON runtime_run(workflow_id, started_at);
CREATE INDEX idx_runtime_trace_run_step ON runtime_trace_step(run_id, started_at);
CREATE INDEX idx_runtime_llm_call_run ON runtime_llm_call(run_id, created_at);
CREATE INDEX idx_runtime_event_run_level ON runtime_event_log(run_id, event_level, created_at);
CREATE INDEX idx_runtime_guardrail_run ON runtime_guardrail_event(run_id, created_at);

CREATE INDEX idx_eval_sample_dataset ON eval_sample(dataset_id, sample_no);
CREATE INDEX idx_eval_task_dataset_status ON eval_task(dataset_id, status);
CREATE INDEX idx_eval_task_run_task_model ON eval_task_run(task_id, model_id, status);
CREATE INDEX idx_eval_score_metric ON eval_score(metric_id, score);

CREATE INDEX idx_notification_recipient_user ON notification_recipient(user_id, read_at, created_at);
CREATE INDEX idx_audit_operation_time ON audit_operation_log(created_at);
CREATE INDEX idx_audit_operation_resource ON audit_operation_log(resource_type, resource_id);
CREATE INDEX idx_file_object_resource ON file_object(resource_type, resource_id);
CREATE INDEX idx_resource_tag_resource ON resource_tag(resource_type, resource_id);

CREATE INDEX idx_agent_team_status ON agent_team(status, owner_user_id);
CREATE INDEX idx_prompt_experiment_status ON prompt_experiment(status, agent_id);
CREATE INDEX idx_model_route_policy_scene ON model_route_policy(scene_type, status);
CREATE INDEX idx_guardrail_policy_scope ON guardrail_policy(apply_scope, enabled);
CREATE INDEX idx_plugin_package_type_status ON plugin_package(plugin_type, status);
CREATE INDEX idx_local_runtime_status ON local_model_runtime(runtime_type, status);
CREATE INDEX idx_data_import_job_status ON data_import_job(target_type, status, created_at);

CREATE OR REPLACE VIEW v_agent_summary AS
SELECT
  a.id, a.agent_code, a.agent_name, a.agent_type, a.category, a.status, a.owner_user_id,
  COUNT(DISTINCT akb.knowledge_base_id) AS knowledge_count,
  COUNT(DISTINCT atb.tool_id) AS tool_count,
  COUNT(DISTINCT awb.workflow_id) AS workflow_count,
  COUNT(DISTINCT CASE WHEN rr.created_at >= DATE_SUB(NOW(3), INTERVAL 7 DAY) THEN rr.id END) AS runs_7d,
  COALESCE(SUM(CASE WHEN rr.created_at >= DATE_SUB(NOW(3), INTERVAL 7 DAY) THEN rr.total_tokens ELSE 0 END), 0) AS tokens_7d,
  COALESCE(SUM(CASE WHEN rr.created_at >= DATE_SUB(NOW(3), INTERVAL 7 DAY) THEN rr.total_cost ELSE 0 END), 0) AS cost_7d
FROM agent a
LEFT JOIN agent_knowledge_binding akb ON akb.agent_id = a.id AND akb.enabled = 1
LEFT JOIN agent_tool_binding atb ON atb.agent_id = a.id AND atb.enabled = 1
LEFT JOIN agent_workflow_binding awb ON awb.agent_id = a.id AND awb.enabled = 1
LEFT JOIN runtime_run rr ON rr.agent_id = a.id
GROUP BY a.id, a.agent_code, a.agent_name, a.agent_type, a.category, a.status, a.owner_user_id;

CREATE OR REPLACE VIEW v_knowledge_base_summary AS
SELECT
  kb.id, kb.kb_code, kb.kb_name, kb.status, kb.visibility, kb.owner_user_id,
  COUNT(DISTINCT d.id) AS document_count,
  COUNT(DISTINCT c.id) AS chunk_count,
  COUNT(DISTINCT e.id) AS embedding_count,
  COALESCE(SUM(d.file_size), 0) AS total_file_size
FROM knowledge_base kb
LEFT JOIN knowledge_document d ON d.kb_id = kb.id
LEFT JOIN knowledge_chunk c ON c.kb_id = kb.id
LEFT JOIN knowledge_embedding e ON e.kb_id = kb.id
GROUP BY kb.id, kb.kb_code, kb.kb_name, kb.status, kb.visibility, kb.owner_user_id;

CREATE OR REPLACE VIEW v_tool_summary AS
SELECT
  t.id, t.tool_code, t.tool_name, t.tool_type, t.risk_level, t.enabled, t.status,
  COUNT(CASE WHEN l.created_at >= DATE_SUB(NOW(3), INTERVAL 7 DAY) THEN l.id END) AS calls_7d,
  COUNT(CASE WHEN l.success = 1 AND l.created_at >= DATE_SUB(NOW(3), INTERVAL 7 DAY) THEN l.id END) AS success_7d,
  COUNT(CASE WHEN l.success = 0 AND l.created_at >= DATE_SUB(NOW(3), INTERVAL 7 DAY) THEN l.id END) AS failure_7d,
  AVG(CASE WHEN l.created_at >= DATE_SUB(NOW(3), INTERVAL 7 DAY) THEN l.latency_ms END) AS avg_latency_ms_7d
FROM tool_definition t
LEFT JOIN tool_invocation_log l ON l.tool_id = t.id
GROUP BY t.id, t.tool_code, t.tool_name, t.tool_type, t.risk_level, t.enabled, t.status;

CREATE OR REPLACE VIEW v_runtime_daily AS
SELECT
  DATE(started_at) AS stat_date,
  run_type,
  COUNT(*) AS run_count,
  SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
  SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failure_count,
  COALESCE(SUM(total_tokens), 0) AS total_tokens,
  COALESCE(SUM(total_cost), 0) AS total_cost,
  AVG(latency_ms) AS avg_latency_ms
FROM runtime_run
GROUP BY DATE(started_at), run_type;

INSERT IGNORE INTO iam_department (id, dept_code, dept_name)
VALUES ('00000000-0000-0000-0000-000000000001', 'root', 'OpenAgentFlow'),
       ('00000000-0000-0000-0000-000000000002', 'rd', '研发中心');

INSERT IGNORE INTO iam_user (id, department_id, username, email, password_hash, display_name, status)
VALUES ('00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000002', 'admin', 'admin@openagentflow.local', '$2a$10$QKjQXTUqgfhqg4ztvkBlpeeac.kfXgwkVE73ihJxMuuIWDTPzRRVi', '系统管理员', 'enabled');

INSERT IGNORE INTO iam_role (id, role_code, role_name, built_in, description)
VALUES
  ('00000000-0000-0000-0000-000000000201', 'super_admin', '超级管理员', 1, '拥有全部系统权限'),
  ('00000000-0000-0000-0000-000000000202', 'admin', '管理员', 1, '管理平台资源和成员'),
  ('00000000-0000-0000-0000-000000000203', 'developer', '开发者', 1, '创建 Agent、工具、工作流和评测任务'),
  ('00000000-0000-0000-0000-000000000204', 'user', '普通用户', 1, '使用已发布 Agent 和工作流');

INSERT IGNORE INTO iam_user_role (user_id, role_id)
VALUES ('00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000201');

INSERT IGNORE INTO iam_permission (permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order)
VALUES
  ('dashboard:view', '工作台查看', 'menu', '/dashboard', 'GET', '/api/dashboard/**', 10),
  ('agent:manage', '智能体管理', 'menu', '/agents', 'ALL', '/api/agents/**', 20),
  ('debug:use', '调试台使用', 'menu', '/debug', 'ALL', '/api/debug/**', 30),
  ('knowledge:manage', '知识库管理', 'menu', '/knowledge', 'ALL', '/api/knowledge/**', 40),
  ('tool:manage', '工具中心管理', 'menu', '/tools', 'ALL', '/api/tools/**', 50),
  ('mcp:manage', 'MCP 管理', 'menu', '/mcp', 'ALL', '/api/mcp/**', 60),
  ('workflow:manage', '工作流管理', 'menu', '/workflow', 'ALL', '/api/workflows/**', 70),
  ('trace:view', '运行日志查看', 'menu', '/logs', 'GET', '/api/runs/**', 80),
  ('eval:manage', '评测中心管理', 'menu', '/eval', 'ALL', '/api/evaluations/**', 90),
  ('template:manage', '模板广场管理', 'menu', '/templates', 'ALL', '/api/templates/**', 100),
  ('setting:manage', '系统设置管理', 'menu', '/settings', 'ALL', '/api/settings/**', 110);

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000201', id FROM iam_permission;

INSERT IGNORE INTO model_provider (id, provider_code, provider_name, provider_type, base_url, default_headers, status, health_status)
VALUES
  ('10000000-0000-0000-0000-000000000001', 'openai-compatible', 'OpenAI 兼容', 'openai_compatible', 'https://api.openai.com/v1', JSON_OBJECT(), 'enabled', 'healthy'),
  ('10000000-0000-0000-0000-000000000002', 'ollama', 'Ollama 本地', 'ollama', 'http://localhost:11434/v1', JSON_OBJECT(), 'enabled', 'unknown'),
  ('10000000-0000-0000-0000-000000000003', 'qwen', '通义千问 Qwen', 'openai_compatible', 'https://dashscope.aliyuncs.com/compatible-mode/v1', JSON_OBJECT(), 'enabled', 'unknown'),
  ('10000000-0000-0000-0000-000000000004', 'deepseek', 'DeepSeek', 'openai_compatible', 'https://api.deepseek.com/v1', JSON_OBJECT(), 'enabled', 'unknown');

INSERT IGNORE INTO model_config (id, provider_id, model_code, model_name, model_type, context_window, max_output_tokens, default_params, support_stream, support_function_calling, is_default)
VALUES
  ('10000000-0000-0000-0000-000000000101', '10000000-0000-0000-0000-000000000001', 'gpt-4o-mini', 'GPT-4o Mini', 'chat', 128000, 16384, JSON_OBJECT(), 1, 1, 1),
  ('10000000-0000-0000-0000-000000000102', '10000000-0000-0000-0000-000000000001', 'text-embedding-3-large', 'Text Embedding 3 Large', 'embedding', 8191, 0, JSON_OBJECT(), 0, 0, 1),
  ('10000000-0000-0000-0000-000000000103', '10000000-0000-0000-0000-000000000003', 'qwen-max', 'Qwen Max', 'chat', 32768, 8192, JSON_OBJECT(), 1, 1, 0),
  ('10000000-0000-0000-0000-000000000104', '10000000-0000-0000-0000-000000000004', 'deepseek-chat', 'DeepSeek Chat', 'chat', 64000, 8192, JSON_OBJECT(), 1, 1, 0);

INSERT IGNORE INTO prompt_template (id, template_code, template_name, prompt_type, content, variables, status, owner_user_id)
VALUES
  ('20000000-0000-0000-0000-000000000001', 'rag-system-default', 'RAG 问答 System Prompt', 'system', '你是企业知识库问答助手。请优先基于上下文回答，并在答案末尾列出引用来源。', JSON_ARRAY(JSON_OBJECT('name','context'), JSON_OBJECT('name','user_input')), 'published', '00000000-0000-0000-0000-000000000100'),
  ('20000000-0000-0000-0000-000000000002', 'tool-system-default', '工具调用 System Prompt', 'tool', '你可以在必要时调用授权工具。高风险工具调用前必须请求用户确认。', JSON_ARRAY(JSON_OBJECT('name','tool_list'), JSON_OBJECT('name','user_input')), 'published', '00000000-0000-0000-0000-000000000100');

INSERT IGNORE INTO agent (id, agent_code, agent_name, category, description, agent_type, model_id, system_prompt_template_id, system_prompt, model_params, memory_strategy, visibility, status, owner_user_id, created_by)
VALUES ('30000000-0000-0000-0000-000000000001', 'customer-support-agent', '客服助手', '客服', '基于企业知识库的智能客服助手，支持多轮问答与工单创建。', 'rag_tool_agent', '10000000-0000-0000-0000-000000000101', '20000000-0000-0000-0000-000000000001', '你是企业智能客服助手，请基于知识库和工具结果回答用户问题。', JSON_OBJECT('temperature', 0.3), 'short_term', 'team', 'published', '00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000100');

INSERT IGNORE INTO knowledge_base (id, kb_code, kb_name, description, embedding_model_id, visibility, status, owner_user_id, created_by)
VALUES ('40000000-0000-0000-0000-000000000001', 'product-manual-kb', '产品手册知识库', '产品功能说明书、操作指南、最佳实践。', '10000000-0000-0000-0000-000000000102', 'team', 'active', '00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000100');

INSERT IGNORE INTO tool_definition (id, tool_code, tool_name, tool_type, description, request_method, endpoint_url, auth_config, headers, request_schema, response_schema, risk_level, require_confirm, enabled, status, owner_user_id, created_by)
VALUES
  ('50000000-0000-0000-0000-000000000001', 'query_order_status', '查询订单状态', 'REST_API', '查询订单配送状态、物流单号与预计送达时间。', 'GET', 'https://api.example.com/v1/orders/{orderId}', JSON_OBJECT(), JSON_OBJECT(), JSON_OBJECT('type','object'), JSON_OBJECT('type','object'), 'low', 0, 1, 'active', '00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000100'),
  ('50000000-0000-0000-0000-000000000002', 'create_order', '创建订单', 'REST_API', '创建订单，属于中高风险变更操作。', 'POST', 'https://api.example.com/v1/orders', JSON_OBJECT(), JSON_OBJECT(), JSON_OBJECT('type','object'), JSON_OBJECT('type','object'), 'medium', 1, 1, 'active', '00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000100');

INSERT IGNORE INTO mcp_server (id, server_code, server_name, description, transport_type, command, args, auth_config, env_vars, allowed_paths, risk_policy, status, owner_user_id, created_by)
VALUES ('60000000-0000-0000-0000-000000000001', 'filesystem-server', 'filesystem-server', '文件系统访问 MCP Server，默认限制白名单路径。', 'stdio', 'npx @modelcontextprotocol/server-filesystem', JSON_ARRAY('/allowed/path'), JSON_OBJECT(), JSON_OBJECT(), JSON_ARRAY('/allowed/path'), JSON_OBJECT(), 'stopped', '00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000100');

INSERT IGNORE INTO eval_metric (metric_code, metric_name, metric_type, description, config_json)
VALUES
  ('accuracy', '准确率', 'rule', '答案是否命中标准答案或评分点', JSON_OBJECT()),
  ('relevance', '相关性', 'llm_as_judge', '答案是否与问题和上下文相关', JSON_OBJECT()),
  ('hallucination', '幻觉率', 'llm_as_judge', '是否出现上下文外事实', JSON_OBJECT()),
  ('schema_valid', '格式合规率', 'json_schema', '输出是否符合 JSON Schema 或格式要求', JSON_OBJECT()),
  ('latency', '平均延迟', 'system', '运行耗时统计', JSON_OBJECT());

INSERT IGNORE INTO agent_template (template_code, template_name, category, description, tags, agent_snapshot, prompt_snapshot, tool_snapshot, knowledge_snapshot, recommended, created_by)
VALUES
  ('knowledge-qa', '知识库问答', '知识管理', '基于 RAG 的企业知识库问答 Agent 模板。', JSON_ARRAY('RAG','问答'), JSON_OBJECT(), JSON_OBJECT(), JSON_ARRAY(), JSON_ARRAY(), 1, '00000000-0000-0000-0000-000000000100'),
  ('sql-analyst', 'SQL 查询助手', '数据分析', '自然语言转 SQL 查询与结果解释 Agent 模板。', JSON_ARRAY('SQL','数据分析'), JSON_OBJECT(), JSON_OBJECT(), JSON_ARRAY(), JSON_ARRAY(), 1, '00000000-0000-0000-0000-000000000100'),
  ('customer-support', '客服助手', '客服服务', '结合知识库与订单查询工具的客服 Agent 模板。', JSON_ARRAY('客服','工具调用'), JSON_OBJECT(), JSON_OBJECT(), JSON_ARRAY(), JSON_ARRAY(), 1, '00000000-0000-0000-0000-000000000100');

INSERT IGNORE INTO sys_config (config_key, config_value, value_type, group_code, description)
VALUES
  ('security.jwt.expire_minutes', '120', 'number', 'security', 'JWT 访问令牌有效期'),
  ('rag.default_top_k', '10', 'number', 'rag', '默认向量检索 Top-K'),
  ('rag.default_score_threshold', '0.3', 'number', 'rag', '默认检索分数阈值'),
  ('tool.high_risk_confirm_required', 'true', 'boolean', 'tool', '高风险工具是否强制二次确认'),
  ('workflow.max_retry_count', '3', 'number', 'workflow', '工作流节点最大重试次数');
