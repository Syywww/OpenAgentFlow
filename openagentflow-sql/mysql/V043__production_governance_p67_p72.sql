USE openagentflow;

-- P67-P72：AI效果门禁、安全合规、SRE、容量、高可用和全局租户隔离。

DROP PROCEDURE IF EXISTS migrate_p67_p72_columns;
DELIMITER $$
CREATE PROCEDURE migrate_p67_p72_columns()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='release_gate_policy' AND COLUMN_NAME='require_golden_baseline') THEN
    ALTER TABLE release_gate_policy ADD COLUMN require_golden_baseline tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否必须存在黄金评测基线' AFTER require_cost_budget;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='release_gate_policy' AND COLUMN_NAME='max_metric_regression') THEN
    ALTER TABLE release_gate_policy ADD COLUMN max_metric_regression decimal(8,4) NOT NULL DEFAULT 0.0500 COMMENT '单项评测指标最大允许退化值' AFTER require_golden_baseline;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='release_gate_execution' AND COLUMN_NAME='baseline_id') THEN
    ALTER TABLE release_gate_execution ADD COLUMN baseline_id char(36) DEFAULT NULL COMMENT '使用的黄金评测基线ID' AFTER target_version;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='release_gate_execution' AND COLUMN_NAME='regression_passed') THEN
    ALTER TABLE release_gate_execution ADD COLUMN regression_passed tinyint(1) DEFAULT NULL COMMENT '黄金基线回归是否通过' AFTER cost_passed;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tool_confirm_request' AND COLUMN_NAME='workspace_id') THEN
    ALTER TABLE tool_confirm_request ADD COLUMN workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID' AFTER id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tool_confirm_request' AND COLUMN_NAME='approval_token_hash') THEN
    ALTER TABLE tool_confirm_request ADD COLUMN approval_token_hash char(64) DEFAULT NULL COMMENT '一次性执行令牌SHA-256哈希' AFTER status;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tool_confirm_request' AND COLUMN_NAME='approval_token_expires_at') THEN
    ALTER TABLE tool_confirm_request ADD COLUMN approval_token_expires_at datetime(3) DEFAULT NULL COMMENT '一次性执行令牌失效时间' AFTER approval_token_hash;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tool_confirm_request' AND COLUMN_NAME='approval_token_used_at') THEN
    ALTER TABLE tool_confirm_request ADD COLUMN approval_token_used_at datetime(3) DEFAULT NULL COMMENT '一次性执行令牌使用时间' AFTER approval_token_expires_at;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ops_alert_event' AND COLUMN_NAME='dedupe_key') THEN
    ALTER TABLE ops_alert_event ADD COLUMN dedupe_key varchar(200) DEFAULT NULL COMMENT '告警收敛去重键' AFTER event_code;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ops_alert_event' AND COLUMN_NAME='escalation_level') THEN
    ALTER TABLE ops_alert_event ADD COLUMN escalation_level int NOT NULL DEFAULT 0 COMMENT '当前告警升级级别' AFTER trigger_count;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ops_alert_event' AND COLUMN_NAME='next_notify_at') THEN
    ALTER TABLE ops_alert_event ADD COLUMN next_notify_at datetime(3) DEFAULT NULL COMMENT '下一次通知补偿时间' AFTER escalation_level;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='eval_task' AND COLUMN_NAME='workspace_id') THEN
    ALTER TABLE eval_task ADD COLUMN workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID' AFTER id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='prompt_template' AND COLUMN_NAME='workspace_id') THEN
    ALTER TABLE prompt_template ADD COLUMN workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID' AFTER id;
  END IF;
END$$
DELIMITER ;
CALL migrate_p67_p72_columns();
DROP PROCEDURE IF EXISTS migrate_p67_p72_columns;

CREATE TABLE IF NOT EXISTS evaluation_baseline (
  id char(36) NOT NULL COMMENT '黄金评测基线主键ID',
  workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID',
  resource_type varchar(32) NOT NULL COMMENT '资源类型：agent、prompt、workflow、knowledge',
  resource_id char(36) NOT NULL COMMENT '资源ID',
  resource_version varchar(80) DEFAULT NULL COMMENT '资源版本',
  eval_task_id char(36) NOT NULL COMMENT '来源评测任务ID',
  baseline_name varchar(160) NOT NULL COMMENT '基线名称',
  metric_values json NOT NULL COMMENT '黄金指标值JSON',
  overall_score decimal(8,4) NOT NULL COMMENT '综合基线得分',
  sample_count int NOT NULL DEFAULT 0 COMMENT '基线样本数',
  active tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否为当前生效基线',
  created_by char(36) DEFAULT NULL COMMENT '创建用户ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_evaluation_baseline_resource (resource_type,resource_id,active,created_at),
  KEY idx_evaluation_baseline_workspace (workspace_id,active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI资源黄金评测基线表';

CREATE TABLE IF NOT EXISTS evaluation_regression (
  id char(36) NOT NULL COMMENT '评测回归主键ID',
  workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID',
  baseline_id char(36) NOT NULL COMMENT '黄金评测基线ID',
  candidate_task_id char(36) NOT NULL COMMENT '候选评测任务ID',
  resource_type varchar(32) NOT NULL COMMENT '资源类型',
  resource_id char(36) NOT NULL COMMENT '资源ID',
  target_version varchar(80) DEFAULT NULL COMMENT '候选资源版本',
  status varchar(32) NOT NULL COMMENT '比较状态：passed、blocked、error',
  baseline_metrics json NOT NULL COMMENT '基线指标JSON',
  candidate_metrics json NOT NULL COMMENT '候选指标JSON',
  regression_detail json NOT NULL COMMENT '退化指标明细JSON',
  created_by char(36) DEFAULT NULL COMMENT '发起用户ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_evaluation_regression_resource (resource_type,resource_id,created_at),
  KEY idx_evaluation_regression_status (status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI资源黄金基线回归比较表';

CREATE TABLE IF NOT EXISTS privacy_consent (
  id char(36) NOT NULL COMMENT '隐私同意主键ID',
  workspace_id char(36) NOT NULL COMMENT '工作空间ID',
  user_id char(36) NOT NULL COMMENT '数据主体用户ID',
  purpose_code varchar(80) NOT NULL COMMENT '数据处理目的编码',
  consent_version varchar(40) NOT NULL COMMENT '隐私条款版本',
  status varchar(32) NOT NULL DEFAULT 'granted' COMMENT '状态：granted、withdrawn、expired',
  granted_at datetime(3) DEFAULT NULL COMMENT '同意时间',
  withdrawn_at datetime(3) DEFAULT NULL COMMENT '撤回时间',
  expires_at datetime(3) DEFAULT NULL COMMENT '失效时间',
  evidence_json json DEFAULT NULL COMMENT '同意来源和证据JSON',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_privacy_consent_scope (workspace_id,user_id,purpose_code,consent_version),
  KEY idx_privacy_consent_status (workspace_id,status,expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PII数据处理同意表';

CREATE TABLE IF NOT EXISTS pii_data_subject_request (
  id char(36) NOT NULL COMMENT '数据主体请求主键ID',
  workspace_id char(36) NOT NULL COMMENT '工作空间ID',
  requester_user_id char(36) NOT NULL COMMENT '申请用户ID',
  request_type varchar(32) NOT NULL COMMENT '请求类型：export、forget、restrict、correct',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '状态：pending、processing、success、failed、rejected',
  scope_json json NOT NULL COMMENT '处理数据范围JSON',
  result_uri varchar(1000) DEFAULT NULL COMMENT '导出结果对象地址',
  error_message varchar(2000) DEFAULT NULL COMMENT '失败原因',
  approved_by char(36) DEFAULT NULL COMMENT '审批用户ID',
  approved_at datetime(3) DEFAULT NULL COMMENT '审批时间',
  completed_at datetime(3) DEFAULT NULL COMMENT '完成时间',
  expires_at datetime(3) DEFAULT NULL COMMENT '导出制品失效时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_pii_subject_request_user (workspace_id,requester_user_id,status,created_at),
  KEY idx_pii_subject_request_status (status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PII数据主体权利请求表';

CREATE TABLE IF NOT EXISTS file_security_scan (
  id char(36) NOT NULL COMMENT '文件安全扫描主键ID',
  workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID',
  document_id char(36) DEFAULT NULL COMMENT '知识文档ID',
  object_bucket varchar(128) NOT NULL COMMENT '对象存储桶',
  object_key varchar(500) NOT NULL COMMENT '对象存储键',
  file_hash varchar(128) DEFAULT NULL COMMENT '文件哈希',
  detected_type varchar(100) DEFAULT NULL COMMENT '真实文件类型',
  scan_engine varchar(80) NOT NULL COMMENT '扫描引擎',
  scan_status varchar(32) NOT NULL COMMENT '扫描状态：pending、clean、infected、blocked、error',
  threat_name varchar(300) DEFAULT NULL COMMENT '威胁名称',
  detail_json json DEFAULT NULL COMMENT '扫描详情JSON',
  scanned_at datetime(3) DEFAULT NULL COMMENT '扫描完成时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_file_security_scan_object (object_bucket,object_key,file_hash),
  KEY idx_file_security_scan_status (workspace_id,scan_status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上传文件病毒与内容安全扫描表';

CREATE TABLE IF NOT EXISTS ops_notification_delivery (
  id char(36) NOT NULL COMMENT '告警通知投递主键ID',
  alert_event_id char(36) NOT NULL COMMENT '告警事件ID',
  channel_id char(36) DEFAULT NULL COMMENT '通知渠道ID',
  channel_type varchar(32) NOT NULL COMMENT '通知渠道类型',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '状态：pending、sent、failed、dead',
  attempt_count int NOT NULL DEFAULT 0 COMMENT '投递尝试次数',
  next_retry_at datetime(3) DEFAULT NULL COMMENT '下次重试时间',
  response_summary varchar(1000) DEFAULT NULL COMMENT '渠道响应摘要',
  error_message varchar(2000) DEFAULT NULL COMMENT '失败原因',
  sent_at datetime(3) DEFAULT NULL COMMENT '成功发送时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_ops_notify_retry (status,next_retry_at),
  KEY idx_ops_notify_event (alert_event_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警通知失败补偿投递表';

CREATE TABLE IF NOT EXISTS capacity_baseline (
  id char(36) NOT NULL COMMENT '容量基线主键ID',
  scenario_code varchar(80) NOT NULL COMMENT '压测场景编码',
  environment_code varchar(80) NOT NULL COMMENT '环境编码',
  concurrency_level int NOT NULL COMMENT '并发级别',
  request_rate decimal(14,2) DEFAULT NULL COMMENT '每秒请求数',
  p50_latency_ms int DEFAULT NULL COMMENT 'P50耗时毫秒',
  p95_latency_ms int DEFAULT NULL COMMENT 'P95耗时毫秒',
  p99_latency_ms int DEFAULT NULL COMMENT 'P99耗时毫秒',
  error_rate decimal(8,4) DEFAULT NULL COMMENT '错误率',
  saturation_json json DEFAULT NULL COMMENT '连接池、队列和资源饱和度JSON',
  dataset_scale_json json DEFAULT NULL COMMENT 'Memory、分片和Trace数据规模JSON',
  passed tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否达到容量目标',
  measured_at datetime(3) NOT NULL COMMENT '测量时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_capacity_baseline_scenario (scenario_code,environment_code,measured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产性能与容量基线表';

CREATE TABLE IF NOT EXISTS disaster_recovery_target (
  id char(36) NOT NULL COMMENT '灾备目标主键ID',
  component_code varchar(80) NOT NULL COMMENT '组件编码',
  deployment_mode varchar(80) NOT NULL COMMENT '高可用部署模式',
  target_rpo_seconds bigint NOT NULL COMMENT '目标RPO秒数',
  target_rto_seconds bigint NOT NULL COMMENT '目标RTO秒数',
  min_replicas int NOT NULL DEFAULT 2 COMMENT '最小副本数',
  backup_strategy varchar(1000) NOT NULL COMMENT '备份策略',
  failover_strategy varchar(1000) NOT NULL COMMENT '故障切换策略',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dr_target_component (component_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产组件灾备目标表';

CREATE TABLE IF NOT EXISTS tenant_isolation_audit (
  id char(36) NOT NULL COMMENT '租户隔离审计主键ID',
  workspace_id char(36) DEFAULT NULL COMMENT '审计目标工作空间ID',
  audit_scope varchar(64) NOT NULL COMMENT '审计范围：mysql、redis、milvus、minio、opensearch、kafka',
  resource_type varchar(80) NOT NULL COMMENT '资源类型',
  resource_id varchar(200) DEFAULT NULL COMMENT '资源ID',
  issue_type varchar(80) NOT NULL COMMENT '隔离问题类型',
  severity varchar(16) NOT NULL COMMENT '严重级别',
  evidence_json json NOT NULL COMMENT '隔离问题证据JSON',
  status varchar(32) NOT NULL DEFAULT 'open' COMMENT '状态：open、resolved、ignored',
  detected_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发现时间',
  resolved_at datetime(3) DEFAULT NULL COMMENT '解决时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_tenant_isolation_status (audit_scope,status,severity,detected_at),
  KEY idx_tenant_isolation_workspace (workspace_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨存储租户隔离审计问题表';

INSERT IGNORE INTO eval_metric (id,metric_code,metric_name,metric_type,description,config_json,enabled)
VALUES
 ('67000000-0000-0000-0000-000000000001','rag_recall_at_k','RAG Recall@K','retrieval','相关证据在前K个结果中的召回率',JSON_OBJECT(),1),
 ('67000000-0000-0000-0000-000000000002','rag_mrr','RAG MRR','retrieval','首个相关结果倒数排名均值',JSON_OBJECT(),1),
 ('67000000-0000-0000-0000-000000000003','rag_ndcg','RAG NDCG','retrieval','检索排序归一化折损累计增益',JSON_OBJECT(),1),
 ('67000000-0000-0000-0000-000000000004','citation_correctness','引用正确率','retrieval','答案引用与来源证据一致比例',JSON_OBJECT(),1),
 ('67000000-0000-0000-0000-000000000005','answer_faithfulness','答案忠实度','quality','答案受证据支持的比例',JSON_OBJECT(),1),
 ('67000000-0000-0000-0000-000000000006','tool_selection_accuracy','工具选择准确率','tool','正确选择工具的比例',JSON_OBJECT(),1),
 ('67000000-0000-0000-0000-000000000007','tool_parameter_accuracy','工具参数正确率','tool','工具参数满足Schema和期望的比例',JSON_OBJECT(),1),
 ('67000000-0000-0000-0000-000000000008','tool_false_call_rate','工具误调用率','tool','无需工具时错误调用工具的比例',JSON_OBJECT(),1),
 ('67000000-0000-0000-0000-000000000009','memory_extraction_accuracy','Memory提取准确率','memory','结构化事实提取正确比例',JSON_OBJECT(),1),
 ('67000000-0000-0000-0000-000000000010','memory_duplicate_rate','Memory重复率','memory','新增记忆中重复事实比例',JSON_OBJECT(),1),
 ('67000000-0000-0000-0000-000000000011','memory_conflict_rate','Memory冲突率','memory','新增记忆与活跃事实冲突比例',JSON_OBJECT(),1),
 ('67000000-0000-0000-0000-000000000012','memory_retrieval_relevance','Memory召回相关性','memory','召回记忆与当前问题相关程度',JSON_OBJECT(),1),
 ('67000000-0000-0000-0000-000000000013','workflow_node_success_rate','工作流节点成功率','workflow','工作流节点执行成功比例',JSON_OBJECT(),1),
 ('67000000-0000-0000-0000-000000000014','workflow_route_accuracy','条件路由准确率','workflow','条件节点路由与期望路径一致比例',JSON_OBJECT(),1),
 ('67000000-0000-0000-0000-000000000015','workflow_recovery_success_rate','工作流恢复成功率','workflow','失败运行恢复后成功完成比例',JSON_OBJECT(),1);

INSERT IGNORE INTO disaster_recovery_target
  (id,component_code,deployment_mode,target_rpo_seconds,target_rto_seconds,min_replicas,backup_strategy,failover_strategy,enabled)
VALUES
 ('71000000-0000-0000-0000-000000000001','mysql','主从或云数据库多可用区',300,900,2,'每日全量加持续Binlog','自动主备切换并执行数据一致性检查',1),
 ('71000000-0000-0000-0000-000000000002','redis','Sentinel或Cluster',60,300,3,'RDB与AOF远端归档','多数派选主并切换客户端拓扑',1),
 ('71000000-0000-0000-0000-000000000003','kafka','多Broker多副本',0,300,3,'Topic三副本且min.insync.replicas为2','控制器自动选主并由Outbox补偿',1),
 ('71000000-0000-0000-0000-000000000004','minio','分布式MinIO或云对象存储',300,900,4,'版本化与跨区域复制','切换备用端点并核对对象清单',1),
 ('71000000-0000-0000-0000-000000000005','milvus','Milvus集群',900,1800,2,'定期备份元数据和Segment','恢复集合后原子切换Alias',1);

UPDATE release_gate_policy SET require_golden_baseline=1,max_metric_regression=0.0500
WHERE resource_type IN ('agent','prompt','workflow','knowledge');

DROP PROCEDURE IF EXISTS oaf_add_index_v043;
DELIMITER $$
CREATE PROCEDURE oaf_add_index_v043(IN p_table varchar(64), IN p_index varchar(64), IN p_columns varchar(500))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name=p_table AND index_name=p_index
  ) THEN
    SET @ddl=CONCAT('CREATE INDEX `',p_index,'` ON `',p_table,'` (',p_columns,')');
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL oaf_add_index_v043('tool_confirm_request','idx_tool_confirm_workspace_status','`workspace_id`,`status`,`expired_at`');
CALL oaf_add_index_v043('eval_task','idx_eval_task_workspace_status','`workspace_id`,`status`,`created_at`');
CALL oaf_add_index_v043('prompt_template','idx_prompt_template_workspace_status','`workspace_id`,`status`,`updated_at`');
DROP PROCEDURE IF EXISTS oaf_add_index_v043;
