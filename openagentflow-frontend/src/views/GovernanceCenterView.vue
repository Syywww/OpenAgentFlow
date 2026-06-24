<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { CheckCircle2, Eye, RefreshCw, Search, ShieldAlert, ShieldCheck, X, XCircle } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import StatCard from '../components/StatCard.vue';
import StatusBadge from '../components/StatusBadge.vue';
import type { StatusTone } from '../types';
import { usePagination } from '../composables/usePagination';
import {
  approveConfirmation,
  fetchGovernanceAudits,
  fetchGovernanceConfirmations,
  fetchGovernanceOverview,
  fetchGovernanceRisks,
  handleGovernanceRisk,
  rejectConfirmation,
  type AuditItem,
  type ConfirmationItem,
  type GovernanceOverview,
  type RiskItem,
} from '../api/governance';

const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const overview = ref<GovernanceOverview | null>(null);
const risks = ref<RiskItem[]>([]);
const audits = ref<AuditItem[]>([]);
const confirmations = ref<ConfirmationItem[]>([]);
const selectedRisk = ref<RiskItem | null>(null);
const riskTotal = ref(0);
const auditTotal = ref(0);
const riskPage = ref(1);
const auditPage = ref(1);
const activePanel = ref<'risks' | 'confirmations' | 'audits'>('risks');
const riskDetailModalOpen = ref(false);
const { currentPage: confirmationPage, pagedItems: pagedConfirmations } = usePagination(confirmations);

const filters = reactive({
  status: 'open',
  riskLevel: 'all',
  eventType: 'all',
  keyword: '',
});

const auditFilters = reactive({
  success: 'all',
  keyword: '',
});

const handleForm = reactive({
  status: 'resolved',
  handleNote: '',
});

const selectedEvidence = computed(() => JSON.stringify(selectedRisk.value?.evidence || {}, null, 2));

onMounted(() => {
  void loadData();
});

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [overviewResult, riskResult, auditResult, confirmationResult] = await Promise.all([
      fetchGovernanceOverview(),
      fetchGovernanceRisks({ ...filters, pageNo: riskPage.value, pageSize: 10 }),
      fetchGovernanceAudits({ ...buildAuditParams(), pageNo: auditPage.value, pageSize: 10 }),
      fetchGovernanceConfirmations('pending'),
    ]);
    overview.value = overviewResult;
    risks.value = riskResult.records;
    riskTotal.value = riskResult.total;
    audits.value = auditResult.records;
    auditTotal.value = auditResult.total;
    confirmations.value = confirmationResult;
    selectedRisk.value = risks.value[0] || null;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '审计与风险治理数据加载失败';
  } finally {
    loading.value = false;
  }
}

async function searchGovernance() {
  riskPage.value = 1;
  auditPage.value = 1;
  await loadData();
}

async function changeRiskPage(page: number) {
  riskPage.value = page;
  await loadData();
}

async function changeAuditPage(page: number) {
  auditPage.value = page;
  await loadData();
}

function buildAuditParams() {
  return {
    success: auditFilters.success === 'all' ? undefined : auditFilters.success === 'success',
    keyword: auditFilters.keyword,
  };
}

async function submitHandleRisk() {
  if (!selectedRisk.value) return;
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    selectedRisk.value = await handleGovernanceRisk(selectedRisk.value.id, {
      status: handleForm.status,
      handleNote: handleForm.handleNote,
    });
    successMessage.value = '风险事件已更新';
    await loadData();
    closeRiskDetail();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '风险处置失败';
  } finally {
    loading.value = false;
  }
}

async function decide(item: ConfirmationItem, approved: boolean) {
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    if (approved) {
      await approveConfirmation(item.id, '治理中心审批通过');
      successMessage.value = '确认请求已通过';
    } else {
      await rejectConfirmation(item.id, '治理中心拒绝执行');
      successMessage.value = '确认请求已拒绝';
    }
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '确认请求处理失败';
  } finally {
    loading.value = false;
  }
}

function selectRisk(item: RiskItem) {
  selectedRisk.value = item;
  handleForm.status = item.status === 'open' ? 'resolved' : item.status;
  handleForm.handleNote = item.handleNote || '';
}

function openRiskDetail(item: RiskItem) {
  selectRisk(item);
  riskDetailModalOpen.value = true;
}

function closeRiskDetail() {
  riskDetailModalOpen.value = false;
}

function riskTone(level?: string): StatusTone {
  if (level === 'high') return 'danger';
  if (level === 'medium') return 'warning';
  return 'info';
}

function statusTone(status?: string): StatusTone {
  if (status === 'resolved' || status === 'approved') return 'success';
  if (status === 'rejected') return 'danger';
  if (status === 'reviewing' || status === 'pending') return 'warning';
  return 'neutral';
}

function statusLabel(status?: string) {
  const map: Record<string, string> = {
    open: '待处置',
    reviewing: '复核中',
    resolved: '已解决',
    ignored: '已忽略',
    rejected: '已拒绝',
    pending: '待确认',
    approved: '已通过',
  };
  return map[status || ''] || status || '未知';
}

function eventTypeLabel(value?: string) {
  const map: Record<string, string> = {
    TOOL_ASSET: '工具资产',
    MCP_CAPABILITY: 'MCP 能力',
    TOOL_CONFIRM_PENDING: '高风险确认',
    TOOL_INVOCATION: '工具调用',
    GUARDRAIL_EVENT: '护栏事件',
  };
  return map[value || ''] || value || '未知';
}

function formatTime(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
}

function formatMs(value?: number) {
  if (!value) return '0ms';
  return value < 1000 ? `${value}ms` : `${(value / 1000).toFixed(2)}s`;
}
</script>

<template>
  <PageHeader title="审计与风险治理" description="统一查看操作审计、高风险工具、MCP 风险、护栏事件和确认请求">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadData">
        <RefreshCw :size="16" /> 刷新
      </button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
  <p v-if="successMessage" class="form-success">{{ successMessage }}</p>

  <section class="metric-grid">
    <StatCard label="待处置风险" :value="String(overview?.openRiskCount || 0)" detail="打开或复核中" icon="ShieldAlert" tone="danger" />
    <StatCard label="待确认请求" :value="String(overview?.pendingConfirmationCount || 0)" detail="高风险工具执行" icon="ShieldCheck" tone="warning" />
    <StatCard label="审计日志" :value="String(overview?.auditCount || 0)" detail="已采集操作" icon="Activity" tone="neutral" />
    <StatCard label="失败操作" :value="String(overview?.failedOperationCount || 0)" detail="接口失败或拒绝" icon="Gauge" tone="info" />
  </section>

  <section class="governance-card-tabs">
    <button class="governance-tab-card" :class="{ active: activePanel === 'risks' }" type="button" @click="activePanel = 'risks'">
      <span>风险事件</span>
      <b>{{ riskTotal }}</b>
      <small>工具、MCP、护栏和调用风险</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'confirmations' }" type="button" @click="activePanel = 'confirmations'">
      <span>高风险确认</span>
      <b>{{ confirmations.length }}</b>
      <small>待审批的高风险执行请求</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'audits' }" type="button" @click="activePanel = 'audits'">
      <span>操作审计</span>
      <b>{{ auditTotal }}</b>
      <small>接口访问、用户和耗时记录</small>
    </button>
  </section>

  <section class="section-block governance-center-panel">
    <template v-if="activePanel === 'risks'">
      <div class="section-title">
        <h2>风险事件</h2>
        <span>共 {{ riskTotal }} 条</span>
      </div>
      <div class="filter-row">
        <select v-model="filters.status" @change="searchGovernance">
          <option value="all">全部状态</option>
          <option value="open">待处置</option>
          <option value="reviewing">复核中</option>
          <option value="resolved">已解决</option>
          <option value="ignored">已忽略</option>
          <option value="rejected">已拒绝</option>
        </select>
        <select v-model="filters.riskLevel" @change="searchGovernance">
          <option value="all">全部风险</option>
          <option value="high">高风险</option>
          <option value="medium">中风险</option>
          <option value="low">低风险</option>
        </select>
        <select v-model="filters.eventType" @change="searchGovernance">
          <option value="all">全部类型</option>
          <option value="TOOL_ASSET">工具资产</option>
          <option value="MCP_CAPABILITY">MCP 能力</option>
          <option value="TOOL_CONFIRM_PENDING">高风险确认</option>
          <option value="TOOL_INVOCATION">工具调用</option>
          <option value="GUARDRAIL_EVENT">护栏事件</option>
        </select>
        <label class="search-input">
          <Search :size="16" />
          <input v-model="filters.keyword" placeholder="搜索风险标题、编码" @keyup.enter="searchGovernance" />
        </label>
      </div>

      <table class="data-table">
        <thead>
          <tr><th>风险</th><th>类型</th><th>级别</th><th>状态</th><th>空间</th><th>时间</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in risks" :key="item.id" :class="{ selected: selectedRisk?.id === item.id }" @click="openRiskDetail(item)">
            <td><b>{{ item.title }}</b><span class="muted block">{{ item.eventCode }}</span></td>
            <td>{{ eventTypeLabel(item.eventType) }}</td>
            <td><StatusBadge :label="item.riskLabel" :tone="riskTone(item.riskLevel)" /></td>
            <td><StatusBadge :label="statusLabel(item.status)" :tone="statusTone(item.status)" /></td>
            <td>{{ item.workspaceName || '-' }}</td>
            <td>{{ formatTime(item.createdAt) }}</td>
            <td>
              <button class="icon-button" type="button" title="详情" @click.stop="openRiskDetail(item)">
                <Eye :size="16" />
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <PaginationBar :page="riskPage" :total="riskTotal" @update:page="changeRiskPage" />
    </template>

    <template v-else-if="activePanel === 'confirmations'">
      <div class="section-title">
        <h2>高风险确认</h2>
        <span>{{ confirmations.length }} 条待处理</span>
      </div>
      <table class="data-table">
        <thead><tr><th>工具</th><th>请求人</th><th>原因</th><th>状态</th><th>过期时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in pagedConfirmations" :key="item.id">
            <td><b>{{ item.toolName || item.toolId }}</b><span class="muted block">{{ item.id }}</span></td>
            <td>{{ item.requesterUserId || '-' }}</td>
            <td>{{ item.reason || '-' }}</td>
            <td><StatusBadge :label="statusLabel(item.status)" :tone="statusTone(item.status)" /></td>
            <td>{{ formatTime(item.expiredAt) }}</td>
            <td>
              <div class="table-actions">
                <button class="icon-button" type="button" title="通过" @click="decide(item, true)"><CheckCircle2 :size="16" /></button>
                <button class="icon-button" type="button" title="拒绝" @click="decide(item, false)"><XCircle :size="16" /></button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="confirmationPage" :total="confirmations.length" />
    </template>

    <template v-else>
      <div class="section-title">
        <h2>操作审计</h2>
        <span>共 {{ auditTotal }} 条</span>
      </div>
      <div class="filter-row">
        <select v-model="auditFilters.success" @change="searchGovernance">
          <option value="all">全部结果</option>
          <option value="success">成功</option>
          <option value="failed">失败</option>
        </select>
        <label class="search-input">
          <Search :size="16" />
          <input v-model="auditFilters.keyword" placeholder="搜索用户、路径、IP" @keyup.enter="searchGovernance" />
        </label>
      </div>
      <table class="data-table">
        <thead><tr><th>用户</th><th>操作</th><th>路径</th><th>结果</th><th>耗时</th><th>时间</th></tr></thead>
        <tbody>
          <tr v-for="item in audits" :key="item.id">
            <td>{{ item.username || '-' }}</td>
            <td>{{ item.operationType }} / {{ item.requestMethod }}</td>
            <td><span class="mono">{{ item.requestPath }}</span></td>
            <td><StatusBadge :label="item.success ? '成功' : '失败'" :tone="item.success ? 'success' : 'danger'" /></td>
            <td>{{ formatMs(item.latencyMs) }}</td>
            <td>{{ formatTime(item.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
      <PaginationBar :page="auditPage" :total="auditTotal" @update:page="changeAuditPage" />
    </template>
  </section>

  <div v-if="riskDetailModalOpen" class="overlay-backdrop" @click.self="closeRiskDetail">
    <section class="modal-panel risk-detail-modal">
      <header class="overlay-header">
        <div>
          <h2>风险详情</h2>
          <p class="muted">{{ selectedRisk ? eventTypeLabel(selectedRisk.eventType) : '未选择' }}</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closeRiskDetail"><X :size="18" /></button>
      </header>

      <div v-if="selectedRisk" class="risk-detail-content">
        <div class="trace-meta">
          <span>级别</span><b>{{ selectedRisk.riskLabel }}</b>
          <span>状态</span><b>{{ statusLabel(selectedRisk.status) }}</b>
          <span>Run ID</span><b>{{ selectedRisk.runId || '-' }}</b>
          <span>工具 ID</span><b>{{ selectedRisk.toolId || '-' }}</b>
        </div>
        <p>{{ selectedRisk.description }}</p>
        <p class="muted">{{ selectedRisk.recommendedAction }}</p>

        <div class="form-grid">
          <label>处置状态
            <select v-model="handleForm.status">
              <option value="reviewing">复核中</option>
              <option value="resolved">已解决</option>
              <option value="ignored">已忽略</option>
              <option value="rejected">已拒绝</option>
            </select>
          </label>
          <label class="wide">处置备注<textarea v-model="handleForm.handleNote" placeholder="记录判断依据、处理动作或后续跟进" /></label>
        </div>
        <button class="primary-button full" type="button" :disabled="loading" @click="submitHandleRisk">
          <CheckCircle2 :size="16" /> 保存处置
        </button>

        <div class="section-title compact-title"><h2>风险证据</h2></div>
        <pre class="json-preview">{{ selectedEvidence }}</pre>
      </div>
      <div v-else class="empty-state">暂无风险事件</div>
    </section>
  </div>
</template>
