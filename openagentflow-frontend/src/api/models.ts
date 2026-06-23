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
