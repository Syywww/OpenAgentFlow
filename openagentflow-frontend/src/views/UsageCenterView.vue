<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Download, Pencil, RefreshCw, Save, Search, Trash2 } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import StatCard from '../components/StatCard.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { fetchAgents, type AgentSummary } from '../api/agents';
import { fetchModelProviders, type ModelConfigSummary, type ModelProviderSummary } from '../api/models';
import {
  createUsageQuota,
  deleteUsageQuota,
  exportUsageCalls,
  fetchUsageBreakdown,
  fetchUsageCalls,
  fetchUsageConsole,
  fetchUsageQuotas,
  recalculateUsageCosts,
  updateUsageQuota,
  type BreakdownItem,
  type QuotaRequest,
  type QuotaSummary,
  type UsageCallDetail,
  type UsageConsoleData,
} from '../api/usage';

const router = useRouter();
const loading = ref(false);
const errorMessage = ref('');
const consoleData = ref<UsageConsoleData | null>(null);
const calls = ref<UsageCallDetail[]>([]);
const quotas = ref<QuotaSummary[]>([]);
const agents = ref<AgentSummary[]>([]);
const providers = ref<ModelProviderSummary[]>([]);
const dimension = ref('model');
const dimensionRows = ref<BreakdownItem[]>([]);
const totalCalls = ref(0);

const filters = reactive({
  startDate: new Date(Date.now() - 6 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
  endDate: new Date().toISOString().slice(0, 10),
  providerId: 'all',
  modelId: 'all',
  agentId: 'all',
  keyword: '',
});

const quotaForm = reactive({
  id: '',
  subjectType: 'GLOBAL',
  subjectId: '',
  providerId: '',
  modelId: '',
  quotaPeriod: 'daily',
  tokenLimit: undefined as number | undefined,
  costLimit: undefined as number | undefined,
});

const allModels = computed<ModelConfigSummary[]>(() => providers.value.flatMap((provider) => provider.models));
const selectedProviderModels = computed(() => {
  if (!filters.providerId || filters.providerId === 'all') return allModels.value;
  return allModels.value.filter((model) => model.providerId === filters.providerId);
});

const successRate = computed(() => {
  const overview = consoleData.value?.overview;
  if (!overview || overview.callCount === 0) return '100%';
  return `${((overview.successCount * 100) / overview.callCount).toFixed(1)}%`;
});

onMounted(() => {
  void loadData();
});

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const params = buildParams();
    const [consoleResult, callsResult, quotaResult, agentResult, providerResult, breakdownResult] = await Promise.all([
      fetchUsageConsole(params),
      fetchUsageCalls({ ...params, pageNo: 1, pageSize: 30 }),
      fetchUsageQuotas(),
      fetchAgents(),
      fetchModelProviders(),
      fetchUsageBreakdown({ ...params, dimension: dimension.value, limit: 10 }),
    ]);
    consoleData.value = consoleResult;
    calls.value = callsResult.records;
    totalCalls.value = callsResult.total;
    quotas.value = quotaResult;
    agents.value = agentResult;
    providers.value = providerResult;
    dimensionRows.value = breakdownResult;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '成本与用量数据加载失败';
  } finally {
    loading.value = false;
  }
}

async function reloadBreakdown() {
  dimensionRows.value = await fetchUsageBreakdown({ ...buildParams(), dimension: dimension.value, limit: 10 });
}

function buildParams() {
  return {
    startDate: filters.startDate,
    endDate: filters.endDate,
    providerId: filters.providerId,
    modelId: filters.modelId,
    agentId: filters.agentId,
    keyword: filters.keyword,
  };
}

function resetQuotaForm() {
  quotaForm.id = '';
  quotaForm.subjectType = 'GLOBAL';
  quotaForm.subjectId = '';
  quotaForm.providerId = '';
  quotaForm.modelId = '';
  quotaForm.quotaPeriod = 'daily';
  quotaForm.tokenLimit = undefined;
  quotaForm.costLimit = undefined;
}

function editQuota(quota: QuotaSummary) {
  quotaForm.id = quota.id;
  quotaForm.subjectType = quota.subjectType;
  quotaForm.subjectId = quota.subjectId === '00000000-0000-0000-0000-000000000000' ? '' : quota.subjectId || '';
  quotaForm.providerId = quota.providerId || '';
  quotaForm.modelId = quota.modelId || '';
  quotaForm.quotaPeriod = quota.quotaPeriod || 'daily';
  quotaForm.tokenLimit = quota.tokenLimit;
  quotaForm.costLimit = quota.costLimit;
}

function buildQuotaPayload(): QuotaRequest {
  return {
    subjectType: quotaForm.subjectType,
    subjectId: quotaForm.subjectId || undefined,
    providerId: quotaForm.providerId || undefined,
    modelId: quotaForm.modelId || undefined,
    quotaPeriod: quotaForm.quotaPeriod,
    tokenLimit: quotaForm.tokenLimit ? Number(quotaForm.tokenLimit) : undefined,
    costLimit: quotaForm.costLimit ? Number(quotaForm.costLimit) : undefined,
  };
}

async function saveQuota() {
  loading.value = true;
  errorMessage.value = '';
  try {
    if (quotaForm.id) {
      await updateUsageQuota(quotaForm.id, buildQuotaPayload());
    } else {
      await createUsageQuota(buildQuotaPayload());
    }
    resetQuotaForm();
    quotas.value = await fetchUsageQuotas();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '配额保存失败';
  } finally {
    loading.value = false;
  }
}

async function removeQuota(quota: QuotaSummary) {
  loading.value = true;
  errorMessage.value = '';
  try {
    await deleteUsageQuota(quota.id);
    quotas.value = await fetchUsageQuotas();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '配额删除失败';
  } finally {
    loading.value = false;
  }
}

async function downloadCalls() {
  const blob = await exportUsageCalls(buildParams());
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'openagentflow-usage-calls.csv';
  link.click();
  URL.revokeObjectURL(url);
}

async function recalculateCosts() {
  loading.value = true;
  errorMessage.value = '';
  try {
    await recalculateUsageCosts();
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '历史成本重算失败';
  } finally {
    loading.value = false;
  }
}

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toFixed(6)}`;
}

function formatNumber(value?: number) {
  return Number(value || 0).toLocaleString();
}

function formatMs(value?: number) {
  if (!value) return '0ms';
  return value < 1000 ? `${value}ms` : `${(value / 1000).toFixed(2)}s`;
}

function quotaLabel(quota: QuotaSummary) {
  const subject = quota.subjectType === 'GLOBAL' ? '全局' : `${quota.subjectType}:${quota.subjectId}`;
  const period = quota.quotaPeriod === 'monthly' ? '每月' : '每日';
  return `${subject} · ${period}`;
}

function providerName(id?: string) {
  if (!id) return '全部服务商';
  return providers.value.find((provider) => provider.id === id)?.providerName || id;
}

function modelName(id?: string) {
  if (!id) return '全部模型';
  return allModels.value.find((model) => model.id === id)?.modelName || id;
}

function riskTone(quota: QuotaSummary) {
  const rate = Math.max(Number(quota.tokenUsageRate || 0), Number(quota.costUsageRate || 0));
  if (rate >= 100) return 'danger';
  if (rate >= 80) return 'warning';
  return 'success';
}
</script>

<template>
  <PageHeader title="成本与用量中心" description="按模型、用户、Agent、工作流和评测追踪真实 Token、成本、耗时与配额">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadData"><RefreshCw :size="16" /> 刷新</button>
      <button class="secondary-button" type="button" :disabled="loading" @click="recalculateCosts"><RefreshCw :size="16" /> 重算历史成本</button>
      <button class="secondary-button" type="button" @click="downloadCalls"><Download :size="16" /> 导出明细</button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
  <div v-if="Number(consoleData?.overview.totalTokens || 0) > 0 && Number(consoleData?.overview.totalCost || 0) === 0" class="insight-strip">
    <b>模型价格未配置</b>
    <p>当前已有 Token 消耗，但模型输入/输出每千 Token 单价仍为 0。请到系统设置中填写模型单价，再点击“重算历史成本”。</p>
  </div>

  <section class="filter-row">
    <input v-model="filters.startDate" type="date" />
    <input v-model="filters.endDate" type="date" />
    <select v-model="filters.providerId">
      <option value="all">服务商全部</option>
      <option v-for="provider in providers" :key="provider.id" :value="provider.id">{{ provider.providerName }}</option>
    </select>
    <select v-model="filters.modelId">
      <option value="all">模型全部</option>
      <option v-for="model in selectedProviderModels" :key="model.id" :value="model.id">{{ model.modelName }}</option>
    </select>
    <select v-model="filters.agentId">
      <option value="all">Agent 全部</option>
      <option v-for="agent in agents" :key="agent.id" :value="agent.id">{{ agent.agentName }}</option>
    </select>
    <input v-model="filters.keyword" placeholder="搜索 Run ID、输入或输出内容" @keydown.enter="loadData" />
    <button class="primary-button" type="button" :disabled="loading" @click="loadData"><Search :size="16" /> 查询</button>
  </section>

  <section class="metric-grid">
    <StatCard label="总成本" :value="formatMoney(consoleData?.overview.totalCost)" :detail="`${formatNumber(consoleData?.overview.callCount)} 次调用`" icon="Coins" tone="danger" />
    <StatCard label="Token 消耗" :value="formatNumber(consoleData?.overview.totalTokens)" :detail="`输入 ${formatNumber(consoleData?.overview.promptTokens)} / 输出 ${formatNumber(consoleData?.overview.completionTokens)}`" icon="Activity" tone="info" />
    <StatCard label="成功率" :value="successRate" :detail="`失败 ${formatNumber(consoleData?.overview.failureCount)} 次`" icon="ShieldCheck" tone="success" />
    <StatCard label="配额风险" :value="String(consoleData?.overview.quotaRiskCount ?? 0)" :detail="`${consoleData?.overview.quotaRuleCount ?? 0} 条配额规则`" icon="Gauge" tone="warning" />
  </section>

  <section class="dashboard-columns">
    <div class="section-block">
      <div class="section-title">
        <h2>成本趋势</h2>
        <span>{{ filters.startDate }} ~ {{ filters.endDate }}</span>
      </div>
      <div class="usage-bars">
        <div v-for="item in consoleData?.daily || []" :key="item.statDate" class="bar-row">
          <div><b>{{ item.statDate }}</b><span>{{ formatMoney(item.totalCost) }} · {{ formatNumber(item.totalTokens) }} Tokens</span></div>
          <i><em :style="{ width: `${Math.min(100, Number(item.totalCost || 0) * 100)}%` }" /></i>
        </div>
        <div v-if="!loading && (consoleData?.daily || []).length === 0" class="empty-state">暂无成本趋势数据</div>
      </div>
    </div>

    <div class="section-block">
      <div class="section-title">
        <h2>维度拆分</h2>
        <select v-model="dimension" @change="reloadBreakdown">
          <option value="model">按模型</option>
          <option value="provider">按服务商</option>
          <option value="agent">按 Agent</option>
          <option value="user">按用户</option>
          <option value="workflow">按工作流</option>
          <option value="eval">按评测任务</option>
        </select>
      </div>
      <div class="usage-bars">
        <div v-for="item in dimensionRows" :key="item.id || item.name" class="bar-row">
          <div><b>{{ item.name || '未命名' }}</b><span>{{ formatMoney(item.totalCost) }} · {{ item.callCount }} 次</span></div>
          <i><em :style="{ width: `${Math.min(100, Number(item.totalTokens || 0) / 100)}%` }" /></i>
        </div>
        <div v-if="dimensionRows.length === 0" class="empty-state">暂无维度统计数据</div>
      </div>
    </div>
  </section>

  <section class="section-block">
    <div class="section-title">
      <h2>调用成本明细</h2>
      <span>{{ totalCalls }} 条</span>
    </div>
    <div v-if="loading" class="empty-state">正在加载用量明细...</div>
    <table v-else class="data-table">
      <thead>
        <tr><th>运行</th><th>模型</th><th>Agent / 工作流</th><th>Token</th><th>成本</th><th>耗时</th><th>状态</th><th>时间</th></tr>
      </thead>
      <tbody>
        <tr v-for="call in calls" :key="call.id" @click="router.push(`/logs/${call.runId}`)">
          <td class="mono">{{ call.runNo || call.runId }}</td>
          <td><b>{{ call.modelName || '-' }}</b><span class="muted block">{{ call.providerName || '-' }}</span></td>
          <td>{{ call.agentName || call.workflowName || '-' }}</td>
          <td>{{ call.promptTokens }} + {{ call.completionTokens }} = {{ call.totalTokens }}</td>
          <td>{{ formatMoney(call.costAmount) }}</td>
          <td>{{ formatMs(call.latencyMs) }}</td>
          <td><StatusBadge :label="call.success ? '成功' : '失败'" :tone="call.success ? 'success' : 'danger'" /></td>
          <td>{{ call.createdAt || '-' }}</td>
        </tr>
        <tr v-if="calls.length === 0"><td colspan="8" class="empty-state">暂无调用明细</td></tr>
      </tbody>
    </table>
  </section>

  <section class="dashboard-columns">
    <div class="section-block">
      <div class="section-title"><h2>配额规则</h2><span>调用前预估拦截，调用后累计真实用量</span></div>
      <table class="data-table">
        <thead><tr><th>规则</th><th>范围</th><th>Token</th><th>成本</th><th>风险</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="quota in quotas" :key="quota.id">
            <td><b>{{ quotaLabel(quota) }}</b><span class="muted block">重置 {{ quota.resetAt || '-' }}</span></td>
            <td>{{ providerName(quota.providerId) }}<span class="muted block">{{ modelName(quota.modelId) }}</span></td>
            <td>{{ formatNumber(quota.tokenUsed) }} / {{ quota.tokenLimit ? formatNumber(quota.tokenLimit) : '不限' }}</td>
            <td>{{ formatMoney(quota.costUsed) }} / {{ quota.costLimit ? formatMoney(quota.costLimit) : '不限' }}</td>
            <td><StatusBadge :label="`${Math.max(Number(quota.tokenUsageRate || 0), Number(quota.costUsageRate || 0)).toFixed(1)}%`" :tone="riskTone(quota)" /></td>
            <td class="table-actions">
              <button class="secondary-button slim" type="button" @click="editQuota(quota)"><Pencil :size="14" /> 编辑</button>
              <button class="secondary-button slim danger-text" type="button" @click="removeQuota(quota)"><Trash2 :size="14" /> 删除</button>
            </td>
          </tr>
          <tr v-if="quotas.length === 0"><td colspan="6" class="empty-state">暂无配额规则</td></tr>
        </tbody>
      </table>
    </div>

    <div class="section-block">
      <div class="section-title"><h2>{{ quotaForm.id ? '编辑配额' : '新增配额' }}</h2><span>超过配额会阻断模型调用</span></div>
      <div class="settings-form">
        <label>主体类型
          <select v-model="quotaForm.subjectType">
            <option value="GLOBAL">全局</option>
            <option value="USER">用户</option>
            <option value="ROLE">角色</option>
            <option value="AGENT">Agent</option>
            <option value="PROVIDER">服务商</option>
            <option value="MODEL">模型</option>
          </select>
        </label>
        <label>主体 ID<input v-model="quotaForm.subjectId" placeholder="全局可留空" /></label>
        <label>服务商范围
          <select v-model="quotaForm.providerId">
            <option value="">全部服务商</option>
            <option v-for="provider in providers" :key="provider.id" :value="provider.id">{{ provider.providerName }}</option>
          </select>
        </label>
        <label>模型范围
          <select v-model="quotaForm.modelId">
            <option value="">全部模型</option>
            <option v-for="model in allModels" :key="model.id" :value="model.id">{{ model.modelName }}</option>
          </select>
        </label>
        <label>周期
          <select v-model="quotaForm.quotaPeriod">
            <option value="daily">每日</option>
            <option value="monthly">每月</option>
          </select>
        </label>
        <label>Token 上限<input v-model.number="quotaForm.tokenLimit" type="number" min="0" /></label>
        <label>成本上限<input v-model.number="quotaForm.costLimit" type="number" min="0" step="0.0001" /></label>
      </div>
      <div class="toolbar compact">
        <button class="primary-button" type="button" :disabled="loading" @click="saveQuota"><Save :size="16" /> 保存配额</button>
        <button class="secondary-button" type="button" @click="resetQuotaForm">重置</button>
      </div>
    </div>
  </section>
</template>
