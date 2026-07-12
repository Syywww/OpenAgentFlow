USE openagentflow;

-- P63：Memory生产级增强，覆盖异步提取、租户隔离、向量一致性、召回质量和治理运营。
DROP PROCEDURE IF EXISTS add_memory_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_memory_column_if_missing(IN p_column varchar(64), IN p_definition text)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'agent_memory' AND column_name = p_column
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE agent_memory ADD COLUMN `', p_column, '` ', p_definition);
    PREPARE statement_handle FROM @ddl;
    EXECUTE statement_handle;
    DEALLOCATE PREPARE statement_handle;
  END IF;
END$$
DELIMITER ;

CALL add_memory_column_if_missing('organization_id', "char(36) NULL COMMENT '所属组织ID，用于组织级数据隔离' AFTER id");
CALL add_memory_column_if_missing('workspace_id', "char(36) NULL COMMENT '所属工作空间ID，用于租户硬隔离' AFTER organization_id");
CALL add_memory_column_if_missing('subject_id', "varchar(160) NULL COMMENT '记忆主体ID，例如客户、员工或设备ID' AFTER session_id");
CALL add_memory_column_if_missing('fact_key', "varchar(200) NULL COMMENT '结构化事实键，用于去重与冲突检测' AFTER memory_key");
CALL add_memory_column_if_missing('content_hash', "char(64) NULL COMMENT '规范化记忆内容SHA-256哈希，用于幂等去重' AFTER memory_value");
CALL add_memory_column_if_missing('confidence_score', "decimal(5,4) NOT NULL DEFAULT 0.7000 COMMENT '事实置信度，范围0到1' AFTER importance_score");
CALL add_memory_column_if_missing('source_reliability', "decimal(5,4) NOT NULL DEFAULT 0.7000 COMMENT '来源可信度，范围0到1' AFTER confidence_score");
CALL add_memory_column_if_missing('utility_score', "decimal(5,4) NOT NULL DEFAULT 0.5000 COMMENT '综合使用价值得分，范围0到1' AFTER source_reliability");
CALL add_memory_column_if_missing('valid_from', "datetime(3) NULL COMMENT '事实生效时间' AFTER utility_score");
CALL add_memory_column_if_missing('valid_to', "datetime(3) NULL COMMENT '事实失效时间' AFTER valid_from");
CALL add_memory_column_if_missing('superseded_by', "char(36) NULL COMMENT '替代当前事实的新记忆ID' AFTER valid_to");
CALL add_memory_column_if_missing('version_no', "int NOT NULL DEFAULT 1 COMMENT '同一事实的版本号' AFTER superseded_by");
CALL add_memory_column_if_missing('embedding_model_id', "char(36) NULL COMMENT '生成向量的Embedding模型ID' AFTER embedding_blob");
CALL add_memory_column_if_missing('embedding_dimension', "int NULL COMMENT '向量维度' AFTER embedding_model_id");
CALL add_memory_column_if_missing('embedding_version', "varchar(64) NULL COMMENT 'Embedding模型或算法版本' AFTER embedding_dimension");
CALL add_memory_column_if_missing('sync_retry_count', "int NOT NULL DEFAULT 0 COMMENT '向量同步重试次数' AFTER sync_status");
CALL add_memory_column_if_missing('sync_error', "varchar(1000) NULL COMMENT '最近一次向量同步失败原因' AFTER sync_retry_count");
CALL add_memory_column_if_missing('deleted_at', "datetime(3) NULL COMMENT '软删除时间' AFTER last_accessed_at");
CALL add_memory_column_if_missing('created_by', "char(36) NULL COMMENT '创建人ID' AFTER deleted_at");
DROP PROCEDURE IF EXISTS add_memory_column_if_missing;

UPDATE agent_memory m
JOIN agent a ON a.id = m.agent_id
SET m.workspace_id = COALESCE(m.workspace_id, a.workspace_id)
WHERE m.workspace_id IS NULL;

UPDATE agent_memory m
LEFT JOIN oaf_workspace w ON w.id = m.workspace_id
SET m.organization_id = COALESCE(m.organization_id, w.organization_id),
    m.fact_key = COALESCE(m.fact_key, m.memory_key),
    m.valid_from = COALESCE(m.valid_from, m.created_at),
    m.content_hash = COALESCE(m.content_hash, SHA2(LOWER(TRIM(m.memory_text)), 256));

CREATE INDEX idx_memory_workspace_user_status ON agent_memory(workspace_id, user_id, status, memory_type, updated_at);
CREATE INDEX idx_memory_workspace_fact ON agent_memory(workspace_id, agent_id, subject_id, fact_key, status, version_no);
CREATE INDEX idx_memory_sync_retry ON agent_memory(workspace_id, sync_status, sync_retry_count, updated_at);
CREATE INDEX idx_memory_content_hash ON agent_memory(workspace_id, user_id, content_hash);
CREATE INDEX idx_memory_validity ON agent_memory(workspace_id, status, valid_from, valid_to);

CREATE TABLE IF NOT EXISTS memory_policy (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '策略主键ID',
  workspace_id char(36) NOT NULL COMMENT '所属工作空间ID',
  agent_id char(36) NULL COMMENT '适用Agent ID，为空表示空间默认策略',
  policy_name varchar(160) NOT NULL COMMENT '策略名称',
  extraction_enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用LLM结构化提取',
  min_importance decimal(5,4) NOT NULL DEFAULT 0.5500 COMMENT '最低重要度阈值',
  min_confidence decimal(5,4) NOT NULL DEFAULT 0.6500 COMMENT '最低事实置信度阈值',
  recall_threshold decimal(5,4) NOT NULL DEFAULT 0.3500 COMMENT '召回最低综合得分',
  recall_limit int NOT NULL DEFAULT 8 COMMENT '召回候选数量',
  prompt_token_budget int NOT NULL DEFAULT 1200 COMMENT '注入Prompt的最大估算Token数',
  short_term_ttl_days int NOT NULL DEFAULT 7 COMMENT '短期记忆保留天数',
  long_term_ttl_days int NULL COMMENT '长期记忆保留天数，空值表示永久',
  max_memories_per_user int NOT NULL DEFAULT 10000 COMMENT '单用户最大有效记忆数量',
  pii_mode varchar(32) NOT NULL DEFAULT 'redact' COMMENT 'PII处理模式：allow、redact、reject',
  conflict_mode varchar(32) NOT NULL DEFAULT 'supersede' COMMENT '冲突处理模式：keep、review、supersede',
  allowed_categories json NULL COMMENT '允许沉淀的记忆分类JSON',
  forbidden_categories json NULL COMMENT '禁止沉淀的记忆分类JSON',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '策略状态：enabled、disabled',
  created_by char(36) NULL COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_memory_policy_scope(workspace_id, agent_id),
  KEY idx_memory_policy_workspace(workspace_id, status)
) COMMENT='Memory提取、召回、保留和隐私策略表';

CREATE TABLE IF NOT EXISTS memory_feedback (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '反馈主键ID',
  workspace_id char(36) NOT NULL COMMENT '所属工作空间ID',
  memory_id char(36) NOT NULL COMMENT '记忆ID',
  run_id char(36) NULL COMMENT '触发反馈的运行ID',
  user_id char(36) NOT NULL COMMENT '反馈用户ID',
  feedback_type varchar(32) NOT NULL COMMENT '反馈类型：helpful、irrelevant、incorrect、outdated、sensitive',
  score decimal(5,4) NULL COMMENT '反馈评分，范围0到1',
  comment_text varchar(1000) NULL COMMENT '反馈说明',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_memory_feedback_memory(memory_id, created_at),
  KEY idx_memory_feedback_workspace(workspace_id, feedback_type, created_at)
) COMMENT='Memory召回质量反馈表';

CREATE TABLE IF NOT EXISTS memory_governance_issue (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '治理问题主键ID',
  workspace_id char(36) NOT NULL COMMENT '所属工作空间ID',
  memory_id char(36) NULL COMMENT '关联记忆ID',
  issue_type varchar(48) NOT NULL COMMENT '问题类型：duplicate、conflict、expired、low_value、pii、sync_failed、orphan',
  severity varchar(16) NOT NULL DEFAULT 'medium' COMMENT '严重级别：low、medium、high、critical',
  issue_detail json NULL COMMENT '问题详情JSON',
  status varchar(24) NOT NULL DEFAULT 'open' COMMENT '状态：open、processing、resolved、ignored',
  resolution varchar(1000) NULL COMMENT '处置说明',
  resolved_by char(36) NULL COMMENT '处置人ID',
  resolved_at datetime(3) NULL COMMENT '处置时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_memory_issue_workspace(workspace_id, status, severity, created_at),
  KEY idx_memory_issue_memory(memory_id, issue_type)
) COMMENT='Memory治理问题表';

CREATE TABLE IF NOT EXISTS memory_access_metric (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '指标主键ID',
  workspace_id char(36) NOT NULL COMMENT '所属工作空间ID',
  agent_id char(36) NULL COMMENT 'Agent ID',
  metric_date date NOT NULL COMMENT '指标日期',
  extraction_total bigint NOT NULL DEFAULT 0 COMMENT '提取次数',
  extraction_accepted bigint NOT NULL DEFAULT 0 COMMENT '提取采纳次数',
  recall_total bigint NOT NULL DEFAULT 0 COMMENT '召回次数',
  recall_hit_total bigint NOT NULL DEFAULT 0 COMMENT '有效命中数量',
  feedback_positive bigint NOT NULL DEFAULT 0 COMMENT '正向反馈数量',
  feedback_negative bigint NOT NULL DEFAULT 0 COMMENT '负向反馈数量',
  avg_recall_latency_ms decimal(12,2) NOT NULL DEFAULT 0 COMMENT '平均召回耗时毫秒',
  embedding_token_total bigint NOT NULL DEFAULT 0 COMMENT 'Embedding Token总量',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_memory_metric_scope(workspace_id, agent_id, metric_date)
) COMMENT='Memory运营聚合指标表';

INSERT IGNORE INTO iam_permission(permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order, visible, status)
VALUES
 ('memory:policy', 'Memory策略管理', 'api', '/memories', 'ALL', '/api/memories/policies/**', 131, 1, 'enabled'),
 ('memory:governance', 'Memory治理处置', 'api', '/memories', 'ALL', '/api/memories/governance/**', 132, 1, 'enabled'),
 ('memory:feedback', 'Memory质量反馈', 'api', '/memories', 'POST', '/api/memories/*/feedback', 133, 1, 'enabled'),
 ('memory:forget', 'Memory用户遗忘', 'api', '/memories', 'DELETE', '/api/memories/subjects/**', 134, 1, 'enabled');

INSERT IGNORE INTO iam_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM iam_role r JOIN iam_permission p
  ON p.permission_code IN ('memory:policy','memory:governance','memory:feedback','memory:forget')
WHERE r.role_code IN ('super_admin','admin');

INSERT IGNORE INTO iam_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM iam_role r JOIN iam_permission p ON p.permission_code IN ('memory:feedback','memory:forget')
WHERE r.role_code IN ('developer','user');

INSERT IGNORE INTO memory_policy(
  id, workspace_id, agent_id, policy_name, extraction_enabled, min_importance, min_confidence,
  recall_threshold, recall_limit, prompt_token_budget, pii_mode, conflict_mode, status, created_by
)
SELECT UUID(), w.id, NULL, CONCAT(w.workspace_name, '默认Memory策略'), 1, 0.55, 0.65,
       0.35, 8, 1200, 'redact', 'supersede', 'enabled', w.owner_user_id
FROM oaf_workspace w
WHERE NOT EXISTS (SELECT 1 FROM memory_policy p WHERE p.workspace_id=w.id AND p.agent_id IS NULL);
