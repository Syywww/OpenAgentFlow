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
