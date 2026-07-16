USE openagentflow;

-- P24：Memory 记忆中心，支持短期记忆、长期记忆、任务记忆、向量记忆和召回治理。
ALTER TABLE agent_memory
  ADD COLUMN status varchar(32) NOT NULL DEFAULT 'active' COMMENT '记忆状态：active启用、archived归档、deleted删除' AFTER expired_at,
  ADD COLUMN privacy_scope varchar(32) NOT NULL DEFAULT 'private' COMMENT '可见范围：private个人、agent智能体、workspace工作空间' AFTER status,
  ADD COLUMN source_run_id char(36) NULL COMMENT '来源运行ID，用于追溯记忆来自哪次执行' AFTER privacy_scope,
  ADD COLUMN source_message_id char(36) NULL COMMENT '来源消息ID，用于追溯记忆来自哪条会话消息' AFTER source_run_id,
  ADD COLUMN tags_json json NULL COMMENT '标签JSON，用于分类筛选和治理' AFTER source_message_id,
  ADD COLUMN hit_count int NOT NULL DEFAULT 0 COMMENT '命中次数，用于评估记忆价值' AFTER tags_json,
  ADD COLUMN last_accessed_at datetime(3) NULL COMMENT '最后命中时间' AFTER hit_count;

ALTER TABLE agent_memory COMMENT='Agent记忆中心表';
ALTER TABLE agent_memory MODIFY COLUMN id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID';
ALTER TABLE agent_memory MODIFY COLUMN agent_id char(36) COMMENT '归属Agent ID，为空表示用户级通用记忆';
ALTER TABLE agent_memory MODIFY COLUMN user_id char(36) COMMENT '归属用户ID，用于个人记忆隔离';
ALTER TABLE agent_memory MODIFY COLUMN session_id char(36) COMMENT '归属会话ID，短期记忆通常绑定该字段';
ALTER TABLE agent_memory MODIFY COLUMN memory_type varchar(32) NOT NULL COMMENT '记忆类型：short_term短期、long_term长期、task任务、vector向量';
ALTER TABLE agent_memory MODIFY COLUMN memory_key varchar(160) COMMENT '记忆密钥，用于业务去重或定位来源';
ALTER TABLE agent_memory MODIFY COLUMN memory_text longtext NOT NULL COMMENT '记忆文本，可注入Prompt或用于召回';
ALTER TABLE agent_memory MODIFY COLUMN memory_value json NOT NULL COMMENT '结构化记忆值JSON';
ALTER TABLE agent_memory MODIFY COLUMN embedding_json json COMMENT '向量JSON，作为MySQL兜底召回依据';
ALTER TABLE agent_memory MODIFY COLUMN embedding_blob longblob COMMENT '向量二进制，预留给本地向量存储';
ALTER TABLE agent_memory MODIFY COLUMN vector_collection_id char(36) COMMENT '向量集合ID';
ALTER TABLE agent_memory MODIFY COLUMN vector_partition_id char(36) COMMENT '向量分区ID';
ALTER TABLE agent_memory MODIFY COLUMN milvus_collection_name varchar(160) COMMENT 'Milvus集合名称';
ALTER TABLE agent_memory MODIFY COLUMN vector_primary_key varchar(160) COMMENT '向量主键';
ALTER TABLE agent_memory MODIFY COLUMN sync_status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '同步状态：pending待同步、synced已同步、failed失败、skipped跳过';
ALTER TABLE agent_memory MODIFY COLUMN last_synced_at datetime(3) COMMENT '最后同步时间';
ALTER TABLE agent_memory MODIFY COLUMN external_vector_id varchar(160) COMMENT '外部向量ID';
ALTER TABLE agent_memory MODIFY COLUMN importance_score decimal(5,4) NOT NULL DEFAULT 0.5 COMMENT '重要度得分，范围0到1';
ALTER TABLE agent_memory MODIFY COLUMN expired_at datetime(3) COMMENT '过期时间';
ALTER TABLE agent_memory MODIFY COLUMN created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间';
ALTER TABLE agent_memory MODIFY COLUMN updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间';

CREATE INDEX idx_agent_memory_status_type ON agent_memory(status, memory_type, updated_at);
CREATE INDEX idx_agent_memory_user_status ON agent_memory(user_id, status, updated_at);
CREATE INDEX idx_agent_memory_agent_session ON agent_memory(agent_id, session_id, memory_type, status);
CREATE INDEX idx_agent_memory_source_run ON agent_memory(source_run_id);

INSERT IGNORE INTO vector_collection (
  id, connection_id, collection_name, collection_alias, business_type,
  dimension, metric_type, index_type, index_params, schema_json, status, loaded
) VALUES (
  '70000000-0000-0000-0000-000000000102',
  '70000000-0000-0000-0000-000000000001',
  'oaf_agent_memory',
  'Agent Memory 默认集合',
  'agent_memory',
  2048,
  'COSINE',
  'HNSW',
  JSON_OBJECT('M', 16, 'efConstruction', 128),
  JSON_OBJECT('primaryKey', 'vector_primary_key', 'vectorField', 'embedding'),
  'active',
  0
);

INSERT IGNORE INTO iam_permission (permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order, visible, status)
VALUES
  ('memory:manage', 'Memory记忆中心管理', 'menu', '/memories', 'ALL', '/api/memories/**', 125, 1, 'enabled'),
  ('memory:view', 'Memory记忆查看', 'api', '/memories', 'GET', '/api/memories/**', 126, 1, 'enabled'),
  ('memory:create', 'Memory记忆创建', 'api', '/memories', 'POST', '/api/memories', 127, 1, 'enabled'),
  ('memory:update', 'Memory记忆编辑', 'api', '/memories/:id', 'PUT', '/api/memories/*', 128, 1, 'enabled'),
  ('memory:delete', 'Memory记忆删除', 'api', '/memories/:id', 'DELETE', '/api/memories/*', 129, 1, 'enabled'),
  ('memory:recall', 'Memory记忆召回测试', 'api', '/memories', 'POST', '/api/memories/recall', 130, 1, 'enabled');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM iam_role r
JOIN iam_permission p ON p.permission_code IN (
  'memory:manage',
  'memory:view',
  'memory:create',
  'memory:update',
  'memory:delete',
  'memory:recall'
)
WHERE r.role_code IN ('super_admin', 'admin');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM iam_role r
JOIN iam_permission p ON p.permission_code IN (
  'memory:manage',
  'memory:view',
  'memory:create',
  'memory:update',
  'memory:recall'
)
WHERE r.role_code = 'developer';

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM iam_role r
JOIN iam_permission p ON p.permission_code IN ('memory:view', 'memory:recall')
WHERE r.role_code = 'user';
