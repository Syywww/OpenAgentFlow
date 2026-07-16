USE openagentflow;

INSERT IGNORE INTO iam_permission
  (id, permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order, visible, status)
VALUES
  ('30000000-0000-0000-0000-000000000318', 'iam:manage', '用户与权限管理', 'menu', '/settings', 'ALL', '/api/iam-admin/**', 318, 1, 'enabled');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code = 'iam:manage'
WHERE role.role_code IN ('super_admin', 'admin');
