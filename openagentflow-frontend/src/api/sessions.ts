import { request } from './http';

export interface AgentSessionSummary {
  id: string;
  agentId: string;
  userId: string;
  sessionTitle: string;
  status: string;
  lastMessage?: string;
  messageCount: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AgentMessageSummary {
  id: string;
  sessionId: string;
  role: 'user' | 'assistant' | 'tool';
  content: string;
  contentType: string;
  toolCallId?: string;
  tokenCount: number;
  metadata?: string;
  createdAt?: string;
}

export async function fetchAgentSessions(agentId: string) {
  return request<AgentSessionSummary[]>(`/agents/${agentId}/sessions`);
}

export async function createAgentSession(agentId: string, sessionTitle?: string) {
  return request<AgentSessionSummary>(`/agents/${agentId}/sessions`, {
    method: 'POST',
    body: JSON.stringify({ sessionTitle }),
  });
}

export async function updateAgentSession(agentId: string, sessionId: string, sessionTitle: string) {
  return request<AgentSessionSummary>(`/agents/${agentId}/sessions/${sessionId}`, {
    method: 'PUT',
    body: JSON.stringify({ sessionTitle }),
  });
}

export async function deleteAgentSession(agentId: string, sessionId: string) {
  return request<void>(`/agents/${agentId}/sessions/${sessionId}`, {
    method: 'DELETE',
  });
}

export async function fetchAgentSessionMessages(agentId: string, sessionId: string) {
  return request<AgentMessageSummary[]>(`/agents/${agentId}/sessions/${sessionId}/messages`);
}
