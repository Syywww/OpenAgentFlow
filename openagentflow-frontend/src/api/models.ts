import { request } from './http';

export interface ModelConfigSummary {
  id: string;
  providerId: string;
  providerName: string;
  modelCode: string;
  modelName: string;
  modelType: string;
  contextWindow?: number;
  maxOutputTokens?: number;
  inputPricePer1k?: number;
  outputPricePer1k?: number;
  supportStream: boolean;
  supportFunctionCalling: boolean;
  supportVision: boolean;
  status: string;
  isDefault: boolean;
}

export interface ModelProviderSummary {
  id: string;
  providerCode: string;
  providerName: string;
  providerType: string;
  baseUrl: string;
  authType: string;
  status: string;
  healthStatus: string;
  keyMask?: string;
  models: ModelConfigSummary[];
}

export interface ModelConfigRequest {
  id?: string;
  modelCode: string;
  modelName: string;
  modelType: string;
  contextWindow?: number;
  maxOutputTokens?: number;
  inputPricePer1k?: number;
  outputPricePer1k?: number;
  supportStream: boolean;
  supportFunctionCalling: boolean;
  supportVision: boolean;
  defaultParams?: string;
  status: string;
  isDefault: boolean;
}

export interface ModelProviderRequest {
  providerCode: string;
  providerName: string;
  providerType: string;
  baseUrl: string;
  authType: string;
  defaultHeaders?: string;
  apiKey?: string;
  status: string;
  sortOrder?: number;
  models: ModelConfigRequest[];
}

export interface ModelConnectivityResult {
  success: boolean;
  healthStatus: string;
  latencyMs: number;
  responseText?: string;
  errorMessage?: string;
}

export interface ModelGatewayOverview {
  enabledPolicyCount: number;
  enabledModelCount: number;
  callCount24h: number;
  failureCount24h: number;
  failureRate24h: number;
  avgLatencyMs24h: number;
  fallbackCount24h: number;
}

export interface ModelRouteCandidateSummary {
  id: string;
  policyId: string;
  modelId: string;
  modelName: string;
  modelCode: string;
  providerName: string;
  priority: number;
  weight: number;
  maxLatencyMs?: number;
  maxCostPer1k?: number;
  enabled: boolean;
  recentFailureRate: number;
  recentAvgLatencyMs: number;
}

export interface ModelRoutePolicySummary {
  id: string;
  policyCode: string;
  policyName: string;
  sceneType: string;
  matchRule: string;
  matchScope?: string;
  workspaceIds?: string[];
  fallbackEnabled: boolean;
  status: string;
  candidates: ModelRouteCandidateSummary[];
  createdAt?: string;
  updatedAt?: string;
}

export interface ModelRouteCandidateRequest {
  id?: string;
  modelId: string;
  priority: number;
  weight: number;
  maxLatencyMs?: number;
  maxCostPer1k?: number;
  enabled: boolean;
}

export interface ModelRoutePolicyRequest {
  policyCode: string;
  policyName: string;
  sceneType: string;
  matchRule: string;
  matchScope?: string;
  workspaceIds?: string[];
  fallbackEnabled: boolean;
  status: string;
  candidates: ModelRouteCandidateRequest[];
}

export interface ModelHealthSummary {
  modelId: string;
  modelName: string;
  modelCode: string;
  providerName: string;
  status: string;
  healthStatus: string;
  recentCallCount: number;
  recentFailureCount: number;
  recentFailureRate: number;
  recentAvgLatencyMs: number;
  recentCost: number;
}

export interface ModelGatewayCallSummary {
  id: string;
  runId: string;
  gatewaySceneType?: string;
  routePolicyId?: string;
  policyName?: string;
  providerName?: string;
  modelName?: string;
  fallbackUsed: boolean;
  success: boolean;
  totalTokens: number;
  costAmount: number;
  latencyMs: number;
  errorMessage?: string;
  createdAt?: string;
}

export async function fetchModelProviders() {
  return request<ModelProviderSummary[]>('/model-providers');
}

export async function fetchChatModels() {
  return request<ModelConfigSummary[]>('/model-providers/chat-models');
}

export async function createModelProvider(payload: ModelProviderRequest) {
  return request<ModelProviderSummary>('/model-providers', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateModelProvider(id: string, payload: ModelProviderRequest) {
  return request<ModelProviderSummary>(`/model-providers/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteModelProvider(id: string) {
  return request<void>(`/model-providers/${id}`, { method: 'DELETE' });
}

export async function testModelProvider(id: string) {
  return request<ModelConnectivityResult>(`/model-providers/${id}/test`, { method: 'POST' });
}

export async function fetchModelGatewayOverview() {
  return request<ModelGatewayOverview>('/model-gateway/overview');
}

export async function fetchModelRoutePolicies() {
  return request<ModelRoutePolicySummary[]>('/model-gateway/policies');
}

export async function createModelRoutePolicy(payload: ModelRoutePolicyRequest) {
  return request<ModelRoutePolicySummary>('/model-gateway/policies', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateModelRoutePolicy(id: string, payload: ModelRoutePolicyRequest) {
  return request<ModelRoutePolicySummary>(`/model-gateway/policies/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteModelRoutePolicy(id: string) {
  return request<void>(`/model-gateway/policies/${id}`, { method: 'DELETE' });
}

export async function fetchModelHealth() {
  return request<ModelHealthSummary[]>('/model-gateway/health');
}

export async function fetchModelGatewayCalls(limit = 30) {
  return request<ModelGatewayCallSummary[]>(`/model-gateway/calls?limit=${limit}`);
}
