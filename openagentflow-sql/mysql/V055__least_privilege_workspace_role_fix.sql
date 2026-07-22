-- 工作空间内置角色最小权限修正：只读成员不能查看权限治理数据。

DELETE relation
FROM iam_workspace_role_permission relation
JOIN iam_workspace_role role ON role.id = relation.role_id
JOIN iam_permission permission ON permission.id = relation.permission_id
WHERE role.role_code NOT IN ('owner', 'admin', 'auditor')
  AND permission.permission_code IN ('iam:governance:view', 'iam:governance:manage', 'iam:acl:manage');
