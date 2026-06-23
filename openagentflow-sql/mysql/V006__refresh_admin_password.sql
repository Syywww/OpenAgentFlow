USE openagentflow;

-- 刷新内置管理员密码。
-- 默认账号：admin
-- 默认密码：123456
-- 说明：如果数据库已经执行过 V003，INSERT IGNORE 不会覆盖旧密码，需要执行本脚本刷新 BCrypt 哈希。
UPDATE iam_user
SET password_hash = '$2a$10$QKjQXTUqgfhqg4ztvkBlpeeac.kfXgwkVE73ihJxMuuIWDTPzRRVi',
    password_changed_at = CURRENT_TIMESTAMP(3),
    updated_at = CURRENT_TIMESTAMP(3)
WHERE username = 'admin';
