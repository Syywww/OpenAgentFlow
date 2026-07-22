import type { ObjectDirective } from 'vue';
import type { CurrentUser } from '../api/auth';

/** 平台级角色始终可以执行受保护操作。 */
const platformRoles = new Set(['super_admin', 'admin']);

/**
 * 判断用户是否拥有任意一个要求权限。
 */
export function canAccess(user: CurrentUser | null, required: string | string[]) {
  if (!user) return false;
  if (user.roles?.some((role) => platformRoles.has(role))) return true;
  const permissions = Array.isArray(required) ? required : [required];
  return permissions.length === 0 || permissions.some((permission) => user.permissions?.includes(permission));
}

/** 从本地登录快照读取当前用户，服务端仍负责最终授权。 */
export function currentPermissionUser() {
  const raw = localStorage.getItem('oaf_current_user');
  if (!raw) return null;
  try {
    return JSON.parse(raw) as CurrentUser;
  } catch {
    return null;
  }
}

/** 根据权限切换元素显示状态。 */
function applyElementPermission(element: HTMLElement, required: string | string[]) {
  const originalDisplay = element.dataset.permissionDisplay ?? element.style.display;
  element.dataset.permissionDisplay = originalDisplay;
  element.style.display = canAccess(currentPermissionUser(), required) ? originalDisplay : 'none';
}

/**
 * Vue按钮级权限指令，用法：v-permission="['iam:manage','iam:acl:manage']"。
 */
export const permissionDirective: ObjectDirective<HTMLElement, string | string[]> = {
  mounted(element, binding) {
    applyElementPermission(element, binding.value);
  },
  updated(element, binding) {
    applyElementPermission(element, binding.value);
  },
};
