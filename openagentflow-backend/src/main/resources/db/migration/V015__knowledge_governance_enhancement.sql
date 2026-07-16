-- P16：知识库治理增强。
-- 目标：把知识库质量、向量同步、长期未维护、未绑定智能体等问题沉淀为可运营的治理问题。

CREATE TABLE IF NOT EXISTS knowledge_governance_policy (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  policy_code varchar(120) NOT NULL COMMENT '策略编码',
  policy_name varchar(160) NOT NULL COMMENT '策略名称',
  kb_id char(36) DEFAULT NULL COMMENT '限定知识库ID，为空表示全局策略',
  stale_days int NOT NULL DEFAULT 90 COMMENT '文档超过多少天未更新视为陈旧',
  min_chunk_tokens int NOT NULL DEFAULT 20 COMMENT '分片最小Token数量',
  max_chunk_tokens int NOT NULL DEFAULT 1200 COMMENT '分片最大Token数量',
  max_failed_documents int NOT NULL DEFAULT 0 COMMENT '允许的最大失败文档数',
  require_agent_binding tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否要求知识库绑定至少一个智能体',
  require_milvus_sync tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否要求向量同步到Milvus',
  auto_issue_enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用自动生成治理问题',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '策略状态：enabled启用、disabled停用',
  created_by char(36) DEFAULT NULL COMMENT '创建人用户ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_governance_policy_code (policy_code),
  KEY idx_knowledge_governance_policy_kb (kb_id),
  KEY idx_knowledge_governance_policy_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库治理策略表';

CREATE TABLE IF NOT EXISTS knowledge_governance_issue (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  kb_id char(36) NOT NULL COMMENT '知识库ID',
  document_id char(36) DEFAULT NULL COMMENT '关联文档ID',
  chunk_id char(36) DEFAULT NULL COMMENT '关联分片ID',
  issue_type varchar(64) NOT NULL COMMENT '问题类型',
  severity varchar(32) NOT NULL DEFAULT 'medium' COMMENT '严重级别：low、medium、high、critical',
  issue_title varchar(200) NOT NULL COMMENT '问题标题',
  issue_detail text COMMENT '问题详情',
  evidence_json json DEFAULT NULL COMMENT '问题证据JSON',
  status varchar(32) NOT NULL DEFAULT 'open' COMMENT '处理状态：open待处理、ignored已忽略、resolved已解决',
  handler_user_id char(36) DEFAULT NULL COMMENT '处理人用户ID',
  handled_at datetime(3) DEFAULT NULL COMMENT '处理时间',
  handle_note varchar(600) DEFAULT NULL COMMENT '处理备注',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_knowledge_governance_issue_kb (kb_id),
  KEY idx_knowledge_governance_issue_document (document_id),
  KEY idx_knowledge_governance_issue_chunk (chunk_id),
  KEY idx_knowledge_governance_issue_status (status, severity),
  KEY idx_knowledge_governance_issue_type (issue_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库治理问题表';

INSERT IGNORE INTO knowledge_governance_policy
  (id, policy_code, policy_name, kb_id, stale_days, min_chunk_tokens, max_chunk_tokens,
   max_failed_documents, require_agent_binding, require_milvus_sync, auto_issue_enabled, status, created_by)
VALUES
  ('98000000-0000-0000-0000-000000000001', 'default-knowledge-governance', '默认知识库治理策略', NULL,
   90, 20, 1200, 0, 1, 1, 1, 'enabled', '00000000-0000-0000-0000-000000000001');

INSERT IGNORE INTO iam_permission
  (id, permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order, visible, status)
VALUES
  ('00000000-0000-0000-0000-000000000315', 'knowledge:governance:view', '知识库治理查看', 'api', '/knowledge-governance', 'GET', '/knowledge-governance/**', 315, 1, 'enabled'),
  ('00000000-0000-0000-0000-000000000316', 'knowledge:governance:manage', '知识库治理管理', 'api', '/knowledge-governance', 'POST', '/knowledge-governance/**', 316, 1, 'enabled');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code IN ('knowledge:governance:view', 'knowledge:governance:manage')
WHERE role.role_code IN ('super_admin', 'admin');
