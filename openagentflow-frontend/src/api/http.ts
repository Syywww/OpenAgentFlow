export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
}

export function getAccessToken() {
  return localStorage.getItem('oaf_access_token');
}

export function setAccessToken(token: string) {
  localStorage.setItem('oaf_access_token', token);
}

export function clearAccessToken() {
  localStorage.removeItem('oaf_access_token');
  localStorage.removeItem('oaf_current_user');
}

export async function request<T>(path: string, options: RequestInit = {}) {
  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');

  const token = getAccessToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });
  const body = (await response.json()) as ApiResponse<T>;

  if (!response.ok || !body.success) {
    throw new Error(body.message || '请求失败');
  }
  return body.data;
}
