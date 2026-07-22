-- 工作空间权限治理补强：允许空间所有者和管理员管理本空间角色及资源授权。

INSERT IGNORE INTO iam_workspace_role_permission(role_id, permission_id, created_by)
SELECT role.id, permission.id, role.created_by
FROM iam_workspace_role role
JOIN iam_permission permission
  ON permission.permission_code IN ('iam:governance:view', 'iam:governance:manage', 'iam:acl:manage')
 AND permission.status = 'enabled'
WHERE role.role_code IN ('owner', 'admin')
  AND role.status = 'enabled';

INSERT IGNORE INTO iam_workspace_role_permission(role_id, permission_id, created_by)
SELECT role.id, permission.id, role.created_by
FROM iam_workspace_role role
JOIN iam_permission permission
  ON permission.permission_code = 'iam:governance:view'
 AND permission.status = 'enabled'
WHERE role.role_code = 'auditor'
  AND role.status = 'enabled';
