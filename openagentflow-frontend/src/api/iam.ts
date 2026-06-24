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
