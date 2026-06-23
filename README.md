# OpenAgentFlow-Java

OpenAgentFlow-Java 是一个基于 **Java 21 + Spring Boot 3 + Vue 3** 的开源 AI Agent 工作流平台。项目面向企业知识库问答、工具调用、MCP 接入、工作流编排、运行 Trace 和模型评测场景，目标是提供一套可运行、可追踪、可评测、可扩展的 AI Agent 应用开发底座。

## 为什么核心链路自研

OpenAgentFlow-Java 的目标不是做一个简单的 AI 调用 Demo，而是完整呈现企业级 Agent 平台的核心链路。项目中的 Agent 编排、RAG 知识库、Tool Calling、MCP 接入、工作流执行、Trace 追踪、模型评测和成本治理均采用自研实现，方便开发者直接理解底层流程、学习关键设计并进行二次开发。

项目不会把能力绑定到某一个 AI 框架。模型、Embedding、向量库和工具调用都按开放适配思路设计，当前默认使用 OpenAI-compatible 接口、MySQL、Redis 和 Milvus；后续也可以按需扩展 Spring AI、LangChain4j 或其他模型网关。为企业落地留下可插拔的工程空间。

## 核心能力

- **模型接入**：OpenAI-compatible、豆包方舟、Ollama、DeepSeek、Qwen 等供应商配置，支持连通性测试、普通对话和 SSE 流式输出。
- **Agent 管理**：Agent CRUD、发布、复制、删除、模型参数、System Prompt、资源级权限和调试运行。
- **RAG 知识库**：知识库 CRUD、文档上传、解析、切片、Embedding、Milvus 写入、检索测试、引用来源和 Agent 绑定。
- **Tool Calling**：REST API、Webhook、数据库查询、MCP 工具，支持 Schema、连通性测试、风险等级、调用日志和 Trace。
- **可视化工作流**：Vue Flow 画布，支持开始、LLM、RAG、工具、条件、结束节点，支持发布、版本和执行 Trace。
- **MCP 接入**：MCP Server CRUD、HTTP JSON-RPC 连接测试、tools/prompts/resources 发现、同步到工具中心和 Agent/工作流调用。
- **运行观测 Trace**：统一记录 LLM、RAG、Tool、Workflow、Evaluation 步骤，展示 Token、耗时、错误和引用来源。
- **成本与用量中心**：按服务商、模型、Agent、用户、工作流、评测统计 Token、成本、耗时，支持明细导出、价格配置和配额拦截。
- **组织空间治理**：组织、工作空间、空间成员、资源归属和空间级访问控制，支持 Agent、知识库、工具、工作流按空间隔离。
- **模型评测 Evaluation**：评测集、样本导入、批量执行 Agent、规则评分、模型/Prompt/知识库策略对比和低分样本 Trace 追溯。
- **Agent 历史会话**：每个 Agent 支持按用户保存历史会话、消息列表、继续对话、新建会话和删除会话。
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
```

Docker Compose 首次初始化 MySQL 时会自动执行这些脚本。

## 当前版本状态

| 阶段 | 状态 | 说明 |
| --- | --- | --- |
| P0 登录、权限、模型接入 | 已完成 | JWT、Redis、模型供应商、SSE |
| P1 Agent 管理 | 已完成 | CRUD、发布、复制、删除、运行、Agent 权限 |
| P2 RAG 知识库 | 已完成 | 上传、解析、切片、Embedding、Milvus、引用来源 |
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

## 演示建议

1. 登录 `admin / 123456`。
2. 在系统设置中配置真实模型 API Key。
3. 在调试台发起一次模型对话，并查看 Run ID。
4. 上传知识库文档，等待处理完成后做检索测试。
5. 绑定知识库到 Agent，在调试台查看引用来源。
6. 创建工具并绑定 Agent，触发 Tool Calling。
7. 打开运行日志，查看 LLM、RAG、Tool 的完整 Trace。
8. 创建工作流并运行，查看工作流 Trace。
9. 接入 MCP Server，发现并测试 MCP 工具。
10. 创建评测集、导入样本、运行评测并跳转低分样本 Trace。
11. 打开用量中心，查看模型成本趋势、调用明细、维度拆分和配额规则。
12. 打开组织空间，创建团队空间、添加成员，并确认 Agent、知识库、工具、工作流归属到空间。
13. 打开任务中心，查看知识库文档解析、切片、Embedding、Milvus 写入的实时进度和日志。

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

## P8 验证记录

- `docker compose config` 已通过，Compose 文件可正常解析。
- 后端已使用 JDK 21 和指定 Maven 本地仓库执行 `mvn -DskipTests compile`，结果通过。
- 前端已执行 `npm run build`，结果通过。
- 已补齐 GitHub Actions CI、Issue 模板、PR 模板、License、`.env.example`、Dockerfile、Docker Compose、开发脚本和中文 docs 文档。
- 已将开源文档文件名中文化：`快速启动.md`、`架构说明.md`、`配置说明.md`、`演示流程.md`、`路线图.md`。
- 已补充中文界面截图：`登录页.png`、`工作台.png`、`智能体管理.png`、`调试台.png`、`工作流编排.png`、`运行日志.png`。
- 已确认真实模型 API Key 不写入源码、SQL、README 或 docs；示例配置只保留占位 Secret 和本地开发默认值。

## 本地启动记录

- `scripts/start-dev.ps1` 会优先释放 8080 和 5173 端口，再启动后端与前端。
- 本地未检测到 Milvus `localhost:19530` 时，脚本会自动设置 `OAF_MILVUS_ENABLED=false`，后端正常启动并保留 MySQL 向量兜底；如需真实 Milvus 写入，请先启动 Milvus 后再运行脚本。

## P9 验证记录

- 已新增 Agent 历史会话接口：`/agents/{agentId}/sessions`、`/agents/{agentId}/sessions/{sessionId}/messages`。
- 已将 Agent 调试消息写入 `agent_session` 和 `agent_message`，并把 `session_id` 写入运行记录。
- 调试台已支持历史会话列表、新建会话、打开历史消息、删除会话和继续对话。
- 后端已使用 JDK 21 和指定 Maven 本地仓库执行 `mvn -DskipTests compile`，结果通过。
- 前端已执行 `npm run build`，结果通过。

## P10 验证记录

- 已新增后端成本接口：`/usage/console`、`/usage/overview`、`/usage/daily`、`/usage/breakdown`、`/usage/calls`、`/usage/calls/export`、`/usage/quotas`。
- 已将模型输入/输出每千 Token 单价接入系统设置页和模型配置接口，真实成本按 `model_config.input_price_per_1k`、`model_config.output_price_per_1k` 计算。
- 已将聊天、Agent 运行和工作流 LLM 节点接入调用前配额预估拦截，支持全局、用户、角色、Agent、服务商、模型配额，调用后写入 `runtime_llm_call`、`runtime_trace_step.cost_amount`、`runtime_run.total_cost`、`runtime_cost_daily` 和 `model_usage_quota` 已用值。
- 已新增前端“用量中心”页面，支持总览卡片、成本趋势、模型/Agent/用户/工作流/评测维度拆分、调用明细、CSV 导出、配额新增/编辑/删除和 Trace 跳转。
- 已新增“重算历史成本”能力：先在系统设置中填写模型输入/输出每千 Token 单价，再在用量中心点击“重算历史成本”，即可把已有 Token 的历史调用成本回填到调用明细、Trace、运行总成本、成本日报和配额已用值。
- 如果用量中心显示有 Token 但成本为 0，表示对应模型的 `input_price_per_1k` 和 `output_price_per_1k` 仍为 0；这不是模型调用失败，而是尚未配置计费单价。
- 已新增 SQL 迁移 `V010__usage_cost_center.sql`，包含用量中心权限、统计索引和示例全局日配额。
- 后端已使用 JDK 21 和指定 Maven 本地仓库执行 `mvn "-Dmaven.repo.local=D:\kfhj\maven\mavenopenagent" -DskipTests compile`，结果通过。
- 前端已执行 `npm run build`，结果通过。

## P11 验证记录

- 已新增 SQL 迁移 `V011__organization_workspace_governance.sql`，创建 `oaf_organization`、`oaf_organization_member`、`oaf_workspace`、`oaf_workspace_member`、`oaf_workspace_resource` 表，并为 Agent、知识库、工具、工作流、MCP Server 增加 `workspace_id` 字段。
- 已初始化默认组织和默认工作空间，已有 Agent、知识库、工具、工作流、MCP Server 会自动归属到默认工作空间。
- 已新增组织/工作空间接口：`/organizations`、`/workspaces`、`/workspaces/{id}`、`/workspaces/{id}/members`。
- 已将 Agent、知识库、工具、工作流接入空间归属和空间成员权限判断，保留原有所有者、ACL 和全局管理员权限。
- 已新增前端“组织空间”页面，支持组织列表、工作空间列表、空间创建/编辑、成员添加/移除和空间资源统计。
- 后端已使用 JDK 21 和指定 Maven 本地仓库执行 `mvn "-Dmaven.repo.local=D:\kfhj\maven\mavenopenagent" -DskipTests compile`，结果通过。
- 前端已执行 `npm run build`，结果通过。

## P12 验证记录

- 已新增 SQL 迁移 `V012__async_task_center.sql`，创建 `async_task` 和 `async_task_log`，每张表和每个字段均包含中文注释。
- 已新增异步任务中心后端接口：`/tasks/overview`、`/tasks`、`/tasks/{id}`、`/tasks/{id}/cancel`、`/tasks/{id}/retry`。
- 已新增平台异步任务线程池 `oafAsyncTaskExecutor`，避免耗时任务占用 Web 请求线程。
- 已将知识库文档上传处理接入任务中心，解析、切片、Embedding、MySQL 保存、Milvus 写入、失败原因都会写入统一任务进度和日志。
- 已新增前端“任务中心”页面，支持任务统计、筛选、列表、详情、日志、取消和重试；知识库详情页可跳转查看任务中心日志。
- 后端已使用 JDK 21 和指定 Maven 本地仓库执行 `mvn "-Dmaven.repo.local=D:\kfhj\maven\mavenopenagent" -DskipTests compile`，结果通过。
- 前端已执行 `npm run build`，结果通过。

## 维护约定

- 新增或修改 Java 类、方法、字段和主要业务逻辑时补充中文注释。
- 每次修改前端、后端、SQL、项目结构、依赖、配置、启动方式或关键功能时，同步更新 README 或 docs。
- 前端页面已按原型图搭建，后续优先保持既有页面结构和视觉，不随意重做页面。
- 不要把真实模型 API Key 写入源码、SQL、README、docs 或提交记录。

## License

[MIT](LICENSE)
