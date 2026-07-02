# OpenAgentFlow-Java

OpenAgentFlow-Java 是一个基于 **Java 21 + Spring Boot 3 + Vue 3** 的开源 AI Agent 工作流平台。项目面向企业知识库问答、工具调用、MCP 接入、工作流编排、运行 Trace 和模型评测场景，目标是提供一套可运行、可追踪、可评测、可扩展的 AI Agent 应用开发底座。

## 为什么核心链路自研

OpenAgentFlow-Java 的目标不是做一个简单的 AI 调用 Demo，而是完整呈现企业级 Agent 平台的核心链路。项目中的 Agent 编排、RAG 知识库、Tool Calling、MCP 接入、工作流执行、Trace 追踪、模型评测和成本治理均采用自研实现，方便开发者直接理解底层流程、学习关键设计并进行二次开发。

项目不会把能力绑定到某一个 AI 框架。模型、Embedding、向量库和工具调用都按开放适配思路设计，当前默认使用 OpenAI-compatible 接口、MySQL、Redis 和 Milvus；后续也可以按需扩展 Spring AI、LangChain4j 或其他模型网关。为企业落地留下可插拔的工程空间。

## 核心能力

- **模型接入**：OpenAI-compatible、豆包方舟、Ollama、DeepSeek、Qwen 等供应商配置，支持连通性测试、普通对话和 SSE 流式输出。
- **Agent 管理**：Agent CRUD、发布、复制、删除、模型参数、System Prompt、资源级权限和调试运行。
- **多 Agent 协作**：协作团队 CRUD、成员分工、顺序/并行/路由/主控/复核模式、真实 Agent 调用、协作执行和 Trace 追踪。
- **Prompt 模板中心**：System、User、RAG、Tool、Evaluation、Workflow Prompt 模板管理，支持变量解析、版本发布、复制、回滚和 Agent 绑定。
- **RAG 知识库**：知识库 CRUD、文档上传、解析、切片、Embedding、Milvus 写入、混合召回、重排、低置信度提示、可信回答模式、强制引用来源和 Agent 绑定。
- **Tool Calling**：REST API、Webhook、数据库查询、MCP 工具，支持 Schema、连通性测试、风险等级、调用日志和 Trace。
- **可视化工作流**：Vue Flow 画布，支持基础信息弹框新建、空画布、双击画布或工具栏弹出下拉式节点类型选择器、开始、LLM、RAG、工具、条件、人工确认、并行、循环、子流程、插件、API、通知、输出、结束节点，支持输出节点右下角智能对话框、手动关闭后点击输出节点重新打开、节点配置保存反馈、重试超时、失败分支、变量映射、模板、API 发布、版本差异、预算、沙箱策略、对话节点输出面板、运行中节点动效、幂等运行、心跳快照、失败重跑和从失败节点恢复。
- **MCP 接入**：MCP Server CRUD、HTTP JSON-RPC 连接测试、tools/prompts/resources 发现、同步到工具中心和 Agent/工作流调用。
- **运行观测 Trace**：统一串联 LLM、RAG、Tool、Workflow、Evaluation 步骤，展示 Token、耗时、错误和引用来源。
- **成本与用量中心**：按服务商、模型、Agent、用户、工作流、评测统计 Token、成本、耗时，支持明细导出、价格配置和配额拦截。
- **组织空间治理**：组织、工作空间、空间成员、资源归属和空间级访问控制，支持 Agent、知识库、工具、工作流按空间隔离。
- **运营监控告警**：统一展示平台健康、关键指标、告警规则、告警事件、通知渠道和一键巡检，支撑日常运营与交付验收。
- **交付验收中心**：面向开源发布、客户交付和部署上线，提供环境检查、核心链路检查、风险提示、交付清单和报告生成。
- **模型评测 Evaluation**：评测集、样本导入、批量执行 Agent、LLM-as-Judge、规则兜底、模型/Prompt/知识库策略对比和低分样本 Trace 追溯。
- **Agent 历史会话**：每个 Agent 支持按用户保存历史会话、消息列表、继续对话、新建会话和删除会话，调试台支持流式生成暂停、保留部分回答并引入补充说明继续，长对话内容、引用来源和工具调用在独立区域内滚动展示。
- **Memory 记忆中心**：支持短期会话记忆、长期记忆、任务记忆、向量记忆、Prompt 同款管理布局、弹框维护、召回测试、过期清理、客服助手长期记忆模板和 Agent 调试链路自动沉淀。
- **开源工程化**：Docker Compose、`.env.example`、CI、脚本、License、Issue/PR 模板和开源文档。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3.5、TypeScript、Vite、Vue Router、Vue Flow、lucide-vue-next |
| 后端 | Java 21、Spring Boot 3.3、Spring Security、JWT、MyBatis-Plus |
| 数据 | MySQL 8、Redis 7、Milvus 2.4 |
| AI | OpenAI-compatible Chat、Embedding、Function Calling、MCP |
| 工程 | Docker Compose、GitHub Actions、PowerShell scripts |

## 界面预览

![工作台](docs/截图/工作台.png)

![调试台](docs/截图/调试台.png)

![工作流编排](docs/截图/工作流编排.png)

更多截图见 [演示流程](docs/演示流程.md)。

## 快速启动

### Docker Compose

```powershell
cd E:\xm\OpenAgentFlow-Java\dm
Copy-Item .env.example .env
docker compose up -d --build
```

访问地址：

- 前端：http://localhost:5173
- 后端：http://localhost:8080/api
- Swagger：http://localhost:8080/api/swagger-ui.html

默认账号：

```text
admin / 123456
```

### 本地开发

```powershell
cd E:\xm\OpenAgentFlow-Java\dm
.\scripts\start-dev.ps1
```

本地构建校验：

```powershell
.\scripts\build-all.ps1
```

停止本地服务：

```powershell
.\scripts\stop-dev.ps1
```

## 目录结构

```text
dm/
  .github/                    GitHub Actions、Issue 模板、PR 模板
  docs/                       架构、快速启动、配置、演示流程、路线图、界面截图
  docs/截图/                  登录页、工作台、智能体、调试台、工作流、运行日志截图
  scripts/                    本地开发和 Docker 启停脚本
  openagentflow-backend/      Spring Boot 后端
  openagentflow-frontend/     Vue 3 前端
  openagentflow-sql/          MySQL 初始化脚本
  docker-compose.yml          一键启动 MySQL、Redis、Milvus、后端、前端
  .env.example                示例环境变量
  LICENSE                     MIT License
```

## 文档

- [快速启动](docs/快速启动.md)
- [架构说明](docs/架构说明.md)
- [配置说明](docs/配置说明.md)
- [演示流程](docs/演示流程.md)
- [路线图](docs/路线图.md)
- [生产部署](docs/生产部署.md)
- [运营监控](docs/运营监控.md)
- [MySQL SQL 说明](openagentflow-sql/mysql/README.md)

## 配置约定

后端默认读取环境变量，未配置时使用本地开发默认值：

- MySQL：`openagentflow`，默认 `root/123456`
- Redis：`localhost:6379`
- Milvus：`localhost:19530`
- Milvus 开关：`OAF_MILVUS_ENABLED`，本地开发脚本会在 19530 未监听时自动设置为 `false`，后端使用 MySQL 向量兜底启动。
- 后端上下文路径：`/api`
- JWT Secret：生产环境必须通过 `OAF_JWT_SECRET` 覆盖

真实模型 API Key 不会写入源码、SQL 或 README。SQL 只初始化模型供应商和模型接入点，真实 Key 请在系统设置页或本地数据库中配置。

## SQL 初始化

MySQL 脚本目录：

```text
openagentflow-sql/mysql
```

执行顺序：

```text
V001__database_common.sql
V002__all_feature_tables.sql
V003__indexes_views_seed.sql
V004__milvus_integration.sql
V005__refresh_zh_comments.sql
V006__refresh_admin_password.sql
V007__seed_doubao_model_provider.sql
V008__agent_crud_permissions.sql
V009__rag_embedding_model_and_permissions.sql
V010__usage_cost_center.sql
V011__organization_workspace_governance.sql
V012__async_task_center.sql
V013__audit_risk_governance_center.sql
V014__model_gateway_governance.sql
V015__knowledge_governance_enhancement.sql
V016__ops_monitor_alert_center.sql
V017__prompt_template_center.sql
V018__iam_admin_center.sql
V019__seed_default_login_users.sql
V020__multi_agent_collaboration.sql
V021__runtime_trace_token_usage_default.sql
V022__memory_center.sql
V023__seed_customer_support_memory_template.sql
V024__rag_production_retrieval_enhancement.sql
V025__evaluation_llm_as_judge.sql
V026__delivery_acceptance_center.sql
V027__workflow_production_enhancement.sql
V028__workflow_execution_reliability_final.sql
```

Docker Compose 首次初始化 MySQL 时会自动执行这些脚本。

## 当前版本状态

| 阶段 | 状态 | 说明 |
| --- | --- | --- |
| P0 登录、权限、模型接入 | 已完成 | JWT、Redis、模型供应商、SSE |
| P1 Agent 管理 | 已完成 | CRUD、发布、复制、删除、运行、Agent 权限 |
| P2 RAG 知识库 | 已完成 | 上传、解析、切片、Embedding、Milvus、引用来源、可信回答模式 |
| P3 Tool Calling | 已完成 | REST API、Webhook、DB Query、工具日志 |
| P4 Trace 运行观测 | 已完成 | 运行列表、步骤详情、RAG/Tool/LLM 统一链路 |
| P5 工作流编排 | 已完成 | Vue Flow、节点执行、上下文变量、Trace |
| P6 MCP 工具接入 | 已完成 | Server 管理、发现、同步、调用、审计 |
| P7 模型评测 | 已完成 | 评测集、批量运行、指标、对比、Trace |
| P8 GitHub 开源发布准备 | 已完成 | Docker、CI、脚本、License、开源文档 |
| P9 Agent 历史会话 | 已完成 | 会话列表、消息持久化、继续对话、调试台历史面板 |
| P10 成本与用量中心 | 已完成 | 用量统计、成本明细、模型价格、配额拦截、日报、导出、Trace 跳转 |
| P11 组织/空间/资源治理 | 已完成 | 组织、工作空间、成员、资源归属、空间权限和前端管理页 |
| P12 异步任务中心 | 已完成 | 统一任务队列、进度、日志、取消、重试，知识库文档处理已接入 |
| P13 审计与风险治理中心 | 已完成 | 操作审计采集、风险事件归集、高风险确认审批、处置闭环 |
| P14 生产部署加固 | 已完成 | prod Profile、Secret 校验、安全头、生产 Compose、非 root 容器、部署文档 |
| P15 模型网关与模型治理 | 已完成 | 路由策略、候选模型、健康统计、失败回退、网关调用观测 |
| P16 知识库治理增强 | 已完成 | 治理策略、问题扫描、质量评分、风险级别、交付问题闭环 |
| P17 平台运营监控与告警中心 | 已完成 | 运营总览、健康矩阵、告警规则、告警事件、通知渠道、一键巡检 |
| P18 通知中心与消息触达 | 后置 | 已按优先级调整为低优先级，后续再做站内通知真实化和外部触达 |
| P19 Prompt 模板中心与版本治理 | 已完成 | Prompt 模板 CRUD、变量解析、版本发布、复制、回滚、Agent 绑定 |
| P20 工作台 Dashboard 全量真实化 | 已完成 | 真实指标、运行趋势、最近运行、模型排行、任务队列、告警健康、知识库质量 |
| P21 多 Agent 协作 | 已完成 | 协作团队 CRUD、成员分工、五种协作模式、真实 Agent 调用、协作执行、Trace 追踪 |
| P24 Memory 记忆中心 | 已完成 | 短期记忆、长期记忆、任务记忆、向量记忆、客服助手长期记忆模板、召回测试、过期清理、调试链路自动沉淀 |
| P26 评测增强 LLM-as-Judge | 已完成 | 裁判模型评分、Judge Prompt、质量维度 JSON、规则兜底、Judge 综合分和低分原因 |
| P27 RAG 生产级召回增强 | 已完成 | 混合召回、候选扩召、向量/关键词权重、文档/页码/元数据过滤、重排、引用高亮、排序原因和低置信度建议 |
| P28 交付验收中心 | 已完成 | 环境检查、核心链路检查、风险提示、交付清单、报告生成和权限菜单 |
| P29 工作流生产级增强 | 已完成 | 基础信息弹框新建、空画布、画布双击加节点、重试超时、失败分支、人工确认、变量映射、条件表达式、调试模式、模板、触发入口、Schema、队列语义、并行汇聚、循环批处理、版本差异、灰度策略、空间治理、预算控制、评测接入、子流程、API 发布、插件、沙箱、对话节点输出和运行中节点动效 |

## 演示建议

1. 登录 `admin / 123456`。
2. 在系统设置中配置真实模型 API Key。
3. 打开 Prompt 模板中心，创建或发布 System Prompt 模板，并在 Agent 详情页绑定。
4. 在调试台发起一次模型对话，并查看 Run ID。
5. 上传知识库文档，等待处理完成后做检索测试。
6. 绑定知识库到 Agent，在调试台查看引用来源。
7. 创建工具并绑定 Agent，触发 Tool Calling。
8. 打开运行日志，查看 LLM、RAG、Tool 的完整 Trace。
9. 创建工作流并运行，查看工作流 Trace。
10. 接入 MCP Server，发现并测试 MCP 工具。
11. 创建评测集、导入样本、运行评测并跳转低分样本 Trace。
12. 打开 Memory 记忆中心，新增长期记忆或向量记忆，并使用召回测试确认 Agent 可参考相关上下文。
13. 打开用量中心，查看模型成本趋势、调用明细、维度拆分和配额规则。
14. 打开组织空间，创建团队空间、添加成员，并确认 Agent、知识库、工具、工作流归属到空间。
15. 打开任务中心，查看知识库文档解析、切片、Embedding、Milvus 写入的实时进度和日志。
16. 打开风险治理，查看审计日志、高风险工具、MCP 风险、护栏事件和待确认请求，并完成处置闭环。
17. 使用 `.env.prod` 和 `docker-compose.prod.yml` 检查生产部署配置，确认默认密钥不能启动生产后端。
18. 打开运营监控，点击立即巡检，查看 MySQL、Redis、Milvus、模型供应商、任务队列、API 质量和模型质量状态，并处理告警事件。
19. 打开交付验收中心，点击一键验收，查看环境、权限、核心链路、配置风险和交付清单。
20. 打开工作流编排，使用模板创建流程，配置节点策略，在调试面板运行并查看 Trace。

## 开源发布清单

- [x] 根级 `.gitignore`
- [x] `.env.example`
- [x] MIT `LICENSE`
- [x] Dockerfile 与 Docker Compose
- [x] GitHub Actions CI
- [x] Issue / PR 模板
- [x] 本地启动、停止、构建脚本
- [x] README 开源首页
- [x] 架构、配置、快速启动、演示、路线图文档
- [x] SQL 初始化说明
- [x] 中文界面截图

## 维护约定

- 新增或修改 Java 类、方法、字段和主要业务逻辑时补充中文注释。
- 每次修改前端、后端、SQL、项目结构、依赖、配置、启动方式或关键功能时，同步更新 README 或 docs。
- 前端页面已按原型图搭建，后续优先保持既有页面结构和视觉，不随意重做页面。
- 不要把真实模型 API Key 写入源码、SQL、README、docs 或 Git 提交历史。

## License

[MIT](LICENSE)
