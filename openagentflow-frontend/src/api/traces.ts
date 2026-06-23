import { request } from './http';

export interface PageResult<T> {
  records: T[];
  total: number;
  pageNo: number;
  pageSize: number;
}

export interface RunSummary {
  id: string;
  runNo: string;
  runType: string;
  agentId?: string;
  agentName?: string;
  userId?: string;
  userName?: string;
  inputText?: string;
  outputText?: string;
  status: string;
  statusLabel: string;
  totalTokens: number;
  promptTokens: number;
  completionTokens: number;
  totalCost: number;
  latencyMs: number;
  errorMessage?: string;
  stepCount: number;
  startedAt?: string;
  finishedAt?: string;
}

export interface TraceStepDetail {
  id: string;
  runId: string;
  parentStepId?: string;
  stepKey?: string;
  stepName: string;
  stepType: string;
  status: string;
  inputPayload?: unknown;
  outputPayload?: unknown;
  prompt?: unknown;
  tokenUsage?: unknown;
  costAmount?: number;
  latencyMs?: number;
  errorMessage?: string;
  llmCall?: Record<string, unknown>;
  toolInvocation?: Record<string, unknown>;
  retrievalLogs?: Record<string, unknown>[];
  startedAt?: string;
  finishedAt?: string;
}

export interface RunDetail extends RunSummary {
  inputPayload?: unknown;
  outputPayload?: unknown;
  metadata?: unknown;
  steps: TraceStepDetail[];
  retrievalLogs: Record<string, unknown>[];
  toolInvocations: Record<string, unknown>[];
  llmCalls: Record<string, unknown>[];
}

export interface RunStats {
  totalRuns: number;
  successRuns: number;
  failedRuns: number;
  runningRuns: number;
  avgLatencyMs: number;
  totalTokens: number;
}

export async function fetchRuns(params: Record<string, string | number | undefined> = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '' && value !== 'all') {
      query.set(key, String(value));
    }
  });
  const suffix = query.toString() ? `?${query.toString()}` : '';
  return request<PageResult<RunSummary>>(`/runs${suffix}`);
}

export async function fetchRunStats() {
  return request<RunStats>('/runs/stats');
}

export async function fetchRunDetail(runId: string) {
  return request<RunDetail>(`/runs/${runId}`);
}

export async function fetchRunSteps(runId: string) {
  return request<TraceStepDetail[]>(`/runs/${runId}/steps`);
}
