-- P53-P62：真实ANN召回、流式续传、首Token指标、主动一致性、门禁豁免、灾备与供应链闭环。

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_column_if_missing(IN table_name_param varchar(128), IN column_name_param varchar(128), IN column_definition text)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name=table_name_param AND column_name=column_name_param
  ) THEN
    SET @ddl=CONCAT('ALTER TABLE `',table_name_param,'` ADD COLUMN `',column_name_param,'` ',column_definition);
    PREPARE statement_handle FROM @ddl;
    EXECUTE statement_handle;
    DEALLOCATE PREPARE statement_handle;
  END IF;
END$$
DELIMITER ;

CALL add_column_if_missing('runtime_run','first_token_latency_ms',"int DEFAULT NULL COMMENT '流式输出首Token延迟毫秒数' AFTER latency_ms");
CALL add_column_if_missing('runtime_llm_call','first_token_latency_ms',"int DEFAULT NULL COMMENT '单次模型调用首Token延迟毫秒数' AFTER latency_ms");
CALL add_column_if_missing('data_consistency_issue','resolution',"varchar(500) DEFAULT NULL COMMENT '一致性问题处理说明' AFTER resolved_at");
DROP PROCEDURE IF EXISTS add_column_if_missing;

-- MODIFY会修复旧环境因客户端字符集错误造成的列注释乱码，同时保持数据和列类型不变。
ALTER TABLE runtime_run MODIFY COLUMN first_token_latency_ms int DEFAULT NULL COMMENT '流式输出首Token延迟毫秒数';
ALTER TABLE runtime_llm_call MODIFY COLUMN first_token_latency_ms int DEFAULT NULL COMMENT '单次模型调用首Token延迟毫秒数';
ALTER TABLE data_consistency_issue MODIFY COLUMN resolution varchar(500) DEFAULT NULL COMMENT '一致性问题处理说明';

CREATE TABLE IF NOT EXISTS release_gate_waiver (
  id char(36) NOT NULL COMMENT '发布门禁豁免主键ID',
  resource_type varchar(32) NOT NULL COMMENT '资源类型：agent、prompt、workflow、knowledge',
  resource_id char(36) NOT NULL COMMENT '资源ID',
  reason varchar(1000) NOT NULL COMMENT '申请豁免原因',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '状态：pending、approved、rejected、expired',
  requested_by char(36) DEFAULT NULL COMMENT '申请用户ID',
  approved_by char(36) DEFAULT NULL COMMENT '审批用户ID',
  approved_at datetime(3) DEFAULT NULL COMMENT '审批时间',
  expires_at datetime(3) NOT NULL COMMENT '豁免失效时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_release_gate_waiver_resource (resource_type,resource_id,status,expires_at),
  KEY idx_release_gate_waiver_expire (status,expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发布质量门禁限时豁免审批表';

CREATE TABLE IF NOT EXISTS disaster_recovery_drill (
  id char(36) NOT NULL COMMENT '灾备演练主键ID',
  drill_code varchar(80) NOT NULL COMMENT '演练编码',
  drill_type varchar(32) NOT NULL COMMENT '演练类型：backup、restore、failover、full',
  status varchar(32) NOT NULL COMMENT '状态：running、success、failed',
  rpo_seconds bigint DEFAULT NULL COMMENT '实际恢复点目标秒数',
  rto_seconds bigint DEFAULT NULL COMMENT '实际恢复时间目标秒数',
  backup_uri varchar(1000) DEFAULT NULL COMMENT '备份制品地址',
  checksum varchar(128) DEFAULT NULL COMMENT '备份制品SHA256校验值',
  detail_json json DEFAULT NULL COMMENT '演练步骤与结果JSON',
  started_at datetime(3) NOT NULL COMMENT '开始时间',
  finished_at datetime(3) DEFAULT NULL COMMENT '结束时间',
  created_by char(36) DEFAULT NULL COMMENT '发起用户ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_disaster_recovery_drill_code (drill_code),
  KEY idx_disaster_recovery_drill_status (status,started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产灾备恢复演练结果表';

-- 为租户上下文常用过滤列幂等补充组合索引，降低强制工作空间隔离后的查询开销。
DROP PROCEDURE IF EXISTS add_index_if_missing;
DELIMITER $$
CREATE PROCEDURE add_index_if_missing(IN table_name_param varchar(128), IN index_name_param varchar(128), IN columns_param varchar(500))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name=table_name_param AND index_name=index_name_param
  ) THEN
    SET @ddl=CONCAT('CREATE INDEX `',index_name_param,'` ON `',table_name_param,'` (',columns_param,')');
    PREPARE statement_handle FROM @ddl;
    EXECUTE statement_handle;
    DEALLOCATE PREPARE statement_handle;
  END IF;
END$$
DELIMITER ;
CALL add_index_if_missing('agent','idx_agent_workspace_status','`workspace_id`,`status`');
CALL add_index_if_missing('knowledge_base','idx_kb_workspace_status','`workspace_id`,`status`');
CALL add_index_if_missing('tool_definition','idx_tool_workspace_status','`workspace_id`,`status`');
CALL add_index_if_missing('workflow_definition','idx_workflow_workspace_status','`workspace_id`,`status`');
DROP PROCEDURE IF EXISTS add_index_if_missing;

INSERT IGNORE INTO release_gate_policy
  (id,workspace_id,resource_type,policy_name,min_eval_score,max_failure_rate,max_p95_latency_ms,
   require_security_pass,require_cost_budget,enabled,created_at,updated_at)
VALUES
  ('53000000-0000-0000-0000-000000000001',NULL,'prompt','Prompt生产发布门禁',0.70,0.05,15000,1,1,1,NOW(3),NOW(3)),
  ('53000000-0000-0000-0000-000000000002',NULL,'workflow','工作流生产发布门禁',0.70,0.05,30000,1,1,1,NOW(3),NOW(3));

UPDATE release_gate_policy SET policy_name='Prompt生产发布门禁'
WHERE id='53000000-0000-0000-0000-000000000001';
UPDATE release_gate_policy SET policy_name='工作流生产发布门禁'
WHERE id='53000000-0000-0000-0000-000000000002';
