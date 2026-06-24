USE openagentflow;

-- P21 修复：多 Agent 协作创建 Trace Step 时要求 token_usage 具备默认空对象，避免初始化步骤还未产生 Token 时插入失败。
ALTER TABLE runtime_trace_step
  MODIFY COLUMN token_usage json NOT NULL DEFAULT (JSON_OBJECT()) COMMENT '令牌用量JSON，记录promptTokens、completionTokens、totalTokens等信息';
