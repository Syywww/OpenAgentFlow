USE openagentflow;

-- P64-P66：质量门禁、Flyway迁移治理与文档DAG分布式一致性。

DROP PROCEDURE IF EXISTS migrate_p64_p66_pipeline;
DELIMITER $$
CREATE PROCEDURE migrate_p64_p66_pipeline()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='knowledge_document' AND COLUMN_NAME='pipeline_generation'
  ) THEN
    ALTER TABLE knowledge_document
      ADD COLUMN pipeline_generation bigint NOT NULL DEFAULT 0 COMMENT '文档处理流水线代次' AFTER parse_error;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='knowledge_document' AND COLUMN_NAME='current_pipeline_root_id'
  ) THEN
    ALTER TABLE knowledge_document
      ADD COLUMN current_pipeline_root_id char(36) DEFAULT NULL COMMENT '当前有效文档DAG根任务ID' AFTER pipeline_generation;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='knowledge_document' AND INDEX_NAME='idx_knowledge_document_pipeline'
  ) THEN
    CREATE INDEX idx_knowledge_document_pipeline
      ON knowledge_document(current_pipeline_root_id, parse_status, updated_at);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='document_pipeline_node' AND COLUMN_NAME='generation_no'
  ) THEN
    ALTER TABLE document_pipeline_node
      ADD COLUMN generation_no bigint NOT NULL DEFAULT 0 COMMENT '节点所属文档流水线代次' AFTER root_task_id;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='document_pipeline_node' AND COLUMN_NAME='expected_item_count'
  ) THEN
    ALTER TABLE document_pipeline_node
      ADD COLUMN expected_item_count int DEFAULT NULL COMMENT '节点预期处理条目数' AFTER item_count;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='document_pipeline_node' AND COLUMN_NAME='actual_item_count'
  ) THEN
    ALTER TABLE document_pipeline_node
      ADD COLUMN actual_item_count int DEFAULT NULL COMMENT '节点实际处理条目数' AFTER expected_item_count;
  END IF;
END$$
DELIMITER ;
CALL migrate_p64_p66_pipeline();
DROP PROCEDURE IF EXISTS migrate_p64_p66_pipeline;

CREATE TABLE IF NOT EXISTS document_pipeline_reconcile_issue (
  id char(36) NOT NULL COMMENT '对账问题主键ID',
  document_id char(36) NOT NULL COMMENT '知识文档ID',
  kb_id char(36) NOT NULL COMMENT '知识库ID',
  root_task_id char(36) DEFAULT NULL COMMENT '关联文档DAG根任务ID',
  pipeline_generation bigint NOT NULL DEFAULT 0 COMMENT '问题所属流水线代次',
  issue_type varchar(64) NOT NULL COMMENT '问题类型：节点缺失、分片缺失、向量缺失或任务停滞',
  severity varchar(16) NOT NULL DEFAULT 'high' COMMENT '严重级别：low、medium、high、critical',
  expected_count bigint DEFAULT NULL COMMENT '预期条目数',
  actual_count bigint DEFAULT NULL COMMENT '实际条目数',
  detail_json json DEFAULT NULL COMMENT '对账问题详情JSON',
  status varchar(32) NOT NULL DEFAULT 'open' COMMENT '处理状态：open、resolved、ignored',
  first_detected_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '首次发现时间',
  last_detected_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最近发现时间',
  resolved_at datetime(3) DEFAULT NULL COMMENT '解决时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_pipeline_reconcile_open (document_id, root_task_id, issue_type),
  KEY idx_pipeline_reconcile_status (status, severity, last_detected_at),
  KEY idx_pipeline_reconcile_kb (kb_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文档分布式流水线自动对账问题表';
