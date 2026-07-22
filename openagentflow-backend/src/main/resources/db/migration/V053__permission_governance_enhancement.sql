-- 权限治理增强：空间角色、部门数据范围、资源ACL生命周期、授权审计和接口权限。

DROP PROCEDURE IF EXISTS oaf_add_column_v053;
DELIMITER $$
CREATE PROCEDURE oaf_add_column_v053(IN p_table varchar(128), IN p_column varchar(128), IN p_definition text)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL oaf_add_column_v053('iam_role', 'data_scope', "`data_scope` varchar(32) NOT NULL DEFAULT 'all' COMMENT '数据范围：all全部、dept本部门、dept_tree本部门及下级、self本人、custom自定义部门' AFTER `status`");
CALL oaf_add_column_v053('iam_resource_acl', 'workspace_id', "`workspace_id` char(36) DEFAULT NULL COMMENT '授权所属工作空间ID' AFTER `id`");
CALL oaf_add_column_v053('iam_resource_acl', 'status', "`status` varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '授权状态' AFTER `permission_level`");
CALL oaf_add_column_v053('iam_resource_acl', 'expires_at', "`expires_at` datetime(3) DEFAULT NULL COMMENT '授权失效时间，空值表示长期有效' AFTER `status`");
CALL oaf_add_column_v053('iam_resource_acl', 'grant_reason', "`grant_reason` varchar(500) DEFAULT NULL COMMENT '授权原因' AFTER `expires_at`");
CALL oaf_add_column_v053('iam_resource_acl', 'granted_by', "`granted_by` char(36) DEFAULT NULL COMMENT '实际授权人ID' AFTER `grant_reason`");

DROP PROCEDURE IF EXISTS oaf_add_column_v053;

CREATE TABLE IF NOT EXISTS iam_workspace_role (
  id char(36) NOT NULL COMMENT '空间角色主键ID', workspace_id char(36) NOT NULL COMMENT '工作空间ID',
  role_code varchar(80) NOT NULL COMMENT '空间内唯一角色编码', role_name varchar(120) NOT NULL COMMENT '角色名称',
  description varchar(500) DEFAULT NULL COMMENT '角色说明', data_scope varchar(32) NOT NULL DEFAULT 'self' COMMENT '数据范围：all全部、dept本部门、dept_tree本部门及下级、self本人、custom自定义部门',
  built_in tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否内置角色', status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '角色状态',
  created_by char(36) DEFAULT NULL COMMENT '创建人ID', created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id), UNIQUE KEY uk_workspace_role_code (workspace_id, role_code), KEY idx_workspace_role_status (workspace_id, status),
  CONSTRAINT fk_workspace_role_workspace FOREIGN KEY (workspace_id) REFERENCES oaf_workspace(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作空间角色定义表';

CREATE TABLE IF NOT EXISTS iam_workspace_role_permission (
  role_id char(36) NOT NULL COMMENT '工作空间角色ID', permission_id char(36) NOT NULL COMMENT '系统权限点ID',
  created_by char(36) DEFAULT NULL COMMENT '创建人ID', created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (role_id, permission_id), KEY idx_workspace_role_permission_permission (permission_id),
  CONSTRAINT fk_workspace_role_permission_role FOREIGN KEY (role_id) REFERENCES iam_workspace_role(id) ON DELETE CASCADE,
  CONSTRAINT fk_workspace_role_permission_permission FOREIGN KEY (permission_id) REFERENCES iam_permission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作空间角色权限关系表';

CREATE TABLE IF NOT EXISTS iam_workspace_member_role (
  workspace_id char(36) NOT NULL COMMENT '工作空间ID', user_id char(36) NOT NULL COMMENT '用户ID', role_id char(36) NOT NULL COMMENT '工作空间角色ID',
  created_by char(36) DEFAULT NULL COMMENT '分配人ID', created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '分配时间',
  PRIMARY KEY (workspace_id, user_id, role_id), KEY idx_workspace_member_role_user (user_id, workspace_id), KEY idx_workspace_member_role_role (role_id),
  CONSTRAINT fk_workspace_member_role_workspace FOREIGN KEY (workspace_id) REFERENCES oaf_workspace(id) ON DELETE CASCADE,
  CONSTRAINT fk_workspace_member_role_user FOREIGN KEY (user_id) REFERENCES iam_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_workspace_member_role_role FOREIGN KEY (role_id) REFERENCES iam_workspace_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作空间成员多角色关系表';

CREATE TABLE IF NOT EXISTS iam_workspace_role_department (
  role_id char(36) NOT NULL COMMENT '工作空间角色ID', department_id char(36) NOT NULL COMMENT '允许访问的部门ID',
  created_by char(36) DEFAULT NULL COMMENT '创建人ID', created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (role_id, department_id), KEY idx_workspace_role_department_dept (department_id),
  CONSTRAINT fk_workspace_role_department_role FOREIGN KEY (role_id) REFERENCES iam_workspace_role(id) ON DELETE CASCADE,
  CONSTRAINT fk_workspace_role_department_department FOREIGN KEY (department_id) REFERENCES iam_department(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作空间角色自定义部门数据范围表';

CREATE TABLE IF NOT EXISTS iam_authorization_audit (
  id char(36) NOT NULL COMMENT '授权审计主键ID', workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID，空值表示平台级变更',
  operator_user_id char(36) DEFAULT NULL COMMENT '操作人用户ID', action_type varchar(64) NOT NULL COMMENT '动作类型：grant授权、revoke撤销、assign_role分配角色、revoke_session强制下线',
  target_type varchar(64) NOT NULL COMMENT '授权目标类型', target_id varchar(160) NOT NULL COMMENT '授权目标ID',
  subject_type varchar(32) DEFAULT NULL COMMENT '被授权主体类型', subject_id varchar(160) DEFAULT NULL COMMENT '被授权主体ID',
  before_data json DEFAULT NULL COMMENT '变更前数据JSON', after_data json DEFAULT NULL COMMENT '变更后数据JSON', reason varchar(500) DEFAULT NULL COMMENT '变更原因',
  client_ip varchar(64) DEFAULT NULL COMMENT '客户端IP', created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id), KEY idx_authorization_audit_workspace (workspace_id, created_at), KEY idx_authorization_audit_target (target_type, target_id, created_at), KEY idx_authorization_audit_subject (subject_type, subject_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限授权变更审计表';

INSERT IGNORE INTO iam_permission (id, permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order, visible, status) VALUES
('00000000-0000-0000-0000-000000000381', 'iam:governance:view', '权限治理查看', 'api', '/settings', 'GET', '/iam-admin/governance/**', 381, 0, 'enabled'),
('00000000-0000-0000-0000-000000000382', 'iam:governance:manage', '权限治理管理', 'api', '/settings', 'ALL', '/iam-admin/governance/**', 382, 0, 'enabled'),
('00000000-0000-0000-0000-000000000383', 'iam:acl:manage', '资源授权管理', 'api', '/settings', 'ALL', '/iam-admin/resource-acls/**', 383, 0, 'enabled'),
('00000000-0000-0000-0000-000000000384', 'iam:session:revoke', '用户会话强制下线', 'api', '/settings', 'POST', '/iam-admin/users/*/revoke-sessions', 384, 0, 'enabled');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM iam_role r JOIN iam_permission p WHERE r.role_code IN ('super_admin','admin') AND p.permission_code IN ('iam:governance:view','iam:governance:manage','iam:acl:manage','iam:session:revoke');

INSERT IGNORE INTO iam_workspace_role (id,workspace_id,role_code,role_name,description,data_scope,built_in,status,created_by)
SELECT UUID(),w.id,s.role_code,s.role_name,s.description,s.data_scope,1,'enabled',w.created_by FROM oaf_workspace w JOIN (
SELECT 'owner' role_code,'空间所有者' role_name,'拥有当前工作空间全部业务权限' description,'all' data_scope UNION ALL
SELECT 'admin','空间管理员','管理当前工作空间成员和业务资源','all' UNION ALL
SELECT 'developer','开发者','创建、调试和运行Agent、RAG、工具与工作流','dept_tree' UNION ALL
SELECT 'auditor','审计员','查看运行、用量、风险和治理数据','all' UNION ALL
SELECT 'viewer','只读成员','只查看被授权的当前工作空间资源','self') s WHERE w.deleted_at IS NULL;

INSERT IGNORE INTO iam_workspace_role_permission(role_id,permission_id,created_by)
SELECT wr.id,p.id,wr.created_by FROM iam_workspace_role wr JOIN iam_permission p ON p.status='enabled'
WHERE wr.role_code IN ('owner','admin') AND p.permission_code REGEXP '^(agent|agent-team|debug|knowledge|tool|mcp|workflow|trace|runtime|usage|ops:monitor|notification|delivery:acceptance|model-gateway|workspace|async-task|governance|prompt|memory|evaluation|eval|template):';
INSERT IGNORE INTO iam_workspace_role_permission(role_id,permission_id,created_by)
SELECT wr.id,p.id,wr.created_by FROM iam_workspace_role wr JOIN iam_permission p ON p.status='enabled'
WHERE wr.role_code='developer' AND p.permission_code REGEXP '^(agent|agent-team|debug|knowledge|tool|mcp|workflow|trace|prompt|memory|evaluation|eval|template):';
INSERT IGNORE INTO iam_workspace_role_permission(role_id,permission_id,created_by)
SELECT wr.id,p.id,wr.created_by FROM iam_workspace_role wr JOIN iam_permission p ON p.status='enabled'
WHERE wr.role_code='auditor' AND (p.permission_code LIKE '%:view' OR p.permission_code IN ('usage:export','knowledge:retrieve','memory:recall'));
INSERT IGNORE INTO iam_workspace_role_permission(role_id,permission_id,created_by)
SELECT wr.id,p.id,wr.created_by FROM iam_workspace_role wr JOIN iam_permission p ON p.status='enabled'
WHERE wr.role_code='viewer' AND (p.permission_code LIKE '%:view' OR p.permission_code IN ('knowledge:retrieve','memory:recall'));

INSERT IGNORE INTO iam_workspace_member_role(workspace_id,user_id,role_id,created_by)
SELECT wm.workspace_id,wm.user_id,wr.id,wm.created_by FROM oaf_workspace_member wm JOIN iam_workspace_role wr ON wr.workspace_id=wm.workspace_id
AND wr.role_code=CASE WHEN wm.member_role IN ('owner','admin') THEN wm.member_role ELSE 'viewer' END WHERE wm.status IN ('active','enabled');

UPDATE iam_resource_acl acl JOIN oaf_workspace_resource r ON r.resource_type=acl.resource_type AND r.resource_id=acl.resource_id
SET acl.workspace_id=r.workspace_id WHERE acl.workspace_id IS NULL;

