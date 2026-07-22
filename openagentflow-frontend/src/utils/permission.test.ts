import { describe, expect, it } from 'vitest';
import { canAccess } from './permission';

const normalUser = {
  id: 'u-1',
  username: 'developer',
  displayName: '开发者',
  email: '',
  roles: ['developer'],
  permissions: ['agent:view', 'agent:run'],
};

describe('按钮权限策略', () => {
  it('拥有任意要求权限时允许访问', () => {
    expect(canAccess(normalUser, ['agent:manage', 'agent:run'])).toBe(true);
  });

  it('缺少全部要求权限时拒绝访问', () => {
    expect(canAccess(normalUser, ['iam:manage', 'iam:acl:manage'])).toBe(false);
  });

  it('平台管理员始终允许访问', () => {
    expect(canAccess({ ...normalUser, roles: ['super_admin'], permissions: [] }, ['iam:manage'])).toBe(true);
  });

  it('未登录用户不能访问受保护操作', () => {
    expect(canAccess(null, ['agent:view'])).toBe(false);
  });
});
