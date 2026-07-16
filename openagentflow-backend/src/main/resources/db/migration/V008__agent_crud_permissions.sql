USE openagentflow;

-- P1：Agent 管理完整 CRUD、发布、复制、删除和运行所需的细粒度权限。
INSERT IGNORE INTO iam_permission (permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order)
VALUES
  ('agent:view', 'Agent 查看', 'api', '/agents', 'GET', '/api/agents/**', 21),
  ('agent:create', 'Agent 创建', 'api', '/agents', 'POST', '/api/agents', 22),
  ('agent:update', 'Agent 编辑', 'api', '/agents/:id', 'PUT', '/api/agents/*', 23),
  ('agent:publish', 'Agent 发布', 'api', '/agents/:id', 'POST', '/api/agents/*/publish', 24),
  ('agent:copy', 'Agent 复制', 'api', '/agents/:id', 'POST', '/api/agents/*/copy', 25),
  ('agent:delete', 'Agent 删除', 'api', '/agents/:id', 'DELETE', '/api/agents/*', 26),
  ('agent:run', 'Agent 调试运行', 'api', '/debug', 'POST', '/api/agents/*/run*', 27);

-- 超级管理员继续拥有全部权限。
INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000201', id
FROM iam_permission
WHERE permission_code IN (
  'agent:view',
  'agent:create',
  'agent:update',
  'agent:publish',
  'agent:copy',
  'agent:delete',
  'agent:run'
);

-- 管理员和开发者拥有常规 Agent 管理能力。
INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code IN (
  'agent:view',
  'agent:create',
  'agent:update',
  'agent:publish',
  'agent:copy',
  'agent:run'
)
WHERE role.role_code IN ('admin', 'developer');

-- 普通用户默认只能查看和运行可见 Agent。
INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code IN ('agent:view', 'agent:run')
WHERE role.role_code = 'user';

-- 为内置客服助手补充所有者 ACL，保证资源级权限和 owner 逻辑一致。
INSERT IGNORE INTO iam_resource_acl
  (id, resource_type, resource_id, subject_type, subject_id, permission_level, created_by)
VALUES
  (
    '90000000-0000-0000-0000-000000000801',
    'agent',
    '30000000-0000-0000-0000-000000000001',
    'user',
    '00000000-0000-0000-0000-000000000100',
    'owner',
    '00000000-0000-0000-0000-000000000100'
  );
