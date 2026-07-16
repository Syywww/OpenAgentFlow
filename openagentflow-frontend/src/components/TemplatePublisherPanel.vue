<script setup lang="ts">
import { reactive, ref } from 'vue';
import { Check, GitBranch, Plus, ScanSearch, Send, Trash2, X } from 'lucide-vue-next';
import type { AgentSummary } from '../api/agents';
import type { AgentTeamSummary } from '../api/agentTeams';
import type { ResourceReference, TemplatePublishRequest, TemplateRequest, TemplateSummary, TemplateVersion } from '../api/templates';
import type { WorkspaceSummary } from '../api/workspaces';
import StatusBadge from './StatusBadge.vue';

const props = defineProps<{
  templates: TemplateSummary[];
  agents: AgentSummary[];
  teams: AgentTeamSummary[];
  workspaces: WorkspaceSummary[];
  pendingReviews: TemplateVersion[];
  loading: boolean;
}>();

const emit = defineEmits<{
  create: [request: TemplateRequest];
  analyze: [request: TemplatePublishRequest];
  publish: [templateId: string, request: TemplatePublishRequest];
  review: [versionId: string, action: string, comment: string];
}>();

const createOpen = ref(false);
const publishOpen = ref(false);
const selectedTemplateId = ref('');
const analyzedResources = ref<ResourceReference[]>([]);
const createForm = reactive<TemplateRequest>({ templateName: '', templateType: 'solution', workspaceId: '', category: '知识管理', description: '', icon: 'Blocks', tags: [], licenseCode: 'Apache-2.0', compatibility: 'OpenAgentFlow-Java 0.1+，Java 21，MySQL 8，Redis 7，Milvus 2.4' });
const publishForm = reactive<TemplatePublishRequest>({ versionNo: '1.0.0', versionName: '首个版本', changeLog: '', compatibilityStatement: '向后兼容当前平台版本', breakingChange: false, entryAgentIds: [], entryTeamIds: [], includeResources: [], excludeResources: [], submitForPublicReview: true });
const manualResource = reactive({ resourceType: 'tool', resourceId: '', resourceName: '' });

function openCreate() {
  createForm.templateName = '';
  createForm.workspaceId = props.workspaces.find((item) => item.defaultFlag)?.id || props.workspaces[0]?.id || '';
  createOpen.value = true;
}

function openPublish(template: TemplateSummary) {
  selectedTemplateId.value = template.id;
  publishForm.versionNo = template.currentVersion ? nextPatch(template.currentVersion) : '1.0.0';
  publishForm.versionName = template.currentVersion ? '功能更新' : '首个版本';
  publishForm.changeLog = '';
  publishForm.entryAgentIds = [];
  publishForm.entryTeamIds = [];
  publishForm.includeResources = [];
  publishForm.excludeResources = [];
  analyzedResources.value = [];
  publishOpen.value = true;
}

function nextPatch(version: string) {
  const parts = version.split('.').map(Number);
  return parts.length === 3 && parts.every(Number.isFinite) ? `${parts[0]}.${parts[1]}.${parts[2] + 1}` : '1.0.0';
}

function analyze() {
  emit('analyze', { ...publishForm, entryAgentIds: [...publishForm.entryAgentIds], entryTeamIds: [...publishForm.entryTeamIds] });
}

function setAnalyzed(resources: ResourceReference[]) {
  analyzedResources.value = resources;
}

function removeResource(resource: ResourceReference) {
  if (!publishForm.excludeResources.some((item) => item.resourceType === resource.resourceType && item.resourceId === resource.resourceId)) {
    publishForm.excludeResources.push({ ...resource });
  }
  publishForm.includeResources = publishForm.includeResources.filter((item) => item.resourceType !== resource.resourceType || item.resourceId !== resource.resourceId);
  analyzedResources.value = analyzedResources.value.filter((item) => item.resourceType !== resource.resourceType || item.resourceId !== resource.resourceId);
}

function addManualResource() {
  if (!manualResource.resourceId.trim()) return;
  const resource: ResourceReference = { resourceType: manualResource.resourceType, resourceId: manualResource.resourceId.trim(), resourceName: manualResource.resourceName.trim() || manualResource.resourceId.trim(), required: true };
  publishForm.excludeResources = publishForm.excludeResources.filter((item) => item.resourceType !== resource.resourceType || item.resourceId !== resource.resourceId);
  if (!publishForm.includeResources.some((item) => item.resourceType === resource.resourceType && item.resourceId === resource.resourceId)) publishForm.includeResources.push(resource);
  if (!analyzedResources.value.some((item) => item.resourceType === resource.resourceType && item.resourceId === resource.resourceId)) analyzedResources.value.push(resource);
  manualResource.resourceId = '';
  manualResource.resourceName = '';
}

defineExpose({ setAnalyzed });
</script>

<template>
  <div class="section-title">
    <h2>我的解决方案模板</h2>
    <button class="primary-button slim" type="button" @click="openCreate"><Plus :size="15" /> 新建模板</button>
  </div>
  <table class="data-table">
    <thead><tr><th>模板</th><th>范围</th><th>版本</th><th>审核</th><th>状态</th><th>操作</th></tr></thead>
    <tbody><tr v-for="item in templates" :key="item.id"><td><b>{{ item.templateName }}</b><span class="muted block mono">{{ item.templateCode }}</span></td><td>{{ item.visibility === 'public' ? '公开' : '工作空间' }}</td><td>{{ item.currentVersion || '尚未发布' }}</td><td><StatusBadge :label="item.reviewStatus" :tone="item.reviewStatus === 'approved' ? 'success' : item.reviewStatus === 'rejected' ? 'danger' : 'warning'" /></td><td>{{ item.status }}</td><td><button class="secondary-button slim" type="button" @click="openPublish(item)"><GitBranch :size="14" /> 发布版本</button></td></tr></tbody>
  </table>
  <div v-if="templates.length === 0" class="empty-state">暂无私有模板，先从工作空间资源创建一个解决方案包</div>

  <div class="section-title compact-title"><h2>待人工审核</h2><span>{{ pendingReviews.length }} 个版本</span></div>
  <table class="data-table"><thead><tr><th>版本</th><th>自动安全检查</th><th>运行检查</th><th>提交时间</th><th>操作</th></tr></thead><tbody><tr v-for="version in pendingReviews" :key="version.id"><td><b>{{ version.versionNo }}</b><span class="muted block">{{ version.versionName }}</span></td><td><StatusBadge :label="version.securityScanResult.passed ? '通过' : '阻断'" :tone="version.securityScanResult.passed ? 'success' : 'danger'" /></td><td><StatusBadge :label="version.runtimeCheckResult.passed ? '通过' : '阻断'" :tone="version.runtimeCheckResult.passed ? 'success' : 'danger'" /></td><td>{{ version.submittedAt?.replace('T', ' ').slice(0, 19) || '-' }}</td><td><div class="table-actions"><button class="primary-button slim" type="button" @click="emit('review', version.id, 'approve', '人工审核通过')"><Check :size="14" /> 通过</button><button class="secondary-button slim danger-text" type="button" @click="emit('review', version.id, 'reject', '人工审核驳回')">驳回</button></div></td></tr></tbody></table>

  <div v-if="createOpen" class="overlay-backdrop" @click.self="createOpen = false">
    <section class="modal-panel compact"><header class="overlay-header"><div><h2>新建解决方案模板</h2><p class="muted">先创建工作空间私有模板，再发布不可变版本。</p></div><button class="icon-button" type="button" title="关闭" @click="createOpen = false"><X :size="18" /></button></header><div class="form-grid"><label>模板名称<input v-model="createForm.templateName" /></label><label>工作空间<select v-model="createForm.workspaceId"><option v-for="workspace in workspaces.filter((item) => item.canManage)" :key="workspace.id" :value="workspace.id">{{ workspace.workspaceName }}</option></select></label><label>分类<select v-model="createForm.category"><option>知识管理</option><option>办公助手</option><option>数据分析</option><option>客服服务</option><option>开发运维</option><option>其他</option></select></label><label>许可证<input v-model="createForm.licenseCode" disabled /></label><label class="wide">模板描述<textarea v-model="createForm.description" rows="3" /></label><label class="wide">兼容性声明<textarea v-model="createForm.compatibility" rows="2" /></label></div><div class="toolbar compact"><button class="secondary-button" type="button" @click="createOpen = false">取消</button><button class="primary-button" type="button" :disabled="loading || !createForm.templateName || !createForm.workspaceId" @click="emit('create', { ...createForm }); createOpen = false"><Plus :size="15" /> 创建草稿</button></div></section>
  </div>

  <div v-if="publishOpen" class="overlay-backdrop" @click.self="publishOpen = false">
    <section class="modal-panel template-publish-modal"><header class="overlay-header"><div><h2>发布解决方案版本</h2><p class="muted">系统会自动收集依赖，清洗敏感配置并执行发布检查。</p></div><button class="icon-button" type="button" title="关闭" @click="publishOpen = false"><X :size="18" /></button></header><div class="form-grid"><label>语义化版本<input v-model="publishForm.versionNo" class="mono" /></label><label>版本名称<input v-model="publishForm.versionName" /></label><label class="wide">入口 Agent<div class="selection-box"><label v-for="agent in agents" :key="agent.id" class="checkbox-row"><input v-model="publishForm.entryAgentIds" type="checkbox" :value="agent.id" /> {{ agent.agentName }}</label></div></label><label class="wide">入口 Agent 团队<div class="selection-box"><label v-for="team in teams" :key="team.id" class="checkbox-row"><input v-model="publishForm.entryTeamIds" type="checkbox" :value="team.id" /> {{ team.teamName }}</label></div></label><label class="wide">更新说明<textarea v-model="publishForm.changeLog" rows="3" /></label><label class="wide">兼容性声明<textarea v-model="publishForm.compatibilityStatement" rows="2" /></label><label class="checkbox-row"><input v-model="publishForm.breakingChange" type="checkbox" /> 破坏性升级</label><label class="checkbox-row"><input v-model="publishForm.submitForPublicReview" type="checkbox" /> 提交公开广场审核</label></div><div class="dependency-preview"><div class="section-title compact-title"><h3>依赖资源</h3><button class="secondary-button slim" type="button" :disabled="!publishForm.entryAgentIds.length && !publishForm.entryTeamIds.length" @click="analyze"><ScanSearch :size="14" /> 重新分析</button></div><div class="manual-resource-row"><select v-model="manualResource.resourceType"><option value="prompt">Prompt</option><option value="tool">工具</option><option value="knowledge">知识库</option><option value="workflow">工作流</option><option value="agent">Agent</option><option value="team">Agent团队</option><option value="memory">记忆</option><option value="mcp">MCP服务</option></select><input v-model="manualResource.resourceId" placeholder="资源 ID" /><input v-model="manualResource.resourceName" placeholder="展示名称（可选）" /><button class="secondary-button slim" type="button" @click="addManualResource"><Plus :size="14" /> 手动加入</button></div><div class="dependency-resource-list"><div v-for="resource in analyzedResources" :key="`${resource.resourceType}-${resource.resourceId}`"><StatusBadge :label="resource.resourceType" /><b>{{ resource.resourceName }}</b><span class="muted mono">{{ resource.resourceId }}</span><button class="icon-button" type="button" title="从模板包排除" @click="removeResource(resource)"><Trash2 :size="15" /></button></div></div><p v-if="analyzedResources.length === 0" class="muted">选择入口资源后分析依赖，也可以手动加入额外资源。</p></div><div class="toolbar compact"><button class="secondary-button" type="button" @click="publishOpen = false">取消</button><button class="primary-button" type="button" :disabled="loading || !analyzedResources.length || !publishForm.changeLog" @click="emit('publish', selectedTemplateId, { ...publishForm, entryAgentIds: [...publishForm.entryAgentIds], entryTeamIds: [...publishForm.entryTeamIds], includeResources: publishForm.includeResources.map((item) => ({ ...item })), excludeResources: publishForm.excludeResources.map((item) => ({ ...item })) }); publishOpen = false"><Send :size="15" /> 创建版本</button></div></section>
  </div>
</template>

<style scoped>
.template-publish-modal { width: min(900px, calc(100vw - 32px)); max-height: calc(100vh - 36px); overflow: auto; }
.selection-box { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 6px 12px; max-height: 130px; overflow: auto; padding: 8px; border: 1px solid var(--border-color); }
.dependency-preview { margin-top: 16px; padding-top: 12px; border-top: 1px solid var(--border-color); }
.manual-resource-row { display: grid; grid-template-columns: 140px minmax(180px, 1fr) minmax(180px, 1fr) auto; gap: 8px; margin-bottom: 10px; }
.dependency-resource-list { display: grid; max-height: 190px; overflow: auto; border-block: 1px solid var(--border-color); }
.dependency-resource-list > div { display: grid; grid-template-columns: 90px minmax(150px, .8fr) minmax(180px, 1fr) 34px; gap: 8px; align-items: center; padding: 7px 2px; border-bottom: 1px solid var(--border-color); }
@media (max-width: 760px) { .manual-resource-row { grid-template-columns: 1fr; } }
</style>
