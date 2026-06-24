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
}

export interface PromptTemplateSummary {
  id: string;
  templateCode: string;
  templateName: string;
  promptType: string;
  promptTypeLabel: string;
  content: string;
  variables: string;
  variableNames: string[];
  description?: string;
  status: string;
  statusLabel: string;
  versionCount: number;
  latestVersionNo?: string;
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
  description?: string;
  status?: string;
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
