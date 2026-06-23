import { request, setAccessToken } from './http';

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
  const result = await request<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

  setAccessToken(result.accessToken);
  localStorage.setItem('oaf_current_user', JSON.stringify(result.currentUser));
  return result;
}

export async function fetchCaptcha() {
  return request<CaptchaResponse>('/auth/captcha');
}

export async function fetchCurrentUser() {
  return request<CurrentUser>('/auth/me');
}
