-- P19：Prompt 模板中心 + Prompt 版本治理。
-- 本脚本只增强已有 prompt_template / prompt_template_version 表的数据和权限，不破坏历史数据。

ALTER TABLE prompt_template COMMENT='Prompt提示词模板表';
ALTER TABLE prompt_template MODIFY COLUMN id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID';
ALTER TABLE prompt_template MODIFY COLUMN template_code varchar(120) NOT NULL COMMENT '模板编码';
ALTER TABLE prompt_template MODIFY COLUMN template_name varchar(160) NOT NULL COMMENT '模板名称';
ALTER TABLE prompt_template MODIFY COLUMN prompt_type varchar(64) NOT NULL COMMENT '提示词类型：system、user、rag、tool、evaluation、workflow';
ALTER TABLE prompt_template MODIFY COLUMN content longtext NOT NULL COMMENT '模板当前内容，支持 {{变量名}} 占位符';
ALTER TABLE prompt_template MODIFY COLUMN variables json NOT NULL COMMENT '变量定义JSON数组，例如 [{"name":"user_input","description":"用户输入"}]';
ALTER TABLE prompt_template MODIFY COLUMN description varchar(500) COMMENT '模板描述';
ALTER TABLE prompt_template MODIFY COLUMN status varchar(32) NOT NULL DEFAULT 'draft' COMMENT '模板状态：draft草稿、published已发布、archived已归档';
ALTER TABLE prompt_template MODIFY COLUMN owner_user_id char(36) COMMENT '所有者用户ID';
ALTER TABLE prompt_template MODIFY COLUMN created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间';
ALTER TABLE prompt_template MODIFY COLUMN updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间';
ALTER TABLE prompt_template MODIFY COLUMN version bigint NOT NULL DEFAULT 0 COMMENT '乐观版本号';

ALTER TABLE prompt_template_version COMMENT='Prompt提示词模板版本表';
ALTER TABLE prompt_template_version MODIFY COLUMN id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID';
ALTER TABLE prompt_template_version MODIFY COLUMN template_id char(36) NOT NULL COMMENT '模板ID';
ALTER TABLE prompt_template_version MODIFY COLUMN version_no varchar(40) NOT NULL COMMENT '版本号，例如 v1、v2 或自定义语义版本';
ALTER TABLE prompt_template_version MODIFY COLUMN content longtext NOT NULL COMMENT '该版本的模板内容快照';
ALTER TABLE prompt_template_version MODIFY COLUMN variables json NOT NULL COMMENT '该版本的变量定义JSON快照';
ALTER TABLE prompt_template_version MODIFY COLUMN change_note varchar(500) COMMENT '版本变更说明';
ALTER TABLE prompt_template_version MODIFY COLUMN created_by char(36) COMMENT '创建人ID';
ALTER TABLE prompt_template_version MODIFY COLUMN created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间';

INSERT IGNORE INTO iam_permission (permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order)
VALUES
  ('prompt:manage', 'Prompt模板中心管理', 'menu', '/prompts', 'ALL', '/api/prompt-templates/**', 105);

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM iam_role r
JOIN iam_permission p ON p.permission_code = 'prompt:manage'
WHERE r.role_code IN ('super_admin', 'admin', 'developer');

INSERT IGNORE INTO prompt_template_version (id, template_id, version_no, content, variables, change_note, created_by)
SELECT UUID(), id, 'v1', content, variables, '初始化默认 Prompt 模板版本', owner_user_id
FROM prompt_template
WHERE template_code IN ('rag-system-default', 'tool-system-default');

INSERT IGNORE INTO prompt_template (id, template_code, template_name, prompt_type, content, variables, description, status, owner_user_id)
VALUES
  (
    '20000000-0000-0000-0000-000000000003',
    'eval-judge-default',
    '评测打分 Judge Prompt',
    'evaluation',
    '你是严格的AI评测裁判。请根据问题、标准答案和模型回答，从准确性、相关性、完整性、幻觉风险四个维度给出0到100分，并输出JSON。',
    JSON_ARRAY(JSON_OBJECT('name','question'), JSON_OBJECT('name','expected_answer'), JSON_OBJECT('name','actual_answer')),
    '用于后续 LLM-as-Judge 评测的默认打分提示词。',
    'published',
    '00000000-0000-0000-0000-000000000100'
  ),
  (
    '20000000-0000-0000-0000-000000000004',
    'workflow-summary-default',
    '工作流总结 Prompt',
    'workflow',
    '请基于工作流上下文 {{context}} 和用户输入 {{user_input}}，生成结构清晰、可交付的最终结果。',
    JSON_ARRAY(JSON_OBJECT('name','context'), JSON_OBJECT('name','user_input')),
    '用于工作流结束节点或LLM节点的默认总结提示词。',
    'published',
    '00000000-0000-0000-0000-000000000100'
  );

INSERT IGNORE INTO prompt_template_version (id, template_id, version_no, content, variables, change_note, created_by)
SELECT UUID(), id, 'v1', content, variables, '初始化默认 Prompt 模板版本', owner_user_id
FROM prompt_template
WHERE template_code IN ('eval-judge-default', 'workflow-summary-default');
