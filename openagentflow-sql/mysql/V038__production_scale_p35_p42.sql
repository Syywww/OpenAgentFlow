-- P35-P42：文档DAG、Runtime执行面、检索版本、生命周期、安全配额和交付治理。

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
  IN table_name_param varchar(128),
  IN column_name_param varchar(128),
  IN column_definition text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = table_name_param AND column_name = column_name_param
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', table_name_param, '` ADD COLUMN `', column_name_param, '` ', column_definition);
    PREPARE statement_handle FROM @ddl;
    EXECUTE statement_handle;
    DEALLOCATE PREPARE statement_handle;
  END IF;
END$$
DELIMITER ;

CALL add_column_if_missing('async_task', 'parent_task_id', "char(36) COMMENT '父任务ID，用于DAG任务聚合' AFTER id");
CALL add_column_if_missing('async_task', 'root_task_id', "char(36) COMMENT '根任务ID，用于查询完整DAG' AFTER parent_task_id");
CALL add_column_if_missing('async_task', 'shard_no', "int NOT NULL DEFAULT 0 COMMENT '分片任务序号' AFTER root_task_id");
CALL add_column_if_missing('async_task', 'shard_total', "int NOT NULL DEFAULT 1 COMMENT '同阶段分片任务总数' AFTER shard_no");
CALL add_column_if_missing('async_task', 'idempotency_key', "varchar(200) COMMENT '任务幂等键' AFTER shard_total");
CALL add_column_if_missing('async_task_stage', 'shard_no', "int NOT NULL DEFAULT 0 COMMENT '阶段分片序号' AFTER stage_order");
CALL add_column_if_missing('async_task_stage', 'attempt_no', "int NOT NULL DEFAULT 1 COMMENT '阶段执行尝试序号' AFTER shard_no");
CALL add_column_if_missing('runtime_run', 'workspace_id', "char(36) COMMENT '运行所属工作空间ID' AFTER user_id");
CALL add_column_if_missing('runtime_run', 'tenant_id', "char(36) COMMENT '运行所属租户ID' AFTER workspace_id");
CALL add_column_if_missing('runtime_run', 'cancel_requested', "tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已请求停止运行' AFTER status");
CALL add_column_if_missing('runtime_run', 'executor_id', "varchar(160) COMMENT '实际执行Runtime实例ID' AFTER cancel_requested");

DROP PROCEDURE IF EXISTS add_column_if_missing;

DROP PROCEDURE IF EXISTS migrate_stage_attempt_index;
DELIMITER $$
CREATE PROCEDURE migrate_stage_attempt_index()
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='async_task_stage' AND index_name='uk_async_task_stage'
  ) THEN
    ALTER TABLE async_task_stage DROP INDEX uk_async_task_stage;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='async_task_stage' AND index_name='uk_async_task_stage_attempt'
  ) THEN
    ALTER TABLE async_task_stage
      ADD UNIQUE KEY uk_async_task_stage_attempt (task_id, stage_code, shard_no, lock_version);
  END IF;
END$$
DELIMITER ;
CALL migrate_stage_attempt_index();
DROP PROCEDURE IF EXISTS migrate_stage_attempt_index;

CREATE TABLE IF NOT EXISTS document_pipeline_node (
  id char(36) NOT NULL COMMENT '流水线节点主键ID',
  root_task_id char(36) NOT NULL COMMENT '文档DAG根任务ID',
  task_id char(36) DEFAULT NULL COMMENT '节点关联异步任务ID',
  document_id char(36) NOT NULL COMMENT '知识文档ID',
  kb_id char(36) NOT NULL COMMENT '知识库ID',
  stage_code varchar(64) NOT NULL COMMENT '阶段编码：parse、chunk、embed、vector_write、finalize',
  shard_no int NOT NULL DEFAULT 0 COMMENT '阶段分片序号',
  shard_total int NOT NULL DEFAULT 1 COMMENT '阶段分片总数',
  dependency_count int NOT NULL DEFAULT 0 COMMENT '尚未完成的前置节点数量',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '节点状态：pending、queued、running、success、failed、canceled',
  artifact_bucket varchar(128) DEFAULT NULL COMMENT '阶段产物存储桶',
  artifact_key varchar(500) DEFAULT NULL COMMENT '阶段产物对象键',
  artifact_hash varchar(64) DEFAULT NULL COMMENT '阶段产物内容哈希',
  item_start bigint DEFAULT NULL COMMENT '当前分片起始位置',
  item_end bigint DEFAULT NULL COMMENT '当前分片结束位置',
  item_count int NOT NULL DEFAULT 0 COMMENT '当前分片处理条目数',
  attempt_no int NOT NULL DEFAULT 0 COMMENT '节点执行尝试次数',
  idempotency_key varchar(200) NOT NULL COMMENT '节点幂等键',
  input_json json DEFAULT NULL COMMENT '节点输入JSON',
  output_json json DEFAULT NULL COMMENT '节点输出JSON',
  error_message varchar(4000) DEFAULT NULL COMMENT '节点最近错误摘要',
  started_at datetime(3) DEFAULT NULL COMMENT '节点开始时间',
  finished_at datetime(3) DEFAULT NULL COMMENT '节点完成时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_document_pipeline_idempotency (idempotency_key),
  KEY idx_document_pipeline_root (root_task_id, stage_code, shard_no),
  KEY idx_document_pipeline_ready (status, dependency_count, created_at),
  KEY idx_document_pipeline_document (document_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识文档分布式DAG节点表';

CREATE TABLE IF NOT EXISTS runtime_control_command (
  id char(36) NOT NULL COMMENT '控制指令主键ID',
  run_id char(36) NOT NULL COMMENT '运行ID',
  command_type varchar(32) NOT NULL COMMENT '指令类型：cancel、pause、resume、supplement',
  command_payload json DEFAULT NULL COMMENT '控制指令参数JSON',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '处理状态：pending、applied、ignored、failed',
  requested_by char(36) DEFAULT NULL COMMENT '发起用户ID',
  applied_by varchar(160) DEFAULT NULL COMMENT '处理指令的Runtime实例ID',
  applied_at datetime(3) DEFAULT NULL COMMENT '指令生效时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_runtime_control_run (run_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent Runtime控制指令表';

CREATE TABLE IF NOT EXISTS knowledge_index_version (
  id char(36) NOT NULL COMMENT '索引版本主键ID',
  kb_id char(36) NOT NULL COMMENT '知识库ID',
  workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID',
  version_no int NOT NULL COMMENT '索引版本号',
  collection_name varchar(200) NOT NULL COMMENT 'Milvus物理集合名称',
  collection_alias varchar(200) NOT NULL COMMENT 'Milvus稳定集合别名',
  keyword_index_name varchar(200) DEFAULT NULL COMMENT 'OpenSearch关键词索引名称',
  embedding_model_id char(36) DEFAULT NULL COMMENT 'Embedding模型ID',
  dimension int DEFAULT NULL COMMENT '向量维度',
  status varchar(32) NOT NULL DEFAULT 'building' COMMENT '版本状态：building、ready、active、retired、failed',
  chunk_count bigint NOT NULL DEFAULT 0 COMMENT '版本包含分片数',
  activated_at datetime(3) DEFAULT NULL COMMENT '版本激活时间',
  retired_at datetime(3) DEFAULT NULL COMMENT '版本退役时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_index_version (kb_id, version_no),
  KEY idx_knowledge_index_active (kb_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库蓝绿索引版本表';

CREATE TABLE IF NOT EXISTS data_lifecycle_job (
  id char(36) NOT NULL COMMENT '生命周期作业主键ID',
  workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID',
  resource_type varchar(64) NOT NULL COMMENT '资源类型',
  resource_id char(36) NOT NULL COMMENT '资源ID',
  action_type varchar(32) NOT NULL COMMENT '动作类型：delete、archive、purge',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '作业状态',
  storage_targets json DEFAULT NULL COMMENT '需要清理的存储目标JSON',
  deleted_counts json DEFAULT NULL COMMENT '各存储删除数量JSON',
  retry_count int NOT NULL DEFAULT 0 COMMENT '重试次数',
  max_retries int NOT NULL DEFAULT 5 COMMENT '最大重试次数',
  last_error varchar(4000) DEFAULT NULL COMMENT '最近错误摘要',
  next_retry_at datetime(3) DEFAULT NULL COMMENT '下次重试时间',
  finished_at datetime(3) DEFAULT NULL COMMENT '完成时间',
  created_by char(36) DEFAULT NULL COMMENT '创建用户ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_data_lifecycle_dispatch (status, next_retry_at, created_at),
  KEY idx_data_lifecycle_resource (resource_type, resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨存储数据生命周期作业表';

CREATE TABLE IF NOT EXISTS tenant_resource_quota (
  id char(36) NOT NULL COMMENT '租户配额主键ID',
  workspace_id char(36) NOT NULL COMMENT '工作空间ID',
  max_documents bigint NOT NULL DEFAULT 100000 COMMENT '最大文档数量',
  max_storage_bytes bigint NOT NULL DEFAULT 107374182400 COMMENT '最大对象存储字节数',
  max_vector_count bigint NOT NULL DEFAULT 10000000 COMMENT '最大向量数量',
  max_runtime_concurrency int NOT NULL DEFAULT 100 COMMENT '最大Runtime并发数',
  max_task_concurrency int NOT NULL DEFAULT 50 COMMENT '最大异步任务并发数',
  max_upload_bytes bigint NOT NULL DEFAULT 1073741824 COMMENT '单文件最大上传字节数',
  monthly_token_limit bigint NOT NULL DEFAULT 100000000 COMMENT '月度Token上限',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用配额限制',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_resource_quota_workspace (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作空间基础设施资源配额表';

CREATE TABLE IF NOT EXISTS platform_security_event (
  id char(36) NOT NULL COMMENT '安全事件主键ID',
  workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID',
  user_id char(36) DEFAULT NULL COMMENT '关联用户ID',
  event_type varchar(64) NOT NULL COMMENT '事件类型',
  risk_level varchar(16) NOT NULL COMMENT '风险等级：low、medium、high、critical',
  resource_type varchar(64) DEFAULT NULL COMMENT '资源类型',
  resource_id varchar(128) DEFAULT NULL COMMENT '资源ID',
  client_ip varchar(64) DEFAULT NULL COMMENT '客户端IP',
  detail_json json DEFAULT NULL COMMENT '安全事件详情JSON',
  handled tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已处置',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_security_event_workspace (workspace_id, created_at),
  KEY idx_security_event_risk (risk_level, handled, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台基础设施安全事件表';

DROP PROCEDURE IF EXISTS add_index_if_missing;
DELIMITER $$
CREATE PROCEDURE add_index_if_missing(
  IN table_name_param varchar(128),
  IN index_name_param varchar(128),
  IN index_definition text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name=table_name_param AND index_name=index_name_param
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', table_name_param, '` ADD ', index_definition);
    PREPARE statement_handle FROM @ddl;
    EXECUTE statement_handle;
    DEALLOCATE PREPARE statement_handle;
  END IF;
END$$
DELIMITER ;

CALL add_index_if_missing('async_task', 'idx_async_task_dag', 'INDEX idx_async_task_dag (root_task_id, parent_task_id, task_type, shard_no)');
CALL add_index_if_missing('async_task', 'uk_async_task_idempotency', 'UNIQUE INDEX uk_async_task_idempotency (idempotency_key)');
CALL add_index_if_missing('runtime_run', 'idx_runtime_run_tenant_status', 'INDEX idx_runtime_run_tenant_status (workspace_id, status, created_at)');

DROP PROCEDURE IF EXISTS add_index_if_missing;
