<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { Boxes, Download, Eye, Heart, Package, RefreshCw, Search, ShieldCheck, Star, TrendingUp, X } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import StatCard from '../components/StatCard.vue';
import StatusBadge from '../components/StatusBadge.vue';
import TemplateInstallWizard from '../components/TemplateInstallWizard.vue';
import TemplatePublisherPanel from '../components/TemplatePublisherPanel.vue';
import { fetchAgents, type AgentSummary } from '../api/agents';
import { fetchAgentTeams, type AgentTeamSummary } from '../api/agentTeams';
import { fetchModelProviders, type ModelConfigSummary } from '../api/models';
import {
  analyzeTemplateDependencies,
  createManagedTemplate,
  fetchManagedTemplates,
  fetchMyTemplateInstalls,
  fetchPendingTemplateReviews,
  fetchTemplateAuthor,
  fetchTemplateDetail,
  fetchTemplateInstall,
  fetchTemplateOverview,
  fetchTemplateReports,
  fetchTemplates,
  installTemplate,
  operateTemplate,
  previewTemplateUpgrade,
  publishTemplateVersion,
  rateTemplate,
  reportTemplate,
  resolveTemplateReport,
  reviewTemplateVersion,
  toggleTemplateFavorite,
  uninstallTemplate,
  upgradeTemplate,
  type ResourceReference,
  type TemplateDetail,
  type TemplateAuthorProfile,
  type TemplateInstallRequest,
  type TemplateInstallSummary,
  type TemplateOverview,
  type TemplatePublishRequest,
  type TemplateReport,
  type TemplateRequest,
  type TemplateSummary,
  type TemplateVersion,
  type UpgradeConflict,
} from '../api/templates';
import { fetchWorkspaces, type WorkspaceSummary } from '../api/workspaces';

const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const overview = ref<TemplateOverview | null>(null);
const templates = ref<TemplateSummary[]>([]);
const managedTemplates = ref<TemplateSummary[]>([]);
const pendingReviews = ref<TemplateVersion[]>([]);
const installs = ref<TemplateInstallSummary[]>([]);
const reports = ref<TemplateReport[]>([]);
const workspaces = ref<WorkspaceSummary[]>([]);
const models = ref<ModelConfigSummary[]>([]);
const agents = ref<AgentSummary[]>([]);
const teams = ref<AgentTeamSummary[]>([]);
const total = ref(0);
const activePanel = ref<'market' | 'installed' | 'publish' | 'operations'>('market');
const selectedTemplate = ref<TemplateDetail | null>(null);
const detailOpen = ref(false);
const installOpen = ref(false);
const activeInstall = ref<TemplateInstallSummary | null>(null);
const authorProfile = ref<TemplateAuthorProfile | null>(null);
const authorOpen = ref(false);
const selectedReport = ref<TemplateReport | null>(null);
const reportResolveOpen = ref(false);
const resolutionForm = reactive({ action: 'resolved' as 'resolved' | 'rejected', resolution: '', offlineTemplate: false });
const publisherRef = ref<{ setAnalyzed: (resources: ResourceReference[]) => void } | null>(null);
const upgradeOpen = ref(false);
const upgradeInstall = ref<TemplateInstallSummary | null>(null);
const upgradeConflicts = ref<UpgradeConflict[]>([]);
const upgradeTargetVersionId = ref('');
const conflictChoices = reactive<Record<string, string>>({});
const reviewForm = reactive({ rating: 5, comment: '' });
const reportForm = reactive({ type: 'content_risk', reason: '' });
const filters = reactive({ category: 'all', keyword: '', sort: 'recommended', favoriteOnly: false, pageNo: 1, pageSize: 10 });
let pollTimer: ReturnType<typeof setInterval> | null = null;

const categories = ['all', '知识管理', '办公助手', '数据分析', '客服服务', '开发运维', '其他'];
const selectedInstallVersion = computed(() => selectedTemplate.value?.versions.find((item) => item.id === upgradeTargetVersionId.value));

onMounted(async () => {
  await loadAll();
  pollTimer = setInterval(() => void pollInstalls(), 2500);
});

onBeforeUnmount(() => { if (pollTimer) clearInterval(pollTimer); });

async function loadAll() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [overviewResult, page, workspaceResult, providerResult, agentResult, teamResult] = await Promise.all([
      fetchTemplateOverview(), fetchTemplates(filters), fetchWorkspaces(), fetchModelProviders(), fetchAgents(), fetchAgentTeams(),
    ]);
    overview.value = overviewResult;
    templates.value = page.records;
    total.value = page.total;
    workspaces.value = workspaceResult;
    models.value = providerResult.flatMap((provider) => provider.models);
    agents.value = agentResult;
    teams.value = teamResult;
    await Promise.all([loadInstalls(), loadManagedData()]);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '模板广场加载失败';
  } finally {
    loading.value = false;
  }
}

async function loadMarket() {
  loading.value = true;
  try {
    const [page, overviewResult] = await Promise.all([fetchTemplates(filters), fetchTemplateOverview()]);
    templates.value = page.records; total.value = page.total; overview.value = overviewResult;
  } catch (error) { errorMessage.value = error instanceof Error ? error.message : '模板列表加载失败'; }
  finally { loading.value = false; }
}

async function loadManagedData() {
  try { managedTemplates.value = await fetchManagedTemplates(); } catch { managedTemplates.value = []; }
  try { pendingReviews.value = await fetchPendingTemplateReviews(); } catch { pendingReviews.value = []; }
  try { reports.value = await fetchTemplateReports('pending'); } catch { reports.value = []; }
}

async function loadInstalls() {
  try { installs.value = await fetchMyTemplateInstalls(); } catch { installs.value = []; }
}

async function pollInstalls() {
  const running = installs.value.filter((item) => ['pending', 'running'].includes(item.installStatus));
  if (!running.length && !activeInstall.value) return;
  for (const item of running) {
    const latest = await fetchTemplateInstall(item.id).catch(() => null);
    if (latest) Object.assign(item, latest);
    if (activeInstall.value?.id === item.id && latest) activeInstall.value = latest;
  }
}

async function openDetail(template: TemplateSummary) {
  loading.value = true;
  try { selectedTemplate.value = await fetchTemplateDetail(template.id); detailOpen.value = true; }
  catch (error) { errorMessage.value = error instanceof Error ? error.message : '模板详情加载失败'; }
  finally { loading.value = false; }
}

async function favorite(template: TemplateSummary) {
  const result = await toggleTemplateFavorite(template.id);
  template.favorite = result;
  template.favoriteCount += result ? 1 : -1;
  if (selectedTemplate.value?.id === template.id) selectedTemplate.value.favorite = result;
}

async function openInstall(template?: TemplateSummary) {
  const target = template || selectedTemplate.value;
  if (!target) return;
  selectedTemplate.value = await fetchTemplateDetail(target.id);
  detailOpen.value = false;
  activeInstall.value = null;
  installOpen.value = true;
}

async function showInstallProgress(item: TemplateInstallSummary) {
  try {
    selectedTemplate.value = await fetchTemplateDetail(item.templateId);
    activeInstall.value = await fetchTemplateInstall(item.id);
    installOpen.value = true;
  } catch (error) { errorMessage.value = error instanceof Error ? error.message : '安装进度加载失败'; }
}

async function openAuthor(userId?: string) {
  if (!userId) return;
  try { authorProfile.value = await fetchTemplateAuthor(userId); authorOpen.value = true; }
  catch (error) { errorMessage.value = error instanceof Error ? error.message : '作者主页加载失败'; }
}

async function submitInstall(payload: TemplateInstallRequest) {
  if (!selectedTemplate.value) return;
  loading.value = true;
  try {
    activeInstall.value = await installTemplate(selectedTemplate.value.id, payload);
    await loadInstalls();
    successMessage.value = '安装任务已提交到 Kafka';
  } catch (error) { errorMessage.value = error instanceof Error ? error.message : '模板安装提交失败'; }
  finally { loading.value = false; }
}

async function submitRating() {
  if (!selectedTemplate.value) return;
  try {
    await rateTemplate(selectedTemplate.value.id, reviewForm.rating, reviewForm.comment);
    selectedTemplate.value = await fetchTemplateDetail(selectedTemplate.value.id);
    successMessage.value = '评分与评论已保存';
  } catch (error) { errorMessage.value = error instanceof Error ? error.message : '评分保存失败'; }
}

async function submitReport() {
  if (!selectedTemplate.value) return;
  try { await reportTemplate(selectedTemplate.value.id, reportForm.type, reportForm.reason); reportForm.reason = ''; successMessage.value = '举报已进入治理队列'; }
  catch (error) { errorMessage.value = error instanceof Error ? error.message : '举报提交失败'; }
}

async function createTemplate(request: TemplateRequest) {
  try { await createManagedTemplate(request); await loadManagedData(); successMessage.value = '工作空间私有模板已创建'; }
  catch (error) { errorMessage.value = error instanceof Error ? error.message : '模板创建失败'; }
}

async function analyzeDependencies(request: TemplatePublishRequest) {
  try { publisherRef.value?.setAnalyzed(await analyzeTemplateDependencies(request)); }
  catch (error) { errorMessage.value = error instanceof Error ? error.message : '依赖分析失败'; }
}

async function publishVersion(templateId: string, request: TemplatePublishRequest) {
  loading.value = true;
  try { await publishTemplateVersion(templateId, request); await loadManagedData(); successMessage.value = request.submitForPublicReview ? '版本已完成自动检查并提交人工审核' : '私有模板版本已发布'; }
  catch (error) { errorMessage.value = error instanceof Error ? error.message : '模板版本发布失败'; }
  finally { loading.value = false; }
}

async function reviewVersion(versionId: string, action: string, comment: string) {
  try { await reviewTemplateVersion(versionId, action, comment); await Promise.all([loadManagedData(), loadMarket()]); successMessage.value = action === 'approve' ? '模板版本已审核上架' : '模板版本已驳回'; }
  catch (error) { errorMessage.value = error instanceof Error ? error.message : '模板审核失败'; }
}

async function openUpgrade(item: TemplateInstallSummary) {
  const template = await fetchTemplateDetail(item.templateId);
  selectedTemplate.value = template;
  upgradeInstall.value = item;
  upgradeTargetVersionId.value = template.currentVersionId || '';
  upgradeConflicts.value = await previewTemplateUpgrade(item.id, upgradeTargetVersionId.value);
  Object.keys(conflictChoices).forEach((key) => delete conflictChoices[key]);
  upgradeConflicts.value.forEach((conflict) => { if (conflict.mergeDecision === 'conflict') conflictChoices[conflict.id] = 'keep_local'; });
  upgradeOpen.value = true;
}

async function submitUpgrade() {
  if (!upgradeInstall.value) return;
  try { await upgradeTemplate(upgradeInstall.value.id, upgradeTargetVersionId.value, { ...conflictChoices }); upgradeOpen.value = false; await loadInstalls(); successMessage.value = '模板升级任务已进入 Kafka 队列'; }
  catch (error) { errorMessage.value = error instanceof Error ? error.message : '模板升级提交失败'; }
}

async function unlinkInstall(item: TemplateInstallSummary) {
  const remove = window.confirm('确定解除模板关联吗？点击“确定”后将继续询问是否删除未修改的模板资源。');
  if (!remove) return;
  const deleteResources = window.confirm('是否同时删除从未修改过的模板资源？用户修改和新增数据始终保留。');
  await uninstallTemplate(item.id, deleteResources);
  await loadInstalls();
}

async function operate(item: TemplateSummary, action: 'recommend' | 'offline' | 'online') {
  try {
    await operateTemplate(item.id, action === 'recommend' ? { recommended: !item.recommended } : { status: action === 'offline' ? 'offline' : 'published' });
    await Promise.all([loadManagedData(), loadMarket()]);
  } catch (error) { errorMessage.value = error instanceof Error ? error.message : '模板运营操作失败'; }
}

function openReportResolution(report: TemplateReport) {
  selectedReport.value = report;
  resolutionForm.action = 'resolved';
  resolutionForm.resolution = '';
  resolutionForm.offlineTemplate = false;
  reportResolveOpen.value = true;
}

async function resolveReport() {
  if (!selectedReport.value || !resolutionForm.resolution.trim()) return;
  try {
    await resolveTemplateReport(selectedReport.value.id, { ...resolutionForm });
    reportResolveOpen.value = false;
    await Promise.all([loadManagedData(), loadMarket()]);
    successMessage.value = '举报处置已完成';
  } catch (error) { errorMessage.value = error instanceof Error ? error.message : '举报处置失败'; }
}

function search() { filters.pageNo = 1; void loadMarket(); }
function formatTime(value?: string) { return value ? value.replace('T', ' ').slice(0, 16) : '-'; }
function resourceTotal(item: TemplateSummary) { return Object.values(item.resourceCounts || {}).reduce((sum, value) => sum + Number(value || 0), 0); }
</script>

<template>
  <PageHeader title="解决方案模板广场" description="发布、审核和安装可独立交付的 Agent 解决方案包">
    <template #actions><button class="secondary-button" type="button" :disabled="loading" @click="loadAll"><RefreshCw :size="16" /> 刷新</button></template>
  </PageHeader>
  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
  <p v-if="successMessage" class="form-success">{{ successMessage }}</p>

  <section class="metric-grid template-metrics">
    <StatCard label="公开模板" :value="String(overview?.publishedCount || 0)" detail="免费解决方案" icon="Package" tone="info" />
    <StatCard label="我的收藏" :value="String(overview?.favoriteCount || 0)" detail="持续关注" icon="Heart" tone="neutral" />
    <StatCard label="已安装" :value="String(overview?.installedCount || 0)" detail="独立副本" icon="Download" tone="success" />
    <StatCard label="安装中" :value="String(overview?.runningInstallCount || 0)" detail="Kafka 异步任务" icon="RefreshCw" tone="warning" />
    <StatCard label="待审核" :value="String(overview?.pendingReviewCount || 0)" detail="公开发布门禁" icon="ShieldCheck" tone="warning" />
  </section>

  <section class="governance-card-tabs template-tabs">
    <button class="governance-tab-card" :class="{ active: activePanel === 'market' }" type="button" @click="activePanel = 'market'"><span>公开广场</span><b>{{ total }}</b><small>分类、排行与推荐</small></button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'installed' }" type="button" @click="activePanel = 'installed'"><span>我的安装</span><b>{{ installs.length }}</b><small>进度、升级与卸载</small></button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'publish' }" type="button" @click="activePanel = 'publish'"><span>发布与审核</span><b>{{ managedTemplates.length }}</b><small>依赖收集与安全门禁</small></button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'operations' }" type="button" @click="activePanel = 'operations'"><span>运营治理</span><b>{{ overview?.pendingReportCount || 0 }}</b><small>推荐、上下架与举报</small></button>
  </section>

  <section class="section-block template-main-panel">
    <template v-if="activePanel === 'market'">
      <div class="filter-row template-filters">
        <div class="search-field"><Search :size="16" /><input v-model="filters.keyword" placeholder="搜索模板、场景或标签" @keyup.enter="search" /></div>
        <select v-model="filters.sort" @change="search"><option value="recommended">推荐排序</option><option value="latest">最新发布</option><option value="rating">评分最高</option><option value="installs">安装最多</option></select>
        <label class="checkbox-row"><input v-model="filters.favoriteOnly" type="checkbox" @change="search" /> 只看收藏</label>
      </div>
      <div class="category-strip"><button v-for="category in categories" :key="category" :class="{ active: filters.category === category }" type="button" @click="filters.category = category; search()">{{ category === 'all' ? '全部' : category }}</button></div>
      <div class="solution-grid">
        <article v-for="item in templates" :key="item.id" class="solution-card">
          <header><div class="solution-icon"><Boxes :size="22" /></div><button class="icon-button" type="button" :title="item.favorite ? '取消收藏' : '收藏'" @click="favorite(item)"><Heart :size="18" :fill="item.favorite ? 'currentColor' : 'none'" /></button></header>
          <div class="solution-title"><h2>{{ item.templateName }}</h2><StatusBadge v-if="item.recommended" label="推荐" tone="success" /></div>
          <p>{{ item.description }}</p>
          <div class="badge-row"><StatusBadge :label="item.category" /><StatusBadge v-for="tag in item.tags.slice(0, 2)" :key="tag" :label="tag" tone="neutral" /></div>
          <div class="solution-metrics"><span><Star :size="14" fill="currentColor" /> {{ Number(item.averageRating).toFixed(1) }}</span><span><Download :size="14" /> {{ item.installCount }}</span><span><Package :size="14" /> {{ resourceTotal(item) }}</span></div>
          <footer><span><button v-if="item.authorUserId" class="author-link" type="button" @click="openAuthor(item.authorUserId)">{{ item.authorName }}</button><template v-else>{{ item.authorName }}</template> · {{ item.currentVersion }}</span><div><button class="icon-button" type="button" title="查看详情" @click="openDetail(item)"><Eye :size="17" /></button><button class="primary-button slim" type="button" @click="openInstall(item)"><Download :size="14" /> 安装</button></div></footer>
        </article>
      </div>
      <div v-if="templates.length === 0" class="empty-state">没有匹配的公开解决方案模板</div>
      <PaginationBar :page="filters.pageNo" :total="total" @update:page="(page) => { filters.pageNo = page; loadMarket(); }" />
    </template>

    <template v-else-if="activePanel === 'installed'">
      <div class="section-title"><h2>我的安装</h2><span>模板升级不会自动覆盖本地修改</span></div>
      <table class="data-table"><thead><tr><th>解决方案</th><th>版本</th><th>状态</th><th>进度</th><th>当前阶段</th><th>完成时间</th><th>操作</th></tr></thead><tbody><tr v-for="item in installs" :key="item.id"><td><b>{{ item.templateName }}</b><span class="muted block mono">{{ item.id }}</span></td><td>{{ item.versionNo }}<StatusBadge v-if="item.upgradeAvailable" label="有更新" tone="warning" /></td><td><StatusBadge :label="item.installStatus" :tone="item.installStatus === 'success' ? 'success' : item.installStatus === 'failed' || item.installStatus === 'rollback' ? 'danger' : 'info'" /></td><td><div class="table-progress"><span :style="{ width: `${item.progressPercent}%` }" /></div>{{ item.progressPercent }}%</td><td>{{ item.currentMessage || '-' }}</td><td>{{ formatTime(item.completedAt) }}</td><td><div class="table-actions"><button v-if="item.upgradeAvailable" class="primary-button slim" type="button" @click="openUpgrade(item)"><TrendingUp :size="14" /> 升级</button><button v-if="['pending','running'].includes(item.installStatus)" class="secondary-button slim" type="button" @click="showInstallProgress(item)">查看进度</button><button v-if="item.installStatus === 'success'" class="secondary-button slim danger-text" type="button" @click="unlinkInstall(item)">卸载</button></div></td></tr></tbody></table>
      <div v-if="installs.length === 0" class="empty-state">尚未安装解决方案模板</div>
    </template>

    <TemplatePublisherPanel v-else-if="activePanel === 'publish'" ref="publisherRef" :templates="managedTemplates" :agents="agents" :teams="teams" :workspaces="workspaces" :pending-reviews="pendingReviews" :loading="loading" @create="createTemplate" @analyze="analyzeDependencies" @publish="publishVersion" @review="reviewVersion" />

    <template v-else>
      <div class="section-title"><h2>模板运营治理</h2><span>公开推荐位与上下架管理</span></div>
      <table class="data-table"><thead><tr><th>模板</th><th>评分</th><th>安装</th><th>收藏</th><th>趋势</th><th>举报</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in templates" :key="item.id"><td><b>{{ item.templateName }}</b><span class="muted block">{{ item.authorName }}</span></td><td>{{ Number(item.averageRating).toFixed(2) }} / {{ item.ratingCount }}</td><td>{{ item.installCount }}</td><td>{{ item.favoriteCount }}</td><td>{{ Number(item.trendScore).toFixed(1) }}</td><td>-</td><td><StatusBadge :label="item.status" :tone="item.status === 'published' ? 'success' : 'neutral'" /></td><td><div class="table-actions"><button class="secondary-button slim" type="button" @click="operate(item, 'recommend')">{{ item.recommended ? '取消推荐' : '设为推荐' }}</button><button class="secondary-button slim danger-text" type="button" @click="operate(item, item.status === 'published' ? 'offline' : 'online')">{{ item.status === 'published' ? '下架' : '上架' }}</button></div></td></tr></tbody></table>
      <div class="section-title compact-title"><h2>举报治理队列</h2><span>{{ reports.length }} 项待处置</span></div>
      <table class="data-table"><thead><tr><th>模板</th><th>举报人</th><th>类型</th><th>原因</th><th>提交时间</th><th>操作</th></tr></thead><tbody><tr v-for="report in reports" :key="report.id"><td><b>{{ report.templateName }}</b></td><td>{{ report.reporterName || report.reporterUserId }}</td><td><StatusBadge :label="report.reportType" tone="warning" /></td><td><span class="truncate-cell" :title="report.reason">{{ report.reason }}</span></td><td>{{ formatTime(report.createdAt) }}</td><td><button class="primary-button slim" type="button" @click="openReportResolution(report)">处置</button></td></tr></tbody></table>
      <div v-if="reports.length === 0" class="empty-state">当前没有待处理的模板举报</div>
    </template>
  </section>

  <div v-if="detailOpen && selectedTemplate" class="overlay-backdrop" @click.self="detailOpen = false">
    <section class="modal-panel template-detail-modal"><header class="overlay-header"><div><div class="detail-title-row"><h2>{{ selectedTemplate.templateName }}</h2><StatusBadge :label="selectedTemplate.currentVersion || '无版本'" tone="info" /></div><p class="muted">{{ selectedTemplate.authorName }} · {{ selectedTemplate.licenseCode }}</p></div><button class="icon-button" type="button" title="关闭" @click="detailOpen = false"><X :size="18" /></button></header><p>{{ selectedTemplate.description }}</p><div class="trace-meta"><span>评分</span><b>{{ Number(selectedTemplate.averageRating).toFixed(1) }} / 5</b><span>安装量</span><b>{{ selectedTemplate.installCount }}</b><span>资源数</span><b>{{ selectedTemplate.resources.length }}</b><span>兼容性</span><b class="truncate-cell" :title="selectedTemplate.compatibility">{{ selectedTemplate.compatibility }}</b></div><div class="section-title compact-title"><h3>资源清单</h3><span>完整独立副本</span></div><div class="resource-list"><div v-for="resource in selectedTemplate.resources" :key="resource.id"><StatusBadge :label="resource.resourceType" /><b>{{ resource.resourceName }}</b><span class="muted mono truncate-cell" :title="resource.contentHash">{{ resource.contentHash }}</span></div></div><div class="section-title compact-title"><h3>版本与安全</h3><span>{{ selectedTemplate.versions.length }} 个版本</span></div><table class="data-table"><thead><tr><th>版本</th><th>更新说明</th><th>安全检查</th><th>运行检查</th><th>发布时间</th></tr></thead><tbody><tr v-for="version in selectedTemplate.versions" :key="version.id"><td><b>{{ version.versionNo }}</b><StatusBadge v-if="version.breakingChange" label="破坏性" tone="warning" /></td><td>{{ version.changeLog }}</td><td><StatusBadge :label="version.securityScanResult.passed ? '通过' : '阻断'" :tone="version.securityScanResult.passed ? 'success' : 'danger'" /></td><td><StatusBadge :label="version.runtimeCheckResult.passed ? '通过' : '阻断'" :tone="version.runtimeCheckResult.passed ? 'success' : 'danger'" /></td><td>{{ formatTime(version.publishedAt) }}</td></tr></tbody></table><div v-if="selectedTemplate.comments.length" class="comment-list"><div v-for="comment in selectedTemplate.comments" :key="comment.id" :class="{ reply: comment.parentCommentId }"><b>{{ comment.userName }}</b><span v-if="comment.rating">{{ '★'.repeat(comment.rating) }}</span><p>{{ comment.content }}</p></div></div><div v-if="selectedTemplate.canReview" class="form-grid review-form"><label>评分<select v-model.number="reviewForm.rating"><option v-for="score in 5" :key="score" :value="score">{{ score }} 分</option></select></label><label class="wide">评论<textarea v-model="reviewForm.comment" rows="2" /></label><button class="secondary-button" type="button" @click="submitRating">保存评价</button></div><div class="form-grid report-form"><label>举报类型<select v-model="reportForm.type"><option value="content_risk">内容风险</option><option value="security">安全问题</option><option value="license">许可证问题</option><option value="misleading">描述不实</option></select></label><label class="wide">举报原因<input v-model="reportForm.reason" placeholder="发现问题时填写" /></label><button class="secondary-button danger-text" type="button" :disabled="!reportForm.reason" @click="submitReport">提交举报</button></div><div class="toolbar compact"><button class="secondary-button" type="button" @click="favorite(selectedTemplate)"><Heart :size="16" /> {{ selectedTemplate.favorite ? '取消收藏' : '收藏' }}</button><span class="toolbar-spacer" /><button class="primary-button" type="button" @click="openInstall()"><Download :size="16" /> 安装解决方案</button></div></section>
  </div>

  <TemplateInstallWizard v-if="installOpen && selectedTemplate" :template="selectedTemplate" :workspaces="workspaces" :models="models" :submitting="loading" :install="activeInstall" @close="installOpen = false; activeInstall = null" @submit="submitInstall" />

  <div v-if="authorOpen && authorProfile" class="overlay-backdrop" @click.self="authorOpen = false">
    <section class="modal-panel author-profile-modal"><header class="overlay-header"><div><h2>{{ authorProfile.authorName }}</h2><p class="muted">解决方案模板作者主页</p></div><button class="icon-button" type="button" title="关闭" @click="authorOpen = false"><X :size="18" /></button></header><div class="trace-meta"><span>公开模板</span><b>{{ authorProfile.publishedTemplateCount }}</b><span>累计安装</span><b>{{ authorProfile.totalInstallCount }}</b><span>累计收藏</span><b>{{ authorProfile.totalFavoriteCount }}</b><span>综合评分</span><b>{{ Number(authorProfile.averageRating).toFixed(2) }}</b></div><div class="author-template-list"><button v-for="item in authorProfile.templates" :key="item.id" type="button" @click="authorOpen = false; openDetail(item)"><b>{{ item.templateName }}</b><span>{{ item.currentVersion }} · {{ item.installCount }} 次安装</span></button></div></section>
  </div>

  <div v-if="reportResolveOpen && selectedReport" class="overlay-backdrop" @click.self="reportResolveOpen = false">
    <section class="modal-panel compact"><header class="overlay-header"><div><h2>处置模板举报</h2><p class="muted">{{ selectedReport.templateName }} · {{ selectedReport.reportType }}</p></div><button class="icon-button" type="button" title="关闭" @click="reportResolveOpen = false"><X :size="18" /></button></header><p>{{ selectedReport.reason }}</p><div class="form-grid"><label>处理动作<select v-model="resolutionForm.action"><option value="resolved">举报成立</option><option value="rejected">举报不成立</option></select></label><label class="checkbox-row"><input v-model="resolutionForm.offlineTemplate" type="checkbox" :disabled="resolutionForm.action !== 'resolved'" /> 同步下架模板</label><label class="wide">处理结论<textarea v-model="resolutionForm.resolution" rows="4" /></label></div><div class="toolbar compact"><button class="secondary-button" type="button" @click="reportResolveOpen = false">取消</button><button class="primary-button" type="button" :disabled="!resolutionForm.resolution.trim()" @click="resolveReport">确认处置</button></div></section>
  </div>

  <div v-if="upgradeOpen && upgradeInstall" class="overlay-backdrop" @click.self="upgradeOpen = false">
    <section class="modal-panel template-upgrade-modal"><header class="overlay-header"><div><h2>三方差异升级</h2><p class="muted">旧模板 {{ upgradeInstall.versionNo }} / 本地副本 / 新模板 {{ selectedInstallVersion?.versionNo }}</p></div><button class="icon-button" type="button" title="关闭" @click="upgradeOpen = false"><X :size="18" /></button></header><table class="data-table"><thead><tr><th>资源</th><th>判定</th><th>处理方式</th></tr></thead><tbody><tr v-for="conflict in upgradeConflicts" :key="conflict.id"><td><b>{{ conflict.resourceName }}</b><span class="muted block">{{ conflict.resourceType }}</span></td><td><StatusBadge :label="conflict.mergeDecision" :tone="conflict.mergeDecision === 'conflict' ? 'warning' : 'success'" /></td><td><select v-if="conflict.mergeDecision === 'conflict'" v-model="conflictChoices[conflict.id]"><option value="keep_local">保留本地修改</option><option value="use_new">采用新版模板</option></select><span v-else>{{ conflict.mergeDecision === 'keep_local' ? '保留本地' : '采用新版' }}</span></td></tr></tbody></table><p class="muted">用户自行新增的知识文档始终保留，不参与模板删除。</p><div class="toolbar compact"><button class="secondary-button" type="button" @click="upgradeOpen = false">取消</button><button class="primary-button" type="button" @click="submitUpgrade"><TrendingUp :size="16" /> 提交异步升级</button></div></section>
  </div>
</template>

<style scoped>
.template-metrics { grid-template-columns: repeat(5, minmax(0, 1fr)); }
.template-tabs { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.template-main-panel { min-height: 420px; }
.template-filters { align-items: center; }
.search-field { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 240px; padding-inline: 10px; border: 1px solid var(--border-color); background: var(--surface-color); }
.search-field input { border: 0; width: 100%; padding-inline: 0; }
.category-strip { display: flex; gap: 4px; overflow-x: auto; margin: 12px 0 16px; border-bottom: 1px solid var(--border-color); }
.category-strip button { padding: 8px 12px; border: 0; border-bottom: 2px solid transparent; background: transparent; white-space: nowrap; }
.category-strip button.active { color: var(--primary-color); border-bottom-color: var(--primary-color); }
.solution-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.solution-card { display: flex; flex-direction: column; min-height: 258px; padding: 16px; border: 1px solid var(--border-color); border-radius: 6px; background: var(--surface-color); }
.solution-card > header, .solution-card > footer, .solution-title, .solution-metrics { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.solution-card h2 { margin: 0; font-size: 17px; }
.solution-card p { flex: 1; color: var(--text-muted); line-height: 1.65; }
.solution-icon { display: grid; place-items: center; width: 38px; height: 38px; color: #0f766e; border: 1px solid #99d5cc; background: #edf9f6; }
.solution-metrics { justify-content: flex-start; margin: 12px 0; padding: 9px 0; border-block: 1px solid var(--border-color); }
.solution-metrics span { display: flex; align-items: center; gap: 4px; }
.solution-card footer > span { color: var(--text-muted); font-size: 12px; }
.author-link { padding: 0; border: 0; color: var(--primary-color); background: transparent; font: inherit; }
.solution-card footer > div { display: flex; gap: 6px; }
.table-progress { display: inline-block; width: 76px; height: 6px; margin-right: 8px; overflow: hidden; background: var(--surface-subtle); vertical-align: middle; }
.table-progress span { display: block; height: 100%; background: #0f766e; }
.template-detail-modal { width: min(980px, calc(100vw - 32px)); max-height: calc(100vh - 32px); overflow: auto; }
.detail-title-row { display: flex; align-items: center; gap: 10px; }
.resource-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); max-height: 230px; overflow: auto; border-block: 1px solid var(--border-color); }
.resource-list > div { display: grid; grid-template-columns: 90px minmax(110px, .7fr) minmax(100px, 1fr); gap: 8px; align-items: center; padding: 9px 4px; border-bottom: 1px solid var(--border-color); }
.comment-list { display: grid; gap: 8px; margin-top: 16px; }
.comment-list > div { padding: 10px; border-left: 2px solid var(--border-color); }
.comment-list > div.reply { margin-left: 28px; border-left-color: #0f766e; }
.comment-list p { margin: 5px 0 0; }
.review-form, .report-form { margin-top: 16px; align-items: end; }
.toolbar-spacer { flex: 1; }
.template-upgrade-modal { width: min(820px, calc(100vw - 32px)); max-height: calc(100vh - 32px); overflow: auto; }
.author-profile-modal { width: min(720px, calc(100vw - 32px)); max-height: calc(100vh - 32px); overflow: auto; }
.author-template-list { display: grid; margin-top: 14px; border-block: 1px solid var(--border-color); }
.author-template-list button { display: flex; justify-content: space-between; gap: 12px; padding: 12px 4px; border: 0; border-bottom: 1px solid var(--border-color); background: transparent; text-align: left; }
.author-template-list span { color: var(--text-muted); }
@media (max-width: 1100px) { .solution-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .template-metrics { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 760px) { .solution-grid, .resource-list { grid-template-columns: 1fr; } .template-metrics, .template-tabs { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
