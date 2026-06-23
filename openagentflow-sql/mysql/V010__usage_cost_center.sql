USE openagentflow;

-- P10：成本与用量中心权限、索引和示例配额规则。
INSERT IGNORE INTO iam_permission (permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order)
VALUES
  ('usage:view', '成本用量查看', 'api', '/usage', 'GET', '/api/usage/**', 100),
  ('usage:export', '成本明细导出', 'api', '/usage', 'GET', '/api/usage/calls/export', 101),
  ('usage:quota:manage', '成本配额管理', 'api', '/usage', '*', '/api/usage/quotas/**', 102);

-- 超级管理员拥有成本中心全部权限。
INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code IN ('usage:view', 'usage:export', 'usage:quota:manage')
WHERE role.role_code = 'super_admin';

-- 管理员默认可以查看、导出并维护配额。
INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code IN ('usage:view', 'usage:export', 'usage:quota:manage')
WHERE role.role_code = 'admin';

-- 开发者默认可以查看和导出成本明细。
INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code IN ('usage:view', 'usage:export')
WHERE role.role_code = 'developer';

-- 用量统计常用过滤索引。
CREATE INDEX idx_runtime_llm_call_created_provider ON runtime_llm_call(created_at, provider_id);
CREATE INDEX idx_runtime_llm_call_created_model ON runtime_llm_call(created_at, model_id);
CREATE INDEX idx_runtime_llm_call_run_success ON runtime_llm_call(run_id, success);
CREATE INDEX idx_runtime_run_agent_user_workflow ON runtime_run(agent_id, user_id, workflow_id);
CREATE INDEX idx_model_usage_quota_subject ON model_usage_quota(subject_type, subject_id);
CREATE INDEX idx_model_usage_quota_scope ON model_usage_quota(provider_id, model_id);

-- 说明：subject_type=GLOBAL 时 subject_id 使用全 0 UUID，表示全局规则。
INSERT IGNORE INTO model_usage_quota
  (id, subject_type, subject_id, provider_id, model_id, quota_period, token_limit, cost_limit, token_used, cost_used, reset_at)
VALUES
  (
    '91000000-0000-0000-0000-000000000010',
    'GLOBAL',
    '00000000-0000-0000-0000-000000000000',
    NULL,
    NULL,
    'daily',
    1000000,
    100.0000,
    0,
    0.0000,
    DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY)
  );
