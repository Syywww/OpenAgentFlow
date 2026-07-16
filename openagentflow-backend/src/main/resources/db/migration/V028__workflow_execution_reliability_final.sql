USE openagentflow;

-- P32：工作流执行可靠性最终增强，补齐幂等、防重复提交、失败恢复、运行心跳和步骤快照。
ALTER TABLE workflow_run
  ADD COLUMN idempotency_key varchar(128) NULL COMMENT '幂等键，用于防止前端重复点击或外部接口重复投递' AFTER error_message,
  ADD COLUMN parent_run_id char(36) NULL COMMENT '父运行ID，用于重跑或恢复运行时追溯来源' AFTER idempotency_key,
  ADD COLUMN resume_from_node_key varchar(120) NULL COMMENT '从哪个节点恢复执行' AFTER parent_run_id,
  ADD COLUMN last_node_key varchar(120) NULL COMMENT '最近完成或失败的节点Key' AFTER resume_from_node_key,
  ADD COLUMN next_node_key varchar(120) NULL COMMENT '下一步预计执行的节点Key' AFTER last_node_key,
  ADD COLUMN locked_by varchar(120) NULL COMMENT '当前运行锁持有者，便于后续多实例执行时做抢占保护' AFTER next_node_key,
  ADD COLUMN locked_at datetime(3) NULL COMMENT '当前运行锁定时间' AFTER locked_by,
  ADD COLUMN heartbeat_at datetime(3) NULL COMMENT '最近心跳时间，用于识别卡住或失联的运行' AFTER locked_at,
  ADD COLUMN retry_count int NOT NULL DEFAULT 0 COMMENT '已重跑次数' AFTER heartbeat_at,
  ADD COLUMN recoverable tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否可从失败节点恢复' AFTER retry_count,
  ADD COLUMN snapshot_json json NULL COMMENT '运行快照JSON，保存恢复所需的关键上下文' AFTER recoverable;

ALTER TABLE workflow_step_run
  ADD COLUMN next_node_key varchar(120) NULL COMMENT '下一节点Key，用于展示步骤流向和恢复定位' AFTER error_message,
  ADD COLUMN recoverable tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否可恢复，失败步骤可据此从当前节点重跑' AFTER next_node_key,
  ADD COLUMN policy_snapshot json NULL COMMENT '节点执行策略快照' AFTER recoverable;

CREATE UNIQUE INDEX uk_workflow_run_idempotency
  ON workflow_run(workflow_id, trigger_user_id, idempotency_key);

CREATE INDEX idx_workflow_run_parent
  ON workflow_run(parent_run_id, created_at);

CREATE INDEX idx_workflow_run_heartbeat
  ON workflow_run(status, heartbeat_at);

CREATE INDEX idx_workflow_run_recoverable
  ON workflow_run(recoverable, status, created_at);

CREATE INDEX idx_workflow_step_recoverable
  ON workflow_step_run(workflow_run_id, recoverable, created_at);

INSERT IGNORE INTO iam_permission
  (id, permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order, visible, status)
VALUES
  ('00000000-0000-0000-0000-000000000332', 'workflow:run:reliability', '工作流运行可靠性治理', 'api', '/workflow', 'ALL', '/workflows/runs/**', 332, 1, 'enabled');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code = 'workflow:run:reliability'
WHERE role.role_code IN ('super_admin', 'admin');
