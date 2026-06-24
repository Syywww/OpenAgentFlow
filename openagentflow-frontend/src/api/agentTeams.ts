import { request } from './http';

export interface AgentTeamMemberRequest {
  agentId: string;
  memberRole: string;
  handoffPolicy?: string;
  sortOrder?: number;
  enabled?: boolean;
}

export interface AgentTeamRequest {
  teamCode?: string;
  teamName: string;
  description?: string;
  collaborationMode?: string;
  coordinatorAgentId?: string;
  status?: string;
  members: AgentTeamMemberRequest[];
}

export interface AgentTeamSummary {
  id: string;
  teamCode: string;
  teamName: string;
  description?: string;
  collaborationMode: string;
  collaborationModeLabel: string;
  coordinatorAgentId?: string;
  coordinatorAgentName?: string;
  status: string;
  statusLabel: string;
  memberCount: number;
  runs7d: number;
  success7d: number;
  ownerUserId?: string;
  canManage: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AgentTeamMemberSummary {
  teamId: string;
  agentId: string;
  agentName: string;
  agentType?: string;
  modelName?: string;
  memberRole: string;
  handoffPolicy?: string;
  sortOrder: number;
  enabled: boolean;
  canRun: boolean;
}

export interface AgentTeamRunHistoryItem {
  collaborationRunId: string;
  runtimeRunId: string;
  objective: string;
  finalResult?: string;
  status: string;
  totalTokens?: number;
  latencyMs?: number;
  startedAt?: string;
  finishedAt?: string;
}

export interface AgentTeamDetail extends AgentTeamSummary {
  members: AgentTeamMemberSummary[];
  recentRuns: AgentTeamRunHistoryItem[];
}

export interface AgentTeamRunRequest {
  objective: string;
  sharedContext?: Record<string, unknown>;
  continueOnError?: boolean;
}

export interface AgentTeamRunStep {
  traceStepId: string;
  agentId: string;
  agentName: string;
  memberRole: string;
  stepName: string;
  input: string;
  output?: string;
  childRunId?: string;
  status: string;
  totalTokens: number;
  latencyMs: number;
  errorMessage?: string;
}

export interface AgentTeamRunResult {
  collaborationRunId: string;
  runtimeRunId: string;
  teamId: string;
  teamName: string;
  objective: string;
  finalResult?: string;
  status: string;
  totalTokens: number;
  latencyMs: number;
  errorMessage?: string;
  steps: AgentTeamRunStep[];
}

export async function fetchAgentTeams() {
  return request<AgentTeamSummary[]>('/agent-teams');
}

export async function fetchAgentTeam(id: string) {
  return request<AgentTeamDetail>(`/agent-teams/${id}`);
}

export async function createAgentTeam(payload: AgentTeamRequest) {
  return request<AgentTeamDetail>('/agent-teams', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateAgentTeam(id: string, payload: AgentTeamRequest) {
  return request<AgentTeamDetail>(`/agent-teams/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function publishAgentTeam(id: string) {
  return request<AgentTeamDetail>(`/agent-teams/${id}/publish`, { method: 'POST' });
}

export async function deleteAgentTeam(id: string) {
  return request<void>(`/agent-teams/${id}`, { method: 'DELETE' });
}

export async function runAgentTeam(id: string, payload: AgentTeamRunRequest) {
  return request<AgentTeamRunResult>(`/agent-teams/${id}/run`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
