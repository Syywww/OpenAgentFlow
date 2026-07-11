-- Kafka 分布式异步任务增强。
-- 为异步任务增加投递、分布式锁、心跳、延迟重试和死信治理字段。

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

CALL add_column_if_missing('async_task', 'queue_topic', "varchar(200) COMMENT '当前投递的Kafka Topic名称'");
CALL add_column_if_missing('async_task', 'kafka_message_id', "varchar(80) COMMENT '最近一次Kafka消息唯一ID'");
CALL add_column_if_missing('async_task', 'locked_by', "varchar(160) COMMENT '当前领取任务的Worker实例ID'");
CALL add_column_if_missing('async_task', 'locked_at', "datetime(3) COMMENT 'Worker领取任务时间'");
CALL add_column_if_missing('async_task', 'heartbeat_at', "datetime(3) COMMENT 'Worker最近心跳时间'");
CALL add_column_if_missing('async_task', 'last_enqueued_at', "datetime(3) COMMENT '最近一次成功投递Kafka时间'");
CALL add_column_if_missing('async_task', 'next_retry_at', "datetime(3) COMMENT '下次允许重试执行时间'");
CALL add_column_if_missing('async_task', 'dead_letter_at', "datetime(3) COMMENT '进入Kafka死信队列时间'");

DROP PROCEDURE IF EXISTS add_column_if_missing;

DROP PROCEDURE IF EXISTS add_index_if_missing;
DELIMITER $$
CREATE PROCEDURE add_index_if_missing(
  IN table_name_param varchar(128),
  IN index_name_param varchar(128),
  IN index_columns text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = table_name_param
      AND index_name = index_name_param
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', table_name_param, '` ADD INDEX `', index_name_param, '` (', index_columns, ')');
    PREPARE statement_handle FROM @ddl;
    EXECUTE statement_handle;
    DEALLOCATE PREPARE statement_handle;
  END IF;
END$$
DELIMITER ;

CALL add_index_if_missing('async_task', 'idx_async_task_recovery', 'status, heartbeat_at, last_enqueued_at');
CALL add_index_if_missing('async_task', 'idx_async_task_retry', 'status, next_retry_at');
CALL add_index_if_missing('async_task', 'idx_async_task_worker', 'locked_by, status');

DROP PROCEDURE IF EXISTS add_index_if_missing;

ALTER TABLE async_task
  MODIFY COLUMN status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '任务状态：pending、running、success、failed、canceled、dead_letter',
  MODIFY COLUMN retry_count int NOT NULL DEFAULT 0 COMMENT '已执行的自动或人工重试次数',
  MODIFY COLUMN max_retries int NOT NULL DEFAULT 3 COMMENT '最大自动重试次数';

UPDATE async_task
SET max_retries = 3
WHERE max_retries IS NULL OR max_retries < 3;
