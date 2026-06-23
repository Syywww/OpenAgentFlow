USE openagentflow;

CREATE TABLE IF NOT EXISTS oaf_organization (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '组织主键ID',
  org_code varchar(120) NOT NULL UNIQUE COMMENT '组织编码',
  org_name varchar(160) NOT NULL COMMENT '组织名称',
  description varchar(1000) COMMENT '组织描述',
  owner_user_id char(36) COMMENT '组织所有者用户ID',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '组织状态',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at datetime(3) COMMENT '删除时间'
) ENGINE=InnoDB COMMENT='组织表';

CREATE TABLE IF NOT EXISTS oaf_organization_member (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '成员主键ID',
  organization_id char(36) NOT NULL COMMENT '组织ID',
  user_id char(36) NOT NULL COMMENT '用户ID',
  member_role varchar(32) NOT NULL DEFAULT 'member' COMMENT '组织成员角色',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '成员状态',
  joined_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加入时间',
  created_by char(36) COMMENT '创建人ID',
  UNIQUE KEY uk_org_member(organization_id, user_id),
  KEY idx_org_member_user(user_id, status),
  CONSTRAINT fk_org_member_org FOREIGN KEY(organization_id) REFERENCES oaf_organization(id) ON DELETE CASCADE,
  CONSTRAINT fk_org_member_user FOREIGN KEY(user_id) REFERENCES iam_user(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='组织成员表';

CREATE TABLE IF NOT EXISTS oaf_workspace (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '工作空间主键ID',
  organization_id char(36) NOT NULL COMMENT '组织ID',
  workspace_code varchar(120) NOT NULL UNIQUE COMMENT '工作空间编码',
  workspace_name varchar(160) NOT NULL COMMENT '工作空间名称',
  description varchar(1000) COMMENT '工作空间描述',
  workspace_type varchar(32) NOT NULL DEFAULT 'team' COMMENT '工作空间类型',
  owner_user_id char(36) COMMENT '工作空间所有者用户ID',
  default_flag tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否默认工作空间',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '工作空间状态',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at datetime(3) COMMENT '删除时间',
  KEY idx_workspace_org(organization_id, status),
  KEY idx_workspace_owner(owner_user_id, status),
  CONSTRAINT fk_workspace_org FOREIGN KEY(organization_id) REFERENCES oaf_organization(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='工作空间表';

CREATE TABLE IF NOT EXISTS oaf_workspace_member (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '成员主键ID',
  workspace_id char(36) NOT NULL COMMENT '工作空间ID',
  user_id char(36) NOT NULL COMMENT '用户ID',
  member_role varchar(32) NOT NULL DEFAULT 'member' COMMENT '工作空间成员角色',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '成员状态',
  joined_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加入时间',
  created_by char(36) COMMENT '创建人ID',
  UNIQUE KEY uk_workspace_member(workspace_id, user_id),
  KEY idx_workspace_member_user(user_id, status),
  CONSTRAINT fk_workspace_member_workspace FOREIGN KEY(workspace_id) REFERENCES oaf_workspace(id) ON DELETE CASCADE,
  CONSTRAINT fk_workspace_member_user FOREIGN KEY(user_id) REFERENCES iam_user(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='工作空间成员表';

CREATE TABLE IF NOT EXISTS oaf_workspace_resource (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '资源主键ID',
  workspace_id char(36) NOT NULL COMMENT '工作空间ID',
  resource_type varchar(64) NOT NULL COMMENT '资源类型',
  resource_id char(36) NOT NULL COMMENT '资源ID',
  owner_user_id char(36) COMMENT '资源所有者用户ID',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY uk_workspace_resource(resource_type, resource_id),
  KEY idx_workspace_resource_workspace(workspace_id, resource_type),
  CONSTRAINT fk_workspace_resource_workspace FOREIGN KEY(workspace_id) REFERENCES oaf_workspace(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='工作空间资源归属表';

ALTER TABLE agent
  ADD COLUMN workspace_id char(36) COMMENT '所属工作空间ID' AFTER model_id;

ALTER TABLE knowledge_base
  ADD COLUMN workspace_id char(36) COMMENT '所属工作空间ID' AFTER description;

ALTER TABLE tool_definition
  ADD COLUMN workspace_id char(36) COMMENT '所属工作空间ID' AFTER tool_type;

ALTER TABLE workflow_definition
  ADD COLUMN workspace_id char(36) COMMENT '所属工作空间ID' AFTER workflow_type;

ALTER TABLE mcp_server
  ADD COLUMN workspace_id char(36) COMMENT '所属工作空间ID' AFTER server_name;

CREATE INDEX idx_agent_workspace_status ON agent(workspace_id, status, updated_at);
CREATE INDEX idx_kb_workspace_status ON knowledge_base(workspace_id, status, updated_at);
CREATE INDEX idx_tool_workspace_status ON tool_definition(workspace_id, status, updated_at);
CREATE INDEX idx_workflow_workspace_status ON workflow_definition(workspace_id, status, updated_at);
CREATE INDEX idx_mcp_workspace_status ON mcp_server(workspace_id, status, updated_at);

INSERT IGNORE INTO oaf_organization
  (id, org_code, org_name, description, owner_user_id, status, created_by)
VALUES
  ('90000000-0000-0000-0000-000000000001', 'default-org', '默认组织', '系统初始化的默认组织，用于承载已有资源和本地演示数据。', '00000000-0000-0000-0000-000000000100', 'enabled', '00000000-0000-0000-0000-000000000100');

INSERT IGNORE INTO oaf_organization_member
  (id, organization_id, user_id, member_role, status, created_by)
VALUES
  ('90000000-0000-0000-0000-000000000011', '90000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000100', 'owner', 'enabled', '00000000-0000-0000-0000-000000000100');

INSERT IGNORE INTO oaf_workspace
  (id, organization_id, workspace_code, workspace_name, description, workspace_type, owner_user_id, default_flag, status, created_by)
VALUES
  ('90000000-0000-0000-0000-000000000101', '90000000-0000-0000-0000-000000000001', 'default-workspace', '默认工作空间', '系统初始化的默认工作空间，已有 Agent、知识库、工具、MCP 和工作流会自动归属到这里。', 'team', '00000000-0000-0000-0000-000000000100', 1, 'enabled', '00000000-0000-0000-0000-000000000100');

INSERT IGNORE INTO oaf_workspace_member
  (id, workspace_id, user_id, member_role, status, created_by)
VALUES
  ('90000000-0000-0000-0000-000000000111', '90000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000100', 'owner', 'enabled', '00000000-0000-0000-0000-000000000100');

UPDATE agent SET workspace_id = '90000000-0000-0000-0000-000000000101' WHERE workspace_id IS NULL;
UPDATE knowledge_base SET workspace_id = '90000000-0000-0000-0000-000000000101' WHERE workspace_id IS NULL;
UPDATE tool_definition SET workspace_id = '90000000-0000-0000-0000-000000000101' WHERE workspace_id IS NULL;
UPDATE workflow_definition SET workspace_id = '90000000-0000-0000-0000-000000000101' WHERE workspace_id IS NULL;
UPDATE mcp_server SET workspace_id = '90000000-0000-0000-0000-000000000101' WHERE workspace_id IS NULL;

INSERT IGNORE INTO oaf_workspace_resource (id, workspace_id, resource_type, resource_id, owner_user_id, created_by)
SELECT UUID(), workspace_id, 'agent', id, owner_user_id, created_by FROM agent WHERE workspace_id IS NOT NULL;

INSERT IGNORE INTO oaf_workspace_resource (id, workspace_id, resource_type, resource_id, owner_user_id, created_by)
SELECT UUID(), workspace_id, 'knowledge_base', id, owner_user_id, created_by FROM knowledge_base WHERE workspace_id IS NOT NULL;

INSERT IGNORE INTO oaf_workspace_resource (id, workspace_id, resource_type, resource_id, owner_user_id, created_by)
SELECT UUID(), workspace_id, 'tool', id, owner_user_id, created_by FROM tool_definition WHERE workspace_id IS NOT NULL;

INSERT IGNORE INTO oaf_workspace_resource (id, workspace_id, resource_type, resource_id, owner_user_id, created_by)
SELECT UUID(), workspace_id, 'workflow', id, owner_user_id, created_by FROM workflow_definition WHERE workspace_id IS NOT NULL;

INSERT IGNORE INTO oaf_workspace_resource (id, workspace_id, resource_type, resource_id, owner_user_id, created_by)
SELECT UUID(), workspace_id, 'mcp_server', id, owner_user_id, created_by FROM mcp_server WHERE workspace_id IS NOT NULL;

INSERT IGNORE INTO iam_permission
  (id, permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order, visible, status)
VALUES
  ('30000000-0000-0000-0000-000000000301', 'workspace:manage', '组织空间管理', 'menu', '/workspaces', 'ALL', '/api/workspaces/**', 301, 1, 'enabled'),
  ('30000000-0000-0000-0000-000000000302', 'workspace:view', '工作空间查看', 'api', '/workspaces', 'GET', '/api/workspaces/**', 302, 0, 'enabled'),
  ('30000000-0000-0000-0000-000000000303', 'workspace:create', '工作空间创建', 'api', '/workspaces', 'POST', '/api/workspaces', 303, 0, 'enabled'),
  ('30000000-0000-0000-0000-000000000304', 'workspace:update', '工作空间编辑', 'api', '/workspaces/:id', 'PUT', '/api/workspaces/*', 304, 0, 'enabled');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code IN ('workspace:manage', 'workspace:view', 'workspace:create', 'workspace:update')
WHERE role.role_code IN ('super_admin', 'admin');
