import { request } from './http';
import type { PageResult } from './traces';

export interface DeliveryManifest {
  appName: string;
  backendVersion: string;
  frontendVersion: string;
  javaVersion: string;
  databaseName: string;
  latestSqlVersion: string;
  dataComponents: Record<string, unknown>;
  moduleCounts: Record<string, unknown>;
}

export interface DeliveryOverview {
  overallStatus: string;
  score: number;
  passedCount: number;
  warningCount: number;
  failedCount: number;
  moduleCount: number;
  componentCount: number;
  latestReportAt?: string;
  latestReportCode?: string;
  metrics: Record<string, unknown>;
  manifest: DeliveryManifest;
}

export interface DeliveryCheckItem {
  checkCode: string;
  checkName: string;
  category: string;
  status: string;
  message: string;
  suggestion: string;
  blocking: boolean;
  actualValue?: unknown;
  expectedValue?: unknown;
  detail?: Record<string, unknown>;
}

export interface DeliveryRiskItem {
  riskLevel: string;
  title: string;
  description: string;
  suggestion: string;
  sourceCheckCode: string;
}

export interface DeliveryReportSummary {
  id: string;
  reportCode: string;
  reportName: string;
  overallStatus: string;
  score: number;
  passedCount: number;
  warningCount: number;
  failedCount: number;
  createdAt?: string;
}

export interface DeliveryReportDetail extends DeliveryReportSummary {
  overview: DeliveryOverview;
  checks: DeliveryCheckItem[];
  risks: DeliveryRiskItem[];
  manifest: DeliveryManifest;
}

export async function fetchDeliveryOverview() {
  return request<DeliveryOverview>('/delivery-acceptance/overview');
}

export async function fetchDeliveryChecks() {
  return request<DeliveryCheckItem[]>('/delivery-acceptance/checks');
}

export async function runDeliveryAcceptance() {
  return request<DeliveryReportDetail>('/delivery-acceptance/run', { method: 'POST' });
}

export async function fetchDeliveryReports(pageNo = 1, pageSize = 10) {
  const query = new URLSearchParams({ pageNo: String(pageNo), pageSize: String(pageSize) });
  return request<PageResult<DeliveryReportSummary>>(`/delivery-acceptance/reports?${query}`);
}

export async function fetchDeliveryReport(id: string) {
  return request<DeliveryReportDetail>(`/delivery-acceptance/reports/${id}`);
}
