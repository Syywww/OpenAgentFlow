import { request } from './http';

export interface DashboardKnowledgeHealth {
  documentCount: number;
  parsedDocumentCount: number;
  failedDocumentCount: number;
  processingDocumentCount: number;
  chunkCount: number;
  embeddingCount: number;
  openIssueCount: number;
  highRiskIssueCount: number;
  unsyncedEmbeddingCount: number;
}

export interface DashboardRunTrendItem {
  statDate: string;
  runCount: number;
  successCount: number;
  failureCount: number;
  tokenCount: number;
  costAmount: number;
}

export interface DashboardRecentRun {
  id: string;
  runNo: string;
  runType: string;
  targetName: string;
  userName: string;
  status: string;
  statusLabel: string;
  totalTokens: number;
  totalCost: number;
  latencyMs: number;
  errorMessage?: string;
  startedAt?: string;
  finishedAt?: string;
}

export interface DashboardModelUsage {
  modelId: string;
  modelName: string;
  providerName: string;
  callCount: number;
  successCount: number;
  failureCount: number;
  totalTokens: number;
  totalCost: number;
  avgLatencyMs: number;
  usagePercent: number;
}

export interface DashboardTaskQueueItem {
  id: string;
  taskName: string;
  taskType: string;
  status: string;
  progressPercent: number;
  currentMessage?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DashboardAlertEvent {
  id: string;
  alertTitle: string;
  severity: string;
  status: string;
  metricSource: string;
  metricValue: number;
  thresholdValue: number;
  lastTriggeredAt?: string;
}

export interface DashboardHealthCheck {
  id: string;
  checkName: string;
  targetType: string;
  targetCode: string;
  status: string;
  message?: string;
  latencyMs: number;
  lastCheckedAt?: string;
}

export interface DashboardInsight {
  title: string;
  content: string;
  tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral' | string;
}

export interface DashboardOverview {
  agentCount: number;
  publishedAgentCount: number;
  knowledgeBaseCount: number;
  toolCount: number;
  enabledToolCount: number;
  mcpServerCount: number;
  workflowCount: number;
  todayRunCount: number;
  todaySuccessCount: number;
  todayFailureCount: number;
  todaySuccessRate: number;
  todayCost: number;
  todayTokenCount: number;
  todayAvgLatencyMs: number;
  taskBacklogCount: number;
  openAlertCount: number;
  unhealthyComponentCount: number;
  knowledgeHealth: DashboardKnowledgeHealth;
  runTrend: DashboardRunTrendItem[];
  recentRuns: DashboardRecentRun[];
  modelUsage: DashboardModelUsage[];
  taskQueue: DashboardTaskQueueItem[];
  openAlerts: DashboardAlertEvent[];
  healthChecks: DashboardHealthCheck[];
  insights: DashboardInsight[];
}

export async function fetchDashboardOverview() {
  return request<DashboardOverview>('/dashboard/overview');
}
