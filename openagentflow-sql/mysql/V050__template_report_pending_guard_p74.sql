-- P74模板举报约束修正：仅限制同一用户对同一模板存在一条待处理举报，历史已处理举报可长期保留。
ALTER TABLE agent_template_report
  DROP INDEX uk_template_report_user_pending,
  ADD COLUMN pending_reporter_guard char(36)
    GENERATED ALWAYS AS (CASE WHEN status='pending' THEN reporter_user_id ELSE NULL END) STORED
    COMMENT '待处理举报用户唯一守卫，非待处理状态为空',
  ADD UNIQUE KEY uk_template_report_pending_guard (template_id, pending_reporter_guard);
