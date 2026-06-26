<script setup lang="ts">
import type { Component } from 'vue';
import { computed } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import {
  Activity,
  Bell,
  Bot,
  Boxes,
  BrainCircuit,
  Braces,
  Building2,
  ChevronLeft,
  CircleHelp,
  ClipboardList,
  Coins,
  Database,
  GalleryVerticalEnd,
  GitBranch,
  Gauge,
  LayoutDashboard,
  Library,
  LogOut,
  MessageSquareText,
  Search,
  Server,
  Settings,
  ShieldAlert,
  ShieldCheck,
  TestTube2,
  UsersRound,
} from 'lucide-vue-next';
import { logout as logoutApi } from '../api/auth';
import { canAccessMenu, readCurrentUser } from '../api/permissions';
import { useOverlay } from '../composables/useOverlay';

const route = useRoute();
const router = useRouter();
const { showDrawer } = useOverlay();
const currentUser = computed(() => readCurrentUser());

const navigation: Array<{ path: string; match: string; label: string; icon: Component }> = [
  { path: '/dashboard', match: '/dashboard', label: '工作台', icon: LayoutDashboard },
  { path: '/agents', match: '/agents', label: '智能体', icon: Bot },
  { path: '/agent-teams', match: '/agent-teams', label: '多 Agent', icon: UsersRound },
  { path: '/debug', match: '/debug', label: '调试台', icon: TestTube2 },
  { path: '/knowledge', match: '/knowledge', label: '知识库', icon: Library },
  { path: '/knowledge-governance', match: '/knowledge-governance', label: '知识治理', icon: ShieldCheck },
  { path: '/tools', match: '/tools', label: '工具中心', icon: Braces },
  { path: '/mcp', match: '/mcp', label: 'MCP', icon: Boxes },
  { path: '/workflow', match: '/workflow', label: '工作流', icon: GitBranch },
  { path: '/logs', match: '/logs', label: '运行日志', icon: Activity },
  { path: '/usage', match: '/usage', label: '用量中心', icon: Coins },
  { path: '/ops', match: '/ops', label: '运营监控', icon: Gauge },
  { path: '/delivery', match: '/delivery', label: '交付验收', icon: ClipboardList },
  { path: '/model-gateway', match: '/model-gateway', label: '模型网关', icon: Server },
  { path: '/workspaces', match: '/workspaces', label: '组织空间', icon: Building2 },
  { path: '/tasks', match: '/tasks', label: '任务中心', icon: ClipboardList },
  { path: '/governance', match: '/governance', label: '风险治理', icon: ShieldAlert },
  { path: '/prompts', match: '/prompts', label: 'Prompt 模板', icon: MessageSquareText },
  { path: '/memories', match: '/memories', label: 'Memory', icon: BrainCircuit },
  { path: '/eval', match: '/eval', label: '评测中心', icon: Database },
  { path: '/templates', match: '/templates', label: '模板广场', icon: GalleryVerticalEnd },
  { path: '/settings', match: '/settings', label: '系统设置', icon: Settings },
];

const activePath = computed(() => route.path);
const visibleNavigation = computed(() => navigation.filter((item) => canAccessMenu(currentUser.value, item.match)));

function isActive(match: string) {
  return activePath.value === match || activePath.value.startsWith(`${match}/`);
}

async function handleLogout() {
  try {
    await logoutApi();
  } catch {
    // 后端 token 已过期或网络异常时，前端也应立即退出到登录页。
  }
  await router.replace('/login');
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand-mark">
        <div class="brand-logo"><Bot :size="20" /></div>
        <div>
          <b>OpenAgentFlow</b>
          <span>Java Edition</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <RouterLink
          v-for="item in visibleNavigation"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.match) }"
        >
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <button class="nav-collapse" type="button" title="收起侧边栏">
        <ChevronLeft :size="18" />
        <span>收起</span>
      </button>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <label class="global-search">
          <Search :size="18" />
          <input placeholder="搜索 Agent、知识库、工作流、运行日志" />
        </label>

        <div class="topbar-actions">
          <button class="icon-button" type="button" title="通知中心" @click="showDrawer('notices')">
            <Bell :size="18" />
          </button>
          <button class="icon-button" type="button" title="帮助">
            <CircleHelp :size="18" />
          </button>
          <div class="user-chip">
            <span>{{ (currentUser?.displayName || currentUser?.username || 'A').slice(0, 1).toUpperCase() }}</span>
            <b>{{ currentUser?.displayName || currentUser?.username || 'admin' }}</b>
          </div>
          <button class="icon-button danger" type="button" title="退出登录" @click="handleLogout">
            <LogOut :size="18" />
          </button>
        </div>
      </header>

      <section class="page-surface">
        <slot />
      </section>
    </main>
  </div>
</template>
