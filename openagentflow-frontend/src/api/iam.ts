import { request } from './http';

export interface IamOverview {
  userCount: number;
  departmentCount: number;
  roleCount: number;
  permissionCount: number;
}

export interface UserSummary {
  id: string;
  departmentId?: string;
  departmentName?: string;
  username: string;
  email?: string;
  phone?: string;
  displayName: string;
  status: string;
  sourceType: string;
  lastLoginAt?: string;
  createdAt?: string;
  roleIds: string[];
  roleCodes: string[];
  roleNames: string[];
}

export interface UserRequest {
  departmentId?: string;
  username: string;
  email?: string;
  phone?: string;
  password?: string;
  displayName: string;
  status: string;
  roleIds: string[];
}

export interface DepartmentNode {
  id: string;
  parentId?: string;
  deptCode: string;
  deptName: string;
  sortOrder: number;
  status: string;
  userCount: number;
  children: DepartmentNode[];
}

export interface DepartmentRequest {
  parentId?: string;
  deptCode: string;
  deptName: string;
  sortOrder: number;
  status: string;
}

export interface RoleSummary {
  id: string;
  roleCode: string;
  roleName: string;
  description?: string;
  builtIn: boolean;
  status: string;
  permissionIds: string[];
  permissionCodes: string[];
  userCount: number;
}

export interface RoleRequest {
  roleCode: string;
  roleName: string;
  description?: string;
  status: string;
  permissionIds: string[];
}

export interface PermissionNode {
  id: string;
  parentId?: string;
  permissionCode: string;
  permissionName: string;
  permissionType: string;
  routePath?: string;
  apiMethod?: string;
  apiPath?: string;
  sortOrder: number;
  visible: boolean;
  status: string;
  children: PermissionNode[];
}

export interface PermissionGovernanceOverview {
  workspaceRoleCount: number;
  memberRoleBindingCount: number;
  activeAclCount: number;
  authorizationAuditCount: number;
}

export interface WorkspaceRoleSummary {
  id: string;
  workspaceId: string;
  roleCode: string;
  roleName: string;
  description?: string;
  dataScope: 'all' | 'dept' | 'dept_tree' | 'self' | 'custom';
  builtIn: boolean;
  status: string;
  permissionIds: string[];
  permissionCodes: string[];
  departmentIds: string[];
  memberCount: number;
}

export interface WorkspaceRoleRequest {
  workspaceId: string;
  roleCode: string;
  roleName: string;
  description?: string;
  dataScope: WorkspaceRoleSummary['dataScope'];
  status: string;
  permissionIds: string[];
  departmentIds: string[];
}

export interface ResourceAclSummary {
  id: string;
  workspaceId: string;
  resourceType: string;
  resourceId: string;
  subjectType: string;
  subjectId: string;
  permissionLevel: string;
  status: string;
  expiresAt?: string;
  grantReason?: string;
  grantedBy?: string;
  createdAt?: string;
}

export interface ResourceAclRequest {
  workspaceId: string;
  resourceType: string;
  resourceId: string;
  subjectType: 'user' | 'role' | 'department';
  subjectId: string;
  permissionLevel: 'read' | 'run' | 'write' | 'owner';
  expiresAt?: string;
  reason?: string;
}

export interface AuthorizationAuditSummary {
  id: string;
  workspaceId?: string;
  operatorUserId?: string;
  actionType: string;
  targetType: string;
  targetId: string;
  subjectType?: string;
  subjectId?: string;
  reason?: string;
  createdAt?: string;
}

export async function fetchIamOverview() {
  return request<IamOverview>('/iam-admin/overview');
}

export async function fetchIamUsers() {
  return request<UserSummary[]>('/iam-admin/users');
}

export async function createIamUser(payload: UserRequest) {
  return request<UserSummary>('/iam-admin/users', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateIamUser(id: string, payload: UserRequest) {
  return request<UserSummary>(`/iam-admin/users/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteIamUser(id: string) {
  return request<void>(`/iam-admin/users/${id}`, { method: 'DELETE' });
}

export async function fetchIamDepartments() {
  return request<DepartmentNode[]>('/iam-admin/departments');
}

export async function createIamDepartment(payload: DepartmentRequest) {
  return request<DepartmentNode[]>('/iam-admin/departments', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateIamDepartment(id: string, payload: DepartmentRequest) {
  return request<DepartmentNode[]>(`/iam-admin/departments/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteIamDepartment(id: string) {
  return request<void>(`/iam-admin/departments/${id}`, { method: 'DELETE' });
}

export async function fetchIamRoles() {
  return request<RoleSummary[]>('/iam-admin/roles');
}

export async function createIamRole(payload: RoleRequest) {
  return request<RoleSummary>('/iam-admin/roles', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateIamRole(id: string, payload: RoleRequest) {
  return request<RoleSummary>(`/iam-admin/roles/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteIamRole(id: string) {
  return request<void>(`/iam-admin/roles/${id}`, { method: 'DELETE' });
}

export async function updateIamRolePermissions(id: string, permissionIds: string[]) {
  return request<RoleSummary>(`/iam-admin/roles/${id}/permissions`, {
    method: 'PUT',
    body: JSON.stringify({ permissionIds }),
  });
}

export async function fetchIamPermissions() {
  return request<PermissionNode[]>('/iam-admin/permissions');
}

export function fetchPermissionGovernanceOverview(workspaceId: string) {
  return request<PermissionGovernanceOverview>(`/iam-admin/governance/overview?workspaceId=${encodeURIComponent(workspaceId)}`);
}

export function fetchWorkspaceRoles(workspaceId: string) {
  return request<WorkspaceRoleSummary[]>(`/iam-admin/governance/workspace-roles?workspaceId=${encodeURIComponent(workspaceId)}`);
}

export function createWorkspaceRole(payload: WorkspaceRoleRequest) {
  return request<WorkspaceRoleSummary>('/iam-admin/governance/workspace-roles', { method: 'POST', body: JSON.stringify(payload) });
}

export function updateWorkspaceRole(id: string, payload: WorkspaceRoleRequest) {
  return request<WorkspaceRoleSummary>(`/iam-admin/governance/workspace-roles/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function deleteWorkspaceRole(id: string, workspaceId: string) {
  return request<void>(`/iam-admin/governance/workspace-roles/${id}?workspaceId=${encodeURIComponent(workspaceId)}`, { method: 'DELETE' });
}

export function assignWorkspaceMemberRoles(workspaceId: string, userId: string, roleIds: string[], reason?: string) {
  return request<void>(`/iam-admin/governance/workspaces/${workspaceId}/members/${userId}/roles`, {
    method: 'PUT', body: JSON.stringify({ roleIds, reason }),
  });
}

export function fetchWorkspaceMemberRoleIds(workspaceId: string, userId: string) {
  return request<string[]>(`/iam-admin/governance/workspaces/${workspaceId}/members/${userId}/roles`);
}

export function fetchResourceAcls(workspaceId: string) {
  return request<ResourceAclSummary[]>(`/iam-admin/resource-acls?workspaceId=${encodeURIComponent(workspaceId)}`);
}

export function grantResourceAcl(payload: ResourceAclRequest) {
  return request<ResourceAclSummary>('/iam-admin/resource-acls', { method: 'POST', body: JSON.stringify(payload) });
}

export function revokeResourceAcl(id: string, workspaceId: string, reason?: string) {
  const suffix = reason ? `&reason=${encodeURIComponent(reason)}` : '';
  return request<void>(`/iam-admin/resource-acls/${id}?workspaceId=${encodeURIComponent(workspaceId)}${suffix}`, { method: 'DELETE' });
}

export function fetchAuthorizationAudits(workspaceId: string) {
  return request<AuthorizationAuditSummary[]>(`/iam-admin/governance/audits?workspaceId=${encodeURIComponent(workspaceId)}`);
}

export function revokeUserSessions(userId: string, reason?: string) {
  return request<number>(`/iam-admin/users/${userId}/revoke-sessions`, { method: 'POST', body: JSON.stringify({ reason }) });
}
