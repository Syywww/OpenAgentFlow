import { request } from './http';

export interface WorkflowSummary {
  id: string;
  workflowCode: string;
  workflowName: string;
  description?: string;
  workflowType: string;
  status: string;
  statusLabel: string;
  publishedVersion?: string;
  visibility: string;
  ownerUserId?: string;
  nodeCount: number;
  canManage: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface WorkflowNodeDto {
  id?: string;
  nodeKey: string;
  nodeName: string;
  nodeType: string;
  positionX: number;
  positionY: number;
  configJson?: Record<string, unknown>;
  inputSchema?: Record<string, unknown>;
  outputSchema?: Record<string, unknown>;
  retryPolicy?: Record<string, unknown>;
  enabled?: boolean;
}

export interface WorkflowEdgeDto {
  id?: string;
  edgeKey: string;
  sourceNodeKey: string;
  targetNodeKey: string;
  conditionExpr?: string;
  label?: string;
  metadata?: Record<string, unknown>;
}

export interface WorkflowVersionSummary {
  id: string;
  versionNo: string;
  publishEnv?: string;
  publishNote?: string;
  status: string;
  createdAt?: string;
}

export interface WorkflowDetail extends WorkflowSummary {
  graphJson?: Record<string, unknown>;
  variableSchema?: Record<string, unknown>;
  nodes: WorkflowNodeDto[];
  edges: WorkflowEdgeDto[];
  versions: WorkflowVersionSummary[];
}

export interface WorkflowRequest {
  workflowCode?: string;
  workflowName: string;
  description?: string;
  workflowType?: string;
  graphJson?: Record<string, unknown>;
  variableSchema?: Record<string, unknown>;
  status?: string;
  visibility?: string;
  nodes: WorkflowNodeDto[];
  edges: WorkflowEdgeDto[];
}

export interface WorkflowRunResult {
  workflowRunId: string;
  runtimeRunId: string;
  status: string;
  outputText: string;
  context?: Record<string, unknown>;
  steps?: Array<Record<string, unknown>>;
  totalTokens?: number;
  latencyMs?: number;
  errorMessage?: string;
}

export interface AgentWorkflowBindingSummary {
  agentId: string;
  workflowId: string;
  workflowName: string;
  workflowCode: string;
  triggerMode: string;
  enabled: boolean;
}

export async function fetchWorkflows() {
  return request<WorkflowSummary[]>('/workflows');
}

export async function fetchWorkflow(id: string) {
  return request<WorkflowDetail>(`/workflows/${id}`);
}

export async function createWorkflow(payload: WorkflowRequest) {
  return request<WorkflowDetail>('/workflows', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateWorkflow(id: string, payload: WorkflowRequest) {
  return request<WorkflowDetail>(`/workflows/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteWorkflow(id: string) {
  return request<void>(`/workflows/${id}`, { method: 'DELETE' });
}

export async function publishWorkflow(id: string, publishNote?: string) {
  return request<WorkflowDetail>(`/workflows/${id}/publish`, {
    method: 'POST',
    body: JSON.stringify({
      versionNo: `v${new Date().toISOString().slice(0, 19).replace(/[-:T]/g, '')}`,
      publishEnv: 'dev',
      publishNote: publishNote || '通过工作流画布发布',
    }),
  });
}

export async function runWorkflow(id: string, agentId: string | undefined, input: string) {
  return request<WorkflowRunResult>(`/workflows/${id}/run`, {
    method: 'POST',
    body: JSON.stringify({ agentId, input, variables: {} }),
  });
}

export async function fetchAgentWorkflowBindings(agentId: string) {
  return request<AgentWorkflowBindingSummary[]>(`/agents/${agentId}/workflows`);
}

export async function saveAgentWorkflowBindings(agentId: string, workflowIds: string[]) {
  return request<AgentWorkflowBindingSummary[]>(`/agents/${agentId}/workflows`, {
    method: 'PUT',
    body: JSON.stringify({ workflowIds, triggerMode: 'agent_run' }),
  });
}
