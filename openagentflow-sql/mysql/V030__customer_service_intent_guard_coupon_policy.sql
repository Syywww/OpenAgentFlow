-- P34：客服助手意图门控与优惠券知识补充。
-- 目标：避免普通优惠券、活动、价格政策咨询误触发订单查询工具。

UPDATE prompt_template
SET content = '你是 OpenAgentFlow 演示客服助手。请优先使用知识库资料回答。只有当用户明确提供订单号，或询问订单状态、物流进度、发货签收等实时订单数据时，才可以调用订单查询工具。优惠券、优惠卷、促销活动、价格政策、会员权益等问题不要调用订单工具，应基于知识库回答；知识库不足时说明资料不足，不编造政策。',
    description = '用于五分钟演示链路的客服 Agent 默认提示词，包含工具调用意图边界。',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE template_code = 'demo-customer-service-system';

UPDATE agent
SET system_prompt = '你是企业智能客服助手，请基于知识库和工具结果回答用户问题。只有当用户明确提供订单号，或询问订单状态、物流进度、发货签收等实时订单数据时，才可以调用订单查询工具。优惠券、优惠卷、促销活动、价格政策、会员权益等问题不要调用订单工具，应基于知识库回答。涉及退款、赔付、账号安全等高风险内容时必须提示人工确认。',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE agent_code = 'customer-support-agent';

UPDATE tool_definition
SET description = '低风险只读 REST 工具，仅用于查询订单状态、物流单号和预计送达时间。只有用户明确提供订单号，或询问订单状态、物流进度、发货签收等实时订单数据时才调用；优惠券、优惠卷、促销活动、价格政策、会员权益等问题不要调用本工具。',
    request_schema = JSON_OBJECT(
      'type','object',
      'required',JSON_ARRAY('orderId'),
      'properties',JSON_OBJECT(
        'orderId',JSON_OBJECT('type','string','description','订单号；仅当用户问题中明确出现订单号或订单实时查询意图时填写')
      )
    ),
    updated_at = CURRENT_TIMESTAMP(3)
WHERE tool_code = 'demo_order_status_rest';

UPDATE tool_definition
SET description = '只读数据库查询工具，仅用于按订单号查询演示订单明细。优惠券、促销活动、价格政策等知识咨询不要调用本工具。',
    request_schema = JSON_OBJECT(
      'type','object',
      'required',JSON_ARRAY('orderNo'),
      'properties',JSON_OBJECT(
        'orderNo',JSON_OBJECT('type','string','description','订单号；仅用于订单实时查询')
      )
    ),
    updated_at = CURRENT_TIMESTAMP(3)
WHERE tool_code = 'demo_readonly_order_sql';

INSERT INTO knowledge_chunk (
  id, kb_id, document_id, chunk_no, title, content, token_count,
  page_no, start_offset, end_offset, metadata, status
) VALUES (
  '42000000-0000-0000-0000-000000000005',
  '40000000-0000-0000-0000-000000000001',
  '41000000-0000-0000-0000-000000000001',
  5,
  '优惠券与促销活动说明',
  '客户询问优惠券、优惠卷、折扣、满减、促销活动、会员权益或积分兑换时，客服助手应先说明当前演示知识库仅提供通用规则：优惠券通常以页面可领取、活动自动发放、会员权益兑换三种方式出现；是否可用取决于活动有效期、适用商品、最低消费门槛和账号状态。此类问题属于政策咨询，不应调用订单状态查询工具；如客户需要查询个人账号可用券，应提示进入用户中心或转人工核对账号权益。',
  132,
  5,
  404,
  536,
  JSON_OBJECT('section','优惠券政策','source','P34 demo','toolPolicy','no_order_tool'),
  'active'
) ON DUPLICATE KEY UPDATE
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
) VALUES (
  '43000000-0000-0000-0000-000000000005',
  '42000000-0000-0000-0000-000000000005',
  '40000000-0000-0000-0000-000000000001',
  '10000000-0000-0000-0000-000000000106',
  '70000000-0000-0000-0000-000000000101',
  '70000000-0000-0000-0000-000000000201',
  'oaf_knowledge_chunks',
  'kb_product_manual',
  'demo_chunk_420000000000000000000000000000000005',
  'mysql_fallback',
  JSON_ARRAY(0.82,0.24,0.71,0.19,0.58,0.36,0.64,0.27,0.75,0.31,0.69,0.22,0.57,0.48,0.33,0.86),
  16,
  MD5((SELECT content FROM knowledge_chunk WHERE id = '42000000-0000-0000-0000-000000000005'))
) ON DUPLICATE KEY UPDATE
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

UPDATE knowledge_document_parse_task
SET result = JSON_OBJECT('chunkCount', 5, 'embeddingCount', 5, 'milvusFallback', true),
    finished_at = CURRENT_TIMESTAMP(3)
WHERE document_id = '41000000-0000-0000-0000-000000000001'
  AND task_type = 'seed_parse';
