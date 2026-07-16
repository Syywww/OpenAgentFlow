-- P38：企业级 RAG 元数据、Parent-Child 分片、版本治理与检索缓存。
-- 目标：支撑大量文档和高请求量场景下的增量切片、父子上下文、权限过滤、版本治理和热点检索缓存。

ALTER TABLE knowledge_base
  MODIFY COLUMN chunk_strategy varchar(64) NOT NULL DEFAULT 'parent_child' COMMENT '分片STRATEGY';

UPDATE knowledge_base
SET chunk_strategy = 'parent_child'
WHERE deleted_at IS NULL
  AND chunk_strategy IN ('fixed_size', 'recursive');

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(
  IN p_table_name varchar(64),
  IN p_column_name varchar(64),
  IN p_column_sql text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = database()
      AND table_name = p_table_name
      AND column_name = p_column_name
  ) THEN
    SET @ddl = p_column_sql;
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL add_column_if_missing('knowledge_chunk', 'parent_chunk_id', 'ALTER TABLE knowledge_chunk ADD COLUMN parent_chunk_id char(36) DEFAULT NULL COMMENT ''父分片ID'' AFTER chunk_no');
CALL add_column_if_missing('knowledge_chunk', 'chunk_level', 'ALTER TABLE knowledge_chunk ADD COLUMN chunk_level varchar(32) NOT NULL DEFAULT ''child'' COMMENT ''分片层级：parent/child'' AFTER parent_chunk_id');
CALL add_column_if_missing('knowledge_chunk', 'section_title', 'ALTER TABLE knowledge_chunk ADD COLUMN section_title varchar(500) DEFAULT NULL COMMENT ''章节标题'' AFTER title');
CALL add_column_if_missing('knowledge_chunk', 'section_path', 'ALTER TABLE knowledge_chunk ADD COLUMN section_path varchar(1000) DEFAULT NULL COMMENT ''章节路径'' AFTER section_title');
CALL add_column_if_missing('knowledge_chunk', 'paragraph_no', 'ALTER TABLE knowledge_chunk ADD COLUMN paragraph_no int DEFAULT NULL COMMENT ''段落序号'' AFTER section_path');
CALL add_column_if_missing('knowledge_chunk', 'strategy_version', 'ALTER TABLE knowledge_chunk ADD COLUMN strategy_version varchar(64) NOT NULL DEFAULT ''rag-chunk-v2'' COMMENT ''切片策略版本'' AFTER end_offset');
CALL add_column_if_missing('knowledge_chunk', 'content_hash', 'ALTER TABLE knowledge_chunk ADD COLUMN content_hash char(32) DEFAULT NULL COMMENT ''分片内容MD5'' AFTER strategy_version');
CALL add_column_if_missing('knowledge_chunk', 'source_hash', 'ALTER TABLE knowledge_chunk ADD COLUMN source_hash char(32) DEFAULT NULL COMMENT ''来源文档MD5'' AFTER content_hash');

DROP PROCEDURE IF EXISTS add_column_if_missing;

DROP PROCEDURE IF EXISTS add_index_if_missing;
DELIMITER //
CREATE PROCEDURE add_index_if_missing(
  IN p_table_name varchar(64),
  IN p_index_name varchar(64),
  IN p_index_sql text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = database()
      AND table_name = p_table_name
      AND index_name = p_index_name
  ) THEN
    SET @ddl = p_index_sql;
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL add_index_if_missing('knowledge_chunk', 'idx_chunk_parent', 'CREATE INDEX idx_chunk_parent ON knowledge_chunk(parent_chunk_id)');
CALL add_index_if_missing('knowledge_chunk', 'idx_chunk_level', 'CREATE INDEX idx_chunk_level ON knowledge_chunk(kb_id, chunk_level, status)');
CALL add_index_if_missing('knowledge_chunk', 'idx_chunk_hash', 'CREATE INDEX idx_chunk_hash ON knowledge_chunk(document_id, content_hash)');
CALL add_index_if_missing('knowledge_chunk', 'idx_chunk_section', 'CREATE INDEX idx_chunk_section ON knowledge_chunk(kb_id, section_title)');

DROP PROCEDURE IF EXISTS add_index_if_missing;

CREATE TABLE IF NOT EXISTS knowledge_base_version (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  kb_id char(36) NOT NULL COMMENT '知识库ID',
  version_no varchar(64) NOT NULL COMMENT '版本号',
  version_name varchar(200) DEFAULT NULL COMMENT '版本名称',
  document_count int NOT NULL DEFAULT 0 COMMENT '文档数量',
  chunk_count int NOT NULL DEFAULT 0 COMMENT '分片数量',
  embedding_count int NOT NULL DEFAULT 0 COMMENT '向量数量',
  chunk_strategy varchar(64) NOT NULL COMMENT '切片策略',
  chunk_size int NOT NULL COMMENT '切片大小',
  chunk_overlap int NOT NULL COMMENT '切片重叠长度',
  snapshot_json json NOT NULL COMMENT '版本快照JSON',
  status varchar(32) NOT NULL DEFAULT 'active' COMMENT '状态',
  created_by char(36) DEFAULT NULL COMMENT '创建人',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY(id),
  UNIQUE KEY uk_kb_version(kb_id, version_no),
  KEY idx_kb_version_status(kb_id, status)
) ENGINE=InnoDB COMMENT='知识库版本表';

CREATE TABLE IF NOT EXISTS knowledge_retrieval_cache (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  cache_key char(32) NOT NULL COMMENT '缓存键MD5',
  kb_id char(36) NOT NULL COMMENT '知识库ID',
  query_hash char(32) NOT NULL COMMENT '问题MD5',
  options_hash char(32) NOT NULL COMMENT '检索参数MD5',
  sources_json json NOT NULL COMMENT '缓存引用来源JSON',
  confidence_score decimal(10,6) NOT NULL DEFAULT 0 COMMENT '置信得分',
  hit_count int NOT NULL DEFAULT 0 COMMENT '命中次数',
  expires_at datetime(3) NOT NULL COMMENT '过期时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY(id),
  UNIQUE KEY uk_retrieval_cache_key(cache_key),
  KEY idx_retrieval_cache_kb(kb_id, expires_at)
) ENGINE=InnoDB COMMENT='知识检索缓存表';
