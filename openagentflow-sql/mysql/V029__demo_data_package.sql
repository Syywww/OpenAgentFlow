USE openagentflow;

-- P33：一键演示数据与交付样例包。
-- 本脚本只写入可公开分享的样例资源，不包含任何真实模型 API Key。

CREATE TABLE IF NOT EXISTS model_price_config (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  provider_id char(36) DEFAULT NULL COMMENT '服务商ID',
  model_id char(36) DEFAULT NULL COMMENT '模型ID',
  price_code varchar(120) NOT NULL COMMENT '价格配置编码',
  price_name varchar(160) NOT NULL COMMENT '价格配置名称',
  currency varchar(16) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  input_price_per_1k decimal(12,8) NOT NULL DEFAULT 0 COMMENT '每千输入Token价格',
  output_price_per_1k decimal(12,8) NOT NULL DEFAULT 0 COMMENT '每千输出Token价格',
  billing_unit varchar(32) NOT NULL DEFAULT 'token_1k' COMMENT '计费单位',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  effective_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '生效时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_model_price_code(price_code),
  KEY idx_model_price_model(model_id, enabled),
  KEY idx_model_price_provider(provider_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模型价格配置表';

SET @quota_enabled_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'model_usage_quota'
    AND COLUMN_NAME = 'enabled'
);
SET @quota_enabled_sql := IF(
  @quota_enabled_exists = 0,
  'ALTER TABLE model_usage_quota ADD COLUMN enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT ''是否启用'' AFTER cost_used',
  'SELECT 1'
);
PREPARE quota_enabled_stmt FROM @quota_enabled_sql;
EXECUTE quota_enabled_stmt;
DEALLOCATE PREPARE quota_enabled_stmt;

UPDATE model_config
SET input_price_per_1k = 0.0008,
    output_price_per_1k = 0.0020
WHERE id = '10000000-0000-0000-0000-000000000105';

UPDATE model_config
SET input_price_per_1k = 0.0001,
    output_price_per_1k = 0.0000
WHERE id = '10000000-0000-0000-0000-000000000106';

INSERT INTO model_price_config (
  id, provider_id, model_id, price_code, price_name, currency,
  input_price_per_1k, output_price_per_1k, billing_unit, enabled
) VALUES
  (
    '91000000-0000-0000-0000-000000000101',
    '10000000-0000-0000-0000-000000000005',
    '10000000-0000-0000-0000-000000000105',
    'demo-doubao-chat-price',
    '演示豆包聊天模型价格',
    'CNY',
    0.00080000,
    0.00200000,
    'token_1k',
    1
  ),
  (
    '91000000-0000-0000-0000-000000000102',
    '10000000-0000-0000-0000-000000000005',
    '10000000-0000-0000-0000-000000000106',
    'demo-doubao-embedding-price',
    '演示豆包向量模型价格',
    'CNY',
    0.00010000,
    0.00000000,
    'token_1k',
    1
  )
ON DUPLICATE KEY UPDATE
  price_name = VALUES(price_name),
  currency = VALUES(currency),
  input_price_per_1k = VALUES(input_price_per_1k),
  output_price_per_1k = VALUES(output_price_per_1k),
  billing_unit = VALUES(billing_unit),
  enabled = VALUES(enabled),
  updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO prompt_template (
  id, template_code, template_name, prompt_type, content, variables,
  description, status, owner_user_id
) VALUES
  (
    '20000000-0000-0000-0000-000000000101',
    'demo-customer-service-system',
    '演示客服助手 System Prompt',
    'system',
    '你是 OpenAgentFlow 演示客服助手。请优先使用知识库资料回答，涉及订单状态时可以调用已授权工具。回答必须结构清晰；当知识库不足时说明资料不足，不编造政策。',
    JSON_ARRAY(
      JSON_OBJECT('name','context','description','知识库上下文'),
      JSON_OBJECT('name','user_input','description','用户输入'),
      JSON_OBJECT('name','tool_result','description','工具返回结果')
    ),
    '用于五分钟演示链路的客服 Agent 默认提示词。',
    'published',
    '00000000-0000-0000-0000-000000000100'
  ),
  (
    '20000000-0000-0000-0000-000000000102',
    'demo-trusted-rag-answer',
    '演示可信 RAG 回答 Prompt',
    'rag',
    '请仅基于引用来源回答问题。若引用来源不足，请输出“当前知识库资料不足，无法确认”。答案末尾列出来源编号和页码。',
    JSON_ARRAY(
      JSON_OBJECT('name','sources','description','引用来源列表'),
      JSON_OBJECT('name','question','description','用户问题')
    ),
    '用于展示可信回答模式、强制引用和低置信拒答。',
    'published',
    '00000000-0000-0000-0000-000000000100'
  )
ON DUPLICATE KEY UPDATE
  template_name = VALUES(template_name),
  prompt_type = VALUES(prompt_type),
  content = VALUES(content),
  variables = VALUES(variables),
  description = VALUES(description),
  status = VALUES(status),
  updated_at = CURRENT_TIMESTAMP(3);

INSERT IGNORE INTO prompt_template_version (
  id, template_id, version_no, content, variables, change_note, created_by
)
SELECT UUID(), id, 'v1', content, variables, 'P33 演示样例包初始版本', owner_user_id
FROM prompt_template
WHERE template_code IN ('demo-customer-service-system', 'demo-trusted-rag-answer');

INSERT INTO agent (
  id, agent_code, agent_name, workspace_id, category, description, agent_type,
  model_id, system_prompt_template_id, system_prompt, model_params, memory_strategy,
  visibility, status, published_version, owner_user_id, created_by
) VALUES
  (
    '30000000-0000-0000-0000-000000000001',
    'customer-support-agent',
    '客服助手',
    '90000000-0000-0000-0000-000000000101',
    '客服',
    '演示用客服助手，串联 RAG 知识库、订单工具、可信回答和 Trace。',
    'rag_tool_agent',
    '10000000-0000-0000-0000-000000000105',
    '20000000-0000-0000-0000-000000000101',
    '你是企业智能客服助手，请基于知识库和工具结果回答用户问题。涉及退款、赔付、账号安全等高风险内容时必须提示人工确认。',
    JSON_OBJECT('temperature', 0.3, 'maxTokens', 2048, 'topP', 0.9),
    'long_term',
    'team',
    'published',
    'v1',
    '00000000-0000-0000-0000-000000000100',
    '00000000-0000-0000-0000-000000000100'
  ),
  (
    '30000000-0000-0000-0000-000000000002',
    'order-analyst-agent',
    '订单分析助手',
    '90000000-0000-0000-0000-000000000101',
    '数据分析',
    '演示用订单分析 Agent，负责把客户问题转成工具参数并解释查询结果。',
    'tool_agent',
    '10000000-0000-0000-0000-000000000105',
    '20000000-0000-0000-0000-000000000101',
    '你是订单分析助手。请识别订单号、问题类型和需要调用的工具，并用简洁语言解释工具返回结果。',
    JSON_OBJECT('temperature', 0.2, 'maxTokens', 1200, 'topP', 0.8),
    'short_term',
    'team',
    'published',
    'v1',
    '00000000-0000-0000-0000-000000000100',
    '00000000-0000-0000-0000-000000000100'
  ),
  (
    '30000000-0000-0000-0000-000000000003',
    'quality-review-agent',
    '质量复核助手',
    '90000000-0000-0000-0000-000000000101',
    '质量治理',
    '演示用复核 Agent，检查回答是否引用来源、是否越权承诺、是否需要人工升级。',
    'review_agent',
    '10000000-0000-0000-0000-000000000105',
    '20000000-0000-0000-0000-000000000102',
    '你是质量复核助手。请检查回答是否基于引用来源，是否存在超出知识库的承诺，并给出可执行修改建议。',
    JSON_OBJECT('temperature', 0.1, 'maxTokens', 1200, 'topP', 0.8),
    'none',
    'team',
    'published',
    'v1',
    '00000000-0000-0000-0000-000000000100',
    '00000000-0000-0000-0000-000000000100'
  )
ON DUPLICATE KEY UPDATE
  agent_name = VALUES(agent_name),
  workspace_id = VALUES(workspace_id),
  category = VALUES(category),
  description = VALUES(description),
  agent_type = VALUES(agent_type),
  model_id = VALUES(model_id),
  system_prompt_template_id = VALUES(system_prompt_template_id),
  system_prompt = VALUES(system_prompt),
  model_params = VALUES(model_params),
  memory_strategy = VALUES(memory_strategy),
  visibility = VALUES(visibility),
  status = VALUES(status),
  published_version = VALUES(published_version),
  updated_at = CURRENT_TIMESTAMP(3),
  deleted_at = NULL;

INSERT IGNORE INTO agent_version (
  id, agent_id, version_no, snapshot, publish_note, status, created_by
)
SELECT UUID(), id, 'v1',
       JSON_OBJECT('agentCode', agent_code, 'agentName', agent_name, 'modelId', model_id, 'systemPrompt', system_prompt),
       'P33 演示样例包默认发布版本',
       'published',
       created_by
FROM agent
WHERE agent_code IN ('customer-support-agent', 'order-analyst-agent', 'quality-review-agent');

INSERT INTO knowledge_base (
  id, kb_code, kb_name, workspace_id, description, embedding_model_id,
  vector_connection_id, vector_collection_id, milvus_collection_name,
  milvus_partition_name, chunk_strategy, chunk_size, chunk_overlap,
  visibility, status, owner_user_id, created_by
) VALUES (
  '40000000-0000-0000-0000-000000000001',
  'product-manual-kb',
  '产品手册知识库',
  '90000000-0000-0000-0000-000000000101',
  '演示知识库，包含客服政策、订单查询、退款边界、工单升级和可信回答要求。',
  '10000000-0000-0000-0000-000000000106',
  '70000000-0000-0000-0000-000000000001',
  '70000000-0000-0000-0000-000000000101',
  'oaf_knowledge_chunks',
  'kb_product_manual',
  'fixed_size',
  512,
  64,
  'team',
  'active',
  '00000000-0000-0000-0000-000000000100',
  '00000000-0000-0000-0000-000000000100'
) ON DUPLICATE KEY UPDATE
  kb_name = VALUES(kb_name),
  workspace_id = VALUES(workspace_id),
  description = VALUES(description),
  embedding_model_id = VALUES(embedding_model_id),
  vector_connection_id = VALUES(vector_connection_id),
  vector_collection_id = VALUES(vector_collection_id),
  milvus_collection_name = VALUES(milvus_collection_name),
  milvus_partition_name = VALUES(milvus_partition_name),
  chunk_strategy = VALUES(chunk_strategy),
  chunk_size = VALUES(chunk_size),
  chunk_overlap = VALUES(chunk_overlap),
  visibility = VALUES(visibility),
  status = VALUES(status),
  updated_at = CURRENT_TIMESTAMP(3),
  deleted_at = NULL;

INSERT INTO knowledge_document (
  id, kb_id, doc_name, doc_type, file_ext, file_size, file_hash,
  storage_bucket, storage_key, source_type, source_url, parse_status,
  metadata, uploaded_by
) VALUES (
  '41000000-0000-0000-0000-000000000001',
  '40000000-0000-0000-0000-000000000001',
  'OpenAgentFlow 客服演示知识库.md',
  'markdown',
  'md',
  8192,
  'demo-product-manual-kb-v1',
  'local-docs',
  'docs/demo/OpenAgentFlow-客服演示知识库.md',
  'seed',
  'docs/demo/OpenAgentFlow-客服演示知识库.md',
  'parsed',
  JSON_OBJECT('demo', true, 'package', 'P33', 'pages', 4),
  '00000000-0000-0000-0000-000000000100'
) ON DUPLICATE KEY UPDATE
  doc_name = VALUES(doc_name),
  doc_type = VALUES(doc_type),
  file_ext = VALUES(file_ext),
  file_size = VALUES(file_size),
  file_hash = VALUES(file_hash),
  storage_bucket = VALUES(storage_bucket),
  storage_key = VALUES(storage_key),
  source_type = VALUES(source_type),
  source_url = VALUES(source_url),
  parse_status = VALUES(parse_status),
  metadata = VALUES(metadata),
  updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO knowledge_document_parse_task (
  id, document_id, task_type, status, progress, config, result,
  started_at, finished_at
) VALUES (
  '41000000-0000-0000-0000-000000000101',
  '41000000-0000-0000-0000-000000000001',
  'seed_parse',
  'success',
  100.00,
  JSON_OBJECT('source', 'P33 demo seed'),
  JSON_OBJECT('chunkCount', 4, 'embeddingCount', 4, 'milvusFallback', true),
  DATE_SUB(NOW(3), INTERVAL 5 MINUTE),
  DATE_SUB(NOW(3), INTERVAL 4 MINUTE)
) ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  progress = VALUES(progress),
  result = VALUES(result),
  finished_at = VALUES(finished_at);

INSERT INTO knowledge_chunk (
  id, kb_id, document_id, chunk_no, title, content, token_count,
  page_no, start_offset, end_offset, metadata, status
) VALUES
  (
    '42000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    '41000000-0000-0000-0000-000000000001',
    1,
    '售后响应与服务边界',
    '客服助手应在 30 秒内确认客户问题类型，并补齐客户称呼、订单号、产品名称、问题现象、发生时间和期望处理结果。涉及退款、赔付、合同承诺、账号安全和数据删除时，客服助手不得直接承诺最终结果，应升级人工确认。',
    96,
    1,
    0,
    96,
    JSON_OBJECT('section','售后响应','source','P33 demo'),
    'active'
  ),
  (
    '42000000-0000-0000-0000-000000000002',
    '40000000-0000-0000-0000-000000000001',
    '41000000-0000-0000-0000-000000000001',
    2,
    '订单状态查询规则',
    '当客户询问订单状态时，客服助手应识别订单号并调用订单状态查询工具。若工具返回 delivered，回复应说明已签收；若返回 shipping，回复应说明物流单号和预计送达时间；若工具失败，应告知客户系统暂时不可用并建议稍后重试或转人工。',
    104,
    2,
    97,
    201,
    JSON_OBJECT('section','订单查询','source','P33 demo'),
    'active'
  ),
  (
    '42000000-0000-0000-0000-000000000003',
    '40000000-0000-0000-0000-000000000001',
    '41000000-0000-0000-0000-000000000001',
    3,
    '可信回答与引用来源',
    '可信回答模式要求至少命中一条可靠引用来源。回答中应使用“来源1、来源2”的形式标注依据。若知识库没有覆盖用户问题，客服助手应说明“当前知识库资料不足，无法确认”，不得编造不存在的政策、价格或服务承诺。',
    98,
    3,
    202,
    300,
    JSON_OBJECT('section','可信回答','source','P33 demo'),
    'active'
  ),
  (
    '42000000-0000-0000-0000-000000000004',
    '40000000-0000-0000-0000-000000000001',
    '41000000-0000-0000-0000-000000000001',
    4,
    '工单创建与升级建议',
    '创建工单前应汇总问题摘要、已收集字段、建议优先级和下一步动作。高优先级工单包括账号无法登录、支付异常、数据丢失、重复扣费和客户明确投诉。客服助手可以生成工单草稿，但最终提交高风险动作需要人工确认。',
    102,
    4,
    301,
    403,
    JSON_OBJECT('section','工单升级','source','P33 demo'),
    'active'
  )
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  content = VALUES(content),
  token_count = VALUES(token_count),
  page_no = VALUES(page_no),
  start_offset = VALUES(start_offset),
  end_offset = VALUES(end_offset),
  metadata = VALUES(metadata),
  status = VALUES(status);

INSERT INTO knowledge_embedding (
  id, chunk_id, kb_id, model_id, vector_collection_id, vector_partition_id,
  milvus_collection_name, milvus_partition_name, vector_primary_key,
  sync_status, embedding_json, embedding_dim, content_hash
) VALUES
  (
    '43000000-0000-0000-0000-000000000001',
    '42000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000106',
    '70000000-0000-0000-0000-000000000101',
    '70000000-0000-0000-0000-000000000201',
    'oaf_knowledge_chunks',
    'kb_product_manual',
    'demo_chunk_420000000000000000000000000000000001',
    'mysql_fallback',
    JSON_ARRAY(0.18,0.42,0.36,0.67,0.21,0.09,0.54,0.33,0.71,0.27,0.63,0.16,0.45,0.58,0.24,0.39),
    16,
    MD5((SELECT content FROM knowledge_chunk WHERE id = '42000000-0000-0000-0000-000000000001'))
  ),
  (
    '43000000-0000-0000-0000-000000000002',
    '42000000-0000-0000-0000-000000000002',
    '40000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000106',
    '70000000-0000-0000-0000-000000000101',
    '70000000-0000-0000-0000-000000000201',
    'oaf_knowledge_chunks',
    'kb_product_manual',
    'demo_chunk_420000000000000000000000000000000002',
    'mysql_fallback',
    JSON_ARRAY(0.66,0.11,0.29,0.72,0.48,0.22,0.31,0.87,0.13,0.64,0.35,0.52,0.26,0.41,0.73,0.19),
    16,
    MD5((SELECT content FROM knowledge_chunk WHERE id = '42000000-0000-0000-0000-000000000002'))
  ),
  (
    '43000000-0000-0000-0000-000000000003',
    '42000000-0000-0000-0000-000000000003',
    '40000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000106',
    '70000000-0000-0000-0000-000000000101',
    '70000000-0000-0000-0000-000000000201',
    'oaf_knowledge_chunks',
    'kb_product_manual',
    'demo_chunk_420000000000000000000000000000000003',
    'mysql_fallback',
    JSON_ARRAY(0.44,0.59,0.12,0.38,0.76,0.25,0.69,0.17,0.52,0.83,0.28,0.47,0.61,0.14,0.34,0.55),
    16,
    MD5((SELECT content FROM knowledge_chunk WHERE id = '42000000-0000-0000-0000-000000000003'))
  ),
  (
    '43000000-0000-0000-0000-000000000004',
    '42000000-0000-0000-0000-000000000004',
    '40000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000106',
    '70000000-0000-0000-0000-000000000101',
    '70000000-0000-0000-0000-000000000201',
    'oaf_knowledge_chunks',
    'kb_product_manual',
    'demo_chunk_420000000000000000000000000000000004',
    'mysql_fallback',
    JSON_ARRAY(0.31,0.74,0.23,0.57,0.18,0.62,0.49,0.35,0.81,0.16,0.53,0.29,0.68,0.22,0.46,0.77),
    16,
    MD5((SELECT content FROM knowledge_chunk WHERE id = '42000000-0000-0000-0000-000000000004'))
  )
ON DUPLICATE KEY UPDATE
  model_id = VALUES(model_id),
  vector_collection_id = VALUES(vector_collection_id),
  vector_partition_id = VALUES(vector_partition_id),
  milvus_collection_name = VALUES(milvus_collection_name),
  milvus_partition_name = VALUES(milvus_partition_name),
  vector_primary_key = VALUES(vector_primary_key),
  sync_status = VALUES(sync_status),
  embedding_json = VALUES(embedding_json),
  embedding_dim = VALUES(embedding_dim),
  content_hash = VALUES(content_hash);

INSERT IGNORE INTO vector_record_mapping (
  id, collection_id, partition_id, vector_primary_key, resource_type,
  resource_id, embedding_model_id, content_hash, status
)
SELECT UUID(),
       vector_collection_id,
       vector_partition_id,
       vector_primary_key,
       'knowledge_chunk',
       chunk_id,
       model_id,
       content_hash,
       'active'
FROM knowledge_embedding
WHERE id IN (
  '43000000-0000-0000-0000-000000000001',
  '43000000-0000-0000-0000-000000000002',
  '43000000-0000-0000-0000-000000000003',
  '43000000-0000-0000-0000-000000000004'
);

CREATE TABLE IF NOT EXISTS demo_order (
  id char(36) NOT NULL COMMENT '主键ID',
  order_no varchar(64) NOT NULL COMMENT '订单号',
  customer_name varchar(80) NOT NULL COMMENT '客户名称',
  status varchar(32) NOT NULL COMMENT '订单状态',
  status_text varchar(80) NOT NULL COMMENT '订单状态中文',
  total_amount decimal(12,2) NOT NULL DEFAULT 0 COMMENT '订单金额',
  carrier varchar(80) DEFAULT NULL COMMENT '承运商',
  logistics_no varchar(80) DEFAULT NULL COMMENT '物流单号',
  current_location varchar(160) DEFAULT NULL COMMENT '当前位置',
  eta varchar(120) DEFAULT NULL COMMENT '预计送达时间',
  refund_policy varchar(500) DEFAULT NULL COMMENT '退款处理建议',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_demo_order_no(order_no),
  KEY idx_demo_order_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='演示订单表';

INSERT INTO demo_order (
  id, order_no, customer_name, status, status_text, total_amount,
  carrier, logistics_no, current_location, eta, refund_policy
) VALUES (
  '96000000-0000-0000-0000-000000000101',
  'OAF-DEMO-1001',
  '演示客户',
  'shipping',
  '运输中',
  199.00,
  '顺丰速运',
  'SF-DEMO-001',
  '上海分拨中心',
  '明天18:00前',
  '运输中订单建议先安抚客户并确认签收时效；如客户坚持退款，按知识库售后规则发起人工复核。'
)
ON DUPLICATE KEY UPDATE
  customer_name = VALUES(customer_name),
  status = VALUES(status),
  status_text = VALUES(status_text),
  total_amount = VALUES(total_amount),
  carrier = VALUES(carrier),
  logistics_no = VALUES(logistics_no),
  current_location = VALUES(current_location),
  eta = VALUES(eta),
  refund_policy = VALUES(refund_policy),
  updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO tool_definition (
  id, tool_code, tool_name, tool_type, workspace_id, description,
  request_method, endpoint_url, auth_type, auth_config, headers,
  request_schema, response_schema, timeout_ms, retry_count, risk_level,
  require_confirm, enabled, status, source_type, owner_user_id, created_by
) VALUES
  (
    '50000000-0000-0000-0000-000000000101',
    'demo_order_status_rest',
    '演示订单状态查询 REST',
    'REST_API',
    '90000000-0000-0000-0000-000000000101',
    '低风险只读 REST 工具，用于查询订单状态、物流单号、预计送达时间、订单数量和订单列表摘要。',
    'GET',
    'https://mock.openagentflow.local/orders/{orderId}',
    'none',
    JSON_OBJECT(),
    JSON_OBJECT('Accept','application/json'),
    JSON_OBJECT('type','object','properties',JSON_OBJECT('orderId',JSON_OBJECT('type','string','description','订单号或完整用户问题'))),
    JSON_OBJECT('type','object','properties',JSON_OBJECT('queryType',JSON_OBJECT('type','string'),'orderCount',JSON_OBJECT('type','integer'),'orders',JSON_OBJECT('type','array'),'status',JSON_OBJECT('type','string'),'trackingNo',JSON_OBJECT('type','string'),'eta',JSON_OBJECT('type','string'))),
    5000,
    1,
    'low',
    0,
    1,
    'active',
    'demo_seed',
    '00000000-0000-0000-0000-000000000100',
    '00000000-0000-0000-0000-000000000100'
  ),
  (
    '50000000-0000-0000-0000-000000000102',
    'demo_customer_event_webhook',
    '演示客户事件 Webhook',
    'WEBHOOK',
    '90000000-0000-0000-0000-000000000101',
    '中风险 Webhook 工具，用于把客户投诉、退款意向和升级请求推送到外部系统。',
    'POST',
    'https://mock.openagentflow.local/webhook/customer-events',
    'signature',
    JSON_OBJECT('signatureHeader','X-OAF-Signature','secretMask','demo****secret'),
    JSON_OBJECT('Content-Type','application/json'),
    JSON_OBJECT('type','object','required',JSON_ARRAY('eventType','summary'),'properties',JSON_OBJECT('eventType',JSON_OBJECT('type','string'),'summary',JSON_OBJECT('type','string'),'priority',JSON_OBJECT('type','string'))),
    JSON_OBJECT('type','object','properties',JSON_OBJECT('accepted',JSON_OBJECT('type','boolean'),'eventId',JSON_OBJECT('type','string'))),
    8000,
    1,
    'medium',
    1,
    1,
    'active',
    'demo_seed',
    '00000000-0000-0000-0000-000000000100',
    '00000000-0000-0000-0000-000000000100'
  ),
  (
    '50000000-0000-0000-0000-000000000103',
    'demo_readonly_order_sql',
    '演示只读订单 SQL',
    'DB_QUERY',
    '90000000-0000-0000-0000-000000000101',
    '数据库查询工具样例，仅允许只读 SQL 模板，用于展示数据库工具治理。',
    'POST',
    'select order_no,status,total_amount from demo_order where order_no = {orderNo} limit 1',
    'none',
    JSON_OBJECT('readonly', true, 'sqlTemplate', 'select order_no,status,total_amount from demo_order where order_no = :orderNo limit 1'),
    JSON_OBJECT(),
    JSON_OBJECT('type','object','required',JSON_ARRAY('orderNo'),'properties',JSON_OBJECT('orderNo',JSON_OBJECT('type','string','description','订单号'))),
    JSON_OBJECT('type','object','properties',JSON_OBJECT('orderNo',JSON_OBJECT('type','string'),'status',JSON_OBJECT('type','string'),'totalAmount',JSON_OBJECT('type','number'))),
    5000,
    0,
    'low',
    0,
    1,
    'active',
    'demo_seed',
    '00000000-0000-0000-0000-000000000100',
    '00000000-0000-0000-0000-000000000100'
  )
ON DUPLICATE KEY UPDATE
  tool_name = VALUES(tool_name),
  tool_type = VALUES(tool_type),
  workspace_id = VALUES(workspace_id),
  description = VALUES(description),
  request_method = VALUES(request_method),
  endpoint_url = VALUES(endpoint_url),
  auth_type = VALUES(auth_type),
  auth_config = VALUES(auth_config),
  headers = VALUES(headers),
  request_schema = VALUES(request_schema),
  response_schema = VALUES(response_schema),
  timeout_ms = VALUES(timeout_ms),
  retry_count = VALUES(retry_count),
  risk_level = VALUES(risk_level),
  require_confirm = VALUES(require_confirm),
  enabled = VALUES(enabled),
  status = VALUES(status),
  source_type = VALUES(source_type),
  updated_at = CURRENT_TIMESTAMP(3),
  deleted_at = NULL;

INSERT INTO tool_test_case (
  id, tool_id, case_name, input_params, expected_result, created_by
) VALUES
  (
    '50000000-0000-0000-0000-000000000201',
    '50000000-0000-0000-0000-000000000101',
    '查询运输中订单',
    JSON_OBJECT('orderId','OAF-DEMO-1001'),
    JSON_OBJECT('status','shipping','trackingNo','SF-DEMO-001','eta','明天18:00前'),
    '00000000-0000-0000-0000-000000000100'
  ),
  (
    '50000000-0000-0000-0000-000000000202',
    '50000000-0000-0000-0000-000000000102',
    '推送客户投诉事件',
    JSON_OBJECT('eventType','complaint','summary','客户反馈重复扣费','priority','high'),
    JSON_OBJECT('accepted',true),
    '00000000-0000-0000-0000-000000000100'
  ),
  (
    '50000000-0000-0000-0000-000000000203',
    '50000000-0000-0000-0000-000000000103',
    '只读查询订单',
    JSON_OBJECT('orderNo','OAF-DEMO-1001'),
    JSON_OBJECT('orderNo','OAF-DEMO-1001','status','shipping','totalAmount',199.00),
    '00000000-0000-0000-0000-000000000100'
  )
ON DUPLICATE KEY UPDATE
  case_name = VALUES(case_name),
  input_params = VALUES(input_params),
  expected_result = VALUES(expected_result),
  updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO agent_knowledge_binding (
  agent_id, knowledge_base_id, retrieval_config, enabled
) VALUES (
  '30000000-0000-0000-0000-000000000001',
  '40000000-0000-0000-0000-000000000001',
  JSON_OBJECT(
    'topK', 4,
    'candidateK', 16,
    'scoreThreshold', 0.18,
    'lowConfidenceThreshold', 0.45,
    'searchMode', 'keyword',
    'rerankEnabled', true,
    'rejectLowConfidence', true,
    'trustedAnswerMode', true,
    'citationRequired', true,
    'minCitationCount', 1,
    'vectorWeight', 0.30,
    'keywordWeight', 0.70
  ),
  1
) ON DUPLICATE KEY UPDATE
  retrieval_config = VALUES(retrieval_config),
  enabled = VALUES(enabled);

INSERT INTO agent_tool_binding (
  agent_id, tool_id, tool_config, require_confirm, enabled
) VALUES
  (
    '30000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000101',
    JSON_OBJECT('demo', true, 'usage', '订单状态查询', 'autoCall', true),
    0,
    1
  ),
  (
    '30000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000102',
    JSON_OBJECT('demo', true, 'usage', '客户事件推送', 'confirmReason', '中风险外部推送'),
    1,
    1
  ),
  (
    '30000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000103',
    JSON_OBJECT('demo', true, 'usage', '只读订单查询', 'readonly', true),
    0,
    1
  ),
  (
    '30000000-0000-0000-0000-000000000002',
    '50000000-0000-0000-0000-000000000101',
    JSON_OBJECT('demo', true, 'usage', '订单分析查询', 'autoCall', true),
    0,
    1
  )
ON DUPLICATE KEY UPDATE
  tool_config = VALUES(tool_config),
  require_confirm = VALUES(require_confirm),
  enabled = VALUES(enabled);

INSERT INTO workflow_definition (
  id, workflow_code, workflow_name, workspace_id, description, workflow_type,
  graph_json, variable_schema, input_schema, output_schema, execution_policy,
  api_enabled, release_strategy, status, published_version, visibility,
  owner_user_id, created_by
) VALUES (
  '80000000-0000-0000-0000-000000000101',
  'demo-customer-service-flow',
  '演示客服 RAG 工具工作流',
  '90000000-0000-0000-0000-000000000101',
  '五分钟演示工作流：开始 -> RAG -> 工具 -> LLM -> 输出 -> 结束。',
  'agent_flow',
  JSON_OBJECT(
    'nodes', JSON_ARRAY(
      JSON_OBJECT('id','start','type','workflowNode','label','开始','position',JSON_OBJECT('x',40,'y',160),'data',JSON_OBJECT('label','开始','nodeType','START','config',JSON_OBJECT())),
      JSON_OBJECT('id','rag','type','workflowNode','label','知识检索','position',JSON_OBJECT('x',260,'y',100),'data',JSON_OBJECT('label','知识检索','nodeType','RAG','config',JSON_OBJECT('knowledgeBaseId','40000000-0000-0000-0000-000000000001','queryTemplate','{{input}}','searchMode','keyword','runConditionEnabled',true,'runConditionMode','RUN_WHEN','runConditionExpr','input contains 产品 || input contains 优惠券 || input contains 优惠卷 || input contains 活动 || input contains 价格 || input contains 会员 || input contains 退款 || input contains 政策 || input contains 服务'))),
      JSON_OBJECT('id','tool','type','workflowNode','label','订单工具','position',JSON_OBJECT('x',500,'y',190),'data',JSON_OBJECT('label','订单工具','nodeType','TOOL','config',JSON_OBJECT('toolCode','demo_order_status_rest','toolName','demo_order_status_rest','toolId','50000000-0000-0000-0000-000000000101','arguments',JSON_OBJECT('orderId','{{input}}'),'runConditionEnabled',true,'runConditionMode','RUN_WHEN','runConditionExpr','input contains OAF- || input contains 多少订单 || input contains 几个订单 || input contains 几笔订单 || input contains 订单数量 || input contains 订单数 || input contains 我的订单 || input contains 订单列表 || input contains 所有订单 || input contains 有哪些订单'))),
      JSON_OBJECT('id','llm','type','workflowNode','label','答案生成','position',JSON_OBJECT('x',740,'y',135),'data',JSON_OBJECT('label','答案生成','nodeType','LLM','config',JSON_OBJECT('agentId','30000000-0000-0000-0000-000000000001','promptTemplate','用户问题：{{input}}\n已执行工具结果：{{toolResult}}\n请优先使用工具结果中的 responseJson 或 responseBody。若 queryType=order_summary，必须回答订单数量和订单摘要；若 found=true 且是单个订单，必须给出订单状态、物流单号、预计送达时间；退款要求按 refundPolicy 提醒人工复核，不要回答缺少工具结果。','temperature',0.3,'maxTokens',2048))),
      JSON_OBJECT('id','output','type','workflowNode','label','智能对话输出','position',JSON_OBJECT('x',980,'y',135),'data',JSON_OBJECT('label','智能对话输出','nodeType','OUTPUT','config',JSON_OBJECT('showChat',true,'title','客服答复'))),
      JSON_OBJECT('id','end','type','workflowNode','label','结束','position',JSON_OBJECT('x',1220,'y',135),'data',JSON_OBJECT('label','结束','nodeType','END','config',JSON_OBJECT()))
    ),
    'edges', JSON_ARRAY(
      JSON_OBJECT('id','e_start_rag','source','start','target','rag'),
      JSON_OBJECT('id','e_rag_tool','source','rag','target','tool'),
      JSON_OBJECT('id','e_tool_llm','source','tool','target','llm'),
      JSON_OBJECT('id','e_llm_output','source','llm','target','output'),
      JSON_OBJECT('id','e_output_end','source','output','target','end')
    )
  ),
  JSON_OBJECT('input', JSON_OBJECT('type','string','title','用户问题'), 'orderId', JSON_OBJECT('type','string','title','订单号')),
  JSON_OBJECT('type','object','required',JSON_ARRAY('input'),'properties',JSON_OBJECT('input',JSON_OBJECT('type','string'),'orderId',JSON_OBJECT('type','string'))),
  JSON_OBJECT('type','object','properties',JSON_OBJECT('answer',JSON_OBJECT('type','string'),'sources',JSON_OBJECT('type','array'))),
  JSON_OBJECT('timeoutMs',90000,'retryCount',1,'budgetTokens',8000,'sandboxLevel','medium','idempotent',true,'resumeFromFailedNode',true),
  1,
  'standard',
  'published',
  'v1',
  'team',
  '00000000-0000-0000-0000-000000000100',
  '00000000-0000-0000-0000-000000000100'
) ON DUPLICATE KEY UPDATE
  workflow_name = VALUES(workflow_name),
  workspace_id = VALUES(workspace_id),
  description = VALUES(description),
  workflow_type = VALUES(workflow_type),
  graph_json = VALUES(graph_json),
  variable_schema = VALUES(variable_schema),
  input_schema = VALUES(input_schema),
  output_schema = VALUES(output_schema),
  execution_policy = VALUES(execution_policy),
  api_enabled = VALUES(api_enabled),
  release_strategy = VALUES(release_strategy),
  status = VALUES(status),
  published_version = VALUES(published_version),
  visibility = VALUES(visibility),
  updated_at = CURRENT_TIMESTAMP(3),
  deleted_at = NULL;

INSERT IGNORE INTO workflow_version (
  id, workflow_id, version_no, graph_json, variable_schema,
  publish_env, publish_note, status, created_by
)
SELECT UUID(), id, 'v1', graph_json, variable_schema, 'demo', 'P33 演示样例包默认发布版本', 'published', created_by
FROM workflow_definition
WHERE workflow_code = 'demo-customer-service-flow';

UPDATE workflow_version version_row
JOIN workflow_definition workflow_row ON workflow_row.id = version_row.workflow_id
SET version_row.graph_json = workflow_row.graph_json,
    version_row.variable_schema = workflow_row.variable_schema,
    version_row.status = 'published'
WHERE workflow_row.workflow_code = 'demo-customer-service-flow'
  AND version_row.version_no = 'v1';

INSERT INTO workflow_node (
  id, workflow_id, node_key, node_name, node_type, position_x, position_y,
  config_json, input_schema, output_schema, retry_policy, enabled
) VALUES
  ('80000000-0000-0000-0000-000000000201','80000000-0000-0000-0000-000000000101','start','开始','START',40,160,JSON_OBJECT(),JSON_OBJECT(),JSON_OBJECT(),JSON_OBJECT('retryCount',0),1),
  ('80000000-0000-0000-0000-000000000202','80000000-0000-0000-0000-000000000101','rag','知识检索','RAG',260,100,JSON_OBJECT('knowledgeBaseId','40000000-0000-0000-0000-000000000001','queryTemplate','{{input}}','searchMode','keyword','runConditionEnabled',true,'runConditionMode','RUN_WHEN','runConditionExpr','input contains 产品 || input contains 优惠券 || input contains 优惠卷 || input contains 活动 || input contains 价格 || input contains 会员 || input contains 退款 || input contains 政策 || input contains 服务'),JSON_OBJECT('input','string'),JSON_OBJECT('sources','array'),JSON_OBJECT('retryCount',1,'timeoutMs',20000),1),
  ('80000000-0000-0000-0000-000000000203','80000000-0000-0000-0000-000000000101','tool','订单工具','TOOL',500,190,JSON_OBJECT('toolCode','demo_order_status_rest','toolName','demo_order_status_rest','toolId','50000000-0000-0000-0000-000000000101','arguments',JSON_OBJECT('orderId','{{input}}'),'runConditionEnabled',true,'runConditionMode','RUN_WHEN','runConditionExpr','input contains OAF- || input contains 多少订单 || input contains 几个订单 || input contains 几笔订单 || input contains 订单数量 || input contains 订单数 || input contains 我的订单 || input contains 订单列表 || input contains 所有订单 || input contains 有哪些订单'),JSON_OBJECT('orderId','string'),JSON_OBJECT('toolResult','object'),JSON_OBJECT('retryCount',1,'timeoutMs',15000),1),
  ('80000000-0000-0000-0000-000000000204','80000000-0000-0000-0000-000000000101','llm','答案生成','LLM',740,135,JSON_OBJECT('agentId','30000000-0000-0000-0000-000000000001','promptTemplate','用户问题：{{input}}\n已执行工具结果：{{toolResult}}\n请优先使用工具结果中的 responseJson 或 responseBody。若 queryType=order_summary，必须回答订单数量和订单摘要；若 found=true 且是单个订单，必须给出订单状态、物流单号、预计送达时间；退款要求按 refundPolicy 提醒人工复核，不要回答缺少工具结果。','temperature',0.3,'maxTokens',2048),JSON_OBJECT('context','object'),JSON_OBJECT('answer','string'),JSON_OBJECT('retryCount',1,'timeoutMs',45000),1),
  ('80000000-0000-0000-0000-000000000205','80000000-0000-0000-0000-000000000101','output','智能对话输出','OUTPUT',980,135,JSON_OBJECT('showChat',true,'title','客服答复'),JSON_OBJECT('answer','string'),JSON_OBJECT('displayed','boolean'),JSON_OBJECT('retryCount',0),1),
  ('80000000-0000-0000-0000-000000000206','80000000-0000-0000-0000-000000000101','end','结束','END',1220,135,JSON_OBJECT(),JSON_OBJECT(),JSON_OBJECT(),JSON_OBJECT('retryCount',0),1)
ON DUPLICATE KEY UPDATE
  node_name = VALUES(node_name),
  node_type = VALUES(node_type),
  position_x = VALUES(position_x),
  position_y = VALUES(position_y),
  config_json = VALUES(config_json),
  input_schema = VALUES(input_schema),
  output_schema = VALUES(output_schema),
  retry_policy = VALUES(retry_policy),
  enabled = VALUES(enabled),
  updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO workflow_edge (
  id, workflow_id, edge_key, source_node_key, target_node_key,
  condition_expr, label, metadata
) VALUES
  ('80000000-0000-0000-0000-000000000301','80000000-0000-0000-0000-000000000101','e_start_rag','start','rag',NULL,'开始检索',JSON_OBJECT('demo',true)),
  ('80000000-0000-0000-0000-000000000302','80000000-0000-0000-0000-000000000101','e_rag_tool','rag','tool',NULL,'补充订单状态',JSON_OBJECT('demo',true)),
  ('80000000-0000-0000-0000-000000000303','80000000-0000-0000-0000-000000000101','e_tool_llm','tool','llm',NULL,'生成回复',JSON_OBJECT('demo',true)),
  ('80000000-0000-0000-0000-000000000304','80000000-0000-0000-0000-000000000101','e_llm_output','llm','output',NULL,'展示对话',JSON_OBJECT('demo',true)),
  ('80000000-0000-0000-0000-000000000305','80000000-0000-0000-0000-000000000101','e_output_end','output','end',NULL,'完成',JSON_OBJECT('demo',true))
ON DUPLICATE KEY UPDATE
  source_node_key = VALUES(source_node_key),
  target_node_key = VALUES(target_node_key),
  condition_expr = VALUES(condition_expr),
  label = VALUES(label),
  metadata = VALUES(metadata);

INSERT INTO workflow_template (
  id, template_code, template_name, template_category, description,
  graph_json, variable_schema, default_policy, enabled, created_by
)
SELECT
  '80000000-0000-0000-0000-000000000401',
  'demo-customer-service-template',
  '演示客服 RAG 工具模板',
  'demo',
  '与 P33 演示工作流一致，可用于新建工作流时快速复制。',
  graph_json,
  variable_schema,
  execution_policy,
  1,
  created_by
FROM workflow_definition
WHERE workflow_code = 'demo-customer-service-flow'
ON DUPLICATE KEY UPDATE
  template_name = VALUES(template_name),
  template_category = VALUES(template_category),
  description = VALUES(description),
  graph_json = VALUES(graph_json),
  variable_schema = VALUES(variable_schema),
  default_policy = VALUES(default_policy),
  enabled = VALUES(enabled),
  updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO agent_workflow_binding (
  agent_id, workflow_id, trigger_mode, enabled
) VALUES (
  '30000000-0000-0000-0000-000000000001',
  '80000000-0000-0000-0000-000000000101',
  'manual',
  1
) ON DUPLICATE KEY UPDATE
  trigger_mode = VALUES(trigger_mode),
  enabled = VALUES(enabled);

INSERT INTO eval_dataset (
  id, dataset_code, dataset_name, description, domain, tags,
  visibility, status, owner_user_id, created_by
) VALUES (
  '88000000-0000-0000-0000-000000000101',
  'demo-customer-service-eval',
  '演示客服问答评测集',
  '覆盖客服政策、订单工具、可信回答和高风险升级的评测样本。',
  'customer_service',
  JSON_ARRAY('demo','rag','tool','trusted-answer'),
  'team',
  'published',
  '00000000-0000-0000-0000-000000000100',
  '00000000-0000-0000-0000-000000000100'
) ON DUPLICATE KEY UPDATE
  dataset_name = VALUES(dataset_name),
  description = VALUES(description),
  domain = VALUES(domain),
  tags = VALUES(tags),
  visibility = VALUES(visibility),
  status = VALUES(status),
  updated_at = CURRENT_TIMESTAMP(3),
  deleted_at = NULL;

INSERT INTO eval_sample (
  id, dataset_id, sample_no, question, expected_answer,
  reference_context, scoring_points, metadata, status
) VALUES
  (
    '88000000-0000-0000-0000-000000000201',
    '88000000-0000-0000-0000-000000000101',
    1,
    '客户询问订单 OAF-DEMO-1001 现在到哪里了，客服助手应该怎么处理？',
    '应识别订单号并调用订单状态查询工具，基于工具结果说明物流状态、物流单号和预计送达时间。',
    '订单状态查询规则',
    JSON_ARRAY('识别订单号','调用订单工具','解释物流状态','不编造工具结果'),
    JSON_OBJECT('tag','tool_calling','difficulty','easy'),
    'active'
  ),
  (
    '88000000-0000-0000-0000-000000000202',
    '88000000-0000-0000-0000-000000000101',
    2,
    '客户要求立即承诺退款和赔付，客服助手能直接答应吗？',
    '不能直接承诺最终结果，应说明退款赔付属于高风险事项，需要人工客服或主管确认，并先收集必要信息。',
    '售后响应与服务边界',
    JSON_ARRAY('识别高风险事项','拒绝直接承诺','提示人工确认','收集必要字段'),
    JSON_OBJECT('tag','risk_governance','difficulty','medium'),
    'active'
  ),
  (
    '88000000-0000-0000-0000-000000000203',
    '88000000-0000-0000-0000-000000000101',
    3,
    '知识库没有覆盖的问题，可信回答模式应该如何回答？',
    '应明确说明当前知识库资料不足，无法确认，不应编造政策、价格或服务承诺。',
    '可信回答与引用来源',
    JSON_ARRAY('低置信拒答','不编造','说明资料不足','要求引用来源'),
    JSON_OBJECT('tag','trusted_answer','difficulty','medium'),
    'active'
  )
ON DUPLICATE KEY UPDATE
  question = VALUES(question),
  expected_answer = VALUES(expected_answer),
  reference_context = VALUES(reference_context),
  scoring_points = VALUES(scoring_points),
  metadata = VALUES(metadata),
  status = VALUES(status),
  updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO eval_task (
  id, task_code, task_name, dataset_id, agent_id, baseline_model_id,
  compare_model_ids, prompt_template_id, eval_config, status,
  total_samples, finished_samples, created_by
) VALUES (
  '88000000-0000-0000-0000-000000000301',
  'demo-customer-service-eval-task',
  '演示客服助手质量评测',
  '88000000-0000-0000-0000-000000000101',
  '30000000-0000-0000-0000-000000000001',
  '10000000-0000-0000-0000-000000000105',
  JSON_ARRAY('10000000-0000-0000-0000-000000000105'),
  '20000000-0000-0000-0000-000000000102',
  JSON_OBJECT('judgeMode','llm_as_judge','ruleFallback',true,'traceEnabled',true),
  'pending',
  3,
  0,
  '00000000-0000-0000-0000-000000000100'
) ON DUPLICATE KEY UPDATE
  task_name = VALUES(task_name),
  dataset_id = VALUES(dataset_id),
  agent_id = VALUES(agent_id),
  baseline_model_id = VALUES(baseline_model_id),
  compare_model_ids = VALUES(compare_model_ids),
  prompt_template_id = VALUES(prompt_template_id),
  eval_config = VALUES(eval_config),
  status = VALUES(status),
  total_samples = VALUES(total_samples),
  updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO agent_team (
  id, team_code, team_name, description, collaboration_mode,
  coordinator_agent_id, status, owner_user_id, created_by
) VALUES (
  '89000000-0000-0000-0000-000000000101',
  'demo-customer-service-squad',
  '演示客服协作团队',
  '客服助手负责面向客户回答，订单分析助手负责工具参数和订单解释，质量复核助手负责可信回答与风险边界检查。',
  'supervisor',
  '30000000-0000-0000-0000-000000000001',
  'published',
  '00000000-0000-0000-0000-000000000100',
  '00000000-0000-0000-0000-000000000100'
) ON DUPLICATE KEY UPDATE
  team_name = VALUES(team_name),
  description = VALUES(description),
  collaboration_mode = VALUES(collaboration_mode),
  coordinator_agent_id = VALUES(coordinator_agent_id),
  status = VALUES(status),
  updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO agent_team_member (
  team_id, agent_id, member_role, handoff_policy, sort_order, enabled
) VALUES
  (
    '89000000-0000-0000-0000-000000000101',
    '30000000-0000-0000-0000-000000000001',
    'coordinator',
    JSON_OBJECT('input','客户问题','output','回答草稿与任务拆解','handoff','将订单问题交给订单分析助手，将风险检查交给质量复核助手'),
    1,
    1
  ),
  (
    '89000000-0000-0000-0000-000000000101',
    '30000000-0000-0000-0000-000000000002',
    'worker',
    JSON_OBJECT('input','订单号和客户问题','output','订单状态解释和工具调用建议','handoff','把查询结果交还客服助手'),
    2,
    1
  ),
  (
    '89000000-0000-0000-0000-000000000101',
    '30000000-0000-0000-0000-000000000003',
    'reviewer',
    JSON_OBJECT('input','客服回答草稿','output','风险与引用复核意见','handoff','把复核建议交给客服助手生成最终答复'),
    3,
    1
  )
ON DUPLICATE KEY UPDATE
  member_role = VALUES(member_role),
  handoff_policy = VALUES(handoff_policy),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled);

INSERT INTO agent_memory (
  id, agent_id, user_id, session_id, memory_type, memory_key,
  memory_text, memory_value, embedding_json, embedding_blob,
  vector_collection_id, vector_partition_id, milvus_collection_name,
  vector_primary_key, sync_status, importance_score, status,
  privacy_scope, tags_json, hit_count
) VALUES (
  '30000000-0000-0000-0000-000000000902',
  '30000000-0000-0000-0000-000000000001',
  '00000000-0000-0000-0000-000000000100',
  NULL,
  'long_term',
  'demo_customer_support_preference',
  '演示偏好：客户更关注处理进度和明确下一步动作。客服助手回答时应先给结论，再列出依据、操作步骤和是否需要人工确认。',
  JSON_OBJECT('scenario','demo','preference','先结论后步骤','riskBoundary','高风险动作需人工确认'),
  JSON_ARRAY(0.24,0.56,0.38,0.49,0.73,0.21,0.64,0.33),
  NULL,
  '70000000-0000-0000-0000-000000000102',
  '70000000-0000-0000-0000-000000000202',
  'oaf_agent_memory',
  'demo_memory_300000000000000000000000000000000902',
  'mysql_fallback',
  0.9000,
  'active',
  'agent',
  JSON_ARRAY('demo','客服','长期记忆'),
  0
) ON DUPLICATE KEY UPDATE
  memory_text = VALUES(memory_text),
  memory_value = VALUES(memory_value),
  embedding_json = VALUES(embedding_json),
  vector_collection_id = VALUES(vector_collection_id),
  vector_partition_id = VALUES(vector_partition_id),
  milvus_collection_name = VALUES(milvus_collection_name),
  vector_primary_key = VALUES(vector_primary_key),
  sync_status = VALUES(sync_status),
  importance_score = VALUES(importance_score),
  status = VALUES(status),
  privacy_scope = VALUES(privacy_scope),
  tags_json = VALUES(tags_json),
  updated_at = CURRENT_TIMESTAMP(3);

INSERT IGNORE INTO oaf_workspace_resource (
  id, workspace_id, resource_type, resource_id, owner_user_id, created_by
)
SELECT UUID(), '90000000-0000-0000-0000-000000000101', 'agent', id, owner_user_id, created_by
FROM agent
WHERE id IN (
  '30000000-0000-0000-0000-000000000001',
  '30000000-0000-0000-0000-000000000002',
  '30000000-0000-0000-0000-000000000003'
);

INSERT IGNORE INTO oaf_workspace_resource (
  id, workspace_id, resource_type, resource_id, owner_user_id, created_by
)
VALUES
  (UUID(), '90000000-0000-0000-0000-000000000101', 'knowledge_base', '40000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000100'),
  (UUID(), '90000000-0000-0000-0000-000000000101', 'workflow', '80000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000100'),
  (UUID(), '90000000-0000-0000-0000-000000000101', 'eval_dataset', '88000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000100'),
  (UUID(), '90000000-0000-0000-0000-000000000101', 'agent_team', '89000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000100');

INSERT IGNORE INTO oaf_workspace_resource (
  id, workspace_id, resource_type, resource_id, owner_user_id, created_by
)
SELECT UUID(), '90000000-0000-0000-0000-000000000101', 'tool', id, owner_user_id, created_by
FROM tool_definition
WHERE id IN (
  '50000000-0000-0000-0000-000000000101',
  '50000000-0000-0000-0000-000000000102',
  '50000000-0000-0000-0000-000000000103'
);

INSERT INTO sys_config (
  config_key, config_value, value_type, group_code, description, encrypted, editable
) VALUES
  ('demo.data.package.version', 'P33', 'string', 'demo', '当前演示样例包版本', 0, 0),
  ('demo.data.quickstart.question', '订单 OAF-DEMO-1001 到哪里了？如果客户要求退款应该怎么处理？', 'string', 'demo', '五分钟体验推荐问题', 0, 1)
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  value_type = VALUES(value_type),
  group_code = VALUES(group_code),
  description = VALUES(description),
  editable = VALUES(editable),
  updated_at = CURRENT_TIMESTAMP(3);
