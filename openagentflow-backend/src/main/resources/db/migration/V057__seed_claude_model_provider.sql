USE openagentflow;

-- 预置 Anthropic Claude 服务商。Claude 走原生 Messages API（REST + SSE，x-api-key + anthropic-version 鉴权，
-- tool_use/tool_result content block 多轮回合，provider_type=claude），与 OpenAI-compatible、星火 WebSocket
-- 都不同，由后端 ModelChatClientRouter 按 provider_type 分发到 ClaudeChatClient。
-- API Key 属于敏感信息，不写入可上传 GitHub 的 SQL 文件，请在平台"模型供应商配置"中补录。
INSERT INTO model_provider (
  id, provider_code, provider_name, provider_type, base_url, auth_type,
  default_headers, status, health_status, sort_order
) VALUES (
  '10000000-0000-0000-0000-000000000007',
  'claude',
  'Anthropic Claude',
  'claude',
  'https://api.anthropic.com',
  'api_key',
  JSON_OBJECT(),
  'enabled',
  'unknown',
  7
) ON DUPLICATE KEY UPDATE
  provider_name = VALUES(provider_name),
  provider_type = VALUES(provider_type),
  base_url = VALUES(base_url),
  auth_type = VALUES(auth_type),
  status = VALUES(status),
  sort_order = VALUES(sort_order);

-- Claude Sonnet 5：上下文 200K，支持流式、工具调用与多模态。
INSERT INTO model_config (
  id, provider_id, model_code, model_name, model_type, context_window,
  max_output_tokens, default_params, support_stream, support_function_calling,
  support_vision, status, is_default
) VALUES (
  '10000000-0000-0000-0000-000000000107',
  '10000000-0000-0000-0000-000000000007',
  'claude-sonnet-5',
  'Claude Sonnet 5',
  'chat',
  200000,
  32000,
  JSON_OBJECT(),
  1,
  1,
  1,
  'enabled',
  0
) ON DUPLICATE KEY UPDATE
  model_name = VALUES(model_name),
  context_window = VALUES(context_window),
  max_output_tokens = VALUES(max_output_tokens),
  default_params = VALUES(default_params),
  support_stream = VALUES(support_stream),
  status = VALUES(status);
