import { request } from './http';
import type { PageResult } from './traces';

export interface NotificationItem {
  id: string;
  notificationType: string;
  title: string;
  content: string;
  severity: 'info' | 'warning' | 'critical';
  resourceType?: string;
  resourceId?: string;
  actionUrl?: string;
  payload?: Record<string, unknown>;
  read: boolean;
  archived: boolean;
  createdAt: string;
  expiresAt?: string;
}

export interface NotificationOverview {
  totalCount: number;
  unreadCount: number;
  criticalUnreadCount: number;
  warningUnreadCount: number;
  archivedCount: number;
}

export interface NotificationPreference {
  enabledTypes: string[];
  minSeverity: 'info' | 'warning' | 'critical';
  stationEnabled: boolean;
  emailEnabled: boolean;
  webhookEnabled: boolean;
  quietStart?: string;
  quietEnd?: string;
  digestMode: 'realtime' | 'hourly' | 'daily';
}

function queryString(params: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '' && value !== 'all') query.set(key, String(value));
  });
  return query.toString() ? `?${query.toString()}` : '';
}

export function fetchNotifications(params: Record<string, string | number | undefined> = {}) {
  return request<PageResult<NotificationItem>>(`/notifications${queryString(params)}`);
}

export function fetchNotificationOverview() {
  return request<NotificationOverview>('/notifications/overview');
}

export function markNotificationRead(id: string) {
  return request<void>(`/notifications/${id}/read`, { method: 'PATCH' });
}

export function markNotificationsRead(notificationIds: string[]) {
  return request<void>('/notifications/read', {
    method: 'PATCH',
    body: JSON.stringify({ notificationIds }),
  });
}

export function markAllNotificationsRead() {
  return request<void>('/notifications/read-all', { method: 'PATCH' });
}

export function archiveNotification(id: string) {
  return request<void>(`/notifications/${id}/archive`, { method: 'PATCH' });
}

export function archiveNotifications(notificationIds: string[]) {
  return request<void>('/notifications/archive', {
    method: 'PATCH',
    body: JSON.stringify({ notificationIds }),
  });
}

export function fetchNotificationPreference() {
  return request<NotificationPreference>('/notifications/preference');
}

export function saveNotificationPreference(payload: NotificationPreference) {
  return request<NotificationPreference>('/notifications/preference', {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function notifyNotificationChanged() {
  window.dispatchEvent(new CustomEvent('oaf-notification-changed'));
}
