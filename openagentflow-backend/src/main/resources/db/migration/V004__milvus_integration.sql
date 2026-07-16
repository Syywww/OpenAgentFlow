USE openagentflow;

CREATE TABLE IF NOT EXISTS vector_store_connection (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  connection_code varchar(120) NOT NULL UNIQUE COMMENT '连接编码',
  connection_name varchar(160) NOT NULL COMMENT '连接名称',
  store_type varchar(40) NOT NULL DEFAULT 'milvus' COMMENT '存储类型',
  endpoint varchar(500) NOT NULL COMMENT '端点',
  database_name varchar(120) NOT NULL DEFAULT 'default' COMMENT 'DATABASE名称',
  auth_type varchar(40) NOT NULL DEFAULT 'none' COMMENT '认证类型',
  username varchar(160) COMMENT '用户名',
  password_cipher text COMMENT '密码CIPHER',
  token_cipher text COMMENT '令牌CIPHER',
  secure tinyint(1) NOT NULL DEFAULT 0 COMMENT '安全连接',
  default_consistency_level varchar(40) NOT NULL DEFAULT 'Bounded' COMMENT '默认一致性级别',
  default_metric_type varchar(40) NOT NULL DEFAULT 'COSINE' COMMENT '默认距离度量类型',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  health_status varchar(32) NOT NULL DEFAULT 'unknown' COMMENT '健康状态',
  last_health_check_at datetime(3) COMMENT 'LAST健康CHECK时间',
  config_json json NOT NULL COMMENT '配置JSON',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='向量存储连接表';

CREATE TABLE IF NOT EXISTS vector_collection (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  connection_id char(36) NOT NULL COMMENT '连接ID',
  collection_name varchar(160) NOT NULL COMMENT '集合名称',
  collection_alias varchar(160) COMMENT '集合别名',
  business_type varchar(64) NOT NULL COMMENT '业务类型',
  owner_resource_type varchar(64) COMMENT '所有者资源类型',
  owner_resource_id char(36) COMMENT '所有者资源ID',
  embedding_model_id char(36) COMMENT '向量模型ID',
  dimension int NOT NULL COMMENT '维度',
  metric_type varchar(40) NOT NULL DEFAULT 'COSINE' COMMENT '距离度量类型',
  index_type varchar(80) NOT NULL DEFAULT 'HNSW' COMMENT '索引类型',
  index_params json NOT NULL COMMENT '索引参数',
  schema_json json NOT NULL COMMENT '字段说明：Schema JSON',
  shard_num int NOT NULL DEFAULT 2 COMMENT '分片NUM',
  replica_number int NOT NULL DEFAULT 1 COMMENT '副本编号',
  consistency_level varchar(40) NOT NULL DEFAULT 'Bounded' COMMENT '一致性级别',
  auto_create_partition tinyint(1) NOT NULL DEFAULT 1 COMMENT 'AUTOCREATE分区',
  status varchar(32) NOT NULL DEFAULT 'active' COMMENT '状态',
  loaded tinyint(1) NOT NULL DEFAULT 0 COMMENT '已加载',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  UNIQUE KEY uk_vector_collection(connection_id, collection_name),
  KEY idx_vector_collection_owner(owner_resource_type, owner_resource_id),
  CONSTRAINT fk_vector_collection_connection FOREIGN KEY(connection_id) REFERENCES vector_store_connection(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='向量集合表';

CREATE TABLE IF NOT EXISTS vector_partition (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  collection_id char(36) NOT NULL COMMENT '集合ID',
  partition_name varchar(160) NOT NULL COMMENT '分区名称',
  partition_key varchar(160) COMMENT '分区密钥',
  business_type varchar(64) COMMENT '业务类型',
  owner_resource_id char(36) COMMENT '所有者资源ID',
  status varchar(32) NOT NULL DEFAULT 'active' COMMENT '状态',
  row_count bigint NOT NULL DEFAULT 0 COMMENT 'ROW数量',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  UNIQUE KEY uk_vector_partition(collection_id, partition_name),
  KEY idx_vector_partition_owner(owner_resource_id),
  CONSTRAINT fk_vector_partition_collection FOREIGN KEY(collection_id) REFERENCES vector_collection(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='向量分区表';

CREATE TABLE IF NOT EXISTS vector_record_mapping (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  collection_id char(36) NOT NULL COMMENT '集合ID',
  partition_id char(36) COMMENT '分区ID',
  vector_primary_key varchar(160) NOT NULL COMMENT '向量主键',
  resource_type varchar(64) NOT NULL COMMENT '资源类型',
  resource_id char(36) NOT NULL COMMENT '资源ID',
  embedding_model_id char(36) COMMENT '向量模型ID',
  content_hash varchar(128) COMMENT '内容哈希',
  status varchar(32) NOT NULL DEFAULT 'active' COMMENT '状态',
  last_synced_at datetime(3) COMMENT '最后同步时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  UNIQUE KEY uk_vector_record(collection_id, vector_primary_key),
  UNIQUE KEY uk_vector_resource(collection_id, resource_type, resource_id),
  KEY idx_vector_record_resource(resource_type, resource_id),
  CONSTRAINT fk_vector_record_collection FOREIGN KEY(collection_id) REFERENCES vector_collection(id) ON DELETE CASCADE,
  CONSTRAINT fk_vector_record_partition FOREIGN KEY(partition_id) REFERENCES vector_partition(id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='向量记录映射表';

CREATE TABLE IF NOT EXISTS vector_sync_task (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  task_code varchar(120) NOT NULL UNIQUE COMMENT '任务编码',
  task_type varchar(64) NOT NULL COMMENT '任务类型',
  collection_id char(36) NOT NULL COMMENT '集合ID',
  partition_id char(36) COMMENT '分区ID',
  resource_type varchar(64) COMMENT '资源类型',
  resource_id char(36) COMMENT '资源ID',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '状态',
  total_count int NOT NULL DEFAULT 0 COMMENT '总数量',
  success_count int NOT NULL DEFAULT 0 COMMENT '成功数量',
  failure_count int NOT NULL DEFAULT 0 COMMENT '失败数量',
  error_message text COMMENT '错误信息',
  config_json json NOT NULL COMMENT '配置JSON',
  started_at datetime(3) COMMENT '开始时间',
  finished_at datetime(3) COMMENT '完成时间',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  KEY idx_vector_sync_collection(collection_id, status, created_at),
  KEY idx_vector_sync_resource(resource_type, resource_id),
  CONSTRAINT fk_vector_sync_collection FOREIGN KEY(collection_id) REFERENCES vector_collection(id) ON DELETE CASCADE,
  CONSTRAINT fk_vector_sync_partition FOREIGN KEY(partition_id) REFERENCES vector_partition(id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='向量同步任务表';

CREATE TABLE IF NOT EXISTS vector_sync_error (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  task_id char(36) NOT NULL COMMENT '任务ID',
  resource_type varchar(64) COMMENT '资源类型',
  resource_id char(36) COMMENT '资源ID',
  error_code varchar(120) COMMENT '错误编码',
  error_message text NOT NULL COMMENT '错误信息',
  payload json COMMENT '载荷',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  KEY idx_vector_sync_error_task(task_id),
  CONSTRAINT fk_vector_sync_error_task FOREIGN KEY(task_id) REFERENCES vector_sync_task(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='向量同步错误表';

ALTER TABLE knowledge_base
  ADD COLUMN vector_connection_id char(36) NULL COMMENT '向量存储连接ID' AFTER rerank_model_id,
  ADD COLUMN vector_collection_id char(36) NULL COMMENT '向量集合ID' AFTER vector_connection_id,
  ADD COLUMN milvus_collection_name varchar(160) NULL COMMENT 'Milvus集合名称' AFTER vector_collection_id,
  ADD COLUMN milvus_partition_name varchar(160) NULL COMMENT 'Milvus分区名称' AFTER milvus_collection_name;

ALTER TABLE knowledge_embedding
  ADD COLUMN vector_collection_id char(36) NULL COMMENT '向量集合ID' AFTER model_id,
  ADD COLUMN vector_partition_id char(36) NULL COMMENT '向量分区ID' AFTER vector_collection_id,
  ADD COLUMN milvus_collection_name varchar(160) NULL COMMENT 'Milvus集合名称' AFTER vector_partition_id,
  ADD COLUMN milvus_partition_name varchar(160) NULL COMMENT 'Milvus分区名称' AFTER milvus_collection_name,
  ADD COLUMN vector_primary_key varchar(160) NULL COMMENT '向量主键' AFTER milvus_partition_name,
  ADD COLUMN sync_status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '同步状态' AFTER vector_primary_key,
  ADD COLUMN last_synced_at datetime(3) NULL COMMENT '最后同步时间' AFTER sync_status;

ALTER TABLE agent_memory
  ADD COLUMN vector_collection_id char(36) NULL COMMENT '向量集合ID' AFTER embedding_blob,
  ADD COLUMN vector_partition_id char(36) NULL COMMENT '向量分区ID' AFTER vector_collection_id,
  ADD COLUMN milvus_collection_name varchar(160) NULL COMMENT 'Milvus集合名称' AFTER vector_partition_id,
  ADD COLUMN vector_primary_key varchar(160) NULL COMMENT '向量主键' AFTER milvus_collection_name,
  ADD COLUMN sync_status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '同步状态' AFTER vector_primary_key,
  ADD COLUMN last_synced_at datetime(3) NULL COMMENT '最后同步时间' AFTER sync_status;

ALTER TABLE knowledge_retrieval_log
  ADD COLUMN vector_collection_id char(36) NULL COMMENT '向量集合ID' AFTER run_id,
  ADD COLUMN milvus_collection_name varchar(160) NULL COMMENT 'Milvus集合名称' AFTER vector_collection_id,
  ADD COLUMN milvus_search_params json NULL COMMENT 'Milvus搜索参数JSON' AFTER query_external_vector_id,
  ADD COLUMN milvus_result_ids json NULL COMMENT 'Milvus结果ID列表JSON' AFTER results;

CREATE INDEX idx_kb_vector_collection ON knowledge_base(vector_collection_id);
CREATE INDEX idx_embedding_vector_collection ON knowledge_embedding(vector_collection_id, sync_status);
CREATE INDEX idx_embedding_vector_pk ON knowledge_embedding(vector_primary_key);
CREATE INDEX idx_agent_memory_vector_collection ON agent_memory(vector_collection_id, sync_status);
CREATE INDEX idx_agent_memory_vector_pk ON agent_memory(vector_primary_key);
CREATE INDEX idx_retrieval_vector_collection ON knowledge_retrieval_log(vector_collection_id, created_at);

INSERT IGNORE INTO vector_store_connection (
  id, connection_code, connection_name, store_type, endpoint, database_name,
  auth_type, default_consistency_level, default_metric_type, status, health_status, config_json
) VALUES (
  '70000000-0000-0000-0000-000000000001',
  'milvus-default',
  'Milvus 默认连接',
  'milvus',
  'http://localhost:19530',
  'default',
  'none',
  'Bounded',
  'COSINE',
  'enabled',
  'unknown',
  JSON_OBJECT('client', 'milvus-java-sdk', 'description', 'Default Milvus connection for RAG and memory vectors')
);

INSERT IGNORE INTO vector_collection (
  id, connection_id, collection_name, collection_alias, business_type, owner_resource_type,
  embedding_model_id, dimension, metric_type, index_type, index_params, schema_json, shard_num, replica_number
) VALUES (
  '70000000-0000-0000-0000-000000000101',
  '70000000-0000-0000-0000-000000000001',
  'oaf_knowledge_chunks',
  '知识库切片向量',
  'knowledge_chunk',
  'knowledge_base',
  '10000000-0000-0000-0000-000000000102',
  1536,
  'COSINE',
  'HNSW',
  JSON_OBJECT('M', 16, 'efConstruction', 200),
  JSON_OBJECT(
    'primaryKey', 'vector_id',
    'vectorField', 'embedding',
    'scalarFields', JSON_ARRAY('chunk_id', 'kb_id', 'document_id', 'tenant_id')
  ),
  2,
  1
), (
  '70000000-0000-0000-0000-000000000102',
  '70000000-0000-0000-0000-000000000001',
  'oaf_agent_memory',
  'Agent 记忆向量',
  'agent_memory',
  'agent',
  '10000000-0000-0000-0000-000000000102',
  1536,
  'COSINE',
  'HNSW',
  JSON_OBJECT('M', 16, 'efConstruction', 200),
  JSON_OBJECT(
    'primaryKey', 'vector_id',
    'vectorField', 'embedding',
    'scalarFields', JSON_ARRAY('memory_id', 'agent_id', 'user_id', 'session_id')
  ),
  2,
  1
);

INSERT IGNORE INTO vector_partition (
  id, collection_id, partition_name, partition_key, business_type, owner_resource_id
) VALUES (
  '70000000-0000-0000-0000-000000000201',
  '70000000-0000-0000-0000-000000000101',
  'kb_product_manual',
  'product-manual-kb',
  'knowledge_chunk',
  '40000000-0000-0000-0000-000000000001'
), (
  '70000000-0000-0000-0000-000000000202',
  '70000000-0000-0000-0000-000000000102',
  'default_memory',
  'default',
  'agent_memory',
  NULL
);

UPDATE knowledge_base
SET vector_connection_id = '70000000-0000-0000-0000-000000000001',
    vector_collection_id = '70000000-0000-0000-0000-000000000101',
    milvus_collection_name = 'oaf_knowledge_chunks',
    milvus_partition_name = 'kb_product_manual'
WHERE id = '40000000-0000-0000-0000-000000000001';

INSERT IGNORE INTO sys_config (config_key, config_value, value_type, group_code, description)
VALUES
  ('vector.store.type', 'milvus', 'string', 'vector', '向量存储类型'),
  ('milvus.endpoint', 'http://localhost:19530', 'string', 'milvus', 'Milvus 服务地址'),
  ('milvus.database', 'default', 'string', 'milvus', 'Milvus database 名称'),
  ('milvus.default_collection.knowledge', 'oaf_knowledge_chunks', 'string', 'milvus', '知识库默认 Milvus collection'),
  ('milvus.default_collection.memory', 'oaf_agent_memory', 'string', 'milvus', 'Agent Memory 默认 Milvus collection'),
  ('milvus.metric_type', 'COSINE', 'string', 'milvus', '默认向量距离类型'),
  ('milvus.index_type', 'HNSW', 'string', 'milvus', '默认 Milvus 索引类型'),
  ('milvus.search_params', JSON_OBJECT('ef', 64), 'json', 'milvus', '默认 Milvus 搜索参数');
