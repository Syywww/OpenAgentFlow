import { API_BASE_URL, getAccessToken, request } from './http';
import type { PageResult } from './traces';

export interface UsageOverview {
  callCount: number;
  successCount: number;
  failureCount: number;
  totalTokens: number;
  promptTokens: number;
  completionTokens: number;
  totalCost: number;
  avgLatencyMs: number;
  quotaRuleCount: number;
  quotaRiskCount: number;
}

export interface DailyUsage {
  statDate: string;
  callCount: number;
  successCount: number;
  failureCount: number;
  totalTokens: number;
  totalCost: number;
}

export interface BreakdownItem {
  id?: string;
  name: string;
  callCount: number;
  totalTokens: number;
  totalCost: number;
  avgLatencyMs: number;
}

export interface UsageConsoleData {
  overview: UsageOverview;
  daily: DailyUsage[];
  modelBreakdown: BreakdownItem[];
  agentBreakdown: BreakdownItem[];
}

export interface UsageCallDetail {
  id: string;
  runId: string;
  stepId: string;
  runNo?: string;
  runType?: string;
  providerName?: string;
  modelName?: string;
  agentName?: string;
  workflowName?: string;
  userName?: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  costAmount: number;
  latencyMs: number;
  success: boolean;
  errorMessage?: string;
  createdAt?: string;
}

export interface QuotaRequest {
  subjectType: string;
  subjectId?: string;
  providerId?: string;
  modelId?: string;
  quotaPeriod: string;
  tokenLimit?: number;
  costLimit?: number;
}

export interface QuotaSummary extends QuotaRequest {
  id: string;
  tokenUsed: number;
  costUsed: number;
  tokenUsageRate: number;
  costUsageRate: number;
  resetAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export function usageQuery(params: Record<string, string | number | undefined> = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '' && value !== 'all') {
      query.set(key, String(value));
    }
  });
  return query.toString() ? `?${query.toString()}` : '';
}

export async function fetchUsageConsole(params: Record<string, string | number | undefined> = {}) {
  return request<UsageConsoleData>(`/usage/console${usageQuery(params)}`);
}

export async function fetchUsageBreakdown(params: Record<string, string | number | undefined> = {}) {
  return request<BreakdownItem[]>(`/usage/breakdown${usageQuery(params)}`);
}

export async function fetchUsageCalls(params: Record<string, string | number | undefined> = {}) {
  return request<PageResult<UsageCallDetail>>(`/usage/calls${usageQuery(params)}`);
}

export async function fetchUsageQuotas() {
  return request<QuotaSummary[]>('/usage/quotas');
}

export async function createUsageQuota(payload: QuotaRequest) {
  return request<QuotaSummary>('/usage/quotas', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateUsageQuota(id: string, payload: QuotaRequest) {
  return request<QuotaSummary>(`/usage/quotas/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteUsageQuota(id: string) {
  return request<void>(`/usage/quotas/${id}`, { method: 'DELETE' });
}

export async function recalculateUsageCosts() {
  return request<number>('/usage/recalculate-costs', { method: 'POST' });
}

export async function exportUsageCalls(params: Record<string, string | number | undefined> = {}) {
  const headers = new Headers();
  const token = getAccessToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  const response = await fetch(`${API_BASE_URL}/usage/calls/export${usageQuery(params)}`, { headers });
  if (!response.ok) {
    throw new Error('导出失败');
  }
  return response.blob();
}
