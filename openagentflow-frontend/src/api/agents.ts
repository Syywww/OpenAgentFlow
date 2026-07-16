import { readSseStream, type ChatCompletionRequest, type ChatCompletionResponse, type StreamHandlers, type StreamResult } from './chat';
import { API_BASE_URL, applyAuthHeaders, request } from './http';

export interface AgentSummary {
  id: string;
  agentCode: string;
  agentName: string;
  category: string;
  description: string;
  agentType: string;
  modelId?: string;
  modelName?: string;
  knowledgeCount: number;
  toolCount: number;
  status: string;
  statusLabel: string;
  visibility: string;
  ownerUserId?: string;
  ownerName?: string;
  canManage: boolean;
  publishedVersion?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AgentDetail extends AgentSummary {
  avatarUrl?: string;
  systemPromptTemplateId?: string;
  systemPromptVersionId?: string;
  promptBindingMode?: 'MANUAL' | 'LOCKED' | 'FOLLOW_STABLE';
  promptVariables?: string;
  systemPrompt?: string;
  modelParams?: string;
  memoryStrategy?: string;
  createdBy?: string;
  deletedAt?: string;
  version?: number;
}

export interface AgentRequest {
  agentCode?: string;
  agentName: string;
  avatarUrl?: string;
  category?: string;
  description?: string;
  agentType?: string;
  modelId?: string;
  systemPromptTemplateId?: string;
  systemPromptVersionId?: string;
  promptBindingMode?: 'MANUAL' | 'LOCKED' | 'FOLLOW_STABLE';
  promptVariables?: string;
  systemPrompt?: string;
  modelParams?: string;
  memoryStrategy?: string;
  visibility?: string;
  status?: string;
}

export interface AgentPublishRequest {
  versionNo?: string;
  publishNote?: string;
}

export async function fetchAgents() {
  return request<AgentSummary[]>('/agents');
}

export async function fetchAgent(id: string) {
  return request<AgentDetail>(`/agents/${id}`);
}

export async function createAgent(payload: AgentRequest) {
  return request<AgentDetail>('/agents', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateAgent(id: string, payload: AgentRequest) {
  return request<AgentDetail>(`/agents/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function publishAgent(id: string, payload: AgentPublishRequest) {
  return request<AgentDetail>(`/agents/${id}/publish`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function copyAgent(id: string) {
  return request<AgentDetail>(`/agents/${id}/copy`, { method: 'POST' });
}

export async function deleteAgent(id: string) {
  return request<void>(`/agents/${id}`, { method: 'DELETE' });
}

export async function runAgent(id: string, payload: ChatCompletionRequest) {
  return request<ChatCompletionResponse>(`/agents/${id}/run`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function streamAgent(
  id: string,
  payload: ChatCompletionRequest,
  handlers: StreamHandlers,
  signal?: AbortSignal,
): Promise<StreamResult> {
  const headers = new Headers();
  headers.set('Content-Type', 'application/json');
  applyAuthHeaders(headers);

  const response = await fetch(`${API_BASE_URL}/agents/${id}/run/stream`, {
    method: 'POST',
    headers,
    body: JSON.stringify(payload),
    signal,
  });

  if (!response.ok || !response.body) {
    throw new Error('Agent 流式调试请求失败');
  }

  return readSseStream(response.body, handlers, signal);
}
