import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '../views/LoginView.vue';
import DashboardView from '../views/DashboardView.vue';
import AgentListView from '../views/agents/AgentListView.vue';
import AgentDetailView from '../views/agents/AgentDetailView.vue';
import DebugWorkbenchView from '../views/DebugWorkbenchView.vue';
import KnowledgeListView from '../views/knowledge/KnowledgeListView.vue';
import KnowledgeDetailView from '../views/knowledge/KnowledgeDetailView.vue';
import KnowledgeGovernanceView from '../views/knowledge/KnowledgeGovernanceView.vue';
import ToolListView from '../views/tools/ToolListView.vue';
import ToolEditView from '../views/tools/ToolEditView.vue';
import McpServerListView from '../views/mcp/McpServerListView.vue';
import McpToolsView from '../views/mcp/McpToolsView.vue';
import WorkflowDesignerView from '../views/WorkflowDesignerView.vue';
import RunLogListView from '../views/logs/RunLogListView.vue';
import RunDetailView from '../views/logs/RunDetailView.vue';
import UsageCenterView from '../views/UsageCenterView.vue';
import OpsMonitorView from '../views/OpsMonitorView.vue';
import ModelGatewayView from '../views/ModelGatewayView.vue';
import EvalDatasetView from '../views/eval/EvalDatasetView.vue';
import EvalResultView from '../views/eval/EvalResultView.vue';
import WorkspaceGovernanceView from '../views/WorkspaceGovernanceView.vue';
import TaskCenterView from '../views/TaskCenterView.vue';
import GovernanceCenterView from '../views/GovernanceCenterView.vue';
import PromptTemplateCenterView from '../views/PromptTemplateCenterView.vue';
import TemplateGalleryView from '../views/TemplateGalleryView.vue';
import SettingsView from '../views/SettingsView.vue';
import { getAccessToken } from '../api/http';
import { canAccessMenu, firstAllowedPath, menuAccessRules, readCurrentUser } from '../api/permissions';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/dashboard', component: DashboardView },
    { path: '/agents', component: AgentListView },
    { path: '/agents/:id', component: AgentDetailView },
    { path: '/debug', component: DebugWorkbenchView },
    { path: '/knowledge', component: KnowledgeListView },
    { path: '/knowledge/:id', component: KnowledgeDetailView },
    { path: '/knowledge-governance', component: KnowledgeGovernanceView },
    { path: '/tools', component: ToolListView },
    { path: '/tools/:id', component: ToolEditView },
    { path: '/mcp', component: McpServerListView },
    { path: '/mcp/tools', component: McpToolsView },
    { path: '/workflow', component: WorkflowDesignerView },
    { path: '/logs', component: RunLogListView },
    { path: '/logs/:id', component: RunDetailView },
    { path: '/usage', component: UsageCenterView },
    { path: '/ops', component: OpsMonitorView },
    { path: '/model-gateway', component: ModelGatewayView },
    { path: '/workspaces', component: WorkspaceGovernanceView },
    { path: '/tasks', component: TaskCenterView },
    { path: '/governance', component: GovernanceCenterView },
    { path: '/prompts', component: PromptTemplateCenterView },
    { path: '/eval', component: EvalDatasetView },
    { path: '/eval/result', component: EvalResultView },
    { path: '/eval/result/:id', component: EvalResultView },
    { path: '/templates', component: TemplateGalleryView },
    { path: '/settings', component: SettingsView },
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
