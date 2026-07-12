import { request } from './http';
import type { AsyncTaskDetail } from './tasks';

export interface PageResult<T> {
  records: T[];
  total: number;
  pageNo: number;
  pageSize: number;
}

export interface MemoryOverview {
  totalCount: number;
  shortTermCount: number;
  longTermCount: number;
  taskCount: number;
  vectorCount: number;
  expiredCount: number;
  pendingSyncCount: number;
}

export interface MemorySummary {
  id: string;
  agentId?: string;
  agentName?: string;
  userId?: string;
  sessionId?: string;
  memoryType: string;
  memoryKey?: string;
  memoryText: string;
  memoryValue?: string;
  syncStatus?: string;
  importanceScore?: number;
  expiredAt?: string;
  status: string;
  privacyScope?: string;
  sourceRunId?: string;
  tagsJson?: string;
  hitCount?: number;
  lastAccessedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface MemorySaveRequest {
  agentId?: string;
  sessionId?: string;
  memoryType: string;
  memoryKey?: string;
  memoryText: string;
  memoryValue?: string;
  importanceScore?: number;
  expiredAt?: string;
  status?: string;
  privacyScope?: string;
  tagsJson?: string;
}

export interface MemoryRecallItem {
  id: string;
  agentId?: string;
  agentName?: string;
  memoryType: string;
  memoryText: string;
  score: number;
  importanceScore?: number;
}

export interface MemoryCleanupResult {
  archivedExpiredCount: number;
  deletedLowValueCount: number;
  messages: string[];
}

export interface MemoryProductionOverview {
  active: number;
  syncFailed: number;
  openIssues: number;
  conflicts: number;
  last30Days: Record<string, number>;
}

export interface MemoryPolicy {
  id: string;
  agent_id?: string;
  policy_name: string;
  extraction_enabled: boolean | number;
  min_importance: number;
  min_confidence: number;
  recall_threshold: number;
  recall_limit: number;
  prompt_token_budget: number;
  short_term_ttl_days: number;
  max_memories_per_user: number;
  pii_mode: string;
  conflict_mode: string;
  status: string;
}

export interface MemoryGovernanceIssue {
  id: string;
  memory_id?: string;
  issue_type: string;
  severity: string;
  status: string;
  memory_text?: string;
  fact_key?: string;
  resolution?: string;
  created_at?: string;
}

export interface MemoryQuery {
  memoryType?: string;
  status?: string;
  agentId?: string;
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
}

export async function fetchMemoryOverview() {
  return request<MemoryOverview>('/memories/overview');
}

export async function fetchMemories(query: MemoryQuery) {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value) !== '') {
      params.set(key, String(value));
    }
  });
  return request<PageResult<MemorySummary>>(`/memories?${params.toString()}`);
}

export async function createMemory(payload: MemorySaveRequest) {
  return request<MemorySummary>('/memories', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateMemory(id: string, payload: MemorySaveRequest) {
  return request<MemorySummary>(`/memories/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteMemory(id: string) {
  return request<void>(`/memories/${id}`, { method: 'DELETE' });
}

export async function recallMemories(payload: { agentId?: string; sessionId?: string; query: string; limit?: number }) {
  return request<MemoryRecallItem[]>('/memories/recall', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function cleanupMemories() {
  return request<AsyncTaskDetail>('/memories/cleanup', { method: 'POST' });
}

export async function fetchMemoryProductionOverview() {
  return request<MemoryProductionOverview>('/memories/production-overview');
}

export async function fetchMemoryPolicies() {
  return request<MemoryPolicy[]>('/memories/policies');
}

export async function saveMemoryPolicy(payload: Record<string, unknown>) {
  return request<MemoryPolicy>('/memories/policies', { method: 'POST', body: JSON.stringify(payload) });
}

export async function fetchMemoryGovernanceIssues(query: { status: string; type: string; pageNo: number; pageSize: number }) {
  const params = new URLSearchParams(Object.entries(query).map(([key, value]) => [key, String(value)]));
  return request<PageResult<MemoryGovernanceIssue>>(`/memories/governance/issues?${params.toString()}`);
}

export async function resolveMemoryGovernanceIssue(id: string, payload: Record<string, unknown>) {
  return request<MemoryGovernanceIssue>(`/memories/governance/issues/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export async function scanMemoryGovernance() {
  return request<AsyncTaskDetail>('/memories/governance/scan', { method: 'POST' });
}

export async function rebuildMemoryVectors() {
  return request<AsyncTaskDetail>('/memories/vectors/rebuild', { method: 'POST' });
}

export async function submitMemoryFeedback(id: string, feedbackType: string) {
  return request<void>(`/memories/${id}/feedback`, { method: 'POST', body: JSON.stringify({ feedbackType }) });
}
