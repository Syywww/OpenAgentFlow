import { request } from './http';

export interface EvaluationDatasetSummary {
  id: string;
  datasetCode: string;
  datasetName: string;
  description?: string;
  domain?: string;
  tags?: string;
  visibility: string;
  status: string;
  sampleCount: number;
  taskCount: number;
  ownerUserId?: string;
  canManage: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface EvaluationDatasetDetail extends EvaluationDatasetSummary {
  samples: EvaluationSampleSummary[];
  recentTasks: EvaluationTaskSummary[];
}

export interface EvaluationDatasetRequest {
  datasetCode?: string;
  datasetName: string;
  description?: string;
  domain?: string;
  tags?: string;
  visibility?: string;
  status?: string;
}

export interface EvaluationSampleRequest {
  sampleNo?: number;
  question: string;
  expectedAnswer?: string;
  referenceContext?: string;
  scoringPoints?: string;
  metadata?: string;
  status?: string;
}

export interface EvaluationSampleSummary extends EvaluationSampleRequest {
  id: string;
  datasetId: string;
  sampleNo: number;
  status: string;
}

export interface SampleImportRequest {
  replaceExisting: boolean;
  samples: EvaluationSampleRequest[];
}

export interface EvaluationRunTaskRequest {
  taskName: string;
  datasetId: string;
  agentId?: string;
  workflowId?: string;
  baselineModelId?: string;
  compareModelIds?: string[];
  promptStrategy?: string;
  promptVariantText?: string;
  knowledgeStrategy?: string;
  temperature?: number;
  maxTokens?: number;
  maxSamples?: number;
  judgeEnabled?: boolean;
  judgeModelId?: string;
  judgePrompt?: string;
  evalConfig?: Record<string, unknown>;
}

export interface EvaluationTaskSummary {
  id: string;
  taskCode: string;
  taskName: string;
  datasetId: string;
  datasetName?: string;
  agentId: string;
  agentName?: string;
  workflowId?: string;
  workflowName?: string;
  baselineModelId?: string;
  baselineModelName?: string;
  compareModelIds?: string;
  evalConfig?: string;
  status: string;
  totalSamples: number;
  finishedSamples: number;
  overallScore?: number;
  successRate?: number;
  totalTokens?: number;
  averageLatencyMs?: number;
  createdAt?: string;
  startedAt?: string;
  finishedAt?: string;
}

export interface EvaluationTaskDetail extends EvaluationTaskSummary {
  summary: Record<string, number | string>;
  modelCompare: Record<string, number | string>[];
  runs: EvaluationTaskRunSummary[];
}

export interface EvaluationTaskRunSummary {
  id: string;
  taskId: string;
  sampleId: string;
  sampleNo?: number;
  question?: string;
  expectedAnswer?: string;
  modelId?: string;
  modelName?: string;
  runId?: string;
  answerText?: string;
  status: string;
  latencyMs?: number;
  tokenCount?: number;
  errorMessage?: string;
  scores: EvaluationScoreSummary[];
}

export interface EvaluationScoreSummary {
  metricId?: string;
  metricCode: string;
  metricName: string;
  score: number;
  passed: boolean;
  judgeType: string;
  judgeDetail?: string;
}

export async function fetchEvaluationDatasets() {
  return request<EvaluationDatasetSummary[]>('/evaluations/datasets');
}

export async function createEvaluationDataset(payload: EvaluationDatasetRequest) {
  return request<EvaluationDatasetDetail>('/evaluations/datasets', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateEvaluationDataset(id: string, payload: EvaluationDatasetRequest) {
  return request<EvaluationDatasetDetail>(`/evaluations/datasets/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteEvaluationDataset(id: string) {
  return request<void>(`/evaluations/datasets/${id}`, { method: 'DELETE' });
}

export async function fetchEvaluationDataset(id: string) {
  return request<EvaluationDatasetDetail>(`/evaluations/datasets/${id}`);
}

export async function importEvaluationSamples(id: string, payload: SampleImportRequest) {
  return request<EvaluationDatasetDetail>(`/evaluations/datasets/${id}/samples/import`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function runEvaluationTask(payload: EvaluationRunTaskRequest) {
  return request<EvaluationTaskDetail>('/evaluations/tasks/run', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function fetchEvaluationTasks() {
  return request<EvaluationTaskSummary[]>('/evaluations/tasks');
}

export async function fetchEvaluationTask(id: string) {
  return request<EvaluationTaskDetail>(`/evaluations/tasks/${id}`);
}
