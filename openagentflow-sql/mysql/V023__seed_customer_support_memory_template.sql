USE openagentflow;

-- P24：客服助手长期记忆模板，用于让默认客服 Agent 在多轮对话中保持统一服务口径。
UPDATE agent
SET memory_strategy = 'long_term'
WHERE id = '30000000-0000-0000-0000-000000000001'
  AND agent_code = 'customer-support-agent';

INSERT IGNORE INTO agent_memory (
  id,
  agent_id,
  user_id,
  session_id,
  memory_type,
  memory_key,
  memory_text,
  memory_value,
  embedding_json,
  embedding_blob,
  vector_collection_id,
  vector_partition_id,
  milvus_collection_name,
  vector_primary_key,
  sync_status,
  last_synced_at,
  external_vector_id,
  importance_score,
  expired_at,
  status,
  privacy_scope,
  source_run_id,
  source_message_id,
  tags_json,
  hit_count,
  last_accessed_at
) VALUES (
  '30000000-0000-0000-0000-000000000901',
  '30000000-0000-0000-0000-000000000001',
  '00000000-0000-0000-0000-000000000100',
  NULL,
  'long_term',
  'customer_support_agent_long_term_template',
  '客服助手长期记忆模板：1. 服务语气保持专业、耐心、简洁，优先使用中文回答。2. 处理客户问题时先确认问题类型，再补齐必要信息，包括客户称呼、联系方式、订单号、产品名称、问题现象、发生时间、期望处理结果。3. 能基于知识库确认的内容要给出明确步骤和引用来源；知识库不足时应说明当前资料不足，不编造政策或承诺。4. 涉及退款、赔付、合同、账号安全、删除数据、审批通过等高风险事项时，不直接承诺最终结果，应提示需要人工客服或主管确认。5. 创建工单前先汇总问题摘要、已收集字段、建议优先级和下一步动作。6. 对情绪激动客户先安抚，再给出可执行方案。7. 对重复问题优先复用历史偏好和已确认信息，减少重复追问。',
  JSON_OBJECT(
    'templateName', '客服助手长期记忆模板',
    'scenario', 'customer_support',
    'expectedBehavior', JSON_ARRAY('统一服务口径', '补齐工单字段', '高风险事项升级', '知识库不足时拒绝编造'),
    'riskBoundary', JSON_ARRAY('退款赔付', '合同承诺', '账号安全', '删除数据', '审批通过')
  ),
  NULL,
  NULL,
  '70000000-0000-0000-0000-000000000102',
  NULL,
  'oaf_agent_memory',
  'memory_30000000-0000-0000-0000-000000000901',
  'pending',
  NULL,
  NULL,
  0.9500,
  NULL,
  'active',
  'agent',
  NULL,
  NULL,
  JSON_ARRAY('客服', '长期记忆', '模板', '服务口径'),
  0,
  NULL
);
