USE openagentflow;

CREATE TABLE IF NOT EXISTS iam_department (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  parent_id char(36) COMMENT '父级ID',
  dept_code varchar(80) NOT NULL UNIQUE COMMENT 'DEPT编码',
  dept_name varchar(120) NOT NULL COMMENT 'DEPT名称',
  sort_order int NOT NULL DEFAULT 0 COMMENT '排序值',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  CONSTRAINT fk_iam_department_parent FOREIGN KEY (parent_id) REFERENCES iam_department(id)
) ENGINE=InnoDB COMMENT='权限部门表';

CREATE TABLE IF NOT EXISTS iam_user (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  department_id char(36) COMMENT '部门ID',
  username varchar(120) NOT NULL UNIQUE COMMENT '用户名',
  email varchar(160) UNIQUE COMMENT '邮箱',
  phone varchar(32) COMMENT '手机号',
  password_hash varchar(255) COMMENT '密码哈希',
  display_name varchar(120) NOT NULL COMMENT '显示名称',
  avatar_url varchar(500) COMMENT '头像URL',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  source_type varchar(32) NOT NULL DEFAULT 'local' COMMENT '来源类型',
  last_login_at datetime(3) COMMENT 'LAST登录时间',
  password_changed_at datetime(3) COMMENT '密码CHANGED时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at datetime(3) COMMENT '删除时间',
  version bigint NOT NULL DEFAULT 0 COMMENT '版本',
  CONSTRAINT fk_iam_user_dept FOREIGN KEY (department_id) REFERENCES iam_department(id)
) ENGINE=InnoDB COMMENT='权限用户表';

CREATE TABLE IF NOT EXISTS iam_role (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  role_code varchar(80) NOT NULL UNIQUE COMMENT '角色编码',
  role_name varchar(120) NOT NULL COMMENT '角色名称',
  description varchar(500) COMMENT '描述',
  built_in tinyint(1) NOT NULL DEFAULT 0 COMMENT '内置IN',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='权限角色表';

CREATE TABLE IF NOT EXISTS iam_permission (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  parent_id char(36) COMMENT '父级ID',
  permission_code varchar(160) NOT NULL UNIQUE COMMENT '权限编码',
  permission_name varchar(120) NOT NULL COMMENT '权限名称',
  permission_type varchar(32) NOT NULL COMMENT '权限类型',
  route_path varchar(300) COMMENT '路由路径',
  api_method varchar(16) COMMENT 'API方法',
  api_path varchar(300) COMMENT 'API路径',
  icon varchar(80) COMMENT '字段说明：ICON',
  sort_order int NOT NULL DEFAULT 0 COMMENT '排序值',
  visible tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可见',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  CONSTRAINT fk_iam_permission_parent FOREIGN KEY (parent_id) REFERENCES iam_permission(id)
) ENGINE=InnoDB COMMENT='权限权限表';

CREATE TABLE IF NOT EXISTS iam_user_role (
  user_id char(36) NOT NULL COMMENT '用户ID',
  role_id char(36) NOT NULL COMMENT '角色ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY(user_id, role_id),
  FOREIGN KEY(user_id) REFERENCES iam_user(id) ON DELETE CASCADE,
  FOREIGN KEY(role_id) REFERENCES iam_role(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='权限用户角色表';
CREATE TABLE IF NOT EXISTS iam_role_permission (
  role_id char(36) NOT NULL COMMENT '角色ID',
  permission_id char(36) NOT NULL COMMENT '权限ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY(role_id, permission_id),
  FOREIGN KEY(role_id) REFERENCES iam_role(id) ON DELETE CASCADE,
  FOREIGN KEY(permission_id) REFERENCES iam_permission(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='权限角色权限表';

CREATE TABLE IF NOT EXISTS iam_resource_acl (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  resource_type varchar(64) NOT NULL COMMENT '资源类型',
  resource_id char(36) NOT NULL COMMENT '资源ID',
  subject_type varchar(32) NOT NULL COMMENT 'SUBJECT类型',
  subject_id char(36) NOT NULL COMMENT '字段说明：SUBJECTID',
  permission_level varchar(32) NOT NULL COMMENT '权限级别',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY uk_iam_acl(resource_type, resource_id, subject_type, subject_id)
) ENGINE=InnoDB COMMENT='权限资源访问控制表';

CREATE TABLE IF NOT EXISTS iam_refresh_token (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  user_id char(36) NOT NULL COMMENT '用户ID',
  token_hash varchar(128) NOT NULL UNIQUE COMMENT '令牌哈希',
  user_agent varchar(500) COMMENT '用户Agent',
  client_ip varchar(64) COMMENT '客户端IP',
  revoked tinyint(1) NOT NULL DEFAULT 0 COMMENT '撤销',
  issued_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '签发时间',
  expired_at datetime(3) NOT NULL COMMENT '过期时间',
  revoked_at datetime(3) COMMENT '撤销时间',
  FOREIGN KEY(user_id) REFERENCES iam_user(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='权限刷新令牌表';

CREATE TABLE IF NOT EXISTS iam_api_client (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  client_id varchar(100) NOT NULL UNIQUE COMMENT '客户端ID',
  client_secret_hash varchar(255) NOT NULL COMMENT '客户端SECRET哈希',
  client_name varchar(160) NOT NULL COMMENT '客户端名称',
  owner_user_id char(36) COMMENT '所有者用户ID',
  scopes json NOT NULL COMMENT '字段说明：SCOPES',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  expired_at datetime(3) COMMENT '过期时间'
) ENGINE=InnoDB COMMENT='权限API客户端表';

CREATE TABLE IF NOT EXISTS iam_login_log (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  user_id char(36) COMMENT '用户ID',
  username varchar(120) COMMENT '用户名',
  login_type varchar(32) NOT NULL COMMENT '登录类型',
  success tinyint(1) NOT NULL COMMENT '成功',
  failure_reason varchar(300) COMMENT '失败REASON',
  client_ip varchar(64) COMMENT '客户端IP',
  user_agent varchar(500) COMMENT '用户Agent',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='权限登录日志表';

CREATE TABLE IF NOT EXISTS iam_user_preference (
  user_id char(36) PRIMARY KEY COMMENT '用户ID',
  locale varchar(32) NOT NULL DEFAULT 'zh-CN' COMMENT '字段说明：LOCALE',
  timezone varchar(64) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '字段说明：TIMEZONE',
  theme varchar(32) NOT NULL DEFAULT 'light' COMMENT '字段说明：THEME',
  settings json NOT NULL COMMENT '字段说明：SETTINGS',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  FOREIGN KEY(user_id) REFERENCES iam_user(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='权限用户偏好表';

CREATE TABLE IF NOT EXISTS model_provider (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  provider_code varchar(80) NOT NULL UNIQUE COMMENT '服务商编码',
  provider_name varchar(120) NOT NULL COMMENT '服务商名称',
  provider_type varchar(64) NOT NULL COMMENT '服务商类型',
  base_url varchar(500) COMMENT '库URL',
  auth_type varchar(32) NOT NULL DEFAULT 'api_key' COMMENT '认证类型',
  default_headers json NOT NULL COMMENT '默认请求头',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  health_status varchar(32) NOT NULL DEFAULT 'unknown' COMMENT '健康状态',
  sort_order int NOT NULL DEFAULT 0 COMMENT '排序值',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='模型服务商表';

CREATE TABLE IF NOT EXISTS model_api_key (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  provider_id char(36) NOT NULL COMMENT '服务商ID',
  key_name varchar(120) NOT NULL COMMENT '密钥名称',
  key_cipher text NOT NULL COMMENT '密钥CIPHER',
  key_mask varchar(80) NOT NULL COMMENT '密钥MASK',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  quota_limit bigint COMMENT '配额LIMIT',
  quota_used bigint NOT NULL DEFAULT 0 COMMENT '配额USED',
  expired_at datetime(3) COMMENT '过期时间',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  FOREIGN KEY(provider_id) REFERENCES model_provider(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='模型API密钥表';

CREATE TABLE IF NOT EXISTS model_config (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  provider_id char(36) NOT NULL COMMENT '服务商ID',
  model_code varchar(120) NOT NULL COMMENT '模型编码',
  model_name varchar(160) NOT NULL COMMENT '模型名称',
  model_type varchar(32) NOT NULL COMMENT '模型类型',
  context_window int COMMENT '上下文WINDOW',
  max_output_tokens int COMMENT 'MAX输出TOKENS',
  input_price_per_1k decimal(12,8) NOT NULL DEFAULT 0 COMMENT '输入PRICEPER1K',
  output_price_per_1k decimal(12,8) NOT NULL DEFAULT 0 COMMENT '输出PRICEPER1K',
  support_stream tinyint(1) NOT NULL DEFAULT 1 COMMENT '字段说明：SUPPORTSTREAM',
  support_function_calling tinyint(1) NOT NULL DEFAULT 0 COMMENT '字段说明：SUPPORTFUNCTIONCALLING',
  support_vision tinyint(1) NOT NULL DEFAULT 0 COMMENT '字段说明：SUPPORTVISION',
  default_params json NOT NULL COMMENT '默认参数',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  is_default tinyint(1) NOT NULL DEFAULT 0 COMMENT 'IS默认',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  UNIQUE KEY uk_model(provider_id, model_code, model_type),
  FOREIGN KEY(provider_id) REFERENCES model_provider(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='模型配置表';

CREATE TABLE IF NOT EXISTS model_connectivity_test (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  provider_id char(36) NOT NULL COMMENT '服务商ID',
  model_id char(36) COMMENT '模型ID',
  test_type varchar(32) NOT NULL COMMENT '测试类型',
  success tinyint(1) NOT NULL COMMENT '成功',
  latency_ms int COMMENT '耗时毫秒',
  request_payload json COMMENT '请求载荷',
  response_payload json COMMENT '响应载荷',
  error_message text COMMENT '错误信息',
  tested_by char(36) COMMENT 'TESTED人',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='模型CONNECTIVITY测试表';

CREATE TABLE IF NOT EXISTS model_usage_quota (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  subject_type varchar(32) NOT NULL COMMENT 'SUBJECT类型',
  subject_id char(36) NOT NULL COMMENT '字段说明：SUBJECTID',
  provider_id char(36) COMMENT '服务商ID',
  model_id char(36) COMMENT '模型ID',
  quota_period varchar(32) NOT NULL COMMENT '配额PERIOD',
  token_limit bigint COMMENT '令牌LIMIT',
  cost_limit decimal(14,4) COMMENT '成本LIMIT',
  token_used bigint NOT NULL DEFAULT 0 COMMENT '令牌USED',
  cost_used decimal(14,4) NOT NULL DEFAULT 0 COMMENT '成本USED',
  reset_at datetime(3) COMMENT 'RESET时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='模型USAGE配额表';

CREATE TABLE IF NOT EXISTS prompt_template (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  template_code varchar(120) NOT NULL UNIQUE COMMENT '模板编码',
  template_name varchar(160) NOT NULL COMMENT '模板名称',
  prompt_type varchar(64) NOT NULL COMMENT '提示词类型',
  content longtext NOT NULL COMMENT '内容',
  variables json NOT NULL COMMENT '字段说明：VARIABLES',
  description varchar(500) COMMENT '描述',
  status varchar(32) NOT NULL DEFAULT 'draft' COMMENT '状态',
  owner_user_id char(36) COMMENT '所有者用户ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  version bigint NOT NULL DEFAULT 0 COMMENT '版本'
) ENGINE=InnoDB COMMENT='提示词模板表';

CREATE TABLE IF NOT EXISTS prompt_template_version (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  template_id char(36) NOT NULL COMMENT '模板ID',
  version_no varchar(40) NOT NULL COMMENT '版本序号',
  content longtext NOT NULL COMMENT '内容',
  variables json NOT NULL COMMENT '字段说明：VARIABLES',
  change_note varchar(500) COMMENT '变更NOTE',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY uk_prompt_version(template_id, version_no),
  FOREIGN KEY(template_id) REFERENCES prompt_template(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='提示词模板版本表';

CREATE TABLE IF NOT EXISTS agent (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  agent_code varchar(120) NOT NULL UNIQUE COMMENT 'Agent编码',
  agent_name varchar(160) NOT NULL COMMENT 'Agent名称',
  avatar_url varchar(500) COMMENT '头像URL',
  category varchar(80) NOT NULL COMMENT '字段说明：CATEGORY',
  description varchar(1000) COMMENT '描述',
  agent_type varchar(64) NOT NULL COMMENT 'Agent类型',
  model_id char(36) COMMENT '模型ID',
  system_prompt_template_id char(36) COMMENT 'SYSTEM提示词模板ID',
  system_prompt longtext COMMENT 'SYSTEM提示词',
  model_params json NOT NULL COMMENT '模型参数',
  memory_strategy varchar(64) NOT NULL DEFAULT 'none' COMMENT '记忆STRATEGY',
  visibility varchar(32) NOT NULL DEFAULT 'private' COMMENT '字段说明：VISIBILITY',
  status varchar(32) NOT NULL DEFAULT 'draft' COMMENT '状态',
  published_version varchar(40) COMMENT 'PUBLISHED版本',
  owner_user_id char(36) COMMENT '所有者用户ID',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at datetime(3) COMMENT '删除时间',
  version bigint NOT NULL DEFAULT 0 COMMENT '版本'
) ENGINE=InnoDB COMMENT='Agent表';

CREATE TABLE IF NOT EXISTS agent_version (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  agent_id char(36) NOT NULL COMMENT '字段说明：AgentID',
  version_no varchar(40) NOT NULL COMMENT '版本序号',
  snapshot json NOT NULL COMMENT '字段说明：SNAPSHOT',
  publish_note varchar(500) COMMENT '字段说明：PUBLISHNOTE',
  status varchar(32) NOT NULL DEFAULT 'draft' COMMENT '状态',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY uk_agent_version(agent_id, version_no),
  FOREIGN KEY(agent_id) REFERENCES agent(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Agent版本表';
CREATE TABLE IF NOT EXISTS agent_knowledge_binding (
  agent_id char(36) NOT NULL COMMENT '字段说明：AgentID',
  knowledge_base_id char(36) NOT NULL COMMENT '知识库ID',
  retrieval_config json NOT NULL COMMENT '检索配置',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY(agent_id, knowledge_base_id)
) ENGINE=InnoDB COMMENT='Agent知识BINDING表';
CREATE TABLE IF NOT EXISTS agent_tool_binding (
  agent_id char(36) NOT NULL COMMENT '字段说明：AgentID',
  tool_id char(36) NOT NULL COMMENT '工具ID',
  tool_config json NOT NULL COMMENT '工具配置',
  require_confirm tinyint(1) NOT NULL DEFAULT 0 COMMENT 'REQUIRE确认',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY(agent_id, tool_id)
) ENGINE=InnoDB COMMENT='Agent工具BINDING表';
CREATE TABLE IF NOT EXISTS agent_workflow_binding (
  agent_id char(36) NOT NULL COMMENT '字段说明：AgentID',
  workflow_id char(36) NOT NULL COMMENT '工作流ID',
  trigger_mode varchar(32) NOT NULL DEFAULT 'manual' COMMENT '字段说明：TRIGGERMODE',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY(agent_id, workflow_id)
) ENGINE=InnoDB COMMENT='Agent工作流BINDING表';
CREATE TABLE IF NOT EXISTS agent_session (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  agent_id char(36) NOT NULL COMMENT '字段说明：AgentID',
  user_id char(36) COMMENT '用户ID',
  session_title varchar(300) COMMENT 'SESSION标题',
  status varchar(32) NOT NULL DEFAULT 'active' COMMENT '状态',
  metadata json NOT NULL COMMENT '元数据JSON',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='AgentSESSION表';
CREATE TABLE IF NOT EXISTS agent_message (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  session_id char(36) NOT NULL COMMENT '字段说明：SESSIONID',
  parent_message_id char(36) COMMENT '父级MESSAGEID',
  role varchar(32) NOT NULL COMMENT '角色',
  content longtext NOT NULL COMMENT '内容',
  content_type varchar(32) NOT NULL DEFAULT 'markdown' COMMENT '内容类型',
  tool_call_id varchar(120) COMMENT '工具CALLID',
  token_count int NOT NULL DEFAULT 0 COMMENT 'Token数量',
  metadata json NOT NULL COMMENT '元数据JSON',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  FOREIGN KEY(session_id) REFERENCES agent_session(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='AgentMESSAGE表';
CREATE TABLE IF NOT EXISTS agent_memory (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  agent_id char(36) COMMENT '字段说明：AgentID',
  user_id char(36) COMMENT '用户ID',
  session_id char(36) COMMENT '字段说明：SESSIONID',
  memory_type varchar(32) NOT NULL COMMENT '记忆类型',
  memory_key varchar(160) COMMENT '记忆密钥',
  memory_text longtext NOT NULL COMMENT '记忆文本',
  memory_value json NOT NULL COMMENT '记忆值',
  embedding_json json COMMENT '向量JSON',
  embedding_blob longblob COMMENT '向量二进制',
  external_vector_id varchar(160) COMMENT '外部向量ID',
  importance_score decimal(5,4) NOT NULL DEFAULT 0.5 COMMENT 'IMPORTANCE得分',
  expired_at datetime(3) COMMENT '过期时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='Agent记忆表';

CREATE TABLE IF NOT EXISTS knowledge_base (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  kb_code varchar(120) NOT NULL UNIQUE COMMENT 'KB编码',
  kb_name varchar(160) NOT NULL COMMENT 'KB名称',
  description varchar(1000) COMMENT '描述',
  embedding_model_id char(36) COMMENT '向量模型ID',
  rerank_model_id char(36) COMMENT 'RERANK模型ID',
  chunk_strategy varchar(64) NOT NULL DEFAULT 'parent_child' COMMENT '分片STRATEGY',
  chunk_size int NOT NULL DEFAULT 512 COMMENT '分片大小',
  chunk_overlap int NOT NULL DEFAULT 64 COMMENT '分片OVERLAP',
  visibility varchar(32) NOT NULL DEFAULT 'private' COMMENT '字段说明：VISIBILITY',
  status varchar(32) NOT NULL DEFAULT 'active' COMMENT '状态',
  owner_user_id char(36) COMMENT '所有者用户ID',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at datetime(3) COMMENT '删除时间',
  version bigint NOT NULL DEFAULT 0 COMMENT '版本'
) ENGINE=InnoDB COMMENT='知识库表';
CREATE TABLE IF NOT EXISTS knowledge_document (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  kb_id char(36) NOT NULL COMMENT '字段说明：KBID',
  parent_document_id char(36) COMMENT '父级文档ID',
  doc_name varchar(300) NOT NULL COMMENT 'DOC名称',
  doc_type varchar(40) NOT NULL COMMENT 'DOC类型',
  file_ext varchar(20) COMMENT '文件EXT',
  file_size bigint COMMENT '文件大小',
  file_hash varchar(128) COMMENT '文件哈希',
  storage_bucket varchar(120) COMMENT 'STORAGE存储桶',
  storage_key varchar(500) COMMENT 'STORAGE密钥',
  source_type varchar(64) NOT NULL DEFAULT 'upload' COMMENT '来源类型',
  source_url varchar(1000) COMMENT '来源URL',
  parse_status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '解析状态',
  parse_error text COMMENT '解析错误',
  metadata json NOT NULL COMMENT '元数据JSON',
  uploaded_by char(36) COMMENT 'UPLOADED人',
  uploaded_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'UPLOADED时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  FOREIGN KEY(kb_id) REFERENCES knowledge_base(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='知识文档表';
CREATE TABLE IF NOT EXISTS knowledge_document_parse_task (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  document_id char(36) NOT NULL COMMENT '文档ID',
  task_type varchar(64) NOT NULL COMMENT '任务类型',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '状态',
  progress decimal(5,2) NOT NULL DEFAULT 0 COMMENT '字段说明：PROGRESS',
  config json NOT NULL COMMENT '配置',
  result json COMMENT '结果',
  error_message text COMMENT '错误信息',
  started_at datetime(3) COMMENT '开始时间',
  finished_at datetime(3) COMMENT '完成时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  FOREIGN KEY(document_id) REFERENCES knowledge_document(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='知识文档解析任务表';
CREATE TABLE IF NOT EXISTS knowledge_chunk (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  kb_id char(36) NOT NULL COMMENT '字段说明：KBID',
  document_id char(36) NOT NULL COMMENT '文档ID',
  chunk_no int NOT NULL COMMENT '分片序号',
  parent_chunk_id char(36) DEFAULT NULL COMMENT '父分片ID',
  chunk_level varchar(32) NOT NULL DEFAULT 'child' COMMENT '分片层级：parent/child',
  title varchar(500) COMMENT '标题',
  section_title varchar(500) DEFAULT NULL COMMENT '章节标题',
  section_path varchar(1000) DEFAULT NULL COMMENT '章节路径',
  paragraph_no int DEFAULT NULL COMMENT '段落序号',
  content longtext NOT NULL COMMENT '内容',
  token_count int NOT NULL DEFAULT 0 COMMENT 'Token数量',
  page_no int COMMENT 'PAGE序号',
  start_offset int COMMENT '开始OFFSET',
  end_offset int COMMENT '字段说明：ENDOFFSET',
  strategy_version varchar(64) NOT NULL DEFAULT 'rag-chunk-v2' COMMENT '切片策略版本',
  content_hash char(32) DEFAULT NULL COMMENT '分片内容MD5',
  source_hash char(32) DEFAULT NULL COMMENT '来源文档MD5',
  metadata json NOT NULL COMMENT '元数据JSON',
  status varchar(32) NOT NULL DEFAULT 'active' COMMENT '状态',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY uk_doc_chunk(document_id, chunk_no),
  KEY idx_chunk_parent(parent_chunk_id),
  KEY idx_chunk_level(kb_id, chunk_level, status),
  KEY idx_chunk_hash(document_id, content_hash),
  KEY idx_chunk_section(kb_id, section_title),
  FOREIGN KEY(kb_id) REFERENCES knowledge_base(id) ON DELETE CASCADE,
  FOREIGN KEY(document_id) REFERENCES knowledge_document(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='知识分片表';
CREATE TABLE IF NOT EXISTS knowledge_embedding (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  chunk_id char(36) NOT NULL UNIQUE COMMENT '分片ID',
  kb_id char(36) NOT NULL COMMENT '字段说明：KBID',
  model_id char(36) COMMENT '模型ID',
  embedding_json json COMMENT '向量JSON',
  embedding_blob longblob COMMENT '向量二进制',
  external_vector_id varchar(160) COMMENT '外部向量ID',
  embedding_dim int NOT NULL DEFAULT 1536 COMMENT '向量DIM',
  content_hash varchar(128) NOT NULL COMMENT '内容哈希',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  FOREIGN KEY(chunk_id) REFERENCES knowledge_chunk(id) ON DELETE CASCADE,
  FOREIGN KEY(kb_id) REFERENCES knowledge_base(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='知识向量表';
CREATE TABLE IF NOT EXISTS knowledge_retrieval_log (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  kb_id char(36) COMMENT '字段说明：KBID',
  agent_id char(36) COMMENT '字段说明：AgentID',
  session_id char(36) COMMENT '字段说明：SESSIONID',
  run_id char(36) COMMENT '运行ID',
  query_text longtext NOT NULL COMMENT '查询文本',
  query_embedding_json json COMMENT '查询向量JSON',
  query_external_vector_id varchar(160) COMMENT '查询外部向量ID',
  top_k int NOT NULL COMMENT '字段说明：TopK',
  score_threshold decimal(5,4) COMMENT '得分阈值',
  rerank_enabled tinyint(1) NOT NULL DEFAULT 0 COMMENT 'RERANK是否启用',
  result_count int NOT NULL DEFAULT 0 COMMENT '结果数量',
  latency_ms int COMMENT '耗时毫秒',
  results json NOT NULL COMMENT '结果',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='知识检索日志表';
CREATE TABLE IF NOT EXISTS knowledge_source_citation (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  message_id char(36) COMMENT '字段说明：MESSAGEID',
  retrieval_log_id char(36) COMMENT '检索日志ID',
  chunk_id char(36) COMMENT '分片ID',
  document_id char(36) COMMENT '文档ID',
  quote_text text COMMENT 'QUOTE文本',
  score decimal(8,6) COMMENT '得分',
  page_no int COMMENT 'PAGE序号',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='知识来源引用表';

CREATE TABLE IF NOT EXISTS tool_definition (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  tool_code varchar(120) NOT NULL UNIQUE COMMENT '工具编码',
  tool_name varchar(160) NOT NULL COMMENT '工具名称',
  tool_type varchar(64) NOT NULL COMMENT '工具类型',
  description varchar(1000) COMMENT '描述',
  request_method varchar(16) COMMENT '请求方法',
  endpoint_url varchar(1000) COMMENT '端点URL',
  auth_type varchar(64) NOT NULL DEFAULT 'none' COMMENT '认证类型',
  auth_config json NOT NULL COMMENT '认证配置',
  headers json NOT NULL COMMENT '请求头',
  request_schema json NOT NULL COMMENT '请求Schema',
  response_schema json NOT NULL COMMENT '响应Schema',
  timeout_ms int NOT NULL DEFAULT 30000 COMMENT '超时毫秒',
  retry_count int NOT NULL DEFAULT 0 COMMENT '重试数量',
  risk_level varchar(32) NOT NULL DEFAULT 'low' COMMENT '风险级别',
  require_confirm tinyint(1) NOT NULL DEFAULT 0 COMMENT 'REQUIRE确认',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  status varchar(32) NOT NULL DEFAULT 'active' COMMENT '状态',
  source_type varchar(64) NOT NULL DEFAULT 'manual' COMMENT '来源类型',
  mcp_server_id char(36) COMMENT 'MCP服务ID',
  mcp_tool_name varchar(160) COMMENT 'MCP工具名称',
  owner_user_id char(36) COMMENT '所有者用户ID',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at datetime(3) COMMENT '删除时间',
  version bigint NOT NULL DEFAULT 0 COMMENT '版本'
) ENGINE=InnoDB COMMENT='工具定义表';
CREATE TABLE IF NOT EXISTS tool_version (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  tool_id char(36) NOT NULL COMMENT '工具ID',
  version_no varchar(40) NOT NULL COMMENT '版本序号',
  snapshot json NOT NULL COMMENT '字段说明：SNAPSHOT',
  change_note varchar(500) COMMENT '变更NOTE',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY uk_tool_version(tool_id, version_no),
  FOREIGN KEY(tool_id) REFERENCES tool_definition(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='工具版本表';
CREATE TABLE IF NOT EXISTS tool_test_case (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  tool_id char(36) NOT NULL COMMENT '工具ID',
  case_name varchar(160) NOT NULL COMMENT '用例名称',
  input_params json NOT NULL COMMENT '输入参数',
  expected_result json COMMENT 'EXPECTED结果',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  FOREIGN KEY(tool_id) REFERENCES tool_definition(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='工具测试用例表';
CREATE TABLE IF NOT EXISTS tool_invocation_log (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  tool_id char(36) COMMENT '工具ID',
  agent_id char(36) COMMENT '字段说明：AgentID',
  workflow_id char(36) COMMENT '工作流ID',
  run_id char(36) COMMENT '运行ID',
  step_id char(36) COMMENT '步骤ID',
  session_id char(36) COMMENT '字段说明：SESSIONID',
  caller_user_id char(36) COMMENT 'CALLER用户ID',
  tool_code varchar(120) COMMENT '工具编码',
  input_params json NOT NULL COMMENT '输入参数',
  output_result json COMMENT '输出结果',
  success tinyint(1) NOT NULL COMMENT '成功',
  risk_level varchar(32) COMMENT '风险级别',
  confirmed_by char(36) COMMENT 'CONFIRMED人',
  confirmed_at datetime(3) COMMENT 'CONFIRMED时间',
  latency_ms int COMMENT '耗时毫秒',
  error_message text COMMENT '错误信息',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='工具调用日志表';
CREATE TABLE IF NOT EXISTS tool_confirm_request (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  tool_id char(36) NOT NULL COMMENT '工具ID',
  requester_user_id char(36) COMMENT 'REQUESTER用户ID',
  agent_id char(36) COMMENT '字段说明：AgentID',
  run_id char(36) COMMENT '运行ID',
  request_payload json NOT NULL COMMENT '请求载荷',
  reason varchar(500) COMMENT '字段说明：REASON',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '状态',
  confirmed_by char(36) COMMENT 'CONFIRMED人',
  confirmed_at datetime(3) COMMENT 'CONFIRMED时间',
  expired_at datetime(3) COMMENT '过期时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='工具确认请求表';

CREATE TABLE IF NOT EXISTS mcp_server (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  server_code varchar(120) NOT NULL UNIQUE COMMENT '服务编码',
  server_name varchar(160) NOT NULL COMMENT '服务名称',
  description varchar(1000) COMMENT '描述',
  transport_type varchar(32) NOT NULL COMMENT 'TRANSPORT类型',
  command varchar(1000) COMMENT '字段说明：COMMAND',
  args json NOT NULL COMMENT '字段说明：ARGS',
  endpoint_url varchar(1000) COMMENT '端点URL',
  auth_type varchar(64) NOT NULL DEFAULT 'none' COMMENT '认证类型',
  auth_config json NOT NULL COMMENT '认证配置',
  env_vars json NOT NULL COMMENT '字段说明：ENVVARS',
  allowed_paths json NOT NULL COMMENT '字段说明：ALLOWEDPATHS',
  risk_policy json NOT NULL COMMENT '风险策略',
  status varchar(32) NOT NULL DEFAULT 'stopped' COMMENT '状态',
  last_heartbeat_at datetime(3) COMMENT 'LASTHEARTBEAT时间',
  owner_user_id char(36) COMMENT '所有者用户ID',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at datetime(3) COMMENT '删除时间',
  version bigint NOT NULL DEFAULT 0 COMMENT '版本'
) ENGINE=InnoDB COMMENT='MCP服务表';
CREATE TABLE IF NOT EXISTS mcp_capability (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  server_id char(36) NOT NULL COMMENT '服务ID',
  capability_type varchar(32) NOT NULL COMMENT '能力类型',
  capability_name varchar(160) NOT NULL COMMENT '能力名称',
  description text COMMENT '描述',
  schema_json json NOT NULL COMMENT '字段说明：Schema JSON',
  metadata json NOT NULL COMMENT '元数据JSON',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  risk_level varchar(32) NOT NULL DEFAULT 'low' COMMENT '风险级别',
  discovered_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'DISCOVERED时间',
  UNIQUE KEY uk_mcp_capability(server_id, capability_type, capability_name),
  FOREIGN KEY(server_id) REFERENCES mcp_server(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='MCP能力表';
CREATE TABLE IF NOT EXISTS mcp_connection_test (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  server_id char(36) COMMENT '服务ID',
  success tinyint(1) NOT NULL COMMENT '成功',
  latency_ms int COMMENT '耗时毫秒',
  tools_count int NOT NULL DEFAULT 0 COMMENT 'TOOLS数量',
  prompts_count int NOT NULL DEFAULT 0 COMMENT 'PROMPTS数量',
  resources_count int NOT NULL DEFAULT 0 COMMENT 'RESOURCES数量',
  request_payload json COMMENT '请求载荷',
  response_payload json COMMENT '响应载荷',
  error_message text COMMENT '错误信息',
  tested_by char(36) COMMENT 'TESTED人',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='MCP连接测试表';
CREATE TABLE IF NOT EXISTS mcp_discovery_task (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  server_id char(36) NOT NULL COMMENT '服务ID',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '状态',
  discovered_tools int NOT NULL DEFAULT 0 COMMENT '字段说明：DISCOVEREDTOOLS',
  discovered_prompts int NOT NULL DEFAULT 0 COMMENT '字段说明：DISCOVEREDPROMPTS',
  discovered_resources int NOT NULL DEFAULT 0 COMMENT '字段说明：DISCOVEREDRESOURCES',
  error_message text COMMENT '错误信息',
  started_at datetime(3) COMMENT '开始时间',
  finished_at datetime(3) COMMENT '完成时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='MCP发现任务表';

CREATE TABLE IF NOT EXISTS workflow_definition (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  workflow_code varchar(120) NOT NULL UNIQUE COMMENT '工作流编码',
  workflow_name varchar(160) NOT NULL COMMENT '工作流名称',
  description varchar(1000) COMMENT '描述',
  workflow_type varchar(64) NOT NULL DEFAULT 'agent_flow' COMMENT '工作流类型',
  graph_json json NOT NULL COMMENT '画布JSON',
  variable_schema json NOT NULL COMMENT '变量Schema',
  status varchar(32) NOT NULL DEFAULT 'draft' COMMENT '状态',
  published_version varchar(40) COMMENT 'PUBLISHED版本',
  visibility varchar(32) NOT NULL DEFAULT 'private' COMMENT '字段说明：VISIBILITY',
  owner_user_id char(36) COMMENT '所有者用户ID',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at datetime(3) COMMENT '删除时间',
  version bigint NOT NULL DEFAULT 0 COMMENT '版本'
) ENGINE=InnoDB COMMENT='工作流定义表';
CREATE TABLE IF NOT EXISTS workflow_version (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  workflow_id char(36) NOT NULL COMMENT '工作流ID',
  version_no varchar(40) NOT NULL COMMENT '版本序号',
  graph_json json NOT NULL COMMENT '画布JSON',
  variable_schema json NOT NULL COMMENT '变量Schema',
  publish_env varchar(40) NOT NULL DEFAULT 'dev' COMMENT '字段说明：PUBLISHENV',
  publish_note varchar(500) COMMENT '字段说明：PUBLISHNOTE',
  status varchar(32) NOT NULL DEFAULT 'draft' COMMENT '状态',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY uk_workflow_version(workflow_id, version_no),
  FOREIGN KEY(workflow_id) REFERENCES workflow_definition(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='工作流版本表';
CREATE TABLE IF NOT EXISTS workflow_node (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  workflow_id char(36) NOT NULL COMMENT '工作流ID',
  node_key varchar(120) NOT NULL COMMENT '节点密钥',
  node_name varchar(160) NOT NULL COMMENT '节点名称',
  node_type varchar(64) NOT NULL COMMENT '节点类型',
  position_x decimal(12,4) COMMENT '字段说明：POSITIONX',
  position_y decimal(12,4) COMMENT '字段说明：POSITIONY',
  config_json json NOT NULL COMMENT '配置JSON',
  input_schema json NOT NULL COMMENT '输入Schema',
  output_schema json NOT NULL COMMENT '输出Schema',
  retry_policy json NOT NULL COMMENT '重试策略',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  UNIQUE KEY uk_workflow_node(workflow_id, node_key),
  FOREIGN KEY(workflow_id) REFERENCES workflow_definition(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='工作流节点表';
CREATE TABLE IF NOT EXISTS workflow_edge (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  workflow_id char(36) NOT NULL COMMENT '工作流ID',
  edge_key varchar(120) NOT NULL COMMENT '连线密钥',
  source_node_key varchar(120) NOT NULL COMMENT '来源节点密钥',
  target_node_key varchar(120) NOT NULL COMMENT 'TARGET节点密钥',
  condition_expr text COMMENT '字段说明：CONDITIONEXPR',
  label varchar(120) COMMENT '字段说明：LABEL',
  metadata json NOT NULL COMMENT '元数据JSON',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY uk_workflow_edge(workflow_id, edge_key),
  FOREIGN KEY(workflow_id) REFERENCES workflow_definition(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='工作流连线表';
CREATE TABLE IF NOT EXISTS workflow_schedule (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  workflow_id char(36) NOT NULL COMMENT '工作流ID',
  schedule_name varchar(160) NOT NULL COMMENT '调度名称',
  cron_expr varchar(120) NOT NULL COMMENT '字段说明：CRONEXPR',
  timezone varchar(64) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '字段说明：TIMEZONE',
  input_payload json NOT NULL COMMENT '输入载荷',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  last_run_at datetime(3) COMMENT 'LAST运行时间',
  next_run_at datetime(3) COMMENT 'NEXT运行时间',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  FOREIGN KEY(workflow_id) REFERENCES workflow_definition(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='工作流调度表';
CREATE TABLE IF NOT EXISTS workflow_run (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  workflow_id char(36) COMMENT '工作流ID',
  workflow_version_id char(36) COMMENT '工作流版本ID',
  agent_id char(36) COMMENT '字段说明：AgentID',
  trigger_type varchar(64) NOT NULL COMMENT 'TRIGGER类型',
  trigger_user_id char(36) COMMENT 'TRIGGER用户ID',
  input_payload json NOT NULL COMMENT '输入载荷',
  context_json json NOT NULL COMMENT '上下文JSON',
  output_payload json COMMENT '输出载荷',
  status varchar(32) NOT NULL DEFAULT 'WAITING' COMMENT '状态',
  error_message text COMMENT '错误信息',
  started_at datetime(3) COMMENT '开始时间',
  finished_at datetime(3) COMMENT '完成时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='工作流运行表';
CREATE TABLE IF NOT EXISTS workflow_step_run (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  workflow_run_id char(36) NOT NULL COMMENT '工作流运行ID',
  workflow_id char(36) COMMENT '工作流ID',
  node_key varchar(120) NOT NULL COMMENT '节点密钥',
  node_name varchar(160) COMMENT '节点名称',
  node_type varchar(64) NOT NULL COMMENT '节点类型',
  input_payload json NOT NULL COMMENT '输入载荷',
  output_payload json COMMENT '输出载荷',
  status varchar(32) NOT NULL DEFAULT 'WAITING' COMMENT '状态',
  attempt_no int NOT NULL DEFAULT 1 COMMENT 'ATTEMPT序号',
  token_count int NOT NULL DEFAULT 0 COMMENT 'Token数量',
  cost_amount decimal(14,6) NOT NULL DEFAULT 0 COMMENT '成本AMOUNT',
  latency_ms int COMMENT '耗时毫秒',
  error_message text COMMENT '错误信息',
  started_at datetime(3) COMMENT '开始时间',
  finished_at datetime(3) COMMENT '完成时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  FOREIGN KEY(workflow_run_id) REFERENCES workflow_run(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='工作流步骤运行表';
CREATE TABLE IF NOT EXISTS workflow_human_task (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  workflow_run_id char(36) NOT NULL COMMENT '工作流运行ID',
  step_run_id char(36) COMMENT '步骤运行ID',
  task_name varchar(160) NOT NULL COMMENT '任务名称',
  assignee_user_id char(36) COMMENT 'ASSIGNEE用户ID',
  payload json NOT NULL COMMENT '载荷',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '状态',
  decision varchar(64) COMMENT '字段说明：DECISION',
  comment text COMMENT '字段说明：COMMENT',
  completed_at datetime(3) COMMENT 'COMPLETED时间',
  expired_at datetime(3) COMMENT '过期时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='工作流人工任务表';

CREATE TABLE IF NOT EXISTS runtime_run (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  run_no varchar(80) NOT NULL UNIQUE COMMENT '运行序号',
  run_type varchar(32) NOT NULL COMMENT '运行类型',
  agent_id char(36) COMMENT '字段说明：AgentID',
  workflow_id char(36) COMMENT '工作流ID',
  workflow_run_id char(36) COMMENT '工作流运行ID',
  session_id char(36) COMMENT '字段说明：SESSIONID',
  user_id char(36) COMMENT '用户ID',
  input_text longtext COMMENT '输入文本',
  input_payload json NOT NULL COMMENT '输入载荷',
  output_text longtext COMMENT '输出文本',
  output_payload json COMMENT '输出载荷',
  status varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '状态',
  total_tokens int NOT NULL DEFAULT 0 COMMENT '总Token数',
  prompt_tokens int NOT NULL DEFAULT 0 COMMENT '提示词Token数',
  completion_tokens int NOT NULL DEFAULT 0 COMMENT '完成Token数',
  total_cost decimal(14,6) NOT NULL DEFAULT 0 COMMENT '总成本',
  latency_ms int COMMENT '耗时毫秒',
  error_message text COMMENT '错误信息',
  metadata json NOT NULL COMMENT '元数据JSON',
  started_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时间',
  finished_at datetime(3) COMMENT '完成时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='运行时运行表';
CREATE TABLE IF NOT EXISTS runtime_trace_step (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  run_id char(36) NOT NULL COMMENT '运行ID',
  parent_step_id char(36) COMMENT '父级步骤ID',
  step_key varchar(120) COMMENT '步骤密钥',
  step_name varchar(160) NOT NULL COMMENT '步骤名称',
  step_type varchar(64) NOT NULL COMMENT '步骤类型',
  status varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '状态',
  input_payload json NOT NULL COMMENT '输入载荷',
  output_payload json COMMENT '输出载荷',
  prompt_text longtext COMMENT '提示词文本',
  model_id char(36) COMMENT '模型ID',
  token_usage json NOT NULL COMMENT '令牌USAGE',
  cost_amount decimal(14,6) NOT NULL DEFAULT 0 COMMENT '成本AMOUNT',
  latency_ms int COMMENT '耗时毫秒',
  error_message text COMMENT '错误信息',
  started_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时间',
  finished_at datetime(3) COMMENT '完成时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  FOREIGN KEY(run_id) REFERENCES runtime_run(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='运行时链路步骤表';
CREATE TABLE IF NOT EXISTS runtime_llm_call (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  run_id char(36) COMMENT '运行ID',
  step_id char(36) COMMENT '步骤ID',
  provider_id char(36) COMMENT '服务商ID',
  model_id char(36) COMMENT '模型ID',
  request_messages json NOT NULL COMMENT '请求MESSAGES',
  response_message json COMMENT '响应MESSAGE',
  stream tinyint(1) NOT NULL DEFAULT 0 COMMENT '字段说明：STREAM',
  prompt_tokens int NOT NULL DEFAULT 0 COMMENT '提示词Token数',
  completion_tokens int NOT NULL DEFAULT 0 COMMENT '完成Token数',
  total_tokens int NOT NULL DEFAULT 0 COMMENT '总Token数',
  cost_amount decimal(14,6) NOT NULL DEFAULT 0 COMMENT '成本AMOUNT',
  latency_ms int COMMENT '耗时毫秒',
  success tinyint(1) NOT NULL COMMENT '成功',
  error_message text COMMENT '错误信息',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='运行时大模型CALL表';
CREATE TABLE IF NOT EXISTS runtime_event_log (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  run_id char(36) COMMENT '运行ID',
  step_id char(36) COMMENT '步骤ID',
  event_level varchar(16) NOT NULL DEFAULT 'INFO' COMMENT '事件级别',
  event_type varchar(80) NOT NULL COMMENT '事件类型',
  message text NOT NULL COMMENT '字段说明：MESSAGE',
  payload json NOT NULL COMMENT '载荷',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='运行时事件日志表';
CREATE TABLE IF NOT EXISTS runtime_cost_daily (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  stat_date date NOT NULL COMMENT '字段说明：STATDATE',
  provider_id char(36) COMMENT '服务商ID',
  model_id char(36) COMMENT '模型ID',
  agent_id char(36) COMMENT '字段说明：AgentID',
  workflow_id char(36) COMMENT '工作流ID',
  run_count bigint NOT NULL DEFAULT 0 COMMENT '运行数量',
  success_count bigint NOT NULL DEFAULT 0 COMMENT '成功数量',
  failure_count bigint NOT NULL DEFAULT 0 COMMENT '失败数量',
  total_tokens bigint NOT NULL DEFAULT 0 COMMENT '总Token数',
  total_cost decimal(18,6) NOT NULL DEFAULT 0 COMMENT '总成本',
  avg_latency_ms decimal(12,2) COMMENT 'AVG耗时毫秒',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  UNIQUE KEY uk_runtime_cost(stat_date, provider_id, model_id, agent_id, workflow_id)
) ENGINE=InnoDB COMMENT='运行时成本每日表';
CREATE TABLE IF NOT EXISTS runtime_guardrail_event (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  run_id char(36) COMMENT '运行ID',
  step_id char(36) COMMENT '步骤ID',
  guardrail_type varchar(64) NOT NULL COMMENT '护栏类型',
  policy_code varchar(120) NOT NULL COMMENT '策略编码',
  action varchar(32) NOT NULL COMMENT '字段说明：ACTION',
  risk_score decimal(5,4) COMMENT '风险得分',
  input_text longtext COMMENT '输入文本',
  output_text longtext COMMENT '输出文本',
  detail json NOT NULL COMMENT '字段说明：DETAIL',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='运行时护栏事件表';

CREATE TABLE IF NOT EXISTS eval_dataset (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  dataset_code varchar(120) NOT NULL UNIQUE COMMENT '数据集编码',
  dataset_name varchar(160) NOT NULL COMMENT '数据集名称',
  description varchar(1000) COMMENT '描述',
  domain varchar(80) COMMENT '字段说明：DOMAIN',
  tags json NOT NULL COMMENT '字段说明：TAGS',
  visibility varchar(32) NOT NULL DEFAULT 'private' COMMENT '字段说明：VISIBILITY',
  status varchar(32) NOT NULL DEFAULT 'draft' COMMENT '状态',
  owner_user_id char(36) COMMENT '所有者用户ID',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at datetime(3) COMMENT '删除时间',
  version bigint NOT NULL DEFAULT 0 COMMENT '版本'
) ENGINE=InnoDB COMMENT='评测数据集表';
CREATE TABLE IF NOT EXISTS eval_sample (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  dataset_id char(36) NOT NULL COMMENT '数据集ID',
  sample_no int NOT NULL COMMENT '样本序号',
  question longtext NOT NULL COMMENT '字段说明：QUESTION',
  expected_answer longtext COMMENT '字段说明：EXPECTEDANSWER',
  reference_context longtext COMMENT 'REFERENCE上下文',
  scoring_points json NOT NULL COMMENT '字段说明：SCORINGPOINTS',
  metadata json NOT NULL COMMENT '元数据JSON',
  status varchar(32) NOT NULL DEFAULT 'active' COMMENT '状态',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  UNIQUE KEY uk_eval_sample(dataset_id, sample_no),
  FOREIGN KEY(dataset_id) REFERENCES eval_dataset(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='评测样本表';
CREATE TABLE IF NOT EXISTS eval_metric (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  metric_code varchar(120) NOT NULL UNIQUE COMMENT '距离度量编码',
  metric_name varchar(160) NOT NULL COMMENT '距离度量名称',
  metric_type varchar(64) NOT NULL COMMENT '距离度量类型',
  description varchar(1000) COMMENT '描述',
  config_json json NOT NULL COMMENT '配置JSON',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='评测距离度量表';
CREATE TABLE IF NOT EXISTS eval_task (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  task_code varchar(120) NOT NULL UNIQUE COMMENT '任务编码',
  task_name varchar(160) NOT NULL COMMENT '任务名称',
  dataset_id char(36) NOT NULL COMMENT '数据集ID',
  agent_id char(36) COMMENT '字段说明：AgentID',
  workflow_id char(36) COMMENT '工作流ID',
  baseline_model_id char(36) COMMENT 'BASELINE模型ID',
  compare_model_ids json NOT NULL COMMENT 'COMPARE模型IDS',
  prompt_template_id char(36) COMMENT '提示词模板ID',
  eval_config json NOT NULL COMMENT '评测配置',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '状态',
  total_samples int NOT NULL DEFAULT 0 COMMENT '总SAMPLES',
  finished_samples int NOT NULL DEFAULT 0 COMMENT '完成SAMPLES',
  created_by char(36) COMMENT '创建人ID',
  started_at datetime(3) COMMENT '开始时间',
  finished_at datetime(3) COMMENT '完成时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='评测任务表';
CREATE TABLE IF NOT EXISTS eval_task_run (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  task_id char(36) NOT NULL COMMENT '任务ID',
  sample_id char(36) NOT NULL COMMENT '样本ID',
  model_id char(36) COMMENT '模型ID',
  run_id char(36) COMMENT '运行ID',
  answer_text longtext COMMENT 'ANSWER文本',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '状态',
  latency_ms int COMMENT '耗时毫秒',
  token_count int NOT NULL DEFAULT 0 COMMENT 'Token数量',
  cost_amount decimal(14,6) NOT NULL DEFAULT 0 COMMENT '成本AMOUNT',
  error_message text COMMENT '错误信息',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='评测任务运行表';
CREATE TABLE IF NOT EXISTS eval_score (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  task_run_id char(36) NOT NULL COMMENT '任务运行ID',
  metric_id char(36) NOT NULL COMMENT '距离度量ID',
  score decimal(8,4) COMMENT '得分',
  passed tinyint(1) COMMENT '字段说明：PASSED',
  judge_type varchar(64) NOT NULL COMMENT 'JUDGE类型',
  judge_detail json NOT NULL COMMENT '字段说明：JUDGEDETAIL',
  judged_by char(36) COMMENT 'JUDGED人',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY uk_eval_score(task_run_id, metric_id)
) ENGINE=InnoDB COMMENT='评测得分表';
CREATE TABLE IF NOT EXISTS eval_report (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  task_id char(36) NOT NULL COMMENT '任务ID',
  report_name varchar(160) NOT NULL COMMENT '报告名称',
  summary json NOT NULL COMMENT '字段说明：SUMMARY',
  model_compare json NOT NULL COMMENT '模型COMPARE',
  artifact_bucket varchar(120) COMMENT 'ARTIFACT存储桶',
  artifact_key varchar(500) COMMENT 'ARTIFACT密钥',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='评测报告表';

CREATE TABLE IF NOT EXISTS agent_template (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  template_code varchar(120) NOT NULL UNIQUE COMMENT '模板编码',
  template_name varchar(160) NOT NULL COMMENT '模板名称',
  category varchar(80) NOT NULL COMMENT '字段说明：CATEGORY',
  description varchar(1000) COMMENT '描述',
  icon varchar(120) COMMENT '字段说明：ICON',
  tags json NOT NULL COMMENT '字段说明：TAGS',
  agent_snapshot json NOT NULL COMMENT '字段说明：AgentSNAPSHOT',
  prompt_snapshot json NOT NULL COMMENT '提示词SNAPSHOT',
  tool_snapshot json NOT NULL COMMENT '工具SNAPSHOT',
  knowledge_snapshot json NOT NULL COMMENT '知识SNAPSHOT',
  recommended tinyint(1) NOT NULL DEFAULT 0 COMMENT '字段说明：RECOMMENDED',
  install_count bigint NOT NULL DEFAULT 0 COMMENT 'INSTALL数量',
  status varchar(32) NOT NULL DEFAULT 'published' COMMENT '状态',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='Agent模板表';
CREATE TABLE IF NOT EXISTS agent_template_install (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  template_id char(36) NOT NULL COMMENT '模板ID',
  target_agent_id char(36) COMMENT '字段说明：TARGETAgentID',
  installed_by char(36) COMMENT 'INSTALLED人',
  install_config json NOT NULL COMMENT 'INSTALL配置',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='Agent模板INSTALL表';
CREATE TABLE IF NOT EXISTS notification (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  notification_type varchar(64) NOT NULL COMMENT '通知类型',
  title varchar(200) NOT NULL COMMENT '标题',
  content text NOT NULL COMMENT '内容',
  severity varchar(32) NOT NULL DEFAULT 'info' COMMENT '字段说明：SEVERITY',
  resource_type varchar(64) COMMENT '资源类型',
  resource_id char(36) COMMENT '资源ID',
  payload json NOT NULL COMMENT '载荷',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='通知表';
CREATE TABLE IF NOT EXISTS notification_recipient (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  notification_id char(36) NOT NULL COMMENT '通知ID',
  user_id char(36) NOT NULL COMMENT '用户ID',
  read_at datetime(3) COMMENT 'READ时间',
  archived_at datetime(3) COMMENT 'ARCHIVED时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY uk_notification_user(notification_id, user_id)
) ENGINE=InnoDB COMMENT='通知接收人表';
CREATE TABLE IF NOT EXISTS audit_operation_log (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  trace_id varchar(120) COMMENT '链路ID',
  user_id char(36) COMMENT '用户ID',
  username varchar(120) COMMENT '用户名',
  operation_type varchar(64) NOT NULL COMMENT '操作类型',
  resource_type varchar(64) COMMENT '资源类型',
  resource_id char(36) COMMENT '资源ID',
  resource_name varchar(200) COMMENT '资源名称',
  request_method varchar(16) COMMENT '请求方法',
  request_path varchar(500) COMMENT '请求路径',
  request_params json COMMENT '请求参数',
  response_status int COMMENT '响应状态',
  success tinyint(1) NOT NULL COMMENT '成功',
  failure_reason text COMMENT '失败REASON',
  client_ip varchar(64) COMMENT '客户端IP',
  user_agent varchar(500) COMMENT '用户Agent',
  latency_ms int COMMENT '耗时毫秒',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='审计操作日志表';
CREATE TABLE IF NOT EXISTS audit_data_change_log (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  operation_log_id char(36) COMMENT '操作日志ID',
  table_name varchar(160) NOT NULL COMMENT '表名称',
  record_id char(36) COMMENT '记录ID',
  change_type varchar(32) NOT NULL COMMENT '变更类型',
  before_data json COMMENT 'BEFORE数据',
  after_data json COMMENT 'AFTER数据',
  changed_by char(36) COMMENT 'CHANGED人',
  changed_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'CHANGED时间'
) ENGINE=InnoDB COMMENT='审计数据变更日志表';
CREATE TABLE IF NOT EXISTS file_object (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  bucket_name varchar(120) NOT NULL COMMENT '存储桶名称',
  object_key varchar(500) NOT NULL COMMENT '对象密钥',
  original_name varchar(300) COMMENT 'ORIGINAL名称',
  content_type varchar(120) COMMENT '内容类型',
  file_ext varchar(20) COMMENT '文件EXT',
  file_size bigint COMMENT '文件大小',
  file_hash varchar(128) COMMENT '文件哈希',
  owner_user_id char(36) COMMENT '所有者用户ID',
  resource_type varchar(64) COMMENT '资源类型',
  resource_id char(36) COMMENT '资源ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY uk_file_object(bucket_name, object_key)
) ENGINE=InnoDB COMMENT='文件对象表';
CREATE TABLE IF NOT EXISTS webhook_endpoint (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  endpoint_code varchar(120) NOT NULL UNIQUE COMMENT '端点编码',
  endpoint_name varchar(160) NOT NULL COMMENT '端点名称',
  secret_hash varchar(255) COMMENT 'SECRET哈希',
  target_type varchar(64) NOT NULL COMMENT 'TARGET类型',
  target_id char(36) COMMENT '字段说明：TARGETID',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  config_json json NOT NULL COMMENT '配置JSON',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='Webhook端点表';
CREATE TABLE IF NOT EXISTS webhook_event (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  endpoint_id char(36) COMMENT '端点ID',
  event_type varchar(120) NOT NULL COMMENT '事件类型',
  payload json NOT NULL COMMENT '载荷',
  signature varchar(255) COMMENT '字段说明：SIGNATURE',
  success tinyint(1) NOT NULL COMMENT '成功',
  response_status int COMMENT '响应状态',
  response_body text COMMENT '响应BODY',
  error_message text COMMENT '错误信息',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='Webhook事件表';
CREATE TABLE IF NOT EXISTS tag (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  tag_name varchar(80) NOT NULL UNIQUE COMMENT '标签名称',
  tag_color varchar(20) COMMENT '标签COLOR',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='标签表';
CREATE TABLE IF NOT EXISTS resource_tag (
  resource_type varchar(64) NOT NULL COMMENT '资源类型',
  resource_id char(36) NOT NULL COMMENT '资源ID',
  tag_id char(36) NOT NULL COMMENT '标签ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY(resource_type, resource_id, tag_id),
  FOREIGN KEY(tag_id) REFERENCES tag(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='资源标签表';

CREATE TABLE IF NOT EXISTS agent_team (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  team_code varchar(120) NOT NULL UNIQUE COMMENT '团队编码',
  team_name varchar(160) NOT NULL COMMENT '团队名称',
  description varchar(1000) COMMENT '描述',
  collaboration_mode varchar(64) NOT NULL DEFAULT 'sequential' COMMENT '协作MODE',
  coordinator_agent_id char(36) COMMENT '字段说明：COORDINATORAgentID',
  status varchar(32) NOT NULL DEFAULT 'draft' COMMENT '状态',
  owner_user_id char(36) COMMENT '所有者用户ID',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='Agent团队表';
CREATE TABLE IF NOT EXISTS agent_team_member (
  team_id char(36) NOT NULL COMMENT '团队ID',
  agent_id char(36) NOT NULL COMMENT '字段说明：AgentID',
  member_role varchar(80) NOT NULL COMMENT '成员角色',
  handoff_policy json NOT NULL COMMENT 'HANDOFF策略',
  sort_order int NOT NULL DEFAULT 0 COMMENT '排序值',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY(team_id, agent_id)
) ENGINE=InnoDB COMMENT='Agent团队成员表';
CREATE TABLE IF NOT EXISTS agent_collaboration_run (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  team_id char(36) COMMENT '团队ID',
  run_id char(36) COMMENT '运行ID',
  objective longtext NOT NULL COMMENT '字段说明：OBJECTIVE',
  shared_context json NOT NULL COMMENT 'SHARED上下文',
  final_result longtext COMMENT 'FINAL结果',
  status varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '状态',
  started_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时间',
  finished_at datetime(3) COMMENT '完成时间'
) ENGINE=InnoDB COMMENT='Agent协作运行表';
CREATE TABLE IF NOT EXISTS prompt_experiment (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  experiment_code varchar(120) NOT NULL UNIQUE COMMENT '实验编码',
  experiment_name varchar(160) NOT NULL COMMENT '实验名称',
  prompt_template_id char(36) COMMENT '提示词模板ID',
  agent_id char(36) COMMENT '字段说明：AgentID',
  dataset_id char(36) COMMENT '数据集ID',
  traffic_policy json NOT NULL COMMENT 'TRAFFIC策略',
  status varchar(32) NOT NULL DEFAULT 'draft' COMMENT '状态',
  owner_user_id char(36) COMMENT '所有者用户ID',
  started_at datetime(3) COMMENT '开始时间',
  ended_at datetime(3) COMMENT 'ENDED时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='提示词实验表';
CREATE TABLE IF NOT EXISTS prompt_experiment_variant (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  experiment_id char(36) NOT NULL COMMENT '实验ID',
  variant_code varchar(80) NOT NULL COMMENT '变体编码',
  prompt_content longtext NOT NULL COMMENT '提示词内容',
  model_params json NOT NULL COMMENT '模型参数',
  traffic_weight decimal(6,4) NOT NULL DEFAULT 0 COMMENT '字段说明：TRAFFICWEIGHT',
  metrics_snapshot json NOT NULL COMMENT '字段说明：METRICSSNAPSHOT',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY uk_prompt_variant(experiment_id, variant_code)
) ENGINE=InnoDB COMMENT='提示词实验变体表';
CREATE TABLE IF NOT EXISTS model_route_policy (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  policy_code varchar(120) NOT NULL UNIQUE COMMENT '策略编码',
  policy_name varchar(160) NOT NULL COMMENT '策略名称',
  scene_type varchar(80) NOT NULL COMMENT 'SCENE类型',
  match_rule json NOT NULL COMMENT '字段说明：MATCHRULE',
  fallback_enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT 'FALLBACK是否启用',
  status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='模型路由策略表';
CREATE TABLE IF NOT EXISTS model_route_candidate (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  policy_id char(36) NOT NULL COMMENT '策略ID',
  model_id char(36) NOT NULL COMMENT '模型ID',
  priority int NOT NULL DEFAULT 0 COMMENT '字段说明：PRIORITY',
  weight decimal(6,4) NOT NULL DEFAULT 1 COMMENT '字段说明：WEIGHT',
  max_latency_ms int COMMENT 'MAX耗时毫秒',
  max_cost_per_1k decimal(12,8) COMMENT 'MAX成本PER1K',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY uk_route_candidate(policy_id, model_id)
) ENGINE=InnoDB COMMENT='模型路由候选表';
CREATE TABLE IF NOT EXISTS guardrail_policy (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  policy_code varchar(120) NOT NULL UNIQUE COMMENT '策略编码',
  policy_name varchar(160) NOT NULL COMMENT '策略名称',
  policy_type varchar(64) NOT NULL COMMENT '策略类型',
  apply_scope varchar(64) NOT NULL DEFAULT 'global' COMMENT '字段说明：APPLYSCOPE',
  config_json json NOT NULL COMMENT '配置JSON',
  action varchar(32) NOT NULL DEFAULT 'warn' COMMENT '字段说明：ACTION',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='护栏策略表';
CREATE TABLE IF NOT EXISTS guardrail_rule (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  policy_id char(36) NOT NULL COMMENT '策略ID',
  rule_code varchar(120) NOT NULL COMMENT 'RULE编码',
  rule_name varchar(160) NOT NULL COMMENT 'RULE名称',
  rule_expr text COMMENT '字段说明：RULEEXPR',
  keywords json NOT NULL COMMENT '字段说明：KEYWORDS',
  risk_level varchar(32) NOT NULL DEFAULT 'medium' COMMENT '风险级别',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY uk_guardrail_rule(policy_id, rule_code)
) ENGINE=InnoDB COMMENT='护栏RULE表';
CREATE TABLE IF NOT EXISTS plugin_package (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  plugin_code varchar(120) NOT NULL UNIQUE COMMENT '插件编码',
  plugin_name varchar(160) NOT NULL COMMENT '插件名称',
  plugin_type varchar(64) NOT NULL COMMENT '插件类型',
  description varchar(1000) COMMENT '描述',
  version_no varchar(40) NOT NULL COMMENT '版本序号',
  package_url varchar(1000) COMMENT '包URL',
  manifest_json json NOT NULL COMMENT '字段说明：MANIFESTJSON',
  risk_level varchar(32) NOT NULL DEFAULT 'low' COMMENT '风险级别',
  verified tinyint(1) NOT NULL DEFAULT 0 COMMENT '字段说明：VERIFIED',
  status varchar(32) NOT NULL DEFAULT 'published' COMMENT '状态',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='插件包表';
CREATE TABLE IF NOT EXISTS plugin_installation (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  plugin_id char(36) NOT NULL COMMENT '插件ID',
  installed_version varchar(40) NOT NULL COMMENT 'INSTALLED版本',
  install_scope varchar(64) NOT NULL DEFAULT 'workspace' COMMENT '字段说明：INSTALLSCOPE',
  config_json json NOT NULL COMMENT '配置JSON',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  installed_by char(36) COMMENT 'INSTALLED人',
  installed_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'INSTALLED时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  UNIQUE KEY uk_plugin_install(plugin_id, install_scope)
) ENGINE=InnoDB COMMENT='插件安装表';
CREATE TABLE IF NOT EXISTS local_model_runtime (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  runtime_code varchar(120) NOT NULL UNIQUE COMMENT '运行时编码',
  runtime_name varchar(160) NOT NULL COMMENT '运行时名称',
  runtime_type varchar(64) NOT NULL COMMENT '运行时类型',
  endpoint_url varchar(1000) COMMENT '端点URL',
  host_info json NOT NULL COMMENT '字段说明：HOSTINFO',
  status varchar(32) NOT NULL DEFAULT 'unknown' COMMENT '状态',
  last_heartbeat_at datetime(3) COMMENT 'LASTHEARTBEAT时间',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='本地模型运行时表';
CREATE TABLE IF NOT EXISTS local_model_deployment (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  runtime_id char(36) NOT NULL COMMENT '运行时ID',
  model_id char(36) COMMENT '模型ID',
  deployment_name varchar(160) NOT NULL COMMENT '部署名称',
  image_name varchar(300) COMMENT 'IMAGE名称',
  model_path varchar(1000) COMMENT '模型路径',
  resource_request json NOT NULL COMMENT '资源请求',
  env_vars json NOT NULL COMMENT '字段说明：ENVVARS',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '状态',
  deployed_at datetime(3) COMMENT 'DEPLOYED时间',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='本地模型部署表';
CREATE TABLE IF NOT EXISTS data_import_job (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  job_code varchar(120) NOT NULL UNIQUE COMMENT '任务编码',
  job_name varchar(160) NOT NULL COMMENT '任务名称',
  import_type varchar(64) NOT NULL COMMENT '导入类型',
  target_type varchar(64) NOT NULL COMMENT 'TARGET类型',
  target_id char(36) COMMENT '字段说明：TARGETID',
  file_object_id char(36) COMMENT '文件对象ID',
  config_json json NOT NULL COMMENT '配置JSON',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '状态',
  total_count int NOT NULL DEFAULT 0 COMMENT '总数量',
  success_count int NOT NULL DEFAULT 0 COMMENT '成功数量',
  failure_count int NOT NULL DEFAULT 0 COMMENT '失败数量',
  error_report json NOT NULL COMMENT '错误报告',
  started_at datetime(3) COMMENT '开始时间',
  finished_at datetime(3) COMMENT '完成时间',
  created_by char(36) COMMENT '创建人ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='数据导入任务表';
