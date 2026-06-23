import type { Agent, KnowledgeBase, McpServer, Metric, Provider, RunLog, ToolDefinition } from '../types';

export const dashboardMetrics: Metric[] = [
  { label: 'Agent 数量', value: '128', detail: '较上周 +12.5%', tone: 'info', icon: 'Bot' },
  { label: '知识库数量', value: '32', detail: '较上周 +8.3%', tone: 'success', icon: 'Library' },
  { label: '今日调用', value: '9,856', detail: '较昨日 +18.6%', tone: 'warning', icon: 'Activity' },
  { label: 'Token 消耗', value: '2.34M', detail: '成本 ¥186.40', tone: 'danger', icon: 'Coins' },
];

export const agents: Agent[] = [
  { id: 'agent-support', name: '客服助手', description: '基于企业知识库的智能客服助手，支持多轮问答与工单创建', category: '客服 / 问答', model: 'GPT-4o', knowledgeBases: 3, tools: 12, status: '运行中', owner: 'admin' },
  { id: 'agent-contract', name: '合同审查专家', description: '对合同条款进行风险识别、合规审查与要点提取', category: '法务 / 审查', model: 'Claude 3.5', knowledgeBases: 5, tools: 8, status: '运行中', owner: 'legal' },
  { id: 'agent-analyst', name: '数据分析师', description: '自然语言数据分析与可视化专家，支持生成分析报告', category: '数据 / 分析', model: 'GPT-4o', knowledgeBases: 2, tools: 16, status: '开发中', owner: 'bi-team' },
  { id: 'agent-hr', name: '招聘助手', description: '简历筛选、岗位匹配与面试问题生成', category: 'HR / 招聘', model: 'Qwen2.5-72B', knowledgeBases: 4, tools: 9, status: '已暂停', owner: 'hr' },
  { id: 'agent-ops', name: '运维管家', description: '运维知识问答、告警分析与操作建议', category: '运维 / AIOps', model: 'GLM-4-Plus', knowledgeBases: 6, tools: 13, status: '异常', owner: 'ops' },
];

export const knowledgeBases: KnowledgeBase[] = [
  { id: 'kb-product', name: '产品手册知识库', description: '产品功能说明书、操作指南、最佳实践', docs: 358, chunks: 8742, embeddingModel: 'text-embedding-3-large', visibility: '团队可见', status: '正常' },
  { id: 'kb-tech', name: '技术文档知识库', description: '技术架构、设计文档、API 说明', docs: 512, chunks: 12658, embeddingModel: 'text-embedding-3-large', visibility: '团队可见', status: '正常' },
  { id: 'kb-faq', name: 'FAQ 问答知识库', description: '常见问题与解决方案', docs: 256, chunks: 5632, embeddingModel: 'text-embedding-3-small', visibility: '公开可见', status: '正常' },
  { id: 'kb-case', name: '项目案例知识库', description: '项目案例、方案与报告', docs: 198, chunks: 4512, embeddingModel: 'text-embedding-3-small', visibility: '团队可见', status: '正常' },
  { id: 'kb-ops', name: '运维知识库', description: '运维手册、故障处理、应急预案', docs: 128, chunks: 2845, embeddingModel: 'bge-large-zh-v1.5', visibility: '团队可见', status: '索引中' },
];

export const tools: ToolDefinition[] = [
  { id: 'tool-user', name: '获取用户信息', type: 'REST API', code: 'get_user_info', risk: '低风险', status: '启用', calls: '1,245' },
  { id: 'tool-order-create', name: '创建订单', type: 'REST API', code: 'create_order', risk: '中风险', status: '启用', calls: '3,852' },
  { id: 'tool-order-query', name: '查询订单状态', type: 'REST API', code: 'query_order_status', risk: '低风险', status: '启用', calls: '2,156' },
  { id: 'tool-user-db', name: '用户资料库', type: 'DB', code: 'user_db', risk: '中风险', status: '启用', calls: '1,002' },
  { id: 'tool-order-db', name: '订单数据库', type: 'DB', code: 'order_db', risk: '高风险', status: '停用', calls: '632' },
  { id: 'tool-webhook', name: '事件通知', type: 'Webhook', code: 'event_notify', risk: '中风险', status: '启用', calls: '4,521' },
];

export const mcpServers: McpServer[] = [
  { id: 'mcp-files', name: 'filesystem-server', transport: 'stdio', endpoint: 'npx @modelcontextprotocol/server-filesystem', auth: '无认证', status: '运行中', heartbeat: '10 秒前' },
  { id: 'mcp-db', name: 'db-tools-server', transport: 'http', endpoint: 'https://mcp.example.com/db', auth: 'Bearer Token', status: '运行中', heartbeat: '20 秒前' },
  { id: 'mcp-web', name: 'web-search-server', transport: 'sse', endpoint: 'https://mcp.example.com/search', auth: 'API Key', status: '运行中', heartbeat: '30 秒前' },
  { id: 'mcp-git', name: 'git-tools-server', transport: 'stdio', endpoint: 'uvx mcp-server-git', auth: '无认证', status: '已停止', heartbeat: '5 分钟前' },
  { id: 'mcp-weather', name: 'weather-server', transport: 'http', endpoint: 'https://mcp.example.com/weather', auth: 'Bearer Token', status: '连接异常', heartbeat: '12 分钟前' },
];

export const runLogs: RunLog[] = [
  { id: 'run_20240526_0001', type: 'Agent', name: '智能客服Agent', status: '成功', duration: '12.45s', cost: '¥0.023', tokens: '2,345', user: 'admin', startedAt: '2024-05-26 14:32:21' },
  { id: 'run_20240526_0002', type: 'Workflow', name: '售后处理流程', status: '成功', duration: '18.32s', cost: '¥0.045', tokens: '4,321', user: 'zhangsan', startedAt: '2024-05-26 14:28:11' },
  { id: 'run_20240526_0003', type: 'Agent', name: '知识问答Agent', status: '失败', duration: '6.21s', cost: '¥0.008', tokens: '1,102', user: 'lisi', startedAt: '2024-05-26 14:20:05' },
  { id: 'run_20240526_0004', type: 'Workflow', name: '内容生成流程', status: '成功', duration: '24.18s', cost: '¥0.068', tokens: '6,781', user: 'admin', startedAt: '2024-05-26 14:18:33' },
  { id: 'run_20240526_0005', type: 'Agent', name: '数据分析Agent', status: '成功', duration: '9.77s', cost: '¥0.015', tokens: '1,988', user: 'wangwu', startedAt: '2024-05-26 14:15:12' },
  { id: 'run_20240526_0006', type: 'Workflow', name: '订单处理流程', status: '运行中', duration: '-', cost: '¥0.002', tokens: '234', user: 'admin', startedAt: '2024-05-26 14:14:01' },
];

export const providers: Provider[] = [
  { name: 'OpenAI 兼容', type: 'Chat / Embedding / Vision', health: '健康', models: 12, successRate: '98.7%' },
  { name: 'Ollama 本地', type: 'Chat / Embedding', health: '健康', models: 8, successRate: '99.1%' },
  { name: '通义千问 Qwen', type: 'Chat / Rerank', health: '健康', models: 10, successRate: '97.8%' },
  { name: 'DeepSeek', type: 'Chat / Reasoning', health: '警告', models: 6, successRate: '94.2%' },
];

export const evaluationSets = [
  { name: '金融问答基准集 v1.2', samples: 12842, tag: '问答', status: '已发布' },
  { name: '电商客服场景集 v2.0', samples: 8653, tag: '客服', status: '已发布' },
  { name: '医疗问答评测集 v1.1', samples: 6421, tag: '问答', status: '已发布' },
  { name: '知识库检索评测集 v1.0', samples: 5231, tag: 'RAG', status: '已发布' },
  { name: '代码生成评测集 v0.9', samples: 4102, tag: '代码', status: '草稿' },
];
