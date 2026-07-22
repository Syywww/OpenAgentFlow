import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '../views/LoginView.vue';
import { getAccessToken } from '../api/http';
import { canAccessMenu, firstAllowedPath, menuAccessRules, readCurrentUser } from '../api/permissions';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/dashboard', component: () => import('../views/DashboardView.vue') },
    { path: '/agents', component: () => import('../views/agents/AgentListView.vue') },
    { path: '/agents/:id', component: () => import('../views/agents/AgentDetailView.vue') },
    { path: '/agent-teams', component: () => import('../views/agents/AgentTeamView.vue') },
    { path: '/debug', component: () => import('../views/DebugWorkbenchView.vue') },
    { path: '/knowledge', component: () => import('../views/knowledge/KnowledgeListView.vue') },
    { path: '/knowledge/:id', component: () => import('../views/knowledge/KnowledgeDetailView.vue') },
    { path: '/knowledge-governance', component: () => import('../views/knowledge/KnowledgeGovernanceView.vue') },
    { path: '/tools', component: () => import('../views/tools/ToolListView.vue') },
    { path: '/tools/:id', component: () => import('../views/tools/ToolEditView.vue') },
    { path: '/mcp', component: () => import('../views/mcp/McpServerListView.vue') },
    { path: '/mcp/tools', component: () => import('../views/mcp/McpToolsView.vue') },
    { path: '/workflow', component: () => import('../views/WorkflowDesignerView.vue') },
    { path: '/logs', component: () => import('../views/logs/RunLogListView.vue') },
    { path: '/logs/:id', component: () => import('../views/logs/RunDetailView.vue') },
    { path: '/usage', component: () => import('../views/UsageCenterView.vue') },
    { path: '/ops', component: () => import('../views/OpsMonitorView.vue') },
    { path: '/notifications', component: () => import('../views/NotificationCenterView.vue') },
    { path: '/delivery', component: () => import('../views/DeliveryAcceptanceView.vue') },
    { path: '/model-gateway', component: () => import('../views/ModelGatewayView.vue') },
    { path: '/workspaces', component: () => import('../views/WorkspaceGovernanceView.vue') },
    { path: '/tasks', component: () => import('../views/TaskCenterView.vue') },
    { path: '/governance', component: () => import('../views/GovernanceCenterView.vue') },
    { path: '/prompts', component: () => import('../views/PromptTemplateCenterView.vue') },
    { path: '/memories', component: () => import('../views/MemoryCenterView.vue') },
    { path: '/eval', component: () => import('../views/eval/EvalDatasetView.vue') },
    { path: '/eval/result', component: () => import('../views/eval/EvalResultView.vue') },
    { path: '/eval/result/:id', component: () => import('../views/eval/EvalResultView.vue') },
    { path: '/templates', component: () => import('../views/TemplateGalleryView.vue') },
    { path: '/settings', component: () => import('../views/SettingsView.vue') },
  ],
});

router.beforeEach((to) => {
  if (to.meta.public) {
    return true;
  }
  if (!getAccessToken()) {
    return '/login';
  }
  const currentUser = readCurrentUser();
  if (!currentUser) {
    return '/login';
  }
  const matchedMenu = [...menuAccessRules]
    .sort((a, b) => b.match.length - a.match.length)
    .find((item) => to.path === item.match || to.path.startsWith(`${item.match}/`));
  if (matchedMenu && !canAccessMenu(currentUser, matchedMenu.match)) {
    return firstAllowedPath(menuAccessRules, currentUser);
  }
  return true;
});

export default router;
