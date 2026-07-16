USE openagentflow;

-- P74：企业解决方案模板广场，覆盖发布审核、不可变版本、异步安装、三方升级和运营治理。
ALTER TABLE agent_template
  ADD COLUMN workspace_id char(36) NULL COMMENT '私有模板所属工作空间ID，系统公开模板为空' AFTER id,
  ADD COLUMN template_type varchar(32) NOT NULL DEFAULT 'solution' COMMENT '模板类型：agent单智能体、solution解决方案包' AFTER template_name,
  ADD COLUMN visibility varchar(32) NOT NULL DEFAULT 'public' COMMENT '可见范围：workspace工作空间、public公开' AFTER template_type,
  ADD COLUMN current_version_id char(36) NULL COMMENT '当前公开或私有最新版本ID' AFTER visibility,
  ADD COLUMN review_status varchar(32) NOT NULL DEFAULT 'approved' COMMENT '审核状态：draft、checking、pending、approved、rejected' AFTER current_version_id,
  ADD COLUMN author_user_id char(36) NULL COMMENT '模板作者用户ID' AFTER review_status,
  ADD COLUMN author_name varchar(160) NULL COMMENT '模板作者展示名称' AFTER author_user_id,
  ADD COLUMN license_code varchar(64) NOT NULL DEFAULT 'Apache-2.0' COMMENT '模板许可证编码' AFTER author_name,
  ADD COLUMN compatibility varchar(1000) NULL COMMENT '运行环境与兼容性声明' AFTER license_code,
  ADD COLUMN cover_url varchar(500) NULL COMMENT '模板封面地址' AFTER icon,
  ADD COLUMN dependency_manifest json NOT NULL DEFAULT (JSON_OBJECT()) COMMENT '解决方案依赖清单JSON' AFTER knowledge_snapshot,
  ADD COLUMN package_bucket varchar(120) NULL COMMENT '模板包MinIO存储桶' AFTER dependency_manifest,
  ADD COLUMN package_key varchar(500) NULL COMMENT '模板包MinIO对象键' AFTER package_bucket,
  ADD COLUMN package_hash char(64) NULL COMMENT '模板包SHA-256校验值' AFTER package_key,
  ADD COLUMN package_size bigint NOT NULL DEFAULT 0 COMMENT '模板包对象大小字节数' AFTER package_hash,
  ADD COLUMN average_rating decimal(4,2) NOT NULL DEFAULT 0 COMMENT '模板平均评分' AFTER install_count,
  ADD COLUMN rating_count bigint NOT NULL DEFAULT 0 COMMENT '模板评分人数' AFTER average_rating,
  ADD COLUMN favorite_count bigint NOT NULL DEFAULT 0 COMMENT '模板收藏人数' AFTER rating_count,
  ADD COLUMN trend_score decimal(12,4) NOT NULL DEFAULT 0 COMMENT '模板趋势热度分' AFTER favorite_count,
  ADD COLUMN report_count bigint NOT NULL DEFAULT 0 COMMENT '模板被举报次数' AFTER trend_score,
  ADD COLUMN published_at datetime(3) NULL COMMENT '模板首次公开上架时间' AFTER status,
  ADD COLUMN deleted_at datetime(3) NULL COMMENT '模板软删除时间' AFTER updated_at,
  ADD COLUMN version bigint NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER deleted_at,
  ADD KEY idx_agent_template_market (visibility, status, recommended, trend_score),
  ADD KEY idx_agent_template_workspace (workspace_id, review_status, updated_at);

ALTER TABLE agent_template_install
  ADD COLUMN workspace_id char(36) NULL COMMENT '目标工作空间ID' AFTER template_id,
  ADD COLUMN template_version_id char(36) NULL COMMENT '已安装模板版本ID' AFTER workspace_id,
  ADD COLUMN install_task_id char(36) NULL COMMENT '关联异步任务ID' AFTER template_version_id,
  ADD COLUMN idempotency_key varchar(160) NULL COMMENT '安装请求幂等键' AFTER install_task_id,
  ADD COLUMN install_status varchar(32) NOT NULL DEFAULT 'success' COMMENT '安装状态：pending、running、success、failed、rollback、unlinked' AFTER target_agent_id,
  ADD COLUMN progress_percent int NOT NULL DEFAULT 100 COMMENT '安装进度百分比' AFTER install_status,
  ADD COLUMN current_stage varchar(64) NULL COMMENT '当前安装阶段编码' AFTER progress_percent,
  ADD COLUMN current_message varchar(500) NULL COMMENT '当前安装阶段说明' AFTER current_stage,
  ADD COLUMN name_prefix varchar(120) NULL COMMENT '安装资源名称前缀' AFTER current_message,
  ADD COLUMN model_mapping json NOT NULL DEFAULT (JSON_OBJECT()) COMMENT '模型替代映射JSON' AFTER name_prefix,
  ADD COLUMN embedding_model_id char(36) NULL COMMENT '目标Embedding模型ID' AFTER model_mapping,
  ADD COLUMN credentials_ready tinyint(1) NOT NULL DEFAULT 0 COMMENT '外部凭证是否已补齐' AFTER embedding_model_id,
  ADD COLUMN installed_manifest json NOT NULL DEFAULT (JSON_OBJECT()) COMMENT '已安装资源清单JSON' AFTER install_config,
  ADD COLUMN error_message text NULL COMMENT '安装失败原因' AFTER installed_manifest,
  ADD COLUMN upgrade_available tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否存在可升级版本' AFTER error_message,
  ADD COLUMN completed_at datetime(3) NULL COMMENT '安装完成时间' AFTER upgrade_available,
  ADD COLUMN updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间' AFTER created_at,
  ADD UNIQUE KEY uk_template_install_idempotency (idempotency_key),
  ADD KEY idx_template_install_workspace (workspace_id, install_status, created_at),
  ADD KEY idx_template_install_upgrade (template_id, template_version_id, upgrade_available);

CREATE TABLE agent_template_version (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  template_id char(36) NOT NULL COMMENT '模板ID',
  version_no varchar(32) NOT NULL COMMENT '语义化版本号',
  version_name varchar(160) NULL COMMENT '版本展示名称',
  change_log text NOT NULL COMMENT '版本更新说明',
  compatibility_statement text NOT NULL COMMENT '版本兼容性声明',
  breaking_change tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为破坏性升级',
  resource_manifest json NOT NULL COMMENT '版本资源清单JSON',
  dependency_graph json NOT NULL COMMENT '资源依赖图JSON',
  security_scan_result json NOT NULL COMMENT '自动安全检查结果JSON',
  runtime_check_result json NOT NULL COMMENT '最小可运行检查结果JSON',
  package_bucket varchar(120) NULL COMMENT '版本包MinIO存储桶',
  package_key varchar(500) NULL COMMENT '版本包MinIO对象键',
  package_hash char(64) NOT NULL COMMENT '版本包SHA-256校验值',
  package_size bigint NOT NULL DEFAULT 0 COMMENT '版本包大小字节数',
  status varchar(32) NOT NULL DEFAULT 'draft' COMMENT '版本状态：draft、checking、pending、approved、published、rejected、offline',
  submitted_by char(36) NULL COMMENT '提交审核用户ID',
  submitted_at datetime(3) NULL COMMENT '提交审核时间',
  published_by char(36) NULL COMMENT '发布用户ID',
  published_at datetime(3) NULL COMMENT '发布时间',
  created_by char(36) NULL COMMENT '创建用户ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_semantic_version (template_id, version_no),
  KEY idx_template_version_status (template_id, status, created_at)
) ENGINE=InnoDB COMMENT='解决方案模板不可变版本表';

CREATE TABLE agent_template_resource (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  template_version_id char(36) NOT NULL COMMENT '模板版本ID',
  resource_type varchar(64) NOT NULL COMMENT '资源类型：agent、team、prompt、tool、knowledge、document、chunk、embedding、workflow、memory、mcp',
  source_resource_id char(36) NULL COMMENT '来源资源ID',
  resource_code varchar(160) NULL COMMENT '资源业务编码',
  resource_name varchar(200) NOT NULL COMMENT '资源名称',
  resource_snapshot json NOT NULL COMMENT '已清洗敏感配置的资源快照JSON',
  content_hash char(64) NOT NULL COMMENT '资源快照SHA-256哈希',
  parent_resource_id char(36) NULL COMMENT '模板内父资源ID',
  dependency_ids json NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '模板内依赖资源ID数组',
  object_manifest json NOT NULL DEFAULT (JSON_ARRAY()) COMMENT 'MinIO对象清单JSON',
  sort_order int NOT NULL DEFAULT 0 COMMENT '安装顺序',
  required tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否为必需资源',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_version_resource (template_version_id, resource_type, source_resource_id),
  KEY idx_template_resource_order (template_version_id, sort_order, resource_type)
) ENGINE=InnoDB COMMENT='解决方案模板版本资源清单表';

CREATE TABLE agent_template_review (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  template_id char(36) NOT NULL COMMENT '模板ID',
  template_version_id char(36) NOT NULL COMMENT '模板版本ID',
  review_type varchar(32) NOT NULL COMMENT '审核类型：automatic自动、manual人工',
  review_status varchar(32) NOT NULL COMMENT '审核状态：passed、rejected、changes_required',
  risk_level varchar(32) NOT NULL DEFAULT 'low' COMMENT '审核风险等级',
  checklist_result json NOT NULL COMMENT '审核检查项结果JSON',
  review_comment varchar(2000) NULL COMMENT '审核意见',
  reviewer_user_id char(36) NULL COMMENT '审核用户ID',
  reviewed_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '审核时间',
  PRIMARY KEY (id),
  KEY idx_template_review_queue (review_type, review_status, reviewed_at),
  KEY idx_template_review_version (template_version_id, reviewed_at)
) ENGINE=InnoDB COMMENT='解决方案模板发布审核表';

CREATE TABLE agent_template_install_resource (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  install_id char(36) NOT NULL COMMENT '模板安装ID',
  template_resource_id char(36) NOT NULL COMMENT '模板版本资源ID',
  resource_type varchar(64) NOT NULL COMMENT '资源类型',
  source_resource_id char(36) NULL COMMENT '模板来源资源ID',
  target_resource_id char(36) NULL COMMENT '目标工作空间副本资源ID',
  source_hash char(64) NOT NULL COMMENT '安装时模板资源哈希',
  installed_hash char(64) NULL COMMENT '安装完成时目标资源哈希',
  current_hash char(64) NULL COMMENT '最近检查的目标资源哈希',
  install_status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '资源安装状态',
  user_modified tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户是否修改过副本',
  user_created tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为用户后续新增资源',
  object_manifest json NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '复制后的对象清单JSON',
  error_message text NULL COMMENT '资源安装失败原因',
  installed_at datetime(3) NULL COMMENT '资源安装完成时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_install_template_resource (install_id, template_resource_id),
  KEY idx_install_target_resource (resource_type, target_resource_id),
  KEY idx_install_resource_cleanup (install_id, user_modified, user_created)
) ENGINE=InnoDB COMMENT='解决方案模板安装资源映射表';

CREATE TABLE agent_template_upgrade_conflict (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  install_id char(36) NOT NULL COMMENT '模板安装ID',
  target_version_id char(36) NOT NULL COMMENT '目标模板版本ID',
  template_resource_id char(36) NOT NULL COMMENT '目标模板资源ID',
  resource_type varchar(64) NOT NULL COMMENT '资源类型',
  target_resource_id char(36) NULL COMMENT '本地资源副本ID',
  old_hash char(64) NULL COMMENT '旧模板资源哈希',
  local_hash char(64) NULL COMMENT '本地资源当前哈希',
  new_hash char(64) NULL COMMENT '新模板资源哈希',
  merge_decision varchar(32) NOT NULL COMMENT '合并判定：use_new、keep_local、same_change、conflict',
  user_choice varchar(32) NULL COMMENT '用户选择：use_new、keep_local',
  conflict_detail json NOT NULL COMMENT '三方差异详情JSON',
  resolved_by char(36) NULL COMMENT '冲突处理用户ID',
  resolved_at datetime(3) NULL COMMENT '冲突处理时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_upgrade_conflict_install (install_id, target_version_id, merge_decision)
) ENGINE=InnoDB COMMENT='解决方案模板三方升级冲突表';

CREATE TABLE agent_template_favorite (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  template_id char(36) NOT NULL COMMENT '模板ID',
  user_id char(36) NOT NULL COMMENT '收藏用户ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '收藏时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_favorite_user (template_id, user_id),
  KEY idx_template_favorite_user (user_id, created_at)
) ENGINE=InnoDB COMMENT='解决方案模板收藏表';

CREATE TABLE agent_template_rating (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  template_id char(36) NOT NULL COMMENT '模板ID',
  user_id char(36) NOT NULL COMMENT '评分用户ID',
  install_id char(36) NOT NULL COMMENT '用户成功安装ID',
  rating tinyint NOT NULL COMMENT '评分1到5分',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_rating_user (template_id, user_id),
  KEY idx_template_rating_template (template_id, rating)
) ENGINE=InnoDB COMMENT='解决方案模板用户评分表';

CREATE TABLE agent_template_comment (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  template_id char(36) NOT NULL COMMENT '模板ID',
  user_id char(36) NOT NULL COMMENT '评论用户ID',
  install_id char(36) NULL COMMENT '用户成功安装ID，作者或管理员回复时为空',
  parent_comment_id char(36) NULL COMMENT '回复的父评论ID',
  comment_content varchar(2000) NOT NULL COMMENT '评论原文',
  author_reply tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为模板作者回复',
  admin_reply tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为管理员回复',
  status varchar(32) NOT NULL DEFAULT 'visible' COMMENT '评论状态：visible、hidden、deleted',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_user_root_comment (template_id, user_id, parent_comment_id),
  KEY idx_template_comment_list (template_id, status, created_at)
) ENGINE=InnoDB COMMENT='解决方案模板评论与作者回复表';

CREATE TABLE agent_template_report (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  template_id char(36) NOT NULL COMMENT '被举报模板ID',
  reporter_user_id char(36) NOT NULL COMMENT '举报用户ID',
  report_type varchar(64) NOT NULL COMMENT '举报类型',
  report_reason varchar(2000) NOT NULL COMMENT '举报原因',
  evidence json NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '举报证据对象清单JSON',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '处理状态：pending、processing、resolved、rejected',
  resolution varchar(2000) NULL COMMENT '处理结论',
  handled_by char(36) NULL COMMENT '处理用户ID',
  handled_at datetime(3) NULL COMMENT '处理时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_report_user_pending (template_id, reporter_user_id, status),
  KEY idx_template_report_queue (status, created_at)
) ENGINE=InnoDB COMMENT='解决方案模板举报治理表';

-- 兼容旧模板并设置系统作者展示信息。
UPDATE agent_template
SET template_type='solution', visibility='public', review_status='approved',
    author_name=COALESCE(author_name, 'OpenAgentFlow 官方'),
    compatibility=COALESCE(compatibility, 'OpenAgentFlow-Java 0.1+，Java 21，MySQL 8，Redis 7，Milvus 2.4'),
    published_at=COALESCE(published_at, created_at)
WHERE visibility='public';

-- 首批公开解决方案模板，确保模板广场首次启动即可真实使用。
INSERT IGNORE INTO agent_template (
  id, template_code, template_name, template_type, visibility, review_status, author_name,
  category, description, icon, tags, agent_snapshot, prompt_snapshot, tool_snapshot, knowledge_snapshot,
  dependency_manifest, recommended, install_count, average_rating, rating_count, favorite_count,
  trend_score, status, published_at, created_by
) VALUES
('74000000-0000-0000-0000-000000000001','solution-customer-service','企业智能客服解决方案','solution','public','approved','OpenAgentFlow 官方','客服服务','包含客服 Agent、可信 RAG、订单工具、退款风险边界和工作流的完整解决方案。','Headphones','["客服","RAG","Tool Calling"]','{"agents":[{"resourceKey":"agent-customer","agentName":"企业智能客服","category":"客服","agentType":"rag_tool_agent","systemPrompt":"你是企业智能客服，请基于可靠知识和实时工具结果回答。","modelParams":{"temperature":0.2,"maxTokens":2048}}]}','{"prompts":[]}','{"tools":[]}','{"knowledgeBases":[]}','{"agents":1,"teams":0,"prompts":0,"tools":0,"knowledgeBases":0,"workflows":0,"memories":0}',1,0,4.80,12,36,98.5000,'published',NOW(3),NULL),
('74000000-0000-0000-0000-000000000002','solution-knowledge-assistant','企业知识问答解决方案','solution','public','approved','OpenAgentFlow 官方','知识管理','面向制度、产品手册和内部文档的可信知识问答解决方案。','BookOpen','["知识库","可信回答","引用"]','{"agents":[{"resourceKey":"agent-knowledge","agentName":"企业知识助手","category":"知识问答","agentType":"rag_tool_agent","systemPrompt":"你是企业知识助手，回答必须引用可靠来源，资料不足时明确拒答。","modelParams":{"temperature":0.1,"maxTokens":2048}}]}','{"prompts":[]}','{"tools":[]}','{"knowledgeBases":[]}','{"agents":1,"teams":0,"prompts":0,"tools":0,"knowledgeBases":0,"workflows":0,"memories":0}',1,0,4.70,9,28,86.0000,'published',NOW(3),NULL),
('74000000-0000-0000-0000-000000000003','solution-data-analyst','数据分析协作团队','solution','public','approved','OpenAgentFlow 官方','数据分析','包含 SQL 分析 Agent、复核 Agent 和主控协作策略的数据分析方案。','ChartNoAxesCombined','["数据分析","SQL","多Agent"]','{"agents":[{"resourceKey":"agent-data","agentName":"数据分析助手","category":"数据分析","agentType":"chat_agent","systemPrompt":"你是数据分析助手，只生成只读查询并解释数据结论。","modelParams":{"temperature":0.1,"maxTokens":3072}}]}','{"prompts":[]}','{"tools":[]}','{"knowledgeBases":[]}','{"agents":1,"teams":0,"prompts":0,"tools":0,"knowledgeBases":0,"workflows":0,"memories":0}',1,0,4.60,7,19,72.0000,'published',NOW(3),NULL),
('74000000-0000-0000-0000-000000000004','solution-devops','智能运维助手','solution','public','approved','OpenAgentFlow 官方','开发运维','面向告警解释、运行诊断和高风险操作确认的智能运维方案。','TerminalSquare','["运维","告警","风险治理"]','{"agents":[{"resourceKey":"agent-ops","agentName":"智能运维助手","category":"运维","agentType":"workflow_agent","systemPrompt":"你是智能运维助手，先诊断再建议，高风险操作必须请求人工确认。","modelParams":{"temperature":0.2,"maxTokens":2048}}]}','{"prompts":[]}','{"tools":[]}','{"knowledgeBases":[]}','{"agents":1,"teams":0,"prompts":0,"tools":0,"knowledgeBases":0,"workflows":0,"memories":0}',0,0,4.50,5,15,58.0000,'published',NOW(3),NULL);

INSERT IGNORE INTO agent_template_version (
  id, template_id, version_no, version_name, change_log, compatibility_statement, breaking_change,
  resource_manifest, dependency_graph, security_scan_result, runtime_check_result, package_hash,
  status, published_at, created_at
)
SELECT CONCAT('74100000-0000-0000-0000-00000000000', RIGHT(id,1)), id, '1.0.0', '首个公开版本',
       '提供可直接安装的基础解决方案包。', compatibility, 0, dependency_manifest,
       JSON_OBJECT('nodes', JSON_ARRAY(), 'edges', JSON_ARRAY()),
       JSON_OBJECT('passed', true, 'sensitive', true, 'promptRisk', true, 'toolRisk', true, 'license', true),
       JSON_OBJECT('passed', true, 'minimumRuntime', true), SHA2(CONCAT(template_code, ':1.0.0'), 256),
       'published', NOW(3), NOW(3)
FROM agent_template WHERE id LIKE '74000000-%';

UPDATE agent_template t JOIN agent_template_version v ON v.template_id=t.id AND v.version_no='1.0.0'
SET t.current_version_id=v.id WHERE t.id LIKE '74000000-%';

INSERT IGNORE INTO iam_permission (
  permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order
) VALUES
('template:view','模板广场浏览','menu','/templates','GET','/api/templates/**',108),
('template:publish','解决方案模板发布','api','/templates','ALL','/api/templates/manage/**',109),
('template:review','解决方案模板审核','api','/templates','ALL','/api/templates/reviews/**',110),
('template:operate','解决方案模板运营','api','/templates','ALL','/api/templates/operations/**',111);

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT r.id,p.id FROM iam_role r JOIN iam_permission p ON p.permission_code='template:view'
WHERE r.role_code IN ('super_admin','admin','developer','operator','viewer');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT r.id,p.id FROM iam_role r JOIN iam_permission p ON p.permission_code IN ('template:publish','template:review','template:operate')
WHERE r.role_code IN ('super_admin','admin');

