import { request } from './http';

export interface McpServerSummary {
  id: string;
  serverCode: string;
  serverName: string;
  description?: string;
  transportType: string;
  command?: string;
  endpointUrl?: string;
  authType?: string;
  status: string;
  lastHeartbeatAt?: string;
  toolsCount: number;
  promptsCount: number;
  resourcesCount: number;
  canManage: boolean;
}

export interface McpServerDetail extends McpServerSummary {
  args?: string;
  authConfig?: string;
  envVars?: string;
  allowedPaths?: string;
  riskPolicy?: string;
  capabilities?: McpCapabilitySummary[];
  lastTest?: McpConnectionTestResult;
}

export interface McpServerRequest {
  serverCode?: string;
  serverName: string;
  description?: string;
  transportType: string;
  command?: string;
  args?: string;
  endpointUrl?: string;
  authType?: string;
  authConfig?: string;
  envVars?: string;
  allowedPaths?: string;
  riskPolicy?: string;
  status?: string;
}

export interface McpCapabilitySummary {
  id: string;
  serverId: string;
  capabilityType: string;
  capabilityName: string;
  description?: string;
  schemaJson?: string;
  metadata?: string;
  enabled: boolean;
  riskLevel: string;
  riskLabel: string;
  discoveredAt?: string;
}

export interface McpConnectionTestResult {
  success: boolean;
  latencyMs: number;
  toolsCount: number;
  promptsCount: number;
  resourcesCount: number;
  responsePayload?: string;
  errorMessage?: string;
  createdAt?: string;
}

export interface McpDiscoveryResult {
  taskId: string;
  status: string;
  toolsCount: number;
  promptsCount: number;
  resourcesCount: number;
  capabilities: McpCapabilitySummary[];
  errorMessage?: string;
}

export async function fetchMcpServers() {
  return request<McpServerSummary[]>('/mcp-servers');
}

export async function fetchMcpServer(id: string) {
  return request<McpServerDetail>(`/mcp-servers/${id}`);
}

export async function createMcpServer(payload: McpServerRequest) {
  return request<McpServerDetail>('/mcp-servers', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateMcpServer(id: string, payload: McpServerRequest) {
  return request<McpServerDetail>(`/mcp-servers/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteMcpServer(id: string) {
  return request<void>(`/mcp-servers/${id}`, { method: 'DELETE' });
}

export async function testMcpServer(id: string) {
  return request<McpConnectionTestResult>(`/mcp-servers/${id}/test`, { method: 'POST' });
}

export async function discoverMcpServer(id: string) {
  return request<McpDiscoveryResult>(`/mcp-servers/${id}/discover`, { method: 'POST' });
}

export async function fetchMcpCapabilities(id: string) {
  return request<McpCapabilitySummary[]>(`/mcp-servers/${id}/capabilities`);
}
