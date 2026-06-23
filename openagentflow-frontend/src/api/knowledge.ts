import { API_BASE_URL, getAccessToken, request } from './http';

export interface KnowledgeSource {
  kbId: string;
  kbName: string;
  documentId: string;
  documentName: string;
  chunkId: string;
  chunkNo: number;
  quoteText: string;
  score: number;
  pageNo?: number;
  retrievalLogId?: string;
}

export interface KnowledgeBaseSummary {
  id: string;
  kbCode: string;
  kbName: string;
  description?: string;
  embeddingModelId?: string;
  embeddingModelName?: string;
  chunkStrategy: string;
  chunkSize: number;
  chunkOverlap: number;
  milvusCollectionName?: string;
  status: string;
  documentCount: number;
  chunkCount: number;
  embeddingCount: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface KnowledgeDocumentSummary {
  id: string;
  kbId: string;
  docName: string;
  docType: string;
  fileExt?: string;
  fileSize?: number;
  fileHash?: string;
  parseStatus: string;
  parseError?: string;
  processStage?: string;
  processStageLabel?: string;
  progressPercent?: number;
  lastMessage?: string;
  embeddingFallbackUsed?: boolean;
  embeddingApi?: string;
  embeddingModelCode?: string;
  embeddingModelName?: string;
  embeddingDimension?: number;
  milvusSynced?: boolean;
  processLogs?: string[];
  chunkCount: number;
  embeddingCount: number;
  uploadedAt?: string;
}

export interface KnowledgeChunkSummary {
  id: string;
  documentId: string;
  chunkNo: number;
  title?: string;
  content: string;
  tokenCount: number;
  status: string;
  syncStatus?: string;
  createdAt?: string;
}

export interface KnowledgeBaseDetail extends KnowledgeBaseSummary {
  documents: KnowledgeDocumentSummary[];
  chunks: KnowledgeChunkSummary[];
}

export interface KnowledgeBaseRequest {
  kbCode?: string;
  kbName: string;
  description?: string;
  embeddingModelId?: string;
  chunkStrategy?: string;
  chunkSize?: number;
  chunkOverlap?: number;
  visibility?: string;
  status?: string;
}

export interface KnowledgeUploadResult {
  document: KnowledgeDocumentSummary;
  chunkCount: number;
  embeddingCount: number;
  milvusSynced: boolean;
  message: string;
}

export interface KnowledgeRetrievalResult {
  retrievalLogId: string;
  sources: KnowledgeSource[];
  latencyMs: number;
}

export interface AgentKnowledgeBindingSummary {
  agentId: string;
  knowledgeBaseId: string;
  kbName: string;
  retrievalConfig: string;
  enabled: boolean;
  createdAt?: string;
}

export async function fetchKnowledgeBases() {
  return request<KnowledgeBaseSummary[]>('/knowledge-bases');
}

export async function fetchKnowledgeBase(id: string) {
  return request<KnowledgeBaseDetail>(`/knowledge-bases/${id}`);
}

export async function createKnowledgeBase(payload: KnowledgeBaseRequest) {
  return request<KnowledgeBaseDetail>('/knowledge-bases', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateKnowledgeBase(id: string, payload: KnowledgeBaseRequest) {
  return request<KnowledgeBaseDetail>(`/knowledge-bases/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteKnowledgeBase(id: string) {
  return request<void>(`/knowledge-bases/${id}`, { method: 'DELETE' });
}

export async function uploadKnowledgeDocument(id: string, file: File) {
  const form = new FormData();
  form.append('file', file);
  const headers = new Headers();
  const token = getAccessToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  const response = await fetch(`${API_BASE_URL}/knowledge-bases/${id}/documents`, {
    method: 'POST',
    headers,
    body: form,
  });
  const body = await response.json();
  if (!response.ok || !body.success) {
    throw new Error(body.message || '文档上传失败');
  }
  return body.data as KnowledgeUploadResult;
}

export async function fetchKnowledgeDocumentStatus(kbId: string, documentId: string) {
  return request<KnowledgeDocumentSummary>(`/knowledge-bases/${kbId}/documents/${documentId}`);
}

export async function retrievalTest(id: string, query: string, topK = 5, scoreThreshold = 0.65) {
  return request<KnowledgeRetrievalResult>(`/knowledge-bases/${id}/retrieval-test`, {
    method: 'POST',
    body: JSON.stringify({ query, topK, scoreThreshold }),
  });
}

export async function fetchAgentKnowledgeBindings(agentId: string) {
  return request<AgentKnowledgeBindingSummary[]>(`/agents/${agentId}/knowledge-bases`);
}

export async function saveAgentKnowledgeBindings(agentId: string, knowledgeBaseIds: string[], topK = 5, scoreThreshold = 0.65) {
  return request<AgentKnowledgeBindingSummary[]>(`/agents/${agentId}/knowledge-bases`, {
    method: 'PUT',
    body: JSON.stringify({ knowledgeBaseIds, topK, scoreThreshold }),
  });
}
