-- P34 企业级异步任务可靠性与文档流水线增强。
-- 增加 Transactional Outbox、Fencing Token、任务检查点和结构化阶段。

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
    WHERE table_schema = DATABASE()
      AND table_name = table_name_param
      AND column_name = column_name_param
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', table_name_param, '` ADD COLUMN `', column_name_param, '` ', column_definition);
    PREPARE statement_handle FROM @ddl;
    EXECUTE statement_handle;
    DEALLOCATE PREPARE statement_handle;
  END IF;
END$$
DELIMITER ;

CALL add_column_if_missing('async_task', 'lock_version', "bigint NOT NULL DEFAULT 0 COMMENT 'Worker执行代次，用于阻止失效Worker提交旧结果' AFTER heartbeat_at");
CALL add_column_if_missing('async_task', 'checkpoint_json', "json COMMENT '可恢复任务检查点JSON' AFTER lock_version");

DROP PROCEDURE IF EXISTS add_column_if_missing;

CREATE TABLE IF NOT EXISTS async_task_outbox (
  id char(36) NOT NULL COMMENT 'Outbox主键ID',
  task_id char(36) NOT NULL COMMENT '关联异步任务ID',
  message_id varchar(80) NOT NULL COMMENT 'Kafka消息唯一ID',
  topic_name varchar(200) NOT NULL COMMENT 'Kafka Topic名称',
  message_key varchar(200) NOT NULL COMMENT 'Kafka分区键',
  schema_version int NOT NULL DEFAULT 1 COMMENT '消息Schema版本',
  payload_json json NOT NULL COMMENT 'Kafka消息JSON',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT 'Outbox状态：pending、sending、sent、failed、dead',
  attempt_count int NOT NULL DEFAULT 0 COMMENT 'Kafka发送尝试次数',
  max_attempts int NOT NULL DEFAULT 20 COMMENT 'Kafka最大发送次数',
  available_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最早允许发送时间',
  locked_by varchar(160) DEFAULT NULL COMMENT '当前领取消息的发布器ID',
  locked_at datetime(3) DEFAULT NULL COMMENT '发布器领取时间',
  sent_at datetime(3) DEFAULT NULL COMMENT 'Kafka Broker确认时间',
  last_error varchar(4000) DEFAULT NULL COMMENT '最近一次发送错误',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_async_task_outbox_message (message_id),
  KEY idx_async_task_outbox_dispatch (status, available_at, created_at),
  KEY idx_async_task_outbox_task (task_id, status),
  KEY idx_async_task_outbox_lock (locked_by, locked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='异步任务Transactional Outbox消息表';

CREATE TABLE IF NOT EXISTS async_task_stage (
  id char(36) NOT NULL COMMENT '阶段主键ID',
  task_id char(36) NOT NULL COMMENT '所属异步任务ID',
  stage_code varchar(80) NOT NULL COMMENT '阶段编码',
  stage_name varchar(160) NOT NULL COMMENT '阶段名称',
  stage_order int NOT NULL DEFAULT 0 COMMENT '阶段顺序',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '阶段状态：pending、running、success、failed、skipped',
  worker_id varchar(160) DEFAULT NULL COMMENT '执行该阶段的Worker ID',
  lock_version bigint NOT NULL DEFAULT 0 COMMENT '执行该阶段时的任务执行代次',
  input_json json DEFAULT NULL COMMENT '阶段输入JSON',
  output_json json DEFAULT NULL COMMENT '阶段输出JSON',
  error_message varchar(4000) DEFAULT NULL COMMENT '阶段错误摘要',
  started_at datetime(3) DEFAULT NULL COMMENT '阶段开始时间',
  finished_at datetime(3) DEFAULT NULL COMMENT '阶段完成时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_async_task_stage (task_id, stage_code),
  KEY idx_async_task_stage_timeline (task_id, stage_order),
  KEY idx_async_task_stage_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='异步任务结构化阶段表';

-- 升级前遗留的待执行任务由 KafkaTaskRecoveryScheduler 自动补充 Outbox。
