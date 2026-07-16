USE openagentflow;

CREATE TABLE IF NOT EXISTS risk_governance_event (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '风险治理事件主键ID',
  event_code varchar(120) NOT NULL UNIQUE COMMENT '风险事件编码',
  event_type varchar(64) NOT NULL COMMENT '风险事件类型',
  source_type varchar(64) NOT NULL COMMENT '来源类型',
  source_id char(36) NOT NULL COMMENT '来源记录ID',
  risk_level varchar(32) NOT NULL DEFAULT 'medium' COMMENT '风险级别',
  status varchar(32) NOT NULL DEFAULT 'open' COMMENT '处置状态',
  title varchar(240) NOT NULL COMMENT '风险标题',
  description text COMMENT '风险描述',
  workspace_id char(36) COMMENT '所属工作空间ID',
  agent_id char(36) COMMENT '关联智能体ID',
  tool_id char(36) COMMENT '关联工具ID',
  run_id char(36) COMMENT '关联运行ID',
  rule_code varchar(120) COMMENT '关联规则编码',
  evidence_json json COMMENT '风险证据JSON',
  recommended_action varchar(1000) COMMENT '建议处置动作',
  handled_by char(36) COMMENT '处置人ID',
  handled_at datetime(3) COMMENT '处置时间',
  handle_note varchar(1000) COMMENT '处置备注',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  UNIQUE KEY uk_risk_source_event(source_type, source_id, event_type),
  KEY idx_risk_status_level(status, risk_level, created_at),
  KEY idx_risk_workspace_status(workspace_id, status, created_at),
  KEY idx_risk_run(run_id),
  KEY idx_risk_tool(tool_id)
) ENGINE=InnoDB COMMENT='风险治理事件表';

CREATE INDEX idx_audit_operation_time_success ON audit_operation_log(created_at, success);
CREATE INDEX idx_tool_invocation_risk_time ON tool_invocation_log(risk_level, success, created_at);
CREATE INDEX idx_guardrail_event_time ON runtime_guardrail_event(created_at, policy_code);
CREATE INDEX idx_tool_confirm_status_time ON tool_confirm_request(status, created_at);

INSERT IGNORE INTO iam_permission
  (id, permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order, visible, status)
VALUES
  ('30000000-0000-0000-0000-000000000341', 'governance:manage', '审计与风险治理管理', 'menu', '/governance', 'ALL', '/api/governance/**', 341, 1, 'enabled'),
  ('30000000-0000-0000-0000-000000000342', 'governance:view', '审计与风险治理查看', 'api', '/governance', 'GET', '/api/governance/**', 342, 0, 'enabled'),
  ('30000000-0000-0000-0000-000000000343', 'governance:handle', '风险事件处置', 'api', '/governance', 'POST', '/api/governance/risks/*/handle', 343, 0, 'enabled'),
  ('30000000-0000-0000-0000-000000000344', 'governance:confirm', '高风险确认审批', 'api', '/governance', 'POST', '/api/governance/confirmations/*', 344, 0, 'enabled');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code IN ('governance:manage', 'governance:view', 'governance:handle', 'governance:confirm')
WHERE role.role_code IN ('super_admin', 'admin');

