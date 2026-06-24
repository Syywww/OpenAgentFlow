-- P17：平台运营监控与告警中心。
-- 目标：把健康巡检、告警规则、告警事件和通知渠道沉淀为可运营、可治理、可交付的统一后台。

CREATE TABLE IF NOT EXISTS ops_alert_rule (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  rule_code varchar(120) NOT NULL COMMENT '告警规则编码',
  rule_name varchar(160) NOT NULL COMMENT '告警规则名称',
  metric_code varchar(120) NOT NULL COMMENT '监控指标编码',
  metric_source varchar(80) NOT NULL COMMENT '指标来源模块',
  operator varchar(16) NOT NULL DEFAULT '>=' COMMENT '比较操作符：>、>=、<、<=、==',
  threshold_value decimal(18,6) NOT NULL DEFAULT 0 COMMENT '阈值',
  severity varchar(32) NOT NULL DEFAULT 'warning' COMMENT '告警级别：info、warning、critical',
  window_minutes int NOT NULL DEFAULT 60 COMMENT '统计窗口分钟数',
  cooldown_minutes int NOT NULL DEFAULT 30 COMMENT '冷却分钟数',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  notify_channels varchar(300) NOT NULL DEFAULT 'station' COMMENT '通知渠道编码，多个渠道用英文逗号分隔',
  description varchar(600) DEFAULT NULL COMMENT '规则说明',
  created_by char(36) DEFAULT NULL COMMENT '创建人用户ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ops_alert_rule_code (rule_code),
  KEY idx_ops_alert_rule_metric (metric_code, enabled),
  KEY idx_ops_alert_rule_severity (severity, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运营监控告警规则表';

CREATE TABLE IF NOT EXISTS ops_alert_event (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  event_code varchar(120) NOT NULL COMMENT '告警事件编码',
  rule_id char(36) DEFAULT NULL COMMENT '告警规则ID',
  rule_code varchar(120) DEFAULT NULL COMMENT '告警规则编码',
  alert_title varchar(200) NOT NULL COMMENT '告警标题',
  severity varchar(32) NOT NULL DEFAULT 'warning' COMMENT '告警级别：info、warning、critical',
  metric_code varchar(120) NOT NULL COMMENT '监控指标编码',
  metric_source varchar(80) NOT NULL COMMENT '指标来源模块',
  metric_value decimal(18,6) NOT NULL DEFAULT 0 COMMENT '当前指标值',
  threshold_value decimal(18,6) NOT NULL DEFAULT 0 COMMENT '阈值',
  alert_detail text COMMENT '告警详情',
  evidence_json json DEFAULT NULL COMMENT '告警证据JSON',
  status varchar(32) NOT NULL DEFAULT 'open' COMMENT '告警状态：open待处理、acknowledged已确认、resolved已解决、ignored已忽略',
  notify_status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '通知状态：pending待发送、sent已发送、failed发送失败',
  handled_by char(36) DEFAULT NULL COMMENT '处理人用户ID',
  handled_at datetime(3) DEFAULT NULL COMMENT '处理时间',
  handle_note varchar(600) DEFAULT NULL COMMENT '处理备注',
  first_triggered_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '首次触发时间',
  last_triggered_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最近触发时间',
  trigger_count int NOT NULL DEFAULT 1 COMMENT '触发次数',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ops_alert_event_code (event_code),
  KEY idx_ops_alert_event_status (status, severity, last_triggered_at),
  KEY idx_ops_alert_event_rule (rule_id, status),
  KEY idx_ops_alert_event_metric (metric_code, last_triggered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运营监控告警事件表';

CREATE TABLE IF NOT EXISTS ops_health_check (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  check_code varchar(120) NOT NULL COMMENT '巡检项编码',
  check_name varchar(160) NOT NULL COMMENT '巡检项名称',
  target_type varchar(80) NOT NULL COMMENT '目标类型：database、cache、vector、model、task、api',
  target_code varchar(120) NOT NULL COMMENT '目标编码',
  status varchar(32) NOT NULL DEFAULT 'unknown' COMMENT '巡检状态：healthy、warning、unhealthy、unknown',
  message varchar(800) DEFAULT NULL COMMENT '巡检消息',
  latency_ms int DEFAULT NULL COMMENT '最近耗时毫秒',
  last_checked_at datetime(3) DEFAULT NULL COMMENT '最近巡检时间',
  next_check_at datetime(3) DEFAULT NULL COMMENT '下一次巡检时间',
  check_interval_seconds int NOT NULL DEFAULT 300 COMMENT '巡检间隔秒数',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  metadata_json json DEFAULT NULL COMMENT '元数据JSON',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ops_health_check_code (check_code),
  KEY idx_ops_health_check_status (status, target_type),
  KEY idx_ops_health_check_next (enabled, next_check_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运营监控巡检项表';

CREATE TABLE IF NOT EXISTS ops_notify_channel (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  channel_code varchar(120) NOT NULL COMMENT '通知渠道编码',
  channel_name varchar(160) NOT NULL COMMENT '通知渠道名称',
  channel_type varchar(64) NOT NULL COMMENT '通知渠道类型：station、webhook、email、dingtalk、wechat',
  config_json json DEFAULT NULL COMMENT '渠道配置JSON',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  last_test_status varchar(32) DEFAULT NULL COMMENT '最近测试状态',
  last_test_message varchar(500) DEFAULT NULL COMMENT '最近测试消息',
  last_test_at datetime(3) DEFAULT NULL COMMENT '最近测试时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ops_notify_channel_code (channel_code),
  KEY idx_ops_notify_channel_type (channel_type, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运营监控通知渠道表';

INSERT IGNORE INTO ops_notify_channel
  (id, channel_code, channel_name, channel_type, config_json, enabled, last_test_status, last_test_message)
VALUES
  ('99000000-0000-0000-0000-000000000001', 'station', '站内通知', 'station', JSON_OBJECT('builtin', true), 1, 'success', '内置站内通知可用'),
  ('99000000-0000-0000-0000-000000000002', 'webhook-default', '默认 Webhook', 'webhook', JSON_OBJECT('url', '', 'method', 'POST'), 0, 'pending', '请配置 Webhook URL 后启用');

INSERT IGNORE INTO ops_health_check
  (id, check_code, check_name, target_type, target_code, status, message, check_interval_seconds, enabled, metadata_json)
VALUES
  ('99000000-0000-0000-0000-000000000101', 'mysql', 'MySQL 数据库', 'database', 'mysql', 'unknown', '等待首次巡检', 300, 1, JSON_OBJECT()),
  ('99000000-0000-0000-0000-000000000102', 'redis', 'Redis 缓存', 'cache', 'redis', 'unknown', '等待首次巡检', 300, 1, JSON_OBJECT()),
  ('99000000-0000-0000-0000-000000000103', 'milvus', 'Milvus 向量库', 'vector', 'milvus', 'unknown', '等待首次巡检', 300, 1, JSON_OBJECT()),
  ('99000000-0000-0000-0000-000000000104', 'model_providers', '模型供应商', 'model', 'model-provider', 'unknown', '等待首次巡检', 300, 1, JSON_OBJECT()),
  ('99000000-0000-0000-0000-000000000105', 'async_tasks', '异步任务队列', 'task', 'async-task', 'unknown', '等待首次巡检', 300, 1, JSON_OBJECT()),
  ('99000000-0000-0000-0000-000000000106', 'api_quality', 'API 质量', 'api', 'api', 'unknown', '等待首次巡检', 300, 1, JSON_OBJECT()),
  ('99000000-0000-0000-0000-000000000107', 'model_quality', '模型调用质量', 'model', 'llm-call', 'unknown', '等待首次巡检', 300, 1, JSON_OBJECT());

INSERT IGNORE INTO ops_alert_rule
  (id, rule_code, rule_name, metric_code, metric_source, operator, threshold_value, severity, window_minutes, cooldown_minutes, enabled, notify_channels, description, created_by)
VALUES
  ('99000000-0000-0000-0000-000000000201', 'api-failure-rate-high', 'API 失败率过高', 'api_failure_rate', 'audit', '>=', 10, 'warning', 60, 30, 1, 'station', '近一小时 API 失败率超过 10% 时告警', '00000000-0000-0000-0000-000000000001'),
  ('99000000-0000-0000-0000-000000000202', 'model-failure-rate-high', '模型调用失败率过高', 'model_failure_rate', 'model', '>=', 10, 'critical', 60, 30, 1, 'station', '近一小时模型调用失败率超过 10% 时告警', '00000000-0000-0000-0000-000000000001'),
  ('99000000-0000-0000-0000-000000000203', 'task-backlog-high', '异步任务积压过高', 'task_backlog_count', 'task', '>=', 50, 'warning', 60, 30, 1, 'station', '异步任务 pending/running 数量超过 50 时告警', '00000000-0000-0000-0000-000000000001'),
  ('99000000-0000-0000-0000-000000000204', 'open-risk-high', '未处理风险事件过多', 'open_risk_count', 'governance', '>=', 20, 'warning', 60, 60, 1, 'station', '打开状态风险事件超过 20 时告警', '00000000-0000-0000-0000-000000000001'),
  ('99000000-0000-0000-0000-000000000205', 'knowledge-issue-high', '知识治理问题过多', 'knowledge_issue_open_count', 'knowledge', '>=', 20, 'warning', 60, 60, 1, 'station', '打开状态知识治理问题超过 20 时告警', '00000000-0000-0000-0000-000000000001');

INSERT IGNORE INTO iam_permission
  (id, permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order, visible, status)
VALUES
  ('00000000-0000-0000-0000-000000000317', 'ops:monitor:view', '运营监控查看', 'api', '/ops', 'GET', '/ops-monitor/**', 317, 1, 'enabled'),
  ('00000000-0000-0000-0000-000000000318', 'ops:monitor:manage', '运营监控管理', 'api', '/ops', 'POST', '/ops-monitor/**', 318, 1, 'enabled');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code IN ('ops:monitor:view', 'ops:monitor:manage')
WHERE role.role_code IN ('super_admin', 'admin');
