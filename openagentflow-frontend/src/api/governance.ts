import { request } from './http';
import type { PageResult } from './traces';

export interface GovernanceOverview {
  auditCount: number;
  failedOperationCount: number;
  openRiskCount: number;
  highRiskCount: number;
  pendingConfirmationCount: number;
  guardrailEventCount: number;
  highRiskToolCount: number;
}

export interface AuditItem {
  id: string;
  userId?: string;
  username?: string;
  operationType?: string;
  resourceType?: string;
  requestMethod?: string;
  requestPath?: string;
  responseStatus?: number;
  success: boolean;
  failureReason?: string;
  clientIp?: string;
  latencyMs?: number;
  createdAt?: string;
}

export interface RiskItem {
  id: string;
  eventCode: string;
  eventType: string;
  sourceType: string;
  sourceId: string;
  riskLevel: string;
  riskLabel: string;
  status: string;
  title: string;
  description?: string;
  workspaceId?: string;
  workspaceName?: string;
  agentId?: string;
  toolId?: string;
  runId?: string;
  ruleCode?: string;
  evidence?: Record<string, unknown>;
  recommendedAction?: string;
  handledBy?: string;
  handledAt?: string;
  handleNote?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ConfirmationItem {
  id: string;
  toolId: string;
  toolName?: string;
  requesterUserId?: string;
  agentId?: string;
  runId?: string;
  requestPayload?: Record<string, unknown>;
  reason?: string;
  status: string;
  confirmedBy?: string;
  confirmedAt?: string;
  expiredAt?: string;
  createdAt?: string;
}

function queryString(params: Record<string, string | number | boolean | undefined> = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '' && value !== 'all') {
      query.set(key, String(value));
    }
  });
  return query.toString() ? `?${query.toString()}` : '';
}

export async function fetchGovernanceOverview() {
  return request<GovernanceOverview>('/governance/overview');
}

export async function fetchGovernanceRisks(params: Record<string, string | number | undefined> = {}) {
  return request<PageResult<RiskItem>>(`/governance/risks${queryString(params)}`);
}

export async function fetchGovernanceAudits(params: Record<string, string | number | boolean | undefined> = {}) {
  return request<PageResult<AuditItem>>(`/governance/audits${queryString(params)}`);
}

export async function fetchGovernanceConfirmations(status = 'pending') {
  return request<ConfirmationItem[]>(`/governance/confirmations${queryString({ status })}`);
}

export async function handleGovernanceRisk(id: string, payload: { status: string; handleNote?: string }) {
  return request<RiskItem>(`/governance/risks/${id}/handle`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function approveConfirmation(id: string, note?: string) {
  return request<ConfirmationItem>(`/governance/confirmations/${id}/approve`, {
    method: 'POST',
    body: JSON.stringify({ note: note || '' }),
  });
}

export async function rejectConfirmation(id: string, note?: string) {
  return request<ConfirmationItem>(`/governance/confirmations/${id}/reject`, {
    method: 'POST',
    body: JSON.stringify({ note: note || '' }),
  });
}

