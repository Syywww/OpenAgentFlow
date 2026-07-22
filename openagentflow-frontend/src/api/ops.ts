import { request } from './http';
import type { PageResult } from './traces';

export interface OpsOverview {
  openAlertCount: number;
  criticalAlertCount: number;
  healthyComponentCount: number;
  unhealthyComponentCount: number;
  apiFailureRate: number;
  modelFailureRate: number;
  taskBacklogCount: number;
  todayCost: number;
  todayRunCount: number;
  lastInspectionAt?: string;
}

export interface OpsHealthItem {
  code: string;
  name: string;
  type: string;
  status: string;
  message?: string;
  latencyMs?: number;
  checkedAt?: string;
  metadata?: Record<string, unknown>;
}

export interface OpsAlertRule {
  id: string;
  ruleCode: string;
  ruleName: string;
  metricCode: string;
  metricSource: string;
  operator: string;
  thresholdValue: number;
  severity: string;
  windowMinutes: number;
  cooldownMinutes: number;
  enabled: boolean;
  notifyChannels?: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface OpsAlertRuleRequest {
  ruleCode: string;
  ruleName: string;
  metricCode: string;
  metricSource: string;
  operator: string;
  thresholdValue: number;
  severity: string;
  windowMinutes: number;
  cooldownMinutes: number;
  enabled: boolean;
  notifyChannels?: string;
  description?: string;
}

export interface OpsAlertEvent {
  id: string;
  eventCode: string;
  ruleCode?: string;
  alertTitle: string;
  severity: string;
  metricCode: string;
  metricSource: string;
  metricValue: number;
  thresholdValue: number;
  alertDetail?: string;
  evidence?: Record<string, unknown>;
  status: string;
  notifyStatus: string;
  handledBy?: string;
  handledAt?: string;
  handleNote?: string;
  lastTriggeredAt?: string;
  triggerCount?: number;
}

export interface OpsHealthCheck {
  id: string;
  checkCode: string;
  checkName: string;
  targetType: string;
  targetCode: string;
  status: string;
  message?: string;
  latencyMs?: number;
  lastCheckedAt?: string;
  nextCheckAt?: string;
  enabled: boolean;
  metadata?: Record<string, unknown>;
}

export interface OpsNotifyChannel {
  id: string;
  channelCode: string;
  channelName: string;
  channelType: string;
  config?: Record<string, unknown>;
  enabled: boolean;
  lastTestStatus?: string;
  lastTestMessage?: string;
  lastTestAt?: string;
  lastSuccessAt?: string;
  failureCount: number;
}

export interface OpsNotifyChannelRequest {
  channelCode: string;
  channelName: string;
  channelType: string;
  config: Record<string, unknown>;
  enabled: boolean;
}

export interface OpsNotifyChannelTestResult {
  success: boolean;
  statusCode?: number;
  latencyMs: number;
  message: string;
}

export interface OpsNotificationDelivery {
  id: string;
  alertEventId: string;
  alertTitle: string;
  channelName?: string;
  channelType: string;
  status: string;
  attemptCount: number;
  nextRetryAt?: string;
  responseSummary?: string;
  errorMessage?: string;
  sentAt?: string;
  createdAt: string;
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

export async function fetchOpsOverview() {
  return request<OpsOverview>('/ops-monitor/overview');
}

export async function fetchOpsHealth() {
  return request<OpsHealthItem[]>('/ops-monitor/health');
}

export async function runOpsInspection() {
  return request<OpsHealthItem[]>('/ops-monitor/inspect', { method: 'POST' });
}

export async function fetchOpsRules(params: Record<string, string | number | boolean | undefined> = {}) {
  return request<PageResult<OpsAlertRule>>(`/ops-monitor/rules${queryString(params)}`);
}

export async function createOpsRule(payload: OpsAlertRuleRequest) {
  return request<OpsAlertRule>('/ops-monitor/rules', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateOpsRule(id: string, payload: OpsAlertRuleRequest) {
  return request<OpsAlertRule>(`/ops-monitor/rules/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteOpsRule(id: string) {
  return request<void>(`/ops-monitor/rules/${id}`, { method: 'DELETE' });
}

export async function fetchOpsEvents(params: Record<string, string | number | undefined> = {}) {
  return request<PageResult<OpsAlertEvent>>(`/ops-monitor/events${queryString(params)}`);
}

export async function handleOpsEvent(id: string, payload: { status: string; handleNote?: string }) {
  return request<OpsAlertEvent>(`/ops-monitor/events/${id}/handle`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function fetchOpsChecks() {
  return request<OpsHealthCheck[]>('/ops-monitor/checks');
}

export async function fetchOpsChannels() {
  return request<OpsNotifyChannel[]>('/ops-monitor/channels');
}

export async function createOpsChannel(payload: OpsNotifyChannelRequest) {
  return request<OpsNotifyChannel>('/ops-monitor/channels', { method: 'POST', body: JSON.stringify(payload) });
}

export async function updateOpsChannel(id: string, payload: OpsNotifyChannelRequest) {
  return request<OpsNotifyChannel>(`/ops-monitor/channels/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export async function setOpsChannelEnabled(id: string, enabled: boolean) {
  return request<OpsNotifyChannel>(`/ops-monitor/channels/${id}/enabled?enabled=${enabled}`, { method: 'PUT' });
}

export async function deleteOpsChannel(id: string) {
  return request<void>(`/ops-monitor/channels/${id}`, { method: 'DELETE' });
}

export async function testOpsChannel(id: string) {
  return request<OpsNotifyChannelTestResult>(`/ops-monitor/channels/${id}/test`, { method: 'POST' });
}

export async function fetchOpsDeliveries(params: Record<string, string | number | undefined> = {}) {
  return request<PageResult<OpsNotificationDelivery>>(`/ops-monitor/deliveries${queryString(params)}`);
}

export async function retryOpsDelivery(id: string) {
  return request<void>(`/ops-monitor/deliveries/${id}/retry`, { method: 'POST' });
}
