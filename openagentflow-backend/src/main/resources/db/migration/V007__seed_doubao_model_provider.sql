USE openagentflow;

-- 预置豆包方舟服务商。API Key 属于敏感信息，不写入可上传 GitHub 的 SQL 文件。
INSERT INTO model_provider (
  id, provider_code, provider_name, provider_type, base_url, auth_type,
  default_headers, status, health_status, sort_order
) VALUES (
  '10000000-0000-0000-0000-000000000005',
  'doubao',
  '豆包 Doubao',
  'openai_compatible',
  'https://ark.cn-beijing.volces.com/api/v3',
  'api_key',
  JSON_OBJECT(),
  'enabled',
  'unknown',
  5
) ON DUPLICATE KEY UPDATE
  provider_name = VALUES(provider_name),
  provider_type = VALUES(provider_type),
  base_url = VALUES(base_url),
  auth_type = VALUES(auth_type),
  status = VALUES(status),
  sort_order = VALUES(sort_order);

-- 第一版真实模型测试默认使用豆包接入点。
UPDATE model_config SET is_default = 0 WHERE model_type = 'chat';

INSERT INTO model_config (
  id, provider_id, model_code, model_name, model_type, context_window,
  max_output_tokens, default_params, support_stream, support_function_calling,
  support_vision, status, is_default
) VALUES (
  '10000000-0000-0000-0000-000000000105',
  '10000000-0000-0000-0000-000000000005',
  'ep-20260605102340-bwv2d',
  '豆包接入点 ep-20260605102340-bwv2d',
  'chat',
  32768,
  4096,
  JSON_OBJECT('temperature', 0.3),
  1,
  0,
  0,
  'enabled',
  1
) ON DUPLICATE KEY UPDATE
  model_name = VALUES(model_name),
  context_window = VALUES(context_window),
  max_output_tokens = VALUES(max_output_tokens),
  default_params = VALUES(default_params),
  support_stream = VALUES(support_stream),
  status = VALUES(status),
  is_default = VALUES(is_default);
