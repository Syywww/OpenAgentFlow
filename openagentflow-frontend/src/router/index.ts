import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '../views/LoginView.vue';
import DashboardView from '../views/DashboardView.vue';
import AgentListView from '../views/agents/AgentListView.vue';
import AgentDetailView from '../views/agents/AgentDetailView.vue';
import DebugWorkbenchView from '../views/DebugWorkbenchView.vue';
import KnowledgeListView from '../views/knowledge/KnowledgeListView.vue';
import KnowledgeDetailView from '../views/knowledge/KnowledgeDetailView.vue';
import ToolListView from '../views/tools/ToolListView.vue';
import ToolEditView from '../views/tools/ToolEditView.vue';
import McpServerListView from '../views/mcp/McpServerListView.vue';
import McpToolsView from '../views/mcp/McpToolsView.vue';
import WorkflowDesignerView from '../views/WorkflowDesignerView.vue';
import RunLogListView from '../views/logs/RunLogListView.vue';
import RunDetailView from '../views/logs/RunDetailView.vue';
import UsageCenterView from '../views/UsageCenterView.vue';
import EvalDatasetView from '../views/eval/EvalDatasetView.vue';
import EvalResultView from '../views/eval/EvalResultView.vue';
import WorkspaceGovernanceView from '../views/WorkspaceGovernanceView.vue';
import TemplateGalleryView from '../views/TemplateGalleryView.vue';
import SettingsView from '../views/SettingsView.vue';
import { getAccessToken } from '../api/http';

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
    { path: '/tools', component: ToolListView },
    { path: '/tools/:id', component: ToolEditView },
    { path: '/mcp', component: McpServerListView },
    { path: '/mcp/tools', component: McpToolsView },
    { path: '/workflow', component: WorkflowDesignerView },
    { path: '/logs', component: RunLogListView },
    { path: '/logs/:id', component: RunDetailView },
    { path: '/usage', component: UsageCenterView },
    { path: '/workspaces', component: WorkspaceGovernanceView },
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
  return true;
});

export default router;
