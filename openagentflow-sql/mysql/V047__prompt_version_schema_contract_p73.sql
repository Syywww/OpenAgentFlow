USE openagentflow;

-- P73补充：Prompt版本必须同时固化内容与强类型变量契约，避免历史版本受模板当前Schema变化影响。
ALTER TABLE prompt_template_version
  ADD COLUMN variable_schema json NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '该Prompt版本固化的强类型变量Schema JSON数组' AFTER variables;

-- 已有版本使用原变量定义建立兼容契约，新发布版本由应用保存完整强类型Schema。
UPDATE prompt_template_version
SET variable_schema = variables
WHERE JSON_LENGTH(variable_schema) = 0;
