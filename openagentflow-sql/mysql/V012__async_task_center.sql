USE openagentflow;

CREATE TABLE IF NOT EXISTS async_task (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '异步任务主键ID',
  task_code varchar(120) NOT NULL UNIQUE COMMENT '异步任务编码',
  task_name varchar(200) NOT NULL COMMENT '异步任务名称',
  task_type varchar(64) NOT NULL COMMENT '异步任务类型',
  biz_type varchar(64) COMMENT '业务类型',
  biz_id char(36) COMMENT '业务对象ID',
  source_table varchar(120) COMMENT '来源业务表名',
  source_id char(36) COMMENT '来源业务记录ID',
  workspace_id char(36) COMMENT '所属工作空间ID',
  owner_user_id char(36) COMMENT '任务所属用户ID',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '任务状态',
  priority int NOT NULL DEFAULT 5 COMMENT '任务优先级',
  progress_percent decimal(5,2) NOT NULL DEFAULT 0 COMMENT '任务进度百分比',
  current_stage varchar(80) COMMENT '当前阶段编码',
  current_message varchar(1000) COMMENT '当前阶段消息',
  total_steps int NOT NULL DEFAULT 0 COMMENT '总步骤数',
  finished_steps int NOT NULL DEFAULT 0 COMMENT '已完成步骤数',
  retry_count int NOT NULL DEFAULT 0 COMMENT '已重试次数',
  max_retries int NOT NULL DEFAULT 1 COMMENT '最大重试次数',
  cancel_requested tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否请求取消',
  request_payload json COMMENT '任务请求参数JSON',
  result_payload json COMMENT '任务结果JSON',
  error_code varchar(120) COMMENT '错误编码',
  error_message text COMMENT '错误消息',
  started_at datetime(3) COMMENT '开始时间',
  finished_at datetime(3) COMMENT '完成时间',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  KEY idx_async_task_status(status, created_at),
  KEY idx_async_task_type_status(task_type, status, created_at),
  KEY idx_async_task_biz(biz_type, biz_id),
  KEY idx_async_task_workspace(workspace_id, status, created_at),
  KEY idx_async_task_owner(owner_user_id, status, created_at)
) ENGINE=InnoDB COMMENT='异步任务中心主表';

CREATE TABLE IF NOT EXISTS async_task_log (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '异步任务日志主键ID',
  task_id char(36) NOT NULL COMMENT '异步任务ID',
  log_level varchar(32) NOT NULL DEFAULT 'info' COMMENT '日志级别',
  stage varchar(80) COMMENT '阶段编码',
  message varchar(1000) NOT NULL COMMENT '日志消息',
  detail_json json COMMENT '日志详情JSON',
  progress_percent decimal(5,2) COMMENT '日志对应进度百分比',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  KEY idx_async_task_log_task(task_id, created_at),
  CONSTRAINT fk_async_task_log_task FOREIGN KEY(task_id) REFERENCES async_task(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='异步任务日志表';

INSERT IGNORE INTO iam_permission
  (id, permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order, visible, status)
VALUES
  ('30000000-0000-0000-0000-000000000321', 'async-task:manage', '异步任务中心管理', 'menu', '/tasks', 'ALL', '/api/tasks/**', 321, 1, 'enabled'),
  ('30000000-0000-0000-0000-000000000322', 'async-task:view', '异步任务查看', 'api', '/tasks', 'GET', '/api/tasks/**', 322, 0, 'enabled'),
  ('30000000-0000-0000-0000-000000000323', 'async-task:cancel', '异步任务取消', 'api', '/tasks/:id', 'POST', '/api/tasks/*/cancel', 323, 0, 'enabled'),
  ('30000000-0000-0000-0000-000000000324', 'async-task:retry', '异步任务重试', 'api', '/tasks/:id', 'POST', '/api/tasks/*/retry', 324, 0, 'enabled');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code IN ('async-task:manage', 'async-task:view', 'async-task:cancel', 'async-task:retry')
WHERE role.role_code IN ('super_admin', 'admin');

