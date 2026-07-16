import { request } from './http';

export interface PageResult<T> { records: T[]; total: number; pageNo: number; pageSize: number }
export interface TemplateOverview { publishedCount: number; favoriteCount: number; installedCount: number; pendingReviewCount: number; pendingReportCount: number; runningInstallCount: number }
export interface TemplateSummary {
  id: string; templateCode: string; templateName: string; templateType: string; visibility: string;
  category: string; description?: string; icon?: string; coverUrl?: string; tags: string[];
  authorUserId?: string; authorName?: string; currentVersion?: string; currentVersionId?: string;
  status: string; reviewStatus: string; recommended: boolean; favorite: boolean; installCount: number;
  averageRating: number; ratingCount: number; favoriteCount: number; trendScore: number;
  resourceCounts: Record<string, number>; publishedAt?: string; updatedAt?: string;
}
export interface TemplateVersion {
  id: string; templateId: string; versionNo: string; versionName?: string; changeLog: string;
  compatibilityStatement: string; breakingChange: boolean; status: string; packageHash: string;
  packageSize: number; securityScanResult: Record<string, unknown>; runtimeCheckResult: Record<string, unknown>;
  submittedAt?: string; publishedAt?: string; createdAt?: string;
}
export interface TemplateResource { id: string; resourceType: string; sourceResourceId: string; resourceCode?: string; resourceName: string; contentHash: string; required: boolean; dependencyIds: string[]; sortOrder: number }
export interface TemplateComment { id: string; userId: string; userName: string; parentCommentId?: string; content: string; authorReply: boolean; adminReply: boolean; rating?: number; createdAt?: string }
export interface TemplateAuthorProfile { userId: string; authorName: string; avatarUrl?: string; publishedTemplateCount: number; totalInstallCount: number; totalFavoriteCount: number; averageRating: number; templates: TemplateSummary[] }
export interface TemplateReport { id: string; templateId: string; templateName: string; reporterUserId: string; reporterName: string; reportType: string; reason: string; evidence: string[]; status: string; resolution?: string; handledBy?: string; createdAt?: string; handledAt?: string }
export interface TemplateDetail extends TemplateSummary { workspaceId?: string; licenseCode: string; compatibility?: string; dependencyManifest: Record<string, unknown>; versions: TemplateVersion[]; resources: TemplateResource[]; comments: TemplateComment[]; canReview: boolean; canManage: boolean }
export interface TemplateRequest { templateCode?: string; templateName: string; templateType?: string; visibility?: string; workspaceId?: string; category?: string; description?: string; icon?: string; coverUrl?: string; tags?: string[]; licenseCode?: string; compatibility?: string }
export interface ResourceReference { resourceType: string; resourceId: string; resourceName?: string; required: boolean }
export interface TemplatePublishRequest { versionNo: string; versionName?: string; changeLog: string; compatibilityStatement: string; breakingChange: boolean; entryAgentIds: string[]; entryTeamIds: string[]; includeResources: ResourceReference[]; excludeResources: ResourceReference[]; submitForPublicReview: boolean }
export interface TemplateInstallRequest { templateVersionId?: string; workspaceId: string; namePrefix?: string; modelMapping: Record<string, string>; embeddingModelId?: string; credentialsReady: boolean; idempotencyKey: string }
export interface TemplateInstallSummary { id: string; templateId: string; templateName: string; workspaceId: string; templateVersionId: string; versionNo: string; installTaskId?: string; installStatus: string; progressPercent: number; currentStage?: string; currentMessage?: string; targetAgentId?: string; upgradeAvailable: boolean; latestVersionNo?: string; errorMessage?: string; createdAt?: string; completedAt?: string }
export interface UpgradeConflict { id: string; resourceType: string; targetResourceId?: string; resourceName: string; mergeDecision: string; userChoice?: string; oldHash?: string; localHash?: string; newHash?: string; detail: Record<string, unknown> }

export const fetchTemplateOverview = () => request<TemplateOverview>('/templates/overview');
export function fetchTemplates(params: { category?: string; keyword?: string; sort?: string; favoriteOnly?: boolean; pageNo?: number; pageSize?: number }) {
  const query = new URLSearchParams({ category: params.category || 'all', sort: params.sort || 'recommended', favoriteOnly: String(Boolean(params.favoriteOnly)), pageNo: String(params.pageNo || 1), pageSize: String(params.pageSize || 10) });
  if (params.keyword) query.set('keyword', params.keyword);
  return request<PageResult<TemplateSummary>>(`/templates?${query}`);
}
export const fetchTemplateDetail = (id: string) => request<TemplateDetail>(`/templates/${id}`);
export const fetchTemplateAuthor = (userId: string) => request<TemplateAuthorProfile>(`/templates/authors/${userId}`);
export const toggleTemplateFavorite = (id: string) => request<boolean>(`/templates/${id}/favorite`, { method: 'POST' });
export const installTemplate = (id: string, payload: TemplateInstallRequest) => request<TemplateInstallSummary>(`/templates/${id}/install`, { method: 'POST', body: JSON.stringify(payload) });
export const fetchMyTemplateInstalls = () => request<TemplateInstallSummary[]>('/templates/installs/mine');
export const fetchTemplateInstall = (id: string) => request<TemplateInstallSummary>(`/templates/installs/${id}`);
export const previewTemplateUpgrade = (id: string, versionId: string) => request<UpgradeConflict[]>(`/templates/installs/${id}/upgrade-preview?targetVersionId=${encodeURIComponent(versionId)}`, { method: 'POST' });
export const upgradeTemplate = (id: string, targetVersionId: string, conflictChoices: Record<string, string>) => request<TemplateInstallSummary>(`/templates/installs/${id}/upgrade`, { method: 'POST', body: JSON.stringify({ targetVersionId, conflictChoices }) });
export const uninstallTemplate = (id: string, deleteUnmodifiedResources: boolean) => request<void>(`/templates/installs/${id}/uninstall`, { method: 'POST', body: JSON.stringify({ deleteUnmodifiedResources }) });
export const rateTemplate = (id: string, rating: number, comment: string) => request<void>(`/templates/${id}/rating`, { method: 'PUT', body: JSON.stringify({ rating, comment }) });
export const reportTemplate = (id: string, reportType: string, reason: string) => request<void>(`/templates/${id}/reports`, { method: 'POST', body: JSON.stringify({ reportType, reason, evidence: [] }) });
export const replyTemplateComment = (id: string, commentId: string, content: string) => request<void>(`/templates/${id}/comments/${commentId}/reply`, { method: 'POST', body: JSON.stringify({ content }) });
export const fetchManagedTemplates = () => request<TemplateSummary[]>('/templates/manage/mine');
export const createManagedTemplate = (payload: TemplateRequest) => request<TemplateDetail>('/templates/manage', { method: 'POST', body: JSON.stringify(payload) });
export const updateManagedTemplate = (id: string, payload: TemplateRequest) => request<TemplateDetail>(`/templates/manage/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
export const deleteManagedTemplate = (id: string) => request<void>(`/templates/manage/${id}`, { method: 'DELETE' });
export const analyzeTemplateDependencies = (payload: TemplatePublishRequest) => request<ResourceReference[]>('/templates/manage/dependencies', { method: 'POST', body: JSON.stringify(payload) });
export const publishTemplateVersion = (id: string, payload: TemplatePublishRequest) => request<TemplateVersion>(`/templates/manage/${id}/versions`, { method: 'POST', body: JSON.stringify(payload) });
export const fetchPendingTemplateReviews = () => request<TemplateVersion[]>('/templates/reviews/pending');
export const reviewTemplateVersion = (versionId: string, action: string, comment: string, riskLevel = 'low') => request<TemplateVersion>(`/templates/reviews/${versionId}`, { method: 'POST', body: JSON.stringify({ action, comment, riskLevel }) });
export const operateTemplate = (id: string, payload: { recommended?: boolean; status?: string }) => request<TemplateDetail>(`/templates/operations/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
export const fetchTemplateReports = (status = 'pending') => request<TemplateReport[]>(`/templates/operations/reports?status=${encodeURIComponent(status)}`);
export const resolveTemplateReport = (id: string, payload: { action: 'resolved' | 'rejected'; resolution: string; offlineTemplate: boolean }) => request<TemplateReport>(`/templates/operations/reports/${id}/resolve`, { method: 'POST', body: JSON.stringify(payload) });
