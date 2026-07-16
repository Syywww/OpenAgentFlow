import { request } from './http';

export interface PageResult<T> {
  records: T[];
  total: number;
  pageNo: number;
  pageSize: number;
}

export interface PromptOverview {
  templateCount: number;
  publishedCount: number;
  draftCount: number;
  versionCount: number;
  runningExperimentCount: number;
  productionReleaseCount: number;
  activeBindingCount: number;
}

export interface PromptTemplateSummary {
  id: string;
  templateCode: string;
  templateName: string;
  promptType: string;
  promptTypeLabel: string;
  content: string;
  variables: string;
  variableSchema: string;
  stableVersionId?: string;
  currentEnvironment: string;
  riskLevel: string;
  variableNames: string[];
  description?: string;
  status: string;
  statusLabel: string;
  versionCount: number;
  latestVersionNo?: string;
  bindingCount: number;
  ownerUserId?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PromptTemplateVersionSummary {
  id: string;
  templateId: string;
  versionNo: string;
  content: string;
  variables: string;
  variableSchema: string;
  contentHash?: string;
  validationStatus?: string;
  validationResult?: string;
  qualityScore?: number;
  environment?: string;
  publishedAt?: string;
  variableNames: string[];
  changeNote?: string;
  createdBy?: string;
  createdAt?: string;
}

export interface PromptTemplateDetail extends PromptTemplateSummary {
  versions: PromptTemplateVersionSummary[];
}

export interface PromptTemplateRequest {
  templateCode?: string;
  templateName: string;
  promptType: string;
  content: string;
  variables?: string;
  variableSchema?: string;
  description?: string;
  status?: string;
  riskLevel?: string;
}

export interface PromptLayer {
  layerCode: string;
  layerName: string;
  content: string;
  orderNo: number;
}

export interface PromptCompileResult {
  templateId?: string;
  versionId?: string;
  versionNo?: string;
  bindingMode: string;
  renderedPrompt: string;
  missingVariables: string[];
  variableSources: Record<string, string>;
  sensitiveVariableNames: string[];
  layers: PromptLayer[];
  estimatedTokens: number;
  contentHash: string;
  warnings: string[];
  experimentId?: string;
  variantId?: string;
  variantCode?: string;
}

export interface PromptVersionDiff {
  fromVersionId: string;
  toVersionId: string;
  addedLines: string[];
  removedLines: string[];
  variableSchemaChanged: boolean;
}

export interface PromptImpactItem {
  resourceType: string;
  resourceId: string;
  resourceName: string;
  bindingMode: string;
  versionId?: string;
}

export interface PromptEnvironmentRelease {
  id: string;
  templateId: string;
  versionId: string;
  environment: string;
  status: string;
  grayPercent: number;
  promotedBy?: string;
  promotedAt?: string;
}

export interface PromptVariantRequest {
  variantCode: string;
  promptVersionId?: string;
  promptContent?: string;
  modelParams?: string;
  trafficWeight: number;
}

export interface PromptExperimentRequest {
  experimentName: string;
  promptTemplateId?: string;
  agentId?: string;
  datasetId?: string;
  metricKey?: string;
  minSampleSize?: number;
  autoWinnerEnabled?: boolean;
  variants: PromptVariantRequest[];
}

export interface PromptVariantSummary {
  id: string;
  variantCode: string;
  promptVersionId?: string;
  trafficWeight: number;
  sampleCount: number;
  successRate: number;
  avgQualityScore: number;
  avgLatencyMs: number;
  totalTokens: number;
  totalCost: number;
}

export interface PromptExperimentSummary {
  id: string;
  experimentCode: string;
  experimentName: string;
  promptTemplateId: string;
  agentId?: string;
  status: string;
  metricKey: string;
  winnerVariantId?: string;
  variants: PromptVariantSummary[];
  createdAt?: string;
}

export interface PromptVersionMetric {
  versionId?: string;
  versionNo: string;
  callCount: number;
  successRate: number;
  avgQualityScore: number;
  avgLatencyMs: number;
  totalTokens: number;
  totalCost: number;
}

export interface PromptPublishRequest {
  versionNo?: string;
  changeNote?: string;
}

export async function fetchPromptOverview() {
  return request<PromptOverview>('/prompt-templates/overview');
}

export async function fetchPromptTemplates(params: {
  promptType?: string;
  status?: string;
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
}) {
  const query = new URLSearchParams();
  query.set('promptType', params.promptType || 'all');
  query.set('status', params.status || 'all');
  query.set('pageNo', String(params.pageNo || 1));
  query.set('pageSize', String(params.pageSize || 10));
  if (params.keyword) query.set('keyword', params.keyword);
  return request<PageResult<PromptTemplateSummary>>(`/prompt-templates?${query.toString()}`);
}

export async function fetchPromptTemplate(id: string) {
  return request<PromptTemplateDetail>(`/prompt-templates/${id}`);
}

export async function createPromptTemplate(payload: PromptTemplateRequest) {
  return request<PromptTemplateDetail>('/prompt-templates', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updatePromptTemplate(id: string, payload: PromptTemplateRequest) {
  return request<PromptTemplateDetail>(`/prompt-templates/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deletePromptTemplate(id: string) {
  return request<void>(`/prompt-templates/${id}`, { method: 'DELETE' });
}

export async function publishPromptTemplate(id: string, payload: PromptPublishRequest) {
  return request<PromptTemplateDetail>(`/prompt-templates/${id}/publish`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function copyPromptTemplate(id: string, payload: { templateName?: string; templateCode?: string }) {
  return request<PromptTemplateDetail>(`/prompt-templates/${id}/copy`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function rollbackPromptTemplate(id: string, versionId: string) {
  return request<PromptTemplateDetail>(`/prompt-templates/${id}/versions/${versionId}/rollback`, {
    method: 'POST',
  });
}

export async function previewPromptTemplate(id: string, payload: {
  versionId?: string;
  content?: string;
  variableSchema?: string;
  variables?: Record<string, unknown>;
  layers?: PromptLayer[];
  strict?: boolean;
}) {
  return request<PromptCompileResult>(`/prompt-templates/${id}/preview`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function diffPromptVersions(id: string, fromVersionId: string, toVersionId: string) {
  const query = new URLSearchParams({ fromVersionId, toVersionId });
  return request<PromptVersionDiff>(`/prompt-templates/${id}/diff?${query.toString()}`);
}

export async function fetchPromptImpacts(id: string) {
  return request<PromptImpactItem[]>(`/prompt-templates/${id}/impacts`);
}

export async function fetchPromptReleases(id: string) {
  return request<PromptEnvironmentRelease[]>(`/prompt-templates/${id}/releases`);
}

export async function promotePromptVersion(id: string, payload: {
  versionId: string;
  environment: string;
  grayPercent: number;
  releaseNote?: string;
}) {
  return request<PromptEnvironmentRelease>(`/prompt-templates/${id}/releases`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function fetchPromptMetrics(id: string) {
  return request<PromptVersionMetric[]>(`/prompt-templates/${id}/metrics`);
}

export async function fetchPromptExperiments(id: string) {
  return request<PromptExperimentSummary[]>(`/prompt-templates/${id}/experiments`);
}

export async function createPromptExperiment(id: string, payload: PromptExperimentRequest) {
  return request<PromptExperimentSummary>(`/prompt-templates/${id}/experiments`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updatePromptExperiment(id: string, experimentId: string, payload: PromptExperimentRequest) {
  return request<PromptExperimentSummary>(`/prompt-templates/${id}/experiments/${experimentId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function startPromptExperiment(id: string, experimentId: string) {
  return request<PromptExperimentSummary>(`/prompt-templates/${id}/experiments/${experimentId}/start`, { method: 'POST' });
}

export async function stopPromptExperiment(id: string, experimentId: string) {
  return request<PromptExperimentSummary>(`/prompt-templates/${id}/experiments/${experimentId}/stop`, { method: 'POST' });
}

export async function choosePromptExperimentWinner(id: string, experimentId: string, variantId: string) {
  const query = new URLSearchParams({ variantId });
  return request<PromptExperimentSummary>(`/prompt-templates/${id}/experiments/${experimentId}/winner?${query.toString()}`, { method: 'POST' });
}

export async function autoChoosePromptExperimentWinner(id: string, experimentId: string) {
  return request<PromptExperimentSummary>(`/prompt-templates/${id}/experiments/${experimentId}/auto-winner`, { method: 'POST' });
}

export async function deletePromptExperiment(id: string, experimentId: string) {
  return request<void>(`/prompt-templates/${id}/experiments/${experimentId}`, { method: 'DELETE' });
}
