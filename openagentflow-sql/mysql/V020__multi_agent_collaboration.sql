USE openagentflow;

-- P21：多 Agent 协作团队、成员编排、运行验证和 Trace 追踪权限。
ALTER TABLE agent_team COMMENT='多Agent协作团队表';
ALTER TABLE agent_team MODIFY COLUMN id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID';
ALTER TABLE agent_team MODIFY COLUMN team_code varchar(120) NOT NULL COMMENT '团队编码';
ALTER TABLE agent_team MODIFY COLUMN team_name varchar(160) NOT NULL COMMENT '团队名称';
ALTER TABLE agent_team MODIFY COLUMN description varchar(1000) COMMENT '团队描述';
ALTER TABLE agent_team MODIFY COLUMN collaboration_mode varchar(64) NOT NULL DEFAULT 'sequential' COMMENT '协作模式：sequential顺序、parallel并行、router路由、supervisor主控、reviewer复核';
ALTER TABLE agent_team MODIFY COLUMN coordinator_agent_id char(36) COMMENT '主控Agent ID，用于规划、路由和汇总';
ALTER TABLE agent_team MODIFY COLUMN status varchar(32) NOT NULL DEFAULT 'draft' COMMENT '团队状态：draft草稿、published已发布、disabled停用、deleted删除';
ALTER TABLE agent_team MODIFY COLUMN owner_user_id char(36) COMMENT '所有者用户ID';
ALTER TABLE agent_team MODIFY COLUMN created_by char(36) COMMENT '创建人ID';
ALTER TABLE agent_team MODIFY COLUMN created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间';
ALTER TABLE agent_team MODIFY COLUMN updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间';

ALTER TABLE agent_team_member COMMENT='多Agent协作团队成员表';
ALTER TABLE agent_team_member MODIFY COLUMN team_id char(36) NOT NULL COMMENT '团队ID';
ALTER TABLE agent_team_member MODIFY COLUMN agent_id char(36) NOT NULL COMMENT '成员Agent ID';
ALTER TABLE agent_team_member MODIFY COLUMN member_role varchar(80) NOT NULL COMMENT '成员职责，例如coordinator、worker、reviewer';
ALTER TABLE agent_team_member MODIFY COLUMN handoff_policy json NOT NULL COMMENT '成员交接策略JSON，描述输入输出约束和交接规则';
ALTER TABLE agent_team_member MODIFY COLUMN sort_order int NOT NULL DEFAULT 0 COMMENT '执行排序值，数值越小越靠前';
ALTER TABLE agent_team_member MODIFY COLUMN enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用该成员';
ALTER TABLE agent_team_member MODIFY COLUMN created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间';

ALTER TABLE agent_collaboration_run COMMENT='多Agent协作运行记录表';
ALTER TABLE agent_collaboration_run MODIFY COLUMN id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID';
ALTER TABLE agent_collaboration_run MODIFY COLUMN team_id char(36) COMMENT '协作团队ID';
ALTER TABLE agent_collaboration_run MODIFY COLUMN run_id char(36) COMMENT '顶层运行ID，对应runtime_run.id';
ALTER TABLE agent_collaboration_run MODIFY COLUMN objective longtext NOT NULL COMMENT '本次协作目标';
ALTER TABLE agent_collaboration_run MODIFY COLUMN shared_context json NOT NULL COMMENT '共享上下文JSON，记录运行变量、步骤摘要和错误信息';
ALTER TABLE agent_collaboration_run MODIFY COLUMN final_result longtext COMMENT '最终协作结果';
ALTER TABLE agent_collaboration_run MODIFY COLUMN status varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '运行状态：RUNNING、SUCCESS、FAILED';
ALTER TABLE agent_collaboration_run MODIFY COLUMN started_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时间';
ALTER TABLE agent_collaboration_run MODIFY COLUMN finished_at datetime(3) COMMENT '完成时间';

INSERT IGNORE INTO iam_permission (permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order)
VALUES
  ('agent-team:manage', '多Agent协作管理', 'menu', '/agent-teams', 'ALL', '/api/agent-teams/**', 28),
  ('agent-team:view', '多Agent协作查看', 'api', '/agent-teams', 'GET', '/api/agent-teams/**', 29),
  ('agent-team:create', '多Agent协作创建', 'api', '/agent-teams', 'POST', '/api/agent-teams', 30),
  ('agent-team:update', '多Agent协作编辑', 'api', '/agent-teams/:id', 'PUT', '/api/agent-teams/*', 31),
  ('agent-team:publish', '多Agent协作发布', 'api', '/agent-teams/:id', 'POST', '/api/agent-teams/*/publish', 32),
  ('agent-team:delete', '多Agent协作删除', 'api', '/agent-teams/:id', 'DELETE', '/api/agent-teams/*', 33),
  ('agent-team:run', '多Agent协作运行', 'api', '/agent-teams/:id', 'POST', '/api/agent-teams/*/run', 34);

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM iam_role r
JOIN iam_permission p ON p.permission_code IN (
  'agent-team:manage',
  'agent-team:view',
  'agent-team:create',
  'agent-team:update',
  'agent-team:publish',
  'agent-team:delete',
  'agent-team:run'
)
WHERE r.role_code IN ('super_admin', 'admin');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM iam_role r
JOIN iam_permission p ON p.permission_code IN (
  'agent-team:manage',
  'agent-team:view',
  'agent-team:create',
  'agent-team:update',
  'agent-team:publish',
  'agent-team:run'
)
WHERE r.role_code = 'developer';

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM iam_role r
JOIN iam_permission p ON p.permission_code IN ('agent-team:view', 'agent-team:run')
WHERE r.role_code = 'user';
