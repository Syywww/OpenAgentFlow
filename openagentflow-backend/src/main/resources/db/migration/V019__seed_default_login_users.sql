USE openagentflow;

-- 初始化开发者演示账号，默认密码为 123456，对应 BCrypt 哈希。
INSERT IGNORE INTO iam_user
  (id, department_id, username, email, password_hash, display_name, status, source_type)
VALUES
  ('00000000-0000-0000-0000-000000000101',
   '00000000-0000-0000-0000-000000000002',
   'developer',
   'developer@openagentflow.local',
   '$2a$10$QKjQXTUqgfhqg4ztvkBlpeeac.kfXgwkVE73ihJxMuuIWDTPzRRVi',
   '开发者',
   'enabled',
   'local');

-- 初始化普通用户演示账号，默认密码为 123456，对应 BCrypt 哈希。
INSERT IGNORE INTO iam_user
  (id, department_id, username, email, password_hash, display_name, status, source_type)
VALUES
  ('00000000-0000-0000-0000-000000000102',
   '00000000-0000-0000-0000-000000000002',
   'user',
   'user@openagentflow.local',
   '$2a$10$QKjQXTUqgfhqg4ztvkBlpeeac.kfXgwkVE73ihJxMuuIWDTPzRRVi',
   '普通用户',
   'enabled',
   'local');

-- 绑定开发者账号到 developer 系统角色。
INSERT IGNORE INTO iam_user_role (user_id, role_id)
SELECT '00000000-0000-0000-0000-000000000101', role.id
FROM iam_role role
WHERE role.role_code = 'developer';

-- 绑定普通用户账号到 user 系统角色。
INSERT IGNORE INTO iam_user_role (user_id, role_id)
SELECT '00000000-0000-0000-0000-000000000102', role.id
FROM iam_role role
WHERE role.role_code = 'user';
