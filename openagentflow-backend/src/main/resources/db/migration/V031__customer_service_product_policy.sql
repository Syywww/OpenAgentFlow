-- P34：客服助手产品知识补充。
-- 目标：让“产品有什么、产品介绍、服务内容”等知识咨询优先命中知识库，而不是误触发订单工具。

UPDATE prompt_template
SET content = '你是 OpenAgentFlow 演示客服助手。请优先使用知识库资料回答。先判断用户问题类型：产品、优惠券、活动、价格政策、会员权益属于知识咨询，必须先检索并依据知识库回答；只有当用户明确提供订单号，并询问订单状态、物流进度、发货签收等实时订单数据时，才可以调用订单查询工具。知识库不足时说明资料不足并追问必要信息，不编造政策。',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE template_code = 'demo-customer-service-system';

UPDATE agent
SET system_prompt = '你是企业智能客服助手，请基于知识库和工具结果回答用户问题。先判断用户问题类型：产品、优惠券、活动、价格政策、会员权益属于知识咨询，必须先检索并依据知识库回答；只有当用户明确提供订单号，并询问订单状态、物流进度、发货签收等实时订单数据时，才可以调用订单查询工具。涉及退款、赔付、账号安全等高风险内容时必须提示人工确认。',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE agent_code = 'customer-support-agent';

INSERT INTO knowledge_chunk (
  id, kb_id, document_id, chunk_no, title, content, token_count,
  page_no, start_offset, end_offset, metadata, status
) VALUES (
  '42000000-0000-0000-0000-000000000006',
  '40000000-0000-0000-0000-000000000001',
  '41000000-0000-0000-0000-000000000001',
  6,
  '产品与服务范围说明',
  '客户询问“产品有什么、有哪些产品、产品介绍、服务内容、产品能力”时，客服助手应按知识咨询处理，不应调用订单状态查询工具。演示知识库中的产品服务范围包括：企业知识库问答、智能客服助手、订单状态查询演示、工单创建建议、可信回答引用来源、工具调用 Trace、评测与交付验收能力。若客户询问具体价格、库存、个人权益或某个真实商品详情，应说明当前知识库只提供演示范围，并追问具体产品名称或引导转人工确认。',
  138,
  6,
  537,
  675,
  JSON_OBJECT('section','产品服务','source','P34 demo','toolPolicy','no_order_tool'),
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
  '43000000-0000-0000-0000-000000000006',
  '42000000-0000-0000-0000-000000000006',
  '40000000-0000-0000-0000-000000000001',
  '10000000-0000-0000-0000-000000000106',
  '70000000-0000-0000-0000-000000000101',
  '70000000-0000-0000-0000-000000000201',
  'oaf_knowledge_chunks',
  'kb_product_manual',
  'demo_chunk_420000000000000000000000000000000006',
  'mysql_fallback',
  JSON_ARRAY(0.77,0.41,0.62,0.28,0.84,0.19,0.56,0.33,0.72,0.25,0.67,0.38,0.59,0.21,0.46,0.81),
  16,
  MD5((SELECT content FROM knowledge_chunk WHERE id = '42000000-0000-0000-0000-000000000006'))
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
SET result = JSON_OBJECT('chunkCount', 6, 'embeddingCount', 6, 'milvusFallback', true),
    finished_at = CURRENT_TIMESTAMP(3)
WHERE document_id = '41000000-0000-0000-0000-000000000001'
  AND task_type = 'seed_parse';
