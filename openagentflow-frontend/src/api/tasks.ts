import { request } from './http';
import type { PageResult } from './traces';

export interface AsyncTaskSummary {
  id: string;
  taskCode: string;
  taskName: string;
  taskType: string;
  taskTypeLabel: string;
  bizType?: string;
  bizId?: string;
  workspaceId?: string;
  workspaceName?: string;
  status: string;
  progressPercent: number;
  currentStage?: string;
  currentMessage?: string;
  totalSteps: number;
  finishedSteps: number;
  retryCount: number;
  maxRetries: number;
  cancelRequested: boolean;
  queueTopic?: string;
  lockedBy?: string;
  heartbeatAt?: string;
  lockVersion?: number;
  checkpoint?: Record<string, unknown>;
  nextRetryAt?: string;
  deadLetterAt?: string;
  errorMessage?: string;
  startedAt?: string;
  finishedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AsyncTaskLogItem {
  id: string;
  logLevel: string;
  stage?: string;
  message: string;
  detail?: Record<string, unknown>;
  progressPercent?: number;
  createdAt?: string;
}

export interface AsyncTaskDetail extends AsyncTaskSummary {
  requestPayload?: Record<string, unknown>;
  resultPayload?: Record<string, unknown>;
  errorCode?: string;
  logs: AsyncTaskLogItem[];
  stages: AsyncTaskStageItem[];
}

export interface AsyncTaskStageItem {
  stageCode: string;
  stageName: string;
  stageOrder: number;
  status: string;
  workerId?: string;
  lockVersion?: number;
  input?: Record<string, unknown>;
  output?: Record<string, unknown>;
  errorMessage?: string;
  startedAt?: string;
  finishedAt?: string;
}

export interface AsyncTaskOverview {
  totalCount: number;
  pendingCount: number;
  runningCount: number;
  successCount: number;
  failedCount: number;
  canceledCount: number;
  deadLetterCount: number;
  outboxPendingCount: number;
  outboxDeadCount: number;
}

export function taskQuery(params: Record<string, string | number | undefined> = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '' && value !== 'all') {
      query.set(key, String(value));
    }
  });
  return query.toString() ? `?${query.toString()}` : '';
}

export async function fetchTaskOverview() {
  return request<AsyncTaskOverview>('/tasks/overview');
}

export async function fetchTasks(params: Record<string, string | number | undefined> = {}) {
  return request<PageResult<AsyncTaskSummary>>(`/tasks${taskQuery(params)}`);
}

export async function fetchTask(id: string) {
  return request<AsyncTaskDetail>(`/tasks/${id}`);
}

export async function cancelTask(id: string) {
  return request<AsyncTaskDetail>(`/tasks/${id}/cancel`, { method: 'POST' });
}

export async function retryTask(id: string) {
  return request<AsyncTaskDetail>(`/tasks/${id}/retry`, { method: 'POST' });
}
