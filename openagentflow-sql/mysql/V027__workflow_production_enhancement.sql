USE openagentflow;

-- P29：工作流生产级增强，覆盖高级节点、模板、API发布、调试、人工确认、灰度和治理配置。
ALTER TABLE workflow_definition
  ADD COLUMN input_schema json NULL COMMENT '工作流对外输入Schema' AFTER variable_schema,
  ADD COLUMN output_schema json NULL COMMENT '工作流对外输出Schema' AFTER input_schema,
  ADD COLUMN execution_policy json NULL COMMENT '工作流执行策略：预算、灰度、失败处理、沙箱、并发和发布策略' AFTER output_schema,
  ADD COLUMN api_enabled tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否允许作为API发布调用' AFTER execution_policy,
  ADD COLUMN release_strategy varchar(32) NOT NULL DEFAULT 'standard' COMMENT '发布策略：standard标准发布、gray灰度发布、manual手动发布' AFTER api_enabled;

CREATE TABLE IF NOT EXISTS workflow_template (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  template_code varchar(120) NOT NULL COMMENT '模板编码',
  template_name varchar(160) NOT NULL COMMENT '模板名称',
  template_category varchar(64) NOT NULL DEFAULT 'general' COMMENT '模板分类',
  description varchar(1000) COMMENT '模板描述',
  graph_json json NOT NULL COMMENT '模板画布JSON',
  variable_schema json NOT NULL COMMENT '变量Schema',
  default_policy json NOT NULL COMMENT '默认执行策略',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_workflow_template_code(template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流模板表';

CREATE TABLE IF NOT EXISTS workflow_api_endpoint (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  workflow_id char(36) NOT NULL COMMENT '工作流ID',
  endpoint_code varchar(120) NOT NULL COMMENT 'API端点编码',
  endpoint_name varchar(160) NOT NULL COMMENT 'API端点名称',
  auth_type varchar(32) NOT NULL DEFAULT 'jwt' COMMENT '认证方式：jwt、secret、none',
  api_secret varchar(255) COMMENT 'API密钥摘要或掩码',
  rate_limit_per_minute int NOT NULL DEFAULT 60 COMMENT '每分钟限流次数',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  last_invoked_at datetime(3) COMMENT '最近调用时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_workflow_api_endpoint_code(endpoint_code),
  KEY idx_workflow_api_endpoint_workflow(workflow_id, enabled),
  CONSTRAINT fk_workflow_api_endpoint_workflow FOREIGN KEY(workflow_id) REFERENCES workflow_definition(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流API发布端点表';

CREATE TABLE IF NOT EXISTS workflow_policy_hit_log (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  workflow_id char(36) COMMENT '工作流ID',
  workflow_run_id char(36) COMMENT '工作流运行ID',
  node_key varchar(120) COMMENT '节点Key',
  policy_type varchar(64) NOT NULL COMMENT '策略类型：budget预算、sandbox沙箱、risk风险、gray灰度、retry重试、timeout超时',
  hit_result varchar(32) NOT NULL COMMENT '命中结果：allow允许、block阻断、warn警告、fallback降级',
  policy_snapshot json NOT NULL COMMENT '策略快照',
  message varchar(1000) COMMENT '命中说明',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_workflow_policy_hit_workflow(workflow_id, created_at),
  KEY idx_workflow_policy_hit_run(workflow_run_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流策略命中日志表';

INSERT INTO workflow_template
  (id, template_code, template_name, template_category, description, graph_json, variable_schema, default_policy, enabled)
VALUES
  (
    '00000000-0000-0000-0000-000000000291',
    'customer-service-rag-tool',
    '客服 RAG + 工具查询流程',
    'customer_service',
    '适合客服问答：先检索知识库，再调用工具查询业务数据，最后由 LLM 汇总。',
    JSON_OBJECT(
      'nodes', JSON_ARRAY(
        JSON_OBJECT('id','start','type','workflowNode','label','开始','position',JSON_OBJECT('x',40,'y',140),'data',JSON_OBJECT('label','开始','nodeType','START','config',JSON_OBJECT())),
        JSON_OBJECT('id','rag','type','workflowNode','label','知识检索','position',JSON_OBJECT('x',260,'y',80),'data',JSON_OBJECT('label','知识检索','nodeType','RAG','config',JSON_OBJECT('queryTemplate','{{input}}'))),
        JSON_OBJECT('id','tool','type','workflowNode','label','工具查询','position',JSON_OBJECT('x',500,'y',180),'data',JSON_OBJECT('label','工具查询','nodeType','TOOL','config',JSON_OBJECT('toolName','','arguments',JSON_OBJECT('input','{{input}}')))),
        JSON_OBJECT('id','llm','type','workflowNode','label','答案生成','position',JSON_OBJECT('x',740,'y',130),'data',JSON_OBJECT('label','答案生成','nodeType','LLM','config',JSON_OBJECT('promptTemplate','请结合知识库来源和工具结果回答：{{input}}','temperature',0.2,'maxTokens',2048))),
        JSON_OBJECT('id','end','type','workflowNode','label','结束','position',JSON_OBJECT('x',980,'y',130),'data',JSON_OBJECT('label','结束','nodeType','END','config',JSON_OBJECT()))
      ),
      'edges', JSON_ARRAY(
        JSON_OBJECT('id','e_start_rag','source','start','target','rag'),
        JSON_OBJECT('id','e_rag_tool','source','rag','target','tool'),
        JSON_OBJECT('id','e_tool_llm','source','tool','target','llm'),
        JSON_OBJECT('id','e_llm_end','source','llm','target','end')
      )
    ),
    JSON_OBJECT('input', JSON_OBJECT('type','string','title','用户问题')),
    JSON_OBJECT('timeoutMs',60000,'retryCount',1,'budgetTokens',8000,'sandboxLevel','medium'),
    1
  ),
  (
    '00000000-0000-0000-0000-000000000292',
    'risk-approval-tool',
    '高风险工具人工确认流程',
    'governance',
    '适合高风险动作：LLM 生成参数后进入人工确认，再执行工具。',
    JSON_OBJECT(
      'nodes', JSON_ARRAY(
        JSON_OBJECT('id','start','type','workflowNode','label','开始','position',JSON_OBJECT('x',40,'y',120),'data',JSON_OBJECT('label','开始','nodeType','START','config',JSON_OBJECT())),
        JSON_OBJECT('id','llm','type','workflowNode','label','参数生成','position',JSON_OBJECT('x',280,'y',120),'data',JSON_OBJECT('label','参数生成','nodeType','LLM','config',JSON_OBJECT('promptTemplate','请将用户请求转成工具参数JSON：{{input}}','temperature',0.1,'maxTokens',1024))),
        JSON_OBJECT('id','human','type','workflowNode','label','人工确认','position',JSON_OBJECT('x',520,'y',120),'data',JSON_OBJECT('label','人工确认','nodeType','HUMAN','config',JSON_OBJECT('taskName','确认高风险工具调用','expireMinutes',60))),
        JSON_OBJECT('id','tool','type','workflowNode','label','工具执行','position',JSON_OBJECT('x',760,'y',120),'data',JSON_OBJECT('label','工具执行','nodeType','TOOL','config',JSON_OBJECT('toolName','','arguments',JSON_OBJECT('payload','{{lastOutput}}')))),
        JSON_OBJECT('id','end','type','workflowNode','label','结束','position',JSON_OBJECT('x',1000,'y',120),'data',JSON_OBJECT('label','结束','nodeType','END','config',JSON_OBJECT()))
      ),
      'edges', JSON_ARRAY(
        JSON_OBJECT('id','e_start_llm','source','start','target','llm'),
        JSON_OBJECT('id','e_llm_human','source','llm','target','human'),
        JSON_OBJECT('id','e_human_tool','source','human','target','tool'),
        JSON_OBJECT('id','e_tool_end','source','tool','target','end')
      )
    ),
    JSON_OBJECT('input', JSON_OBJECT('type','string','title','执行请求')),
    JSON_OBJECT('timeoutMs',120000,'retryCount',0,'budgetTokens',4000,'sandboxLevel','high','humanRequired',true),
    1
  )
ON DUPLICATE KEY UPDATE
  template_name = VALUES(template_name),
  template_category = VALUES(template_category),
  description = VALUES(description),
  graph_json = VALUES(graph_json),
  variable_schema = VALUES(variable_schema),
  default_policy = VALUES(default_policy),
  enabled = VALUES(enabled);

INSERT IGNORE INTO iam_permission
  (id, permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order, visible, status)
VALUES
  ('00000000-0000-0000-0000-000000000328', 'workflow:advanced:manage', '工作流高级治理', 'api', '/workflow', 'ALL', '/workflows/**', 328, 1, 'enabled'),
  ('00000000-0000-0000-0000-000000000329', 'workflow:api:invoke', '工作流API调用', 'api', '/workflow', 'POST', '/workflow-api/**', 329, 0, 'enabled');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM iam_role role
JOIN iam_permission permission ON permission.permission_code IN ('workflow:advanced:manage', 'workflow:api:invoke')
WHERE role.role_code IN ('super_admin', 'admin');
