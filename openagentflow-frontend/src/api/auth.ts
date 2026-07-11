import { clearAccessToken, request, setAccessToken, setActiveWorkspaceId } from './http';

export interface LoginRequest {
  username: string;
  password: string;
  captchaKey: string;
  captcha?: string;
  rememberMe?: boolean;
}

export interface CaptchaResponse {
  captchaKey: string;
  imageBase64: string;
  expireSeconds: number;
}

export interface CurrentUser {
  id: string;
  username: string;
  displayName: string;
  email: string;
  avatarUrl?: string;
  roles: string[];
  permissions: string[];
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  currentUser: CurrentUser;
}

export async function login(payload: LoginRequest) {
  setActiveWorkspaceId(undefined);
  const result = await request<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

  setAccessToken(result.accessToken);
  localStorage.setItem('oaf_current_user', JSON.stringify(result.currentUser));
  // 登录完成后选中默认工作空间，确保生产环境首个租户资源请求就携带可信空间上下文。
  const workspaces = await request<Array<{ id: string; defaultFlag?: boolean }>>('/workspaces');
  const activeWorkspace = workspaces.find((item) => item.defaultFlag) ?? workspaces[0];
  setActiveWorkspaceId(activeWorkspace?.id);
  return result;
}

export async function fetchCaptcha() {
  return request<CaptchaResponse>('/auth/captcha');
}

export async function fetchCurrentUser() {
  return request<CurrentUser>('/auth/me');
}

export async function logout() {
  try {
    await request<void>('/auth/logout', { method: 'POST' });
  } finally {
    clearAccessToken();
  }
}
