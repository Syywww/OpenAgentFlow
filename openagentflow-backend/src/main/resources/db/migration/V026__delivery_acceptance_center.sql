USE openagentflow;

-- P28：交付验收中心，用于沉淀交付评分、检查项、风险提示和交付清单快照。
CREATE TABLE IF NOT EXISTS delivery_acceptance_report (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  report_code varchar(120) NOT NULL COMMENT '报告编码',
  report_name varchar(180) NOT NULL COMMENT '报告名称',
  overall_status varchar(32) NOT NULL COMMENT '总体状态：ready可交付、warning有警告、failed有阻断项',
  score decimal(8,2) NOT NULL DEFAULT 0 COMMENT '交付评分，范围0到100',
  passed_count int NOT NULL DEFAULT 0 COMMENT '通过项数量',
  warning_count int NOT NULL DEFAULT 0 COMMENT '警告项数量',
  failed_count int NOT NULL DEFAULT 0 COMMENT '失败项数量',
  summary_json json NOT NULL COMMENT '总览快照JSON',
  checklist_json json NOT NULL COMMENT '检查项快照JSON',
  risk_json json NOT NULL COMMENT '风险提示快照JSON',
  manifest_json json NOT NULL COMMENT '交付清单快照JSON',
  created_by char(36) DEFAULT NULL COMMENT '创建人用户ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_delivery_acceptance_report_code (report_code),
  KEY idx_delivery_acceptance_report_status (overall_status, created_at),
  KEY idx_delivery_acceptance_report_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交付验收报告表';

INSERT IGNORE INTO iam_permission
  (id, permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order, visible, status)
VALUES
  ('00000000-0000-0000-0000-000000000326', 'delivery:acceptance:view', '交付验收查看', 'menu', '/delivery', 'GET', '/delivery-acceptance/**', 326, 1, 'enabled'),
  ('00000000-0000-0000-0000-000000000327', 'delivery:acceptance:manage', '交付验收管理', 'api', '/delivery', 'POST', '/delivery-acceptance/**', 327, 1, 'enabled');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code IN ('delivery:acceptance:view', 'delivery:acceptance:manage')
WHERE role.role_code IN ('super_admin', 'admin');
