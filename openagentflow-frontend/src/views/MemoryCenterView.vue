<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { DatabaseZap, Plus, RefreshCw, Save, ScanSearch, Search, Sparkles, ThumbsDown, ThumbsUp, X } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import StatCard from '../components/StatCard.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { fetchAgents, type AgentSummary } from '../api/agents';
import {
  cleanupMemories,
  createMemory,
  deleteMemory,
  fetchMemories,
  fetchMemoryOverview,
  recallMemories,
  updateMemory,
  fetchMemoryProductionOverview,
  fetchMemoryPolicies,
  fetchMemoryGovernanceIssues,
  saveMemoryPolicy,
  resolveMemoryGovernanceIssue,
  scanMemoryGovernance,
  rebuildMemoryVectors,
  submitMemoryFeedback,
  type MemoryOverview,
  type MemoryProductionOverview,
  type MemoryPolicy,
  type MemoryGovernanceIssue,
  type MemoryRecallItem,
  type MemorySaveRequest,
  type MemorySummary,
} from '../api/memories';

const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const overview = ref<MemoryOverview | null>(null);
const memories = ref<MemorySummary[]>([]);
const agents = ref<AgentSummary[]>([]);
const total = ref(0);
const activePanel = ref<'list' | 'recall' | 'cleanup' | 'operations' | 'issues' | 'policies'>('list');
const memoryModalOpen = ref(false);
const editingId = ref('');
const recallResults = ref<MemoryRecallItem[]>([]);
const productionOverview = ref<MemoryProductionOverview | null>(null);
const policies = ref<MemoryPolicy[]>([]);
const issues = ref<MemoryGovernanceIssue[]>([]);
const issueTotal = ref(0);
const policyModalOpen = ref(false);
const issueFilters = reactive({ status: 'open', type: 'all', pageNo: 1, pageSize: 10 });
const policyForm = reactive({
  id: '', agentId: '', policyName: 'Memory生产策略', extractionEnabled: true,
  minImportance: 0.55, minConfidence: 0.65, recallThreshold: 0.35, recallLimit: 8,
  promptTokenBudget: 1200, shortTermTtlDays: 7, maxMemoriesPerUser: 10000,
  piiMode: 'redact', conflictMode: 'supersede', status: 'enabled',
});

const filters = reactive({
  memoryType: 'all',
  status: 'active',
  agentId: 'all',
  keyword: '',
  pageNo: 1,
  pageSize: 10,
});

const form = reactive<MemorySaveRequest>({
  agentId: '',
  sessionId: '',
  memoryType: 'long_term',
  memoryKey: '',
  memoryText: '',
  memoryValue: '{}',
  importanceScore: 0.7,
  expiredAt: '',
  status: 'active',
  privacyScope: 'private',
  tagsJson: '[]',
});

const recallForm = reactive({
  agentId: '',
  sessionId: '',
  query: '',
  limit: 5,
});

const selectedAgentName = computed(() => {
  if (filters.agentId === 'all') return '全部 Agent';
  return agents.value.find((item) => item.id === filters.agentId)?.agentName || '未知 Agent';
});

onMounted(() => {
  void loadData();
});

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [overviewResult, listResult, agentResult, productionResult, policyResult, issueResult] = await Promise.all([
      fetchMemoryOverview(),
      fetchMemories(filters),
      fetchAgents(),
      fetchMemoryProductionOverview(),
      fetchMemoryPolicies(),
      fetchMemoryGovernanceIssues(issueFilters),
    ]);
    overview.value = overviewResult;
    memories.value = listResult.records;
    total.value = listResult.total;
    agents.value = agentResult;
    productionOverview.value = productionResult;
    policies.value = policyResult;
    issues.value = issueResult.records;
    issueTotal.value = issueResult.total;
    if (!recallForm.agentId && agentResult[0]) {
      recallForm.agentId = agentResult[0].id;
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Memory 记忆中心加载失败';
  } finally {
    loading.value = false;
  }
}

async function reloadList() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [overviewResult, listResult] = await Promise.all([fetchMemoryOverview(), fetchMemories(filters)]);
    overview.value = overviewResult;
    memories.value = listResult.records;
    total.value = listResult.total;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Memory 刷新失败';
  } finally {
    loading.value = false;
  }
}

async function searchMemories() {
  filters.pageNo = 1;
  await reloadList();
}

async function changePage(page: number) {
  filters.pageNo = page;
  await reloadList();
}

function resetForm() {
  editingId.value = '';
  form.agentId = agents.value[0]?.id || '';
  form.sessionId = '';
  form.memoryType = 'long_term';
  form.memoryKey = '';
  form.memoryText = '';
  form.memoryValue = '{}';
  form.importanceScore = 0.7;
  form.expiredAt = '';
  form.status = 'active';
  form.privacyScope = 'private';
  form.tagsJson = '[]';
}

function openCreateModal() {
  resetForm();
  memoryModalOpen.value = true;
}

function editMemory(memory: MemorySummary) {
  editingId.value = memory.id;
  form.agentId = memory.agentId || '';
  form.sessionId = memory.sessionId || '';
  form.memoryType = memory.memoryType;
  form.memoryKey = memory.memoryKey || '';
  form.memoryText = memory.memoryText;
  form.memoryValue = memory.memoryValue || '{}';
  form.importanceScore = Number(memory.importanceScore ?? 0.7);
  form.expiredAt = memory.expiredAt ? memory.expiredAt.slice(0, 19) : '';
  form.status = memory.status || 'active';
  form.privacyScope = memory.privacyScope || 'private';
  form.tagsJson = memory.tagsJson || '[]';
  memoryModalOpen.value = true;
}

function closeModal() {
  memoryModalOpen.value = false;
  resetForm();
}

async function saveMemory() {
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    const payload = normalizeForm();
    if (editingId.value) {
      await updateMemory(editingId.value, payload);
    } else {
      await createMemory(payload);
    }
    successMessage.value = '记忆已保存';
    closeModal();
    await reloadList();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '记忆保存失败';
  } finally {
    loading.value = false;
  }
}

async function removeMemory(memory: MemorySummary) {
  if (!window.confirm(`确认删除这条 ${memoryTypeLabel(memory.memoryType)} 吗？`)) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    await deleteMemory(memory.id);
    successMessage.value = '记忆已删除';
    await reloadList();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '记忆删除失败';
  } finally {
    loading.value = false;
  }
}

async function runRecall() {
  if (!recallForm.query.trim()) {
    errorMessage.value = '请输入召回测试问题';
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    recallResults.value = await recallMemories({
      agentId: recallForm.agentId || undefined,
      sessionId: recallForm.sessionId || undefined,
      query: recallForm.query,
      limit: Number(recallForm.limit || 5),
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '记忆召回测试失败';
  } finally {
    loading.value = false;
  }
}

async function runCleanup() {
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    const result = await cleanupMemories();
    successMessage.value = `Memory 清理任务已提交：${result.taskCode}，可在异步任务中心查看进度`;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '记忆清理失败';
  } finally {
    loading.value = false;
  }
}

async function runGovernanceScan() {
  loading.value = true;
  try {
    const task = await scanMemoryGovernance();
    successMessage.value = `治理扫描任务已提交：${task.taskCode}`;
  } catch (error) { errorMessage.value = error instanceof Error ? error.message : '治理扫描提交失败'; }
  finally { loading.value = false; }
}

async function runVectorRebuild() {
  loading.value = true;
  try {
    const task = await rebuildMemoryVectors();
    successMessage.value = `向量重建任务已提交：${task.taskCode}`;
  } catch (error) { errorMessage.value = error instanceof Error ? error.message : '向量重建提交失败'; }
  finally { loading.value = false; }
}

async function loadIssues(page = issueFilters.pageNo) {
  issueFilters.pageNo = page;
  const result = await fetchMemoryGovernanceIssues(issueFilters);
  issues.value = result.records;
  issueTotal.value = result.total;
}

async function resolveIssue(issue: MemoryGovernanceIssue) {
  await resolveMemoryGovernanceIssue(issue.id, { status: 'resolved', resolution: '已在Memory治理中心确认处置' });
  successMessage.value = '治理问题已处置';
  await loadIssues();
}

function openPolicyModal(policy?: MemoryPolicy) {
  policyForm.id = policy?.id || '';
  policyForm.agentId = policy?.agent_id || '';
  policyForm.policyName = policy?.policy_name || 'Memory生产策略';
  policyForm.extractionEnabled = policy ? Boolean(policy.extraction_enabled) : true;
  policyForm.minImportance = Number(policy?.min_importance ?? 0.55);
  policyForm.minConfidence = Number(policy?.min_confidence ?? 0.65);
  policyForm.recallThreshold = Number(policy?.recall_threshold ?? 0.35);
  policyForm.recallLimit = Number(policy?.recall_limit ?? 8);
  policyForm.promptTokenBudget = Number(policy?.prompt_token_budget ?? 1200);
  policyForm.shortTermTtlDays = Number(policy?.short_term_ttl_days ?? 7);
  policyForm.maxMemoriesPerUser = Number(policy?.max_memories_per_user ?? 10000);
  policyForm.piiMode = policy?.pii_mode || 'redact';
  policyForm.conflictMode = policy?.conflict_mode || 'supersede';
  policyForm.status = policy?.status || 'enabled';
  policyModalOpen.value = true;
}

async function persistPolicy() {
  loading.value = true;
  try {
    await saveMemoryPolicy({ ...policyForm, id: policyForm.id || undefined, agentId: policyForm.agentId || undefined });
    policies.value = await fetchMemoryPolicies();
    policyModalOpen.value = false;
    successMessage.value = 'Memory策略已保存';
  } catch (error) { errorMessage.value = error instanceof Error ? error.message : '策略保存失败'; }
  finally { loading.value = false; }
}

async function feedback(item: MemoryRecallItem, type: 'helpful' | 'irrelevant') {
  if (item.id.startsWith('redis:')) return;
  await submitMemoryFeedback(item.id, type);
  successMessage.value = type === 'helpful' ? '已标记为有帮助' : '已标记为不相关';
}

function normalizeForm(): MemorySaveRequest {
  return {
    ...form,
    agentId: form.agentId || undefined,
    sessionId: form.sessionId || undefined,
    memoryValue: form.memoryValue || '{}',
    importanceScore: Number(form.importanceScore ?? 0.7),
    expiredAt: form.expiredAt || undefined,
    tagsJson: form.tagsJson || '[]',
  };
}

function memoryTypeLabel(value?: string) {
  const labels: Record<string, string> = {
    short_term: '短期记忆',
    long_term: '长期记忆',
    task: '任务记忆',
    vector: '向量记忆',
  };
  return labels[value || ''] || value || '未知';
}

function statusLabel(value?: string) {
  const labels: Record<string, string> = {
    active: '启用',
    archived: '归档',
    deleted: '删除',
  };
  return labels[value || ''] || value || '未知';
}

function syncLabel(value?: string) {
  const labels: Record<string, string> = {
    pending: '待同步',
    synced: '已同步',
    failed: '失败',
    skipped: '跳过',
  };
  return labels[value || ''] || value || '未知';
}

function tone(value?: string) {
  if (['active', 'synced'].includes(value || '')) return 'success';
  if (['pending', 'archived', 'skipped'].includes(value || '')) return 'warning';
  if (['failed', 'deleted'].includes(value || '')) return 'danger';
  return 'neutral';
}

function formatTime(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
}

function shortText(value?: string, length = 36) {
  if (!value) return '-';
  return value.length > length ? `${value.slice(0, length)}...` : value;
}
</script>

<template>
  <PageHeader title="Memory 记忆中心" description="管理 Agent 短期记忆、长期记忆、任务记忆和向量记忆">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="reloadList">
        <RefreshCw :size="16" /> 刷新
      </button>
      <button class="primary-button" type="button" @click="openCreateModal">
        <Plus :size="16" /> 新增记忆
      </button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
  <p v-if="successMessage" class="form-success">{{ successMessage }}</p>

  <section class="metric-grid">
    <StatCard label="总记忆" :value="String(overview?.totalCount || 0)" detail="当前可见" icon="BrainCircuit" tone="neutral" />
    <StatCard label="长期记忆" :value="String(overview?.longTermCount || 0)" detail="跨会话可召回" icon="Sparkles" tone="success" />
    <StatCard label="向量记忆" :value="String(overview?.vectorCount || 0)" detail="语义召回" icon="Database" tone="info" />
    <StatCard label="待同步" :value="String(overview?.pendingSyncCount || 0)" detail="等待向量化" icon="RefreshCw" tone="warning" />
  </section>

  <section class="section-block">
    <div class="filter-row">
      <select v-model="filters.memoryType" @change="searchMemories">
        <option value="all">全部类型</option>
        <option value="short_term">短期记忆</option>
        <option value="long_term">长期记忆</option>
        <option value="task">任务记忆</option>
        <option value="vector">向量记忆</option>
      </select>
      <select v-model="filters.status" @change="searchMemories">
        <option value="all">全部状态</option>
        <option value="active">启用</option>
        <option value="archived">归档</option>
      </select>
      <select v-model="filters.agentId" @change="searchMemories">
        <option value="all">全部 Agent</option>
        <option v-for="agent in agents" :key="agent.id" :value="agent.id">{{ agent.agentName }}</option>
      </select>
      <label class="search-box">
        <Search :size="16" />
        <input v-model="filters.keyword" placeholder="搜索记忆内容或密钥" @keyup.enter="searchMemories" />
      </label>
      <button class="secondary-button" type="button" @click="searchMemories">查询</button>
    </div>
  </section>

  <section class="governance-card-tabs memory-card-tabs">
    <button class="governance-tab-card" :class="{ active: activePanel === 'list' }" type="button" @click="activePanel = 'list'">
      <span>记忆列表</span>
      <b>{{ total }}</b>
      <small>{{ selectedAgentName }}</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'recall' }" type="button" @click="activePanel = 'recall'">
      <span>召回测试</span>
      <b>{{ recallResults.length }}</b>
      <small>查看 Agent 可参考的上下文</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'cleanup' }" type="button" @click="activePanel = 'cleanup'">
      <span>记忆治理</span>
      <b>{{ overview?.expiredCount || 0 }}</b>
      <small>过期归档和低价值清理</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'operations' }" type="button" @click="activePanel = 'operations'">
      <span>运营指标</span><b>{{ productionOverview?.last30Days?.recallTotal || 0 }}</b><small>近30天召回调用</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'issues' }" type="button" @click="activePanel = 'issues'">
      <span>治理问题</span><b>{{ productionOverview?.openIssues || 0 }}</b><small>冲突、低价值和同步失败</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'policies' }" type="button" @click="activePanel = 'policies'">
      <span>策略配置</span><b>{{ policies.length }}</b><small>提取、召回、隐私和配额</small>
    </button>
  </section>

  <section class="section-block prompt-center-panel memory-center-panel">
    <template v-if="activePanel === 'list'">
      <div class="section-title">
        <h2>记忆列表</h2>
        <div class="title-actions">
          <span>共 {{ total }} 条</span>
          <button class="primary-button slim" type="button" @click="openCreateModal"><Plus :size="14" /> 新增记忆</button>
        </div>
      </div>

      <table class="data-table memory-table">
        <thead>
          <tr><th>记忆</th><th>Agent</th><th>状态</th><th>向量</th><th>重要度</th><th>命中</th><th>更新时间</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="memory in memories" :key="memory.id">
            <td>
              <b>{{ memoryTypeLabel(memory.memoryType) }}</b>
              <span class="muted block" :title="memory.memoryText">{{ shortText(memory.memoryText, 54) }}</span>
            </td>
            <td><span :title="memory.agentName">{{ shortText(memory.agentName, 18) }}</span></td>
            <td><StatusBadge :label="statusLabel(memory.status)" :tone="tone(memory.status)" /></td>
            <td><StatusBadge :label="syncLabel(memory.syncStatus)" :tone="tone(memory.syncStatus)" /></td>
            <td>{{ Number(memory.importanceScore ?? 0).toFixed(2) }}</td>
            <td>{{ memory.hitCount || 0 }}</td>
            <td>{{ formatTime(memory.updatedAt) }}</td>
            <td>
              <div class="table-actions">
                <button class="secondary-button slim" type="button" @click="editMemory(memory)">编辑</button>
                <button class="secondary-button slim danger-text" type="button" @click="removeMemory(memory)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <PaginationBar :page="filters.pageNo" :total="total" :page-size="filters.pageSize" @update:page="changePage" />
      <div v-if="!loading && memories.length === 0" class="empty-state">暂无记忆</div>
    </template>

    <template v-else-if="activePanel === 'recall'">
      <div class="section-title">
        <h2>召回测试</h2>
        <div class="title-actions"><span>{{ recallResults.length }} 条命中</span></div>
      </div>
      <div class="form-grid compact-form">
        <label>Agent
          <select v-model="recallForm.agentId">
            <option value="">请选择 Agent</option>
            <option v-for="agent in agents" :key="agent.id" :value="agent.id">{{ agent.agentName }}</option>
          </select>
        </label>
        <label>会话 ID<input v-model="recallForm.sessionId" placeholder="短期记忆可填会话 ID" /></label>
        <label>返回条数<input v-model.number="recallForm.limit" type="number" min="1" max="20" /></label>
        <label class="wide">测试问题<textarea v-model="recallForm.query" rows="4" placeholder="输入用户问题，测试当前 Agent 能召回哪些记忆"></textarea></label>
      </div>
      <div class="toolbar compact">
        <button class="primary-button" type="button" :disabled="loading" @click="runRecall">
          <Search :size="16" /> 开始召回
        </button>
      </div>
      <div class="memory-result-list">
        <article v-for="item in recallResults" :key="item.id" class="memory-result-card">
          <div class="section-title compact-title">
            <h2>{{ memoryTypeLabel(item.memoryType) }}</h2>
            <StatusBadge :label="`得分 ${item.score.toFixed(4)}`" />
          </div>
          <p>{{ item.memoryText }}</p>
          <div v-if="!item.id.startsWith('redis:')" class="table-actions">
            <button class="icon-button" type="button" title="有帮助" @click="feedback(item, 'helpful')"><ThumbsUp :size="15" /></button>
            <button class="icon-button" type="button" title="不相关" @click="feedback(item, 'irrelevant')"><ThumbsDown :size="15" /></button>
          </div>
        </article>
        <p v-if="recallResults.length === 0" class="empty-state">暂无召回结果</p>
      </div>
    </template>

    <template v-else-if="activePanel === 'cleanup'">
      <div class="section-title">
        <h2>记忆治理</h2>
        <div class="title-actions">
          <span>归档过期记忆，清理长期未命中的低价值记忆</span>
          <button class="primary-button slim" type="button" :disabled="loading" @click="runCleanup">
            <Sparkles :size="14" /> 执行清理
          </button>
        </div>
      </div>
      <div class="metric-grid">
        <StatCard label="短期记忆" :value="String(overview?.shortTermCount || 0)" detail="会话内优先" icon="MessageSquareText" tone="info" />
        <StatCard label="任务记忆" :value="String(overview?.taskCount || 0)" detail="复杂任务上下文" icon="ClipboardList" tone="warning" />
        <StatCard label="过期记忆" :value="String(overview?.expiredCount || 0)" detail="可归档" icon="Timer" tone="danger" />
        <StatCard label="待同步" :value="String(overview?.pendingSyncCount || 0)" detail="需向量化" icon="RefreshCw" tone="neutral" />
      </div>
    </template>

    <template v-else-if="activePanel === 'operations'">
      <div class="section-title"><h2>Memory 运营指标</h2><span>近30天提取、召回、反馈和向量健康</span></div>
      <div class="metric-grid">
        <StatCard label="有效记忆" :value="String(productionOverview?.active || 0)" detail="当前空间" icon="Activity" tone="success" />
        <StatCard label="提取采纳" :value="String(productionOverview?.last30Days?.extractionAccepted || 0)" detail="结构化事实" icon="Sparkles" tone="info" />
        <StatCard label="召回命中" :value="String(productionOverview?.last30Days?.recallHits || 0)" detail="注入上下文" icon="Search" tone="neutral" />
        <StatCard label="向量失败" :value="String(productionOverview?.syncFailed || 0)" detail="需要补偿" icon="Database" tone="danger" />
      </div>
      <div class="toolbar compact">
        <button class="secondary-button" type="button" :disabled="loading" @click="runGovernanceScan"><ScanSearch :size="16" /> 扫描治理问题</button>
        <button class="primary-button" type="button" :disabled="loading" @click="runVectorRebuild"><DatabaseZap :size="16" /> 重建失败向量</button>
      </div>
    </template>

    <template v-else-if="activePanel === 'issues'">
      <div class="section-title">
        <h2>治理问题</h2>
        <div class="title-actions">
          <select v-model="issueFilters.status" @change="loadIssues(1)"><option value="open">待处理</option><option value="resolved">已处置</option><option value="all">全部</option></select>
          <select v-model="issueFilters.type" @change="loadIssues(1)"><option value="all">全部类型</option><option value="conflict">事实冲突</option><option value="sync_failed">同步失败</option><option value="low_value">低价值</option><option value="sensitive">敏感内容</option></select>
          <button class="secondary-button slim" type="button" @click="runGovernanceScan"><ScanSearch :size="14" /> 立即扫描</button>
        </div>
      </div>
      <table class="data-table"><thead><tr><th>类型</th><th>严重度</th><th>事实键</th><th>内容</th><th>状态</th><th>操作</th></tr></thead>
        <tbody><tr v-for="issue in issues" :key="issue.id">
          <td>{{ issue.issue_type }}</td><td><StatusBadge :label="issue.severity" :tone="issue.severity === 'high' || issue.severity === 'critical' ? 'danger' : 'warning'" /></td>
          <td>{{ issue.fact_key || '-' }}</td><td><span :title="issue.memory_text">{{ shortText(issue.memory_text, 48) }}</span></td><td>{{ issue.status }}</td>
          <td><button v-if="issue.status === 'open'" class="primary-button slim" type="button" @click="resolveIssue(issue)">确认处置</button></td>
        </tr></tbody>
      </table>
      <PaginationBar :page="issueFilters.pageNo" :total="issueTotal" :page-size="issueFilters.pageSize" @update:page="loadIssues" />
      <div v-if="issues.length === 0" class="empty-state">暂无治理问题</div>
    </template>

    <template v-else>
      <div class="section-title"><h2>Memory 策略</h2><button class="primary-button slim" type="button" @click="openPolicyModal()"><Plus :size="14" /> 新增策略</button></div>
      <table class="data-table"><thead><tr><th>策略</th><th>范围</th><th>重要度</th><th>置信度</th><th>召回阈值</th><th>Token预算</th><th>隐私</th><th>状态</th><th>操作</th></tr></thead>
        <tbody><tr v-for="policy in policies" :key="policy.id"><td>{{ policy.policy_name }}</td><td>{{ policy.agent_id ? 'Agent' : '空间默认' }}</td>
          <td>{{ Number(policy.min_importance).toFixed(2) }}</td><td>{{ Number(policy.min_confidence).toFixed(2) }}</td><td>{{ Number(policy.recall_threshold).toFixed(2) }}</td>
          <td>{{ policy.prompt_token_budget }}</td><td>{{ policy.pii_mode }}</td><td><StatusBadge :label="policy.status" :tone="policy.status === 'enabled' ? 'success' : 'warning'" /></td>
          <td><button class="secondary-button slim" type="button" @click="openPolicyModal(policy)">编辑</button></td></tr></tbody>
      </table>
    </template>
  </section>

  <div v-if="memoryModalOpen" class="overlay-backdrop">
    <div class="modal-panel memory-modal">
      <div class="overlay-header">
        <div>
          <h2>{{ editingId ? '编辑记忆' : '新增记忆' }}</h2>
          <p>手动维护 Agent 可召回的长期、任务或向量记忆</p>
        </div>
        <button class="icon-button" type="button" @click="closeModal"><X :size="18" /></button>
      </div>
      <div class="memory-modal-body">
        <div class="form-grid">
          <label>Agent
            <select v-model="form.agentId">
              <option value="">用户通用记忆</option>
              <option v-for="agent in agents" :key="agent.id" :value="agent.id">{{ agent.agentName }}</option>
            </select>
          </label>
          <label>记忆类型
            <select v-model="form.memoryType">
              <option value="short_term">短期记忆</option>
              <option value="long_term">长期记忆</option>
              <option value="task">任务记忆</option>
              <option value="vector">向量记忆</option>
            </select>
          </label>
          <label>状态
            <select v-model="form.status">
              <option value="active">启用</option>
              <option value="archived">归档</option>
            </select>
          </label>
          <label>重要度<input v-model.number="form.importanceScore" type="number" step="0.01" min="0" max="1" /></label>
          <label>记忆密钥<input v-model="form.memoryKey" placeholder="可选，用于去重或定位来源" /></label>
          <label>会话 ID<input v-model="form.sessionId" placeholder="短期记忆可绑定会话" /></label>
        </div>
        <label>记忆文本<textarea v-model="form.memoryText" rows="6" placeholder="填写需要 Agent 后续参考的事实、偏好、任务上下文"></textarea></label>
        <div class="form-grid">
          <label>过期时间<input v-model="form.expiredAt" type="datetime-local" /></label>
          <label>可见范围
            <select v-model="form.privacyScope">
              <option value="private">个人</option>
              <option value="agent">Agent</option>
              <option value="workspace">工作空间</option>
            </select>
          </label>
        </div>
        <label>标签 JSON<textarea v-model="form.tagsJson" rows="2"></textarea></label>
        <label>结构化值 JSON<textarea v-model="form.memoryValue" rows="3"></textarea></label>
      </div>
      <div class="form-actions modal-actions">
        <button class="secondary-button" type="button" @click="closeModal"><X :size="16" /> 取消</button>
        <button class="primary-button" type="button" :disabled="loading || !form.memoryText.trim()" @click="saveMemory">
          <Save :size="16" /> 保存
        </button>
      </div>
    </div>
  </div>

  <div v-if="policyModalOpen" class="overlay-backdrop">
    <div class="modal-panel memory-modal">
      <div class="overlay-header"><div><h2>Memory 策略</h2><p>控制结构化提取、召回阈值、Prompt预算、隐私和容量</p></div><button class="icon-button" type="button" @click="policyModalOpen = false"><X :size="18" /></button></div>
      <div class="memory-modal-body">
        <div class="form-grid">
          <label>策略名称<input v-model="policyForm.policyName" /></label>
          <label>适用 Agent<select v-model="policyForm.agentId"><option value="">空间默认</option><option v-for="agent in agents" :key="agent.id" :value="agent.id">{{ agent.agentName }}</option></select></label>
          <label>最低重要度<input v-model.number="policyForm.minImportance" type="number" min="0" max="1" step="0.05" /></label>
          <label>最低置信度<input v-model.number="policyForm.minConfidence" type="number" min="0" max="1" step="0.05" /></label>
          <label>召回阈值<input v-model.number="policyForm.recallThreshold" type="number" min="0" max="1" step="0.05" /></label>
          <label>召回上限<input v-model.number="policyForm.recallLimit" type="number" min="1" max="50" /></label>
          <label>Prompt Token预算<input v-model.number="policyForm.promptTokenBudget" type="number" min="200" max="8000" /></label>
          <label>短期保留天数<input v-model.number="policyForm.shortTermTtlDays" type="number" min="1" max="365" /></label>
          <label>单用户容量<input v-model.number="policyForm.maxMemoriesPerUser" type="number" min="100" /></label>
          <label>PII策略<select v-model="policyForm.piiMode"><option value="redact">脱敏</option><option value="reject">拒绝保存</option><option value="allow">允许</option></select></label>
          <label>冲突策略<select v-model="policyForm.conflictMode"><option value="supersede">新事实替代旧事实</option><option value="review">进入人工治理</option><option value="keep">并存</option></select></label>
          <label>状态<select v-model="policyForm.status"><option value="enabled">启用</option><option value="disabled">停用</option></select></label>
        </div>
      </div>
      <div class="form-actions modal-actions"><button class="secondary-button" type="button" @click="policyModalOpen = false"><X :size="16" /> 取消</button><button class="primary-button" type="button" :disabled="loading" @click="persistPolicy"><Save :size="16" /> 保存策略</button></div>
    </div>
  </div>
</template>
