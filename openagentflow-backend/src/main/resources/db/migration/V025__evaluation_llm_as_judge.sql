USE openagentflow;

-- P26：评测增强 LLM-as-Judge，补齐裁判模型评分指标和默认配置。
INSERT INTO eval_metric(id, metric_code, metric_name, metric_type, description, config_json, enabled)
VALUES
  (UUID(), 'llm_judge_overall', 'LLM Judge 综合分', 'llm_as_judge', '裁判模型给出的综合质量分', JSON_OBJECT('scale', '0-100', 'source', 'judge_model'), 1)
ON DUPLICATE KEY UPDATE
  metric_name = VALUES(metric_name),
  metric_type = VALUES(metric_type),
  description = VALUES(description),
  config_json = VALUES(config_json),
  enabled = VALUES(enabled);

UPDATE eval_metric
SET metric_type = 'llm_as_judge',
    config_json = JSON_OBJECT('scale', '0-100', 'source', 'judge_model', 'fallback', 'rule')
WHERE metric_code IN ('accuracy', 'relevance', 'completeness', 'hallucination_control');

UPDATE eval_metric
SET metric_type = 'rule',
    config_json = JSON_OBJECT('scale', '0-100', 'source', 'platform_rule')
WHERE metric_code IN ('citation_correctness', 'tool_success');

ALTER TABLE eval_metric COMMENT='评测指标表';
ALTER TABLE eval_metric MODIFY COLUMN metric_code varchar(120) NOT NULL COMMENT '指标编码';
ALTER TABLE eval_metric MODIFY COLUMN metric_name varchar(160) NOT NULL COMMENT '指标名称';
ALTER TABLE eval_metric MODIFY COLUMN metric_type varchar(64) NOT NULL COMMENT '指标类型：rule规则、llm_as_judge裁判模型、rag引用、tool工具';
ALTER TABLE eval_metric MODIFY COLUMN description varchar(1000) NULL COMMENT '指标说明';
ALTER TABLE eval_metric MODIFY COLUMN config_json json NOT NULL COMMENT '指标配置JSON';
ALTER TABLE eval_metric MODIFY COLUMN enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用';

ALTER TABLE eval_score COMMENT='评测得分表';
ALTER TABLE eval_score MODIFY COLUMN judge_type varchar(64) NOT NULL COMMENT '裁判类型：rule规则或llm_as_judge裁判模型';
ALTER TABLE eval_score MODIFY COLUMN judge_detail json NOT NULL COMMENT '裁判详情JSON，包含理由、模型、耗时、Token和兜底信息';
ALTER TABLE eval_score MODIFY COLUMN judged_by char(36) NULL COMMENT '裁判模型ID或系统规则标识';
