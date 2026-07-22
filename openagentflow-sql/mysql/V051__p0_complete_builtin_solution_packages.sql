-- P0：补齐内置解决方案模板资源包，并让历史模板具备可安装版本。

-- 四个解决方案版本使用 MySQL 快照资源交付，不依赖外部对象存储包。
UPDATE agent_template_version v
JOIN agent_template t ON t.id = v.template_id
SET v.resource_manifest = JSON_OBJECT(
        'prompts', 1, 'promptVersions', 1, 'tools', 1, 'knowledgeBases', 1,
        'documents', 1, 'chunks', 1, 'workflows', 1, 'agents', 1, 'teams', 1, 'memories', 1),
    v.dependency_graph = JSON_OBJECT('mode', 'topological', 'resourceCount', 10),
    v.runtime_check_result = JSON_OBJECT('passed', TRUE, 'installMode', 'database_snapshot')
WHERE t.template_code IN ('solution-customer-service', 'solution-knowledge-assistant',
                          'solution-data-analyst', 'solution-devops')
  AND v.version_no = '1.0.0';

-- Prompt 模板。
INSERT IGNORE INTO agent_template_resource
    (id, template_version_id, resource_type, source_resource_id, resource_code, resource_name,
     resource_snapshot, content_hash, dependency_ids, object_manifest, sort_order, required)
SELECT UUID(), v.id, 'prompt',
       CONCAT('75110000-0000-0000-0000-00000000000', RIGHT(t.id, 1)),
       CONCAT(t.template_code, '-prompt'), CONCAT(t.template_name, ' Prompt'),
       JSON_OBJECT(
           'workspace_id', '00000000-0000-0000-0000-000000000001',
           'template_code', CONCAT(t.template_code, '-prompt'),
           'template_name', CONCAT(t.template_name, ' Prompt'),
           'prompt_type', 'system',
           'content', CASE t.template_code
               WHEN 'solution-customer-service' THEN '你是企业智能客服。业务事实优先调用工具，制度问题优先检索知识库，资料不足时明确说明。'
               WHEN 'solution-knowledge-assistant' THEN '你是企业知识助手。仅依据可靠引用回答，缺少证据时拒绝编造。'
               WHEN 'solution-data-analyst' THEN '你是数据分析协作团队主控。只执行只读分析，并对结论给出数据依据。'
               ELSE '你是智能运维助手。先诊断再建议，高风险操作必须人工确认。' END,
           'variables', JSON_ARRAY(), 'status', 'published', 'risk_level', 'low'),
       SHA2(CONCAT(t.template_code, ':prompt:1.0.0'), 256), JSON_ARRAY(), JSON_ARRAY(), 10, 1
FROM agent_template t JOIN agent_template_version v ON v.template_id = t.id AND v.version_no = '1.0.0'
WHERE t.template_code IN ('solution-customer-service', 'solution-knowledge-assistant',
                          'solution-data-analyst', 'solution-devops');

-- Prompt 稳定版本。
INSERT IGNORE INTO agent_template_resource
    (id, template_version_id, resource_type, source_resource_id, resource_code, resource_name,
     resource_snapshot, content_hash, parent_resource_id, dependency_ids, object_manifest, sort_order, required)
SELECT UUID(), v.id, 'prompt_version',
       CONCAT('75120000-0000-0000-0000-00000000000', RIGHT(t.id, 1)),
       CONCAT(t.template_code, '-prompt-v1'), CONCAT(t.template_name, ' Prompt 1.0.0'),
       JSON_OBJECT(
           'template_id', CONCAT('75110000-0000-0000-0000-00000000000', RIGHT(t.id, 1)),
           'version_no', '1.0.0',
           'content', JSON_UNQUOTE(JSON_EXTRACT(r.resource_snapshot, '$.content')),
           'variables', JSON_ARRAY(), 'change_summary', '内置解决方案首个稳定版本',
           'status', 'stable', 'risk_level', 'low'),
       SHA2(CONCAT(t.template_code, ':prompt-version:1.0.0'), 256), r.source_resource_id,
       JSON_ARRAY(r.source_resource_id), JSON_ARRAY(), 20, 1
FROM agent_template t
JOIN agent_template_version v ON v.template_id = t.id AND v.version_no = '1.0.0'
JOIN agent_template_resource r ON r.template_version_id = v.id AND r.resource_type = 'prompt'
WHERE t.template_code IN ('solution-customer-service', 'solution-knowledge-assistant',
                          'solution-data-analyst', 'solution-devops');

-- 可安全测试的内置 REST 工具；安装器会默认关闭，待用户检查后启用。
INSERT IGNORE INTO agent_template_resource
    (id, template_version_id, resource_type, source_resource_id, resource_code, resource_name,
     resource_snapshot, content_hash, dependency_ids, object_manifest, sort_order, required)
SELECT UUID(), v.id, 'tool',
       CONCAT('75130000-0000-0000-0000-00000000000', RIGHT(t.id, 1)),
       CONCAT(t.template_code, '-tool'), CONCAT(t.template_name, '业务工具'),
       JSON_OBJECT(
           'tool_code', CONCAT(t.template_code, '-tool'), 'tool_name', CONCAT(t.template_name, '业务工具'),
           'tool_type', 'rest_api', 'description', '内置解决方案业务工具，请在目标空间检查连接配置后启用。',
           'request_method', 'POST', 'endpoint_url', 'http://mock.openagentflow.local/template/action',
           'auth_type', 'none', 'auth_config', JSON_OBJECT(), 'headers', JSON_OBJECT(),
           'request_schema', JSON_OBJECT('type', 'object', 'additionalProperties', TRUE),
           'response_schema', JSON_OBJECT('type', 'object'), 'risk_level',
           IF(t.template_code = 'solution-devops', 'high', 'low'),
           'require_confirm', IF(t.template_code = 'solution-devops', TRUE, FALSE),
           'enabled', FALSE, 'status', 'draft'),
       SHA2(CONCAT(t.template_code, ':tool:1.0.0'), 256), JSON_ARRAY(), JSON_ARRAY(), 30, 1
FROM agent_template t JOIN agent_template_version v ON v.template_id = t.id AND v.version_no = '1.0.0'
WHERE t.template_code IN ('solution-customer-service', 'solution-knowledge-assistant',
                          'solution-data-analyst', 'solution-devops');

-- 知识库、文档和示例切片。
INSERT IGNORE INTO agent_template_resource
    (id, template_version_id, resource_type, source_resource_id, resource_code, resource_name,
     resource_snapshot, content_hash, dependency_ids, object_manifest, sort_order, required)
SELECT UUID(), v.id, 'knowledge',
       CONCAT('75140000-0000-0000-0000-00000000000', RIGHT(t.id, 1)),
       CONCAT(t.template_code, '-kb'), CONCAT(t.template_name, '知识库'),
       JSON_OBJECT('kb_code', CONCAT(t.template_code, '-kb'), 'kb_name', CONCAT(t.template_name, '知识库'),
                   'description', CONCAT(t.template_name, '内置交付资料'), 'chunk_strategy', 'semantic',
                   'chunk_size', 800, 'chunk_overlap', 120, 'retrieval_mode', 'hybrid',
                   'top_k', 6, 'score_threshold', 0.45, 'visibility', 'private', 'status', 'draft'),
       SHA2(CONCAT(t.template_code, ':knowledge:1.0.0'), 256), JSON_ARRAY(), JSON_ARRAY(), 40, 1
FROM agent_template t JOIN agent_template_version v ON v.template_id = t.id AND v.version_no = '1.0.0'
WHERE t.template_code IN ('solution-customer-service', 'solution-knowledge-assistant',
                          'solution-data-analyst', 'solution-devops');

INSERT IGNORE INTO agent_template_resource
    (id, template_version_id, resource_type, source_resource_id, resource_code, resource_name,
     resource_snapshot, content_hash, parent_resource_id, dependency_ids, object_manifest, sort_order, required)
SELECT UUID(), v.id, 'document',
       CONCAT('75150000-0000-0000-0000-00000000000', RIGHT(t.id, 1)),
       CONCAT(t.template_code, '-guide'), CONCAT(t.template_name, '使用指南'),
       JSON_OBJECT('kb_id', k.source_resource_id, 'doc_name', CONCAT(t.template_name, '使用指南.md'),
                   'doc_type', 'markdown', 'source_type', 'template', 'parse_status', 'success',
                   'process_status', 'success', 'chunk_count', 1, 'metadata', JSON_OBJECT('builtin', TRUE),
                   'parsed_content', CONCAT(t.template_name, '用于演示可信检索、工具调用和工作流协作。')),
       SHA2(CONCAT(t.template_code, ':document:1.0.0'), 256), k.source_resource_id,
       JSON_ARRAY(k.source_resource_id), JSON_ARRAY(), 50, 1
FROM agent_template t
JOIN agent_template_version v ON v.template_id = t.id AND v.version_no = '1.0.0'
JOIN agent_template_resource k ON k.template_version_id = v.id AND k.resource_type = 'knowledge'
WHERE t.template_code IN ('solution-customer-service', 'solution-knowledge-assistant',
                          'solution-data-analyst', 'solution-devops');

INSERT IGNORE INTO agent_template_resource
    (id, template_version_id, resource_type, source_resource_id, resource_code, resource_name,
     resource_snapshot, content_hash, parent_resource_id, dependency_ids, object_manifest, sort_order, required)
SELECT UUID(), v.id, 'chunk',
       CONCAT('75160000-0000-0000-0000-00000000000', RIGHT(t.id, 1)),
       CONCAT(t.template_code, '-chunk-1'), CONCAT(t.template_name, '示例切片'),
       JSON_OBJECT('kb_id', k.source_resource_id, 'document_id', d.source_resource_id, 'chunk_no', 1,
                   'content', CONCAT(t.template_name, '提供可追溯的知识引用。实时业务数据必须通过工具获取，高风险动作必须经过人工确认。'),
                   'token_count', 38, 'char_count', 55, 'enabled', TRUE,
                   'metadata', JSON_OBJECT('section', '内置说明', 'builtin', TRUE)),
       SHA2(CONCAT(t.template_code, ':chunk:1.0.0'), 256), d.source_resource_id,
       JSON_ARRAY(k.source_resource_id, d.source_resource_id), JSON_ARRAY(), 60, 1
FROM agent_template t
JOIN agent_template_version v ON v.template_id = t.id AND v.version_no = '1.0.0'
JOIN agent_template_resource k ON k.template_version_id = v.id AND k.resource_type = 'knowledge'
JOIN agent_template_resource d ON d.template_version_id = v.id AND d.resource_type = 'document'
WHERE t.template_code IN ('solution-customer-service', 'solution-knowledge-assistant',
                          'solution-data-analyst', 'solution-devops');

-- 工作流快照携带节点和连线，安装完成时物化到 workflow_node/workflow_edge。
INSERT IGNORE INTO agent_template_resource
    (id, template_version_id, resource_type, source_resource_id, resource_code, resource_name,
     resource_snapshot, content_hash, dependency_ids, object_manifest, sort_order, required)
SELECT UUID(), v.id, 'workflow',
       CONCAT('75170000-0000-0000-0000-00000000000', RIGHT(t.id, 1)),
       CONCAT(t.template_code, '-workflow'), CONCAT(t.template_name, '标准工作流'),
       JSON_OBJECT(
           'workflow_code', CONCAT(t.template_code, '-workflow'), 'workflow_name', CONCAT(t.template_name, '标准工作流'),
           'description', '开始、知识检索、业务工具与输出节点组成的内置流程。',
           'graph_json', JSON_OBJECT(), 'variable_schema', JSON_OBJECT(), 'status', 'draft', 'visibility', 'private',
           'release_strategy', 'full', 'gray_percent', 100,
           '_nodes', JSON_ARRAY(
               JSON_OBJECT('node_key', 'start', 'node_name', '开始', 'node_type', 'START', 'config_json', JSON_OBJECT()),
               JSON_OBJECT('node_key', 'rag', 'node_name', '知识检索', 'node_type', 'RAG',
                           'config_json', JSON_OBJECT('knowledgeBaseId', k.source_resource_id, 'topK', 6)),
               JSON_OBJECT('node_key', 'tool', 'node_name', '业务工具', 'node_type', 'TOOL',
                           'config_json', JSON_OBJECT('toolId', x.source_resource_id, 'failureStrategy', 'CONTINUE')),
               JSON_OBJECT('node_key', 'output', 'node_name', '生成回答', 'node_type', 'OUTPUT',
                           'config_json', JSON_OBJECT('template', '{{lastOutput}}')),
               JSON_OBJECT('node_key', 'end', 'node_name', '结束', 'node_type', 'END', 'config_json', JSON_OBJECT())),
           '_edges', JSON_ARRAY(
               JSON_OBJECT('edge_key', 'e-start-rag', 'source_node_key', 'start', 'target_node_key', 'rag'),
               JSON_OBJECT('edge_key', 'e-rag-tool', 'source_node_key', 'rag', 'target_node_key', 'tool'),
               JSON_OBJECT('edge_key', 'e-tool-output', 'source_node_key', 'tool', 'target_node_key', 'output'),
               JSON_OBJECT('edge_key', 'e-output-end', 'source_node_key', 'output', 'target_node_key', 'end'))),
       SHA2(CONCAT(t.template_code, ':workflow:1.0.0'), 256),
       JSON_ARRAY(k.source_resource_id, x.source_resource_id), JSON_ARRAY(), 70, 1
FROM agent_template t
JOIN agent_template_version v ON v.template_id = t.id AND v.version_no = '1.0.0'
JOIN agent_template_resource k ON k.template_version_id = v.id AND k.resource_type = 'knowledge'
JOIN agent_template_resource x ON x.template_version_id = v.id AND x.resource_type = 'tool'
WHERE t.template_code IN ('solution-customer-service', 'solution-knowledge-assistant',
                          'solution-data-analyst', 'solution-devops');

-- Agent 绑定 Prompt、知识库、工具与工作流。
INSERT IGNORE INTO agent_template_resource
    (id, template_version_id, resource_type, source_resource_id, resource_code, resource_name,
     resource_snapshot, content_hash, dependency_ids, object_manifest, sort_order, required)
SELECT UUID(), v.id, 'agent',
       CONCAT('75180000-0000-0000-0000-00000000000', RIGHT(t.id, 1)),
       CONCAT(t.template_code, '-agent'), CONCAT(t.template_name, ' Agent'),
       JSON_OBJECT(
           'agent_code', CONCAT(t.template_code, '-agent'), 'agent_name', CONCAT(t.template_name, ' Agent'),
           'category', t.category, 'description', t.description, 'agent_type', 'workflow_agent',
           'system_prompt', JSON_UNQUOTE(JSON_EXTRACT(p.resource_snapshot, '$.content')),
           'model_params', JSON_OBJECT('temperature', 0.2, 'maxTokens', 2048),
           'memory_strategy', 'long_term', 'prompt_binding_mode', 'TEMPLATE',
           'system_prompt_template_id', p.source_resource_id, 'system_prompt_version_id', pv.source_resource_id,
           'prompt_variables', JSON_OBJECT(), 'status', 'draft', 'visibility', 'private',
           '_tool_ids', JSON_ARRAY(x.source_resource_id), '_knowledge_ids', JSON_ARRAY(k.source_resource_id),
           '_workflow_ids', JSON_ARRAY(w.source_resource_id)),
       SHA2(CONCAT(t.template_code, ':agent:1.0.0'), 256),
       JSON_ARRAY(p.source_resource_id, pv.source_resource_id, x.source_resource_id, k.source_resource_id, w.source_resource_id),
       JSON_ARRAY(), 80, 1
FROM agent_template t
JOIN agent_template_version v ON v.template_id = t.id AND v.version_no = '1.0.0'
JOIN agent_template_resource p ON p.template_version_id = v.id AND p.resource_type = 'prompt'
JOIN agent_template_resource pv ON pv.template_version_id = v.id AND pv.resource_type = 'prompt_version'
JOIN agent_template_resource x ON x.template_version_id = v.id AND x.resource_type = 'tool'
JOIN agent_template_resource k ON k.template_version_id = v.id AND k.resource_type = 'knowledge'
JOIN agent_template_resource w ON w.template_version_id = v.id AND w.resource_type = 'workflow'
WHERE t.template_code IN ('solution-customer-service', 'solution-knowledge-assistant',
                          'solution-data-analyst', 'solution-devops');

-- 团队和长期记忆模板。
INSERT IGNORE INTO agent_template_resource
    (id, template_version_id, resource_type, source_resource_id, resource_code, resource_name,
     resource_snapshot, content_hash, dependency_ids, object_manifest, sort_order, required)
SELECT UUID(), v.id, 'team',
       CONCAT('75190000-0000-0000-0000-00000000000', RIGHT(t.id, 1)),
       CONCAT(t.template_code, '-team'), CONCAT(t.template_name, '协作团队'),
       JSON_OBJECT('team_code', CONCAT(t.template_code, '-team'), 'team_name', CONCAT(t.template_name, '协作团队'),
                   'description', '内置解决方案协作团队', 'orchestration_mode', 'supervisor',
                   'status', 'draft', '_members', JSON_ARRAY(JSON_OBJECT(
                       'agent_id', a.source_resource_id, 'member_role', 'supervisor',
                       'handoff_policy', JSON_OBJECT(), 'sort_order', 1))),
       SHA2(CONCAT(t.template_code, ':team:1.0.0'), 256), JSON_ARRAY(a.source_resource_id), JSON_ARRAY(), 90, 1
FROM agent_template t
JOIN agent_template_version v ON v.template_id = t.id AND v.version_no = '1.0.0'
JOIN agent_template_resource a ON a.template_version_id = v.id AND a.resource_type = 'agent'
WHERE t.template_code IN ('solution-customer-service', 'solution-knowledge-assistant',
                          'solution-data-analyst', 'solution-devops');

INSERT IGNORE INTO agent_template_resource
    (id, template_version_id, resource_type, source_resource_id, resource_code, resource_name,
     resource_snapshot, content_hash, dependency_ids, object_manifest, sort_order, required)
SELECT UUID(), v.id, 'memory',
       CONCAT('751a0000-0000-0000-0000-00000000000', RIGHT(t.id, 1)),
       CONCAT(t.template_code, '-memory'), CONCAT(t.template_name, '长期记忆模板'),
       JSON_OBJECT('agent_id', a.source_resource_id, 'memory_type', 'semantic',
                   'memory_key', CONCAT(t.template_code, ':profile'),
                   'memory_text', '记录用户稳定偏好和已确认业务事实，不保存密码、密钥和敏感身份信息。',
                   'memory_value', JSON_OBJECT('template', TRUE), 'source_type', 'template',
                   'status', 'active', 'confidence_score', 1.0),
       SHA2(CONCAT(t.template_code, ':memory:1.0.0'), 256), JSON_ARRAY(a.source_resource_id), JSON_ARRAY(), 100, 1
FROM agent_template t
JOIN agent_template_version v ON v.template_id = t.id AND v.version_no = '1.0.0'
JOIN agent_template_resource a ON a.template_version_id = v.id AND a.resource_type = 'agent'
WHERE t.template_code IN ('solution-customer-service', 'solution-knowledge-assistant',
                          'solution-data-analyst', 'solution-devops');

-- 三个历史官方模板补建已发布版本。
INSERT IGNORE INTO agent_template_version
    (id, template_id, version_no, version_name, change_log, compatibility_statement, breaking_change,
     resource_manifest, dependency_graph, security_scan_result, runtime_check_result, package_hash,
     package_size, status, published_at, created_at)
SELECT UUID(), t.id, '1.0.0', '可安装版本', '补齐历史模板的独立副本安装能力',
       COALESCE(t.compatibility, 'OpenAgentFlow-Java 0.1+'), 0,
       JSON_OBJECT('agents', 1), JSON_OBJECT('mode', 'topological', 'resourceCount', 1),
       JSON_OBJECT('passed', TRUE), JSON_OBJECT('passed', TRUE, 'installMode', 'database_snapshot'),
       SHA2(CONCAT(t.template_code, ':1.0.0'), 256), 0, 'published', NOW(3), NOW(3)
FROM agent_template t
WHERE t.template_code IN ('customer-support', 'knowledge-qa', 'sql-analyst');

UPDATE agent_template t
JOIN agent_template_version v ON v.template_id = t.id AND v.version_no = '1.0.0'
SET t.current_version_id = v.id
WHERE t.template_code IN ('customer-support', 'knowledge-qa', 'sql-analyst')
  AND t.current_version_id IS NULL;

INSERT IGNORE INTO agent_template_resource
    (id, template_version_id, resource_type, source_resource_id, resource_code, resource_name,
     resource_snapshot, content_hash, dependency_ids, object_manifest, sort_order, required)
SELECT UUID(), v.id, 'agent', UUID(), t.template_code, t.template_name,
       JSON_OBJECT('agent_code', t.template_code, 'agent_name', t.template_name,
                   'category', t.category, 'description', t.description, 'agent_type', 'chat_agent',
                   'system_prompt', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(t.agent_snapshot, '$.agents[0].systemPrompt')),
                                             CONCAT('你是', t.template_name, '。')),
                   'model_params', JSON_OBJECT('temperature', 0.2, 'maxTokens', 2048),
                   'memory_strategy', 'none', 'prompt_binding_mode', 'MANUAL',
                   'prompt_variables', JSON_OBJECT(), 'status', 'draft', 'visibility', 'private'),
       SHA2(CONCAT(t.template_code, ':agent:1.0.0'), 256), JSON_ARRAY(), JSON_ARRAY(), 10, 1
FROM agent_template t
JOIN agent_template_version v ON v.template_id = t.id AND v.version_no = '1.0.0'
WHERE t.template_code IN ('customer-support', 'knowledge-qa', 'sql-analyst');

