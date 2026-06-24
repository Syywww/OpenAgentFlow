import type { CurrentUser } from './auth';

export interface MenuAccessRule {
  path: string;
  match: string;
  permissions: string[];
}

export const menuAccessRules: MenuAccessRule[] = [
  { path: '/dashboard', match: '/dashboard', permissions: ['dashboard:view'] },
  { path: '/agents', match: '/agents', permissions: ['agent:manage', 'agent:view', 'agent:create', 'agent:update'] },
  { path: '/agent-teams', match: '/agent-teams', permissions: ['agent-team:manage', 'agent-team:view', 'agent-team:run'] },
  { path: '/debug', match: '/debug', permissions: ['debug:use', 'agent:run'] },
  { path: '/knowledge', match: '/knowledge', permissions: ['knowledge:manage', 'knowledge:view', 'knowledge:retrieve'] },
  { path: '/knowledge-governance', match: '/knowledge-governance', permissions: ['knowledge:governance:view', 'knowledge:governance:manage'] },
  { path: '/tools', match: '/tools', permissions: ['tool:manage'] },
  { path: '/mcp', match: '/mcp', permissions: ['mcp:manage'] },
  { path: '/workflow', match: '/workflow', permissions: ['workflow:manage'] },
  { path: '/logs', match: '/logs', permissions: ['trace:view', 'trace:manage', 'runtime:manage'] },
  { path: '/usage', match: '/usage', permissions: ['usage:view', 'usage:export', 'usage:quota:manage'] },
  { path: '/ops', match: '/ops', permissions: ['ops:monitor:view', 'ops:monitor:manage'] },
  { path: '/model-gateway', match: '/model-gateway', permissions: ['model-gateway:manage', 'model:manage'] },
  { path: '/workspaces', match: '/workspaces', permissions: ['workspace:manage', 'workspace:view'] },
  { path: '/tasks', match: '/tasks', permissions: ['async-task:manage', 'async-task:view'] },
  { path: '/governance', match: '/governance', permissions: ['governance:manage', 'governance:view'] },
  { path: '/prompts', match: '/prompts', permissions: ['prompt:manage'] },
  { path: '/eval', match: '/eval', permissions: ['eval:manage'] },
  { path: '/templates', match: '/templates', permissions: ['template:manage'] },
  { path: '/settings', match: '/settings', permissions: ['iam:manage'] },
];

export function readCurrentUser() {
  const raw = localStorage.getItem('oaf_current_user');
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as CurrentUser;
  } catch {
    return null;
  }
}

export function hasFullMenuAccess(user: CurrentUser | null) {
  return Boolean(user?.roles?.some((role) => ['super_admin', 'admin'].includes(role)));
}

export function canAccessMenu(user: CurrentUser | null, match: string) {
  if (hasFullMenuAccess(user)) {
    return true;
  }
  const rule = menuAccessRules.find((item) => item.match === match);
  if (!rule) {
    return false;
  }
  return rule.permissions.some((permission) => user?.permissions?.includes(permission));
}

export function firstAllowedPath(paths: Array<{ path: string; match: string }>, user: CurrentUser | null) {
  return paths.find((item) => canAccessMenu(user, item.match))?.path || '/login';
}
