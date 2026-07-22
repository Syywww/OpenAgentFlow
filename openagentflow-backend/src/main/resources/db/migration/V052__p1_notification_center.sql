-- P1：通知中心与消息触达真实化。

ALTER TABLE notification
  ADD COLUMN workspace_id char(36) DEFAULT NULL COMMENT '所属工作空间ID' AFTER id,
  ADD COLUMN action_url varchar(500) DEFAULT NULL COMMENT '通知点击后的前端跳转地址' AFTER resource_id,
  ADD COLUMN dedupe_key varchar(200) DEFAULT NULL COMMENT '业务去重键，同类型通知内唯一' AFTER action_url,
  ADD COLUMN expires_at datetime(3) DEFAULT NULL COMMENT '通知失效时间，空值表示永不过期' AFTER dedupe_key,
  ADD UNIQUE KEY uk_notification_type_dedupe (notification_type, dedupe_key),
  ADD KEY idx_notification_workspace_created (workspace_id, created_at),
  ADD KEY idx_notification_expires (expires_at);

CREATE TABLE IF NOT EXISTS notification_preference (
  id char(36) NOT NULL COMMENT '通知偏好主键ID',
  user_id char(36) NOT NULL COMMENT '用户ID',
  enabled_types json NOT NULL COMMENT '允许接收的通知类型JSON数组，空数组表示全部类型',
  min_severity varchar(32) NOT NULL DEFAULT 'info' COMMENT '最低接收级别：info、warning、critical',
  station_enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否接收站内通知',
  email_enabled tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否接收邮件通知',
  webhook_enabled tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否接收Webhook通知',
  quiet_start time DEFAULT NULL COMMENT '免打扰开始时间',
  quiet_end time DEFAULT NULL COMMENT '免打扰结束时间',
  digest_mode varchar(32) NOT NULL DEFAULT 'realtime' COMMENT '发送频率：realtime、hourly、daily',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_preference_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户通知接收偏好表';

ALTER TABLE ops_notify_channel
  ADD COLUMN created_by char(36) DEFAULT NULL COMMENT '创建人ID' AFTER last_test_at,
  ADD COLUMN last_success_at datetime(3) DEFAULT NULL COMMENT '最近成功投递时间' AFTER created_by,
  ADD COLUMN failure_count int NOT NULL DEFAULT 0 COMMENT '连续失败次数' AFTER last_success_at;

ALTER TABLE ops_notification_delivery
  ADD COLUMN request_payload json DEFAULT NULL COMMENT '本次投递的脱敏请求载荷' AFTER next_retry_at;

INSERT IGNORE INTO iam_permission
  (id, permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order, visible, status)
VALUES
  ('00000000-0000-0000-0000-000000000375', 'notification:view', '个人通知查看', 'api', '/notifications', 'GET', '/notifications/**', 375, 1, 'enabled'),
  ('00000000-0000-0000-0000-000000000376', 'notification:manage', '通知发布管理', 'api', '/notifications', 'POST', '/notifications/publish', 376, 1, 'enabled'),
  ('00000000-0000-0000-0000-000000000377', 'notification:channel:manage', '通知渠道管理', 'api', '/ops', 'POST', '/ops-monitor/channels/**', 377, 1, 'enabled');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code IN ('notification:view')
WHERE role.status = 'enabled';

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code IN ('notification:manage', 'notification:channel:manage')
WHERE role.role_code IN ('super_admin', 'admin');
