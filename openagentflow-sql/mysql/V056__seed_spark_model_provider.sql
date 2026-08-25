USE openagentflow;

-- 预置讯飞星火服务商。星火走 WebSocket + HMAC-SHA256 动态签名鉴权（provider_type=spark），
-- 与 OpenAI-compatible 完全不同，由后端 ModelChatClientRouter 按 provider_type 分发到 SparkChatClient。
-- 星火三凭证（APPID/APIKey/APISecret）以 {appId}:{apiKey}:{apiSecret} 复合串存储，
-- 属于敏感信息，不写入可上传 GitHub 的 SQL 文件，请在平台"模型供应商配置"中补录。
INSERT INTO model_provider (
  id, provider_code, provider_name, provider_type, base_url, auth_type,
  default_headers, status, health_status, sort_order
) VALUES (
  '10000000-0000-0000-0000-000000000006',
  'spark',
  '讯飞星火 Spark',
  'spark',
  'wss://spark-api.xf-yun.com/v4.0/chat',
  'api_key',
  JSON_OBJECT(),
  'enabled',
  'unknown',
  6
) ON DUPLICATE KEY UPDATE
  provider_name = VALUES(provider_name),
  provider_type = VALUES(provider_type),
  base_url = VALUES(base_url),
  auth_type = VALUES(auth_type),
  status = VALUES(status),
  sort_order = VALUES(sort_order);

-- 星火模型使用 Spark Ultra 4.0 接入点，domain 从 default_params.domain 读取。
INSERT INTO model_config (
  id, provider_id, model_code, model_name, model_type, context_window,
  max_output_tokens, default_params, support_stream, support_function_calling,
  support_vision, status, is_default
) VALUES (
  '10000000-0000-0000-0000-000000000106',
  '10000000-0000-0000-0000-000000000006',
  'spark-4.0-ultra',
  'Spark Ultra 4.0',
  'chat',
  32768,
  8192,
  JSON_OBJECT('domain', '4.0Ultra'),
  1,
  0,
  0,
  'enabled',
  0
) ON DUPLICATE KEY UPDATE
  model_name = VALUES(model_name),
  context_window = VALUES(context_window),
  max_output_tokens = VALUES(max_output_tokens),
  default_params = VALUES(default_params),
  support_stream = VALUES(support_stream),
  status = VALUES(status);