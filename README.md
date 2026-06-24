# OpenAgentFlow-Java

OpenAgentFlow-Java 是一个基于 **Java 21 + Spring Boot 3 + Vue 3** 的开源 AI Agent 工作流平台。项目面向企业知识库问答、工具调用、MCP 接入、工作流编排、运行 Trace 和模型评测场景，目标是提供一套可运行、可追踪、可评测、可扩展的 AI Agent 应用开发底座。

## 为什么核心链路自研

OpenAgentFlow-Java 的目标不是做一个简单的 AI 调用 Demo，而是完整呈现企业级 Agent 平台的核心链路。项目中的 Agent 编排、RAG 知识库、Tool Calling、MCP 接入、工作流执行、Trace 追踪、模型评测和成本治理均采用自研实现，方便开发者直接理解底层流程、学习关键设计并进行二次开发。

项目不会把能力绑定到某一个 AI 框架。模型、Embedding、向量库和工具调用都按开放适配思路设计，当前默认使用 OpenAI-compatible 接口、MySQL、Redis 和 Milvus；后续也可以按需扩展 Spring AI、LangChain4j 或其他模型网关。为企业落地留下可插拔的工程空间。

## 核心能力

- **模型接入**：OpenAI-compatible、豆包方舟、Ollama、DeepSeek、Qwen 等供应商配置，支持连通性测试、普通对话和 SSE 流式输出。
- **Agent 管理**：Agent CRUD、发布、复制、删除、模型参数、System Prompt、资源级权限和调试运行。
- **多 Agent 协作**：协作团队 CRUD、成员分工、顺序/并行/路由/主控/复核模式、真实 Agent 调用、运行验证和 Trace 追踪。
- **Prompt 模板中心**：System、User、RAG、Tool、Evaluation、Workflow Prompt 模板管理，支持变量解析、版本发布、复制、回滚和 Agent 绑定。
- **RAG 知识库**：知识库 CRUD、文档上传、解析、切片、Embedding、Milvus 写入、检索测试、引用来源和 Agent 绑定。
- **Tool Calling**：REST API、Webhook、数据库查询、MCP 工具，支持 Schema、连通性测试、风险等级、调用日志和 Trace。
- **可视化工作流**：Vue Flow 画布，支持开始、LLM、RAG、工具、条件、结束节点，支持发布、版本和执行 Trace。
- **MCP 接入**：MCP Server CRUD、HTTP JSON-RPC 连接测试、tools/prompts/resources 发现、同步到工具中心和 Agent/工作流调用。
- **运行观测 Trace**：统一记录 LLM、RAG、Tool、Workflow、Evaluation 步骤，展示 Token、耗时、错误和引用来源。
- **成本与用量中心**：按服务商、模型、Agent、用户、工作流、评测统计 Token、成本、耗时，支持明细导出、价格配置和配额拦截。
- **组织空间治理**：组织、工作空间、空间成员、资源归属和空间级访问控制，支持 Agent、知识库、工具、工作流按空间隔离。
- **运营监控告警**：统一展示平台健康、关键指标、告警规则、告警事件、通知渠道和一键巡检，支撑日常运营与交付验收。
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
| P13 审计与风险治理中心 | 已完成 | 操作审计采集、风险事件归集、高风险确认审批、处置闭环 |
| P14 生产部署加固 | 已完成 | prod Profile、Secret 校验、安全头、生产 Compose、非 root 容器、部署文档 |
| P15 模型网关与模型治理 | 已完成 | 路由策略、候选模型、健康统计、失败回退、网关调用观测 |
| P16 知识库治理增强 | 已完成 | 治理策略、问题扫描、质量评分、风险级别、交付问题闭环 |
| P17 平台运营监控与告警中心 | 已完成 | 运营总览、健康矩阵、告警规则、告警事件、通知渠道、一键巡检 |
| P18 通知中心与消息触达 | 后置 | 已按优先级调整为低优先级，后续再做站内通知真实化和外部触达 |
| P19 Prompt 模板中心与版本治理 | 已完成 | Prompt 模板 CRUD、变量解析、版本发布、复制、回滚、Agent 绑定 |
| P20 工作台 Dashboard 全量真实化 | 已完成 | 真实指标、运行趋势、最近运行、模型排行、任务队列、告警健康、知识库质量 |
| P21 多 Agent 协作 | 已完成 | 协作团队 CRUD、成员分工、五种协作模式、真实 Agent 调用、运行验证、Trace 追踪 |

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
12. 打开用量中心，查看模型成本趋势、调用明细、维度拆分和配额规则。
13. 打开组织空间，创建团队空间、添加成员，并确认 Agent、知识库、工具、工作流归属到空间。
14. 打开任务中心，查看知识库文档解析、切片、Embedding、Milvus 写入的实时进度和日志。
15. 打开风险治理，查看审计日志、高风险工具、MCP 风险、护栏事件和待确认请求，并完成处置记录。
16. 使用 `.env.prod` 和 `docker-compose.prod.yml` 验证生产部署配置，确认默认密钥不能启动生产后端。
17. 打开运营监控，点击立即巡检，查看 MySQL、Redis、Milvus、模型供应商、任务队列、API 质量和模型质量状态，并处理告警事件。

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

## 本地启动记录

- `scripts/start-dev.ps1` 会优先释放 8080 和 5173 端口，再启动后端与前端。
- 本地未检测到 Milvus `localhost:19530` 时，脚本会自动设置 `OAF_MILVUS_ENABLED=false`，后端正常启动并保留 MySQL 向量兜底；如需真实 Milvus 写入，请先启动 Milvus 后再运行脚本。

## 维护约定

- 新增或修改 Java 类、方法、字段和主要业务逻辑时补充中文注释。
- 每次修改前端、后端、SQL、项目结构、依赖、配置、启动方式或关键功能时，同步更新 README 或 docs。
- 前端页面已按原型图搭建，后续优先保持既有页面结构和视觉，不随意重做页面。
- 不要把真实模型 API Key 写入源码、SQL、README、docs 或提交记录。

## 全局列表分页更新记录

- 已新增前端通用分页能力：`components/PaginationBar.vue` 和 `composables/usePagination.ts`，所有前端列表统一按每页 10 条展示，并提供上一页、下一页、总数和当前范围提示。
- 已接入分页的主要页面包括：工作台最近运行记录、智能体列表/详情绑定列表、知识库列表/详情列表、知识治理问题/策略/质量列表、工具中心、MCP Server/MCP 工具/能力列表、工作流列表、运行记录与 Trace 步骤、模型网关、评测集/评测结果、组织空间、异步任务中心、审计风险治理中心、用量中心、运营监控、系统设置、模板广场。
- 调试台为高频操作页面，左侧智能体、历史会话和右侧引用来源、工具调用不使用分页，改为固定高度列表框并在内容超出时使用内部滚动条。
- 已将服务端分页默认值统一为 10 条：运行记录、用量调用明细、风险治理事件、审计日志、异步任务中心在未显式传入 `pageSize` 时都会默认返回 10 条。

## 菜单栏修复记录

- 已修复左侧菜单项增多后菜单栏超出页面的问题：侧边栏保持固定高度，品牌区和收起按钮固定，菜单列表独立纵向滚动。
- 已为菜单文字增加单行省略和横向溢出保护，避免中文菜单项或后续新增菜单撑宽侧边栏。
- 已保持移动端窄侧栏样式不变，窄屏下仍只展示图标。

## 调试台布局修复记录

- 已取消调试台左侧智能体列表、历史会话列表、右侧引用来源列表、工具调用列表的分页展示。
- 左侧智能体和历史会话已拆分为两个固定高度列表框，内容超出时各自使用内部滚动条。
- 右侧 Trace 面板已改为固定网格布局：基础 Trace 信息固定，引用来源列表和工具调用列表分别拥有独立滚动条，不再使用右侧整体滚动条。
- 已将左侧调试面板改为固定网格布局：顶部 Agent/模型选择器固定，智能体列表和历史会话列表分别拥有独立滚动条，不再使用左侧整体滚动条。

## 知识治理页面布局更新记录

- 已将知识治理页面的“治理问题列表”“治理策略列表”“知识库质量列表”改为三个卡片式切换入口，每次只展示当前选中的列表区域。
- 已移除治理策略的页面内联新增/编辑表单，改为在“治理策略列表”卡片中通过右上角“新增治理策略”按钮打开弹窗。
- 治理策略编辑也复用同一个弹窗，列表本身保留分页、编辑和删除操作。

## 用量中心页面布局更新记录

- 已将用量中心页面的“成本趋势”“维度拆分”“调用成本明细”“配额规则”改为四个卡片式切换入口，每次只展示当前选中的业务区域。
- 已移除配额规则的页面内联新增/编辑表单，改为在“配额规则”卡片中通过右上角“新增配额”按钮打开弹窗。
- 配额编辑也复用同一个弹窗，配额规则列表本身保留分页、编辑和删除操作。

## 运营监控页面布局更新记录

- 已将运营监控顶部“打开告警”“异常组件”“任务积压”“今日成本”改为和智能体列表统计卡一致的横排样式。
- 已将“平台健康矩阵”“告警事件”“告警处理”“告警规则”“巡检项”“通知渠道”改为六个卡片式切换入口，每次只展示当前选中的业务区域。
- 已移除告警规则的页面内联新建/编辑表单，改为在“告警规则”卡片中通过右上角“新建告警规则”按钮打开弹窗。
- 告警规则编辑也复用同一个弹窗，告警规则列表本身保留分页、筛选、编辑和删除操作。

## 模型网关页面布局更新记录

- 已将模型网关页面的“路由策略”“模型健康”“最近网关调用”改为三个卡片式切换入口，每次只展示当前选中的业务区域。
- 已移除路由策略的页面内联新增/编辑表单，改为在“路由策略”卡片中通过右上角“新增策略”按钮打开弹窗。
- 路由策略编辑也复用同一个弹窗，路由策略列表本身保留分页、编辑和删除操作。

## 组织空间页面布局更新记录

- 已将组织空间页面顶部“组织数”“工作空间”“空间成员”“纳管资源”改为和智能体列表统计卡一致的横排样式。
- 已将“组织列表”“工作空间”“成员与角色”改为三个卡片式切换入口，每次只展示当前选中的业务区域。
- 已移除工作空间的页面内联新增/编辑表单，改为在“工作空间”卡片中通过右上角“新增工作空间”按钮打开弹窗。
- 工作空间编辑也复用同一个弹窗，工作空间列表本身保留分页、选择和编辑操作。
- 已将组织列表中的新增组织从页面内联表单改为右上角“新增组织”按钮触发弹窗。
- 已将成员与角色中的新增成员与角色从页面内联表单改为右上角“新增成员与角色”按钮触发弹窗。

## 运营类页面统一布局更新记录

- 已将任务中心页面顶部统计改为横排统计卡，任务队列保留为主列表视图。
- 已将风险治理页面顶部统计改为横排统计卡，并将“风险事件”“高风险确认”“操作审计”改为卡片式切换入口。
- 已将风险治理页面的风险详情从卡片切换改为弹框展示，点击风险行或详情按钮即可查看和处置风险。
- 已将评测集管理页面改为横排统计卡，并将“评测集”“样本导入”“运行评测”“最近评测任务”改为卡片式切换入口；新建/编辑评测集改为弹窗。
- 已将评测结果页面的“模型策略对比”“样本结果”“低分样本详情”改为卡片式切换入口。
- 已将系统设置页面的“用户与角色权限”“模型供应商配置”“模型列表”改为卡片式切换入口；新增/编辑模型供应商改为弹窗。
- 已将任务中心页面的任务详情从卡片切换改为弹框展示，点击任务行或详情按钮即可查看完整执行信息。

## IAM 用户与权限中心更新记录

- 已新增 SQL 迁移 `V018__iam_admin_center.sql`，初始化 `iam:manage` 权限，并默认授权给 `super_admin` 和 `admin` 角色。
- 已新增后端 IAM 管理接口：`/iam-admin/overview`、`/iam-admin/users`、`/iam-admin/departments`、`/iam-admin/roles`、`/iam-admin/roles/{id}/permissions`、`/iam-admin/permissions`。
- 已新增后端 IAM 管理服务，支持用户 CRUD、用户软删除、BCrypt 密码保存、用户所属部门设置、系统角色分配、部门树 CRUD、角色 CRUD 和角色权限批量配置。
- 已将系统设置页从静态用户 mock 改为真实接口，保留原“模型供应商配置”和“模型列表”卡片，并新增“用户管理”“部门树”“角色权限”卡片切换。
- 已在前端新增用户弹框、部门弹框和角色权限弹框；用户弹框可设置所属部门和系统角色，角色弹框可勾选菜单/API 权限。
## 登录退出更新记录

- 已在前端顶栏新增退出登录按钮，点击后调用后端 `/auth/logout`，由后端删除 Redis 中的 token 状态。
- 已新增前端 `logout()` API 封装，退出时会清理 `oaf_access_token` 和 `oaf_current_user`，即使 token 已过期或网络异常也会回到登录页。
- 顶栏用户信息已从本地当前用户缓存读取，不再固定显示 `admin`。
## 默认演示账号修复记录

- 已确认原始种子数据只初始化了 `admin` 登录账号，`developer` 和 `user` 仅存在于 `iam_role` 角色表中，因此此前不能直接用 `user / 123456` 登录。
- 已新增 SQL 迁移 `V019__seed_default_login_users.sql`，初始化 `developer / 123456` 和 `user / 123456` 两个演示登录账号，并分别绑定 `developer`、`user` 系统角色。
- 已在本地 MySQL `openagentflow` 成功写入并校验：`admin`、`developer`、`user` 三个账号的密码 `123456` 均可通过 BCrypt 匹配。
## 菜单权限过滤更新记录

- 已新增前端权限工具 `src/api/permissions.ts`，统一维护菜单与权限编码的映射关系。
- 左侧菜单已改为按当前登录用户 `currentUser.permissions` 动态过滤；`super_admin` 和 `admin` 角色默认显示全部菜单。
- 登录成功后不再固定跳转 `/dashboard`，会跳转到当前用户第一个有权限的菜单，避免普通用户进入无权限页面。
- 前端路由守卫已接入菜单权限判断，用户直接访问无权限菜单路径时会自动跳转到第一个可访问菜单。
## 菜单栏自适应高度更新记录

- 已调整左侧菜单栏布局：菜单项较少时不再强制把“收起”按钮顶到底部，侧边栏内容会按实际菜单高度自然排列。
- 菜单项较多时仍保留菜单列表内部滚动，避免整页被长菜单撑出视口。
## 列表密度与长字段展示更新记录

- 已为所有包含分页组件的标准列表卡片增加统一视口高度限制：当列表内容超过页面可用高度时，列表卡片内部出现纵向滚动条，分页条固定在卡片底部，表头在滚动时保持吸顶。
- 已为模板广场、工作流侧栏列表、弹窗绑定列表等非标准表格分页场景补充内部滚动规则，避免长列表继续撑高页面。
- 已新增前端全局长字段悬浮查看能力：表格单元格、列表行标题/说明、编码类字段默认单行省略，鼠标移动到被截断或较长文本上时展示完整内容浮层。
- 已将顶部汇总数据卡片高度压缩约一半，并同步压缩卡片切换入口、快捷卡片、模板卡片、供应商卡片、列表行和筛选区间距，让下方业务列表获得更多可视空间。

## 指标卡片方块化更新记录

- 已将各页面顶部汇总指标卡从横向铺满的大卡片改为固定小宽度的近方形卡片，桌面端不再四等分撑满整行，视觉上更像紧凑数据块。
- 指标卡片现在使用自动填充布局，宽屏横向排列，空间不足时自动换行；数值、标题和说明保持单行省略，避免把卡片撑宽或撑高。

## 指标卡片横向自适应更新记录

- 已按最新要求恢复各页面顶部汇总指标卡横向自适应铺满整行，卡片按当前数量平均分配宽度，不再自动换行。
- 保留紧凑高度、单行省略和图标右上角布局，保证下方列表区域仍能获得更多可视空间。

## P26 RAG 检索质量增强记录

- 已增强知识库检索测试接口 `/knowledge-bases/{id}/retrieval-test`：支持向量检索、关键词检索、混合检索、候选召回数、TopK、相似度阈值、低置信阈值、低置信拒答和本地规则 Rerank。
- 已为检索结果补充分项解释：最终得分、向量得分、关键词得分、重排得分、命中原因、候选数量、是否低置信、是否建议回答和拒答原因。
- 已将 Agent 绑定知识库的默认检索配置升级为混合检索 + 本地规则重排 + 低置信拒答；聊天调试时如果知识库没有召回可靠来源，会注入系统约束，要求模型明确说明资料不足，避免无依据编造。
- 已新增知识库向量重建接口 `/knowledge-bases/{id}/vectors/rebuild`，通过异步任务中心执行分批 Embedding、MySQL 向量更新和 Milvus 同步；任务中心新增“知识库向量重建”类型筛选。
- 已在知识库详情页新增检索质量参数面板、低置信提示、分项得分展示和“重建向量”入口，重建任务提交后可跳转任务中心查看进度。
- 已修复知识库详情页“切片预览 / 检索测试 / 引用来源”卡片点击无反应的问题，三个卡片现在按选中状态切换展示，检索测试完成后会自动切换到引用来源查看命中片段。

## P20 工作台 Dashboard 全量真实化记录

- 已扩展后端 `/dashboard/overview` 为工作台全量聚合接口，保留原有字段并新增已发布 Agent、启用工具、工作流、今日成功率、Token、平均耗时、任务积压、打开告警、异常组件等真实指标。
- 已新增最近 7 天运行趋势、最近运行记录、模型使用排行、任务队列、打开告警、平台健康检查、知识库健康和运营洞察数据，统一从现有 MySQL 业务表实时汇总，不再依赖前端 mock。
- 已新增前端 `api/dashboard.ts`，工作台页面改为真实接口加载，快捷入口改为实际路由跳转，最近运行可跳转 Trace 详情，用量、任务、告警、知识治理均可跳转对应治理页面。
- 已补充工作台专用紧凑布局样式，运行趋势、模型排行、任务队列、告警健康和知识库质量在首页集中展示，并保留列表分页和长文本省略展示习惯。

## IDEA 后端控制台日志更新记录

- 已新增后端 `logback-spring.xml`，IDEA 直接运行 `OpenAgentFlowApplication` 时会在 Run Console 输出带时间、级别、线程、Logger 和消息的彩色控制台日志。
- 已新增 `StartupLogRunner` 启动提示组件，Spring Boot 启动完成后会在控制台打印本机访问地址、局域网地址、Swagger 地址、MySQL、Redis、Milvus 等关键配置，方便确认服务已经启动成功。
- 支持通过环境变量临时调整日志级别：`OAF_APP_LOG_LEVEL`、`OAF_MYBATIS_LOG_LEVEL`、`OAF_JDBC_LOG_LEVEL`、`OAF_SECURITY_LOG_LEVEL`；默认业务日志为 `INFO`，MyBatis/JDBC 为 `WARN`，避免控制台被 SQL 细节刷屏。

## IDEA Debug 控制台日志更新记录

- 已新增 `application-debug.yml`，在 IDEA 的 Run/Debug Configuration 中把 Active profiles 设置为 `debug` 后，Debug Console 会自动输出更详细的后端调试日志。
- `debug` Profile 下已默认打开 `com.openagentflow`、`org.springframework.web`、`org.springframework.security`、MyBatis 和 JDBC 的关键日志，便于在断点调试时同步观察请求、鉴权、Mapper 和业务执行过程。
- 启动成功提示中已新增 `Debug日志: 已启用/未启用`，方便确认 IDEA Debug 是否真正加载了 `debug` Profile。
- 如需临时覆盖级别，可继续使用环境变量：`OAF_APP_LOG_LEVEL=TRACE`、`OAF_WEB_LOG_LEVEL=DEBUG`、`OAF_SECURITY_LOG_LEVEL=DEBUG`、`OAF_MYBATIS_LOG_LEVEL=DEBUG`、`OAF_JDBC_LOG_LEVEL=DEBUG`。

## P21 多 Agent 协作记录

- 已新增后端协作团队接口：`/agent-teams`、`/agent-teams/{id}`、`/agent-teams/{id}/publish`、`/agent-teams/{id}/run`，支持团队 CRUD、发布、删除和运行验证。
- 已新增多 Agent 协作执行服务，支持 `sequential` 顺序协作、`parallel` 并行汇总、`router` 路由分派、`supervisor` 主控规划、`reviewer` 复核审阅五种模式。
- 协作运行会创建顶层 `runtime_run`，每个成员 Agent 调用会写入 `runtime_trace_step`，并在 Step 输出中保留成员自身 `childRunId`，可从前端跳转 Trace 追踪完整链路。
- 已新增 SQL 迁移 `V020__multi_agent_collaboration.sql`，刷新 `agent_team`、`agent_team_member`、`agent_collaboration_run` 中文表字段注释，并初始化 `agent-team:*` 权限。
- 已新增 SQL 迁移 `V021__runtime_trace_token_usage_default.sql`，为 `runtime_trace_step.token_usage` 增加默认空 JSON，避免协作 Step 初始化时还没有 Token 用量而插入失败。
- 已新增前端“多 Agent 协作”页面和左侧导航入口 `/agent-teams`，支持团队列表、筛选分页、新建/编辑弹框、成员配置、最近运行历史、发布、删除、运行验证和 Trace 跳转。
- 已按交互统一要求将团队详情从页面右侧面板改为弹框展示，点击团队行或“详情”按钮后弹出成员分工、最近运行、运行验证和 Trace 跳转。

## License

[MIT](LICENSE)
