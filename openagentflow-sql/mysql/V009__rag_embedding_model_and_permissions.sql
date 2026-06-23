USE openagentflow;

-- 预置豆包多模态向量模型配置；API Key 属于本地敏感配置，不写入可分享 SQL。
INSERT INTO model_config (
  id, provider_id, model_code, model_name, model_type, context_window, max_output_tokens,
  input_price_per_1k, output_price_per_1k, support_stream, support_function_calling,
  support_vision, default_params, status, is_default
) VALUES (
  '10000000-0000-0000-0000-000000000106',
  '10000000-0000-0000-0000-000000000005',
  'ep-20260615092553-lqvch',
  '豆包多模态向量接入点 ep-20260615092553-lqvch',
  'embedding',
  8192,
  0,
  0.000000,
  0.000000,
  0,
  0,
  1,
  JSON_OBJECT('purpose', 'knowledge_embedding', 'embeddingApi', 'multimodal', 'modelId', 'doubao-embedding-vision-251215'),
  'enabled',
  1
) ON DUPLICATE KEY UPDATE
  model_code = VALUES(model_code),
  model_name = VALUES(model_name),
  model_type = VALUES(model_type),
  support_vision = VALUES(support_vision),
  default_params = VALUES(default_params),
  status = VALUES(status),
  is_default = VALUES(is_default),
  updated_at = CURRENT_TIMESTAMP(3);

UPDATE knowledge_base
SET embedding_model_id = '10000000-0000-0000-0000-000000000106'
WHERE embedding_model_id IS NULL;

INSERT INTO iam_permission (
  id, permission_code, permission_name, permission_type, api_method, api_path,
  sort_order, visible, status
) VALUES
  ('30000000-0000-0000-0000-000000000201', 'knowledge:view', '知识库查看', 'api', 'GET', '/api/knowledge-bases/**', 201, 0, 'enabled'),
  ('30000000-0000-0000-0000-000000000202', 'knowledge:create', '知识库创建', 'api', 'POST', '/api/knowledge-bases', 202, 0, 'enabled'),
  ('30000000-0000-0000-0000-000000000203', 'knowledge:update', '知识库编辑', 'api', 'PUT', '/api/knowledge-bases/**', 203, 0, 'enabled'),
  ('30000000-0000-0000-0000-000000000204', 'knowledge:delete', '知识库删除', 'api', 'DELETE', '/api/knowledge-bases/**', 204, 0, 'enabled'),
  ('30000000-0000-0000-0000-000000000205', 'knowledge:upload', '知识库文档上传', 'api', 'POST', '/api/knowledge-bases/*/documents', 205, 0, 'enabled'),
  ('30000000-0000-0000-0000-000000000206', 'knowledge:retrieve', '知识库检索测试', 'api', 'POST', '/api/knowledge-bases/*/retrieval-test', 206, 0, 'enabled'),
  ('30000000-0000-0000-0000-000000000207', 'knowledge:bind', '知识库绑定 Agent', 'api', 'PUT', '/api/agents/*/knowledge-bases', 207, 0, 'enabled')
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  permission_type = VALUES(permission_type),
  api_method = VALUES(api_method),
  api_path = VALUES(api_path),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status);

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM iam_role r
JOIN iam_permission p ON p.permission_code IN (
  'knowledge:view', 'knowledge:create', 'knowledge:update', 'knowledge:delete',
  'knowledge:upload', 'knowledge:retrieve', 'knowledge:bind'
)
WHERE r.role_code IN ('super_admin', 'admin');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM iam_role r
JOIN iam_permission p ON p.permission_code IN ('knowledge:view', 'knowledge:retrieve')
WHERE r.role_code IN ('developer', 'viewer');
