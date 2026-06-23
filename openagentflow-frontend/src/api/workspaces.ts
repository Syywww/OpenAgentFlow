import { request } from './http';

export interface OrganizationSummary {
  id: string;
  orgCode: string;
  orgName: string;
  description?: string;
  status: string;
  memberCount: number;
  workspaceCount: number;
  canManage: boolean;
  createdAt?: string;
}

export interface WorkspaceSummary {
  id: string;
  organizationId: string;
  organizationName?: string;
  workspaceCode: string;
  workspaceName: string;
  description?: string;
  workspaceType: string;
  memberCount: number;
  agentCount: number;
  knowledgeBaseCount: number;
  toolCount: number;
  workflowCount: number;
  defaultFlag: boolean;
  currentUserRole?: string;
  canManage: boolean;
  createdAt?: string;
}

export interface WorkspaceMember {
  id: string;
  userId: string;
  username?: string;
  displayName?: string;
  memberRole: string;
  status: string;
  joinedAt?: string;
}

export interface WorkspaceDetail extends WorkspaceSummary {
  members: WorkspaceMember[];
}

export interface OrganizationRequest {
  orgCode?: string;
  orgName: string;
  description?: string;
}

export interface WorkspaceRequest {
  organizationId?: string;
  workspaceCode?: string;
  workspaceName: string;
  description?: string;
  workspaceType?: string;
  defaultFlag?: boolean;
}

export interface MemberRequest {
  userId: string;
  memberRole?: string;
}

export function fetchOrganizations() {
  return request<OrganizationSummary[]>('/organizations');
}

export function createOrganization(payload: OrganizationRequest) {
  return request<OrganizationSummary>('/organizations', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function fetchWorkspaces() {
  return request<WorkspaceSummary[]>('/workspaces');
}

export function createWorkspace(payload: WorkspaceRequest) {
  return request<WorkspaceDetail>('/workspaces', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateWorkspace(id: string, payload: WorkspaceRequest) {
  return request<WorkspaceDetail>(`/workspaces/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function fetchWorkspace(id: string) {
  return request<WorkspaceDetail>(`/workspaces/${id}`);
}

export function saveWorkspaceMember(id: string, payload: MemberRequest) {
  return request<WorkspaceDetail>(`/workspaces/${id}/members`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function removeWorkspaceMember(id: string, userId: string) {
  return request<WorkspaceDetail>(`/workspaces/${id}/members/${userId}`, {
    method: 'DELETE',
  });
}
