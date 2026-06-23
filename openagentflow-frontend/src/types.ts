export type StatusTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

export interface Metric {
  label: string;
  value: string;
  detail: string;
  tone: StatusTone;
  icon: string;
}

export interface Agent {
  id: string;
  name: string;
  description: string;
  category: string;
  model: string;
  knowledgeBases: number;
  tools: number;
  status: string;
  owner: string;
}

export interface KnowledgeBase {
  id: string;
  name: string;
  description: string;
  docs: number;
  chunks: number;
  embeddingModel: string;
  visibility: string;
  status: string;
}

export interface ToolDefinition {
  id: string;
  name: string;
  type: string;
  code: string;
  risk: string;
  status: string;
  calls: string;
}

export interface McpServer {
  id: string;
  name: string;
  transport: 'stdio' | 'http' | 'sse';
  endpoint: string;
  auth: string;
  status: string;
  heartbeat: string;
}

export interface RunLog {
  id: string;
  type: 'Agent' | 'Workflow';
  name: string;
  status: string;
  duration: string;
  cost: string;
  tokens: string;
  user: string;
  startedAt: string;
}

export interface Provider {
  name: string;
  type: string;
  health: string;
  models: number;
  successRate: string;
}
