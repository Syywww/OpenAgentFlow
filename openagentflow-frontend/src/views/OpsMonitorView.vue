<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Pencil, Plus, RefreshCw, Save, Search, Trash2, X } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import StatCard from '../components/StatCard.vue';
import StatusBadge from '../components/StatusBadge.vue';
import {
  createOpsRule,
  deleteOpsRule,
  fetchOpsChannels,
  fetchOpsChecks,
  fetchOpsEvents,
  fetchOpsHealth,
  fetchOpsOverview,
  fetchOpsRules,
  handleOpsEvent,
  runOpsInspection,
  updateOpsRule,
  type OpsAlertEvent,
  type OpsAlertRule,
  type OpsAlertRuleRequest,
  type OpsHealthCheck,
  type OpsHealthItem,
  type OpsNotifyChannel,
  type OpsOverview,
} from '../api/ops';
import { usePagination } from '../composables/usePagination';

const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const overview = ref<OpsOverview | null>(null);
const healthItems = ref<OpsHealthItem[]>([]);
const alertEvents = ref<OpsAlertEvent[]>([]);
const alertRules = ref<OpsAlertRule[]>([]);
const checks = ref<OpsHealthCheck[]>([]);
const channels = ref<OpsNotifyChannel[]>([]);
const eventTotal = ref(0);
const ruleTotal = ref(0);
const selectedEvent = ref<OpsAlertEvent | null>(null);
const editingRuleId = ref('');
const activePanel = ref<'health' | 'events' | 'handle' | 'rules' | 'checks' | 'channels'>('health');
const ruleModalOpen = ref(false);

const eventFilters = reactive({
  status: 'open',
  severity: 'all',
  keyword: '',
  pageNo: 1,
  pageSize: 10,
});

const ruleFilters = reactive({
  keyword: '',
  enabled: 'all',
  pageNo: 1,
  pageSize: 10,
});

const ruleForm = reactive<OpsAlertRuleRequest>({
  ruleCode: 'custom-alert-rule',
  ruleName: '自定义告警规则',
  metricCode: 'api_failure_rate',
  metricSource: 'audit',
  operator: '>=',
  thresholdValue: 10,
  severity: 'warning',
  windowMinutes: 60,
  cooldownMinutes: 30,
  enabled: true,
  notifyChannels: 'station',
  description: '',
});

const handleForm = reactive({
  status: 'resolved',
  handleNote: '',
});

const { currentPage: healthPage, pagedItems: pagedHealthItems } = usePagination(healthItems);
const { currentPage: checkPage, pagedItems: pagedChecks } = usePagination(checks);
const { currentPage: channelPage, pagedItems: pagedChannels } = usePagination(channels);

const statusOptions = [
  { value: 'all', label: '全部状态' },
  { value: 'open', label: '待处理' },
  { value: 'acknowledged', label: '已确认' },
  { value: 'resolved', label: '已解决' },
  { value: 'ignored', label: '已忽略' },
];

const severityOptions = [
  { value: 'all', label: '全部级别' },
  { value: 'info', label: '信息' },
  { value: 'warning', label: '警告' },
  { value: 'critical', label: '严重' },
];

const metricOptions = [
  { value: 'api_failure_rate', label: 'API 失败率', source: 'audit' },
  { value: 'api_avg_latency_ms', label: 'API 平均耗时', source: 'audit' },
  { value: 'model_failure_rate', label: '模型失败率', source: 'model' },
  { value: 'model_avg_latency_ms', label: '模型平均耗时', source: 'model' },
  { value: 'task_backlog_count', label: '任务积压数', source: 'task' },
  { value: 'task_failed_count', label: '任务失败数', source: 'task' },
  { value: 'open_risk_count', label: '未处理风险数', source: 'governance' },
  { value: 'knowledge_issue_open_count', label: '知识治理问题数', source: 'knowledge' },
  { value: 'today_cost', label: '今日成本', source: 'usage' },
];

const openEventCount = computed(() => overview.value?.openAlertCount || 0);

onMounted(() => {
  void loadAll();
});

async function loadAll() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [overviewResult, healthResult, ruleResult, eventResult, checkResult, channelResult] = await Promise.all([
      fetchOpsOverview(),
      fetchOpsHealth(),
      fetchOpsRules(ruleQuery()),
      fetchOpsEvents(eventQuery()),
      fetchOpsChecks(),
      fetchOpsChannels(),
    ]);
    overview.value = overviewResult;
    healthItems.value = healthResult;
    alertRules.value = ruleResult.records;
    ruleTotal.value = ruleResult.total;
    alertEvents.value = eventResult.records;
    eventTotal.value = eventResult.total;
    checks.value = checkResult;
    channels.value = channelResult;
    selectedEvent.value = alertEvents.value[0] || null;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '运营监控数据加载失败';
  } finally {
    loading.value = false;
  }
}

async function refreshOverview() {
  const [overviewResult, healthResult, checkResult, channelResult] = await Promise.all([
    fetchOpsOverview(),
    fetchOpsHealth(),
    fetchOpsChecks(),
    fetchOpsChannels(),
  ]);
  overview.value = overviewResult;
  healthItems.value = healthResult;
  checks.value = checkResult;
  channels.value = channelResult;
}

async function inspectNow() {
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    healthItems.value = await runOpsInspection();
    await Promise.all([loadEvents(), refreshOverview()]);
    successMessage.value = '巡检完成，告警规则已重新评估';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '手动巡检失败';
  } finally {
    loading.value = false;
  }
}

async function loadEvents() {
  const result = await fetchOpsEvents(eventQuery());
  alertEvents.value = result.records;
  eventTotal.value = result.total;
  selectedEvent.value = alertEvents.value[0] || null;
}

async function loadRules() {
  const result = await fetchOpsRules(ruleQuery());
  alertRules.value = result.records;
  ruleTotal.value = result.total;
}

async function changeEventPage(page: number) {
  eventFilters.pageNo = page;
  await loadEvents();
}

async function changeRulePage(page: number) {
  ruleFilters.pageNo = page;
  await loadRules();
}

async function searchEvents() {
  eventFilters.pageNo = 1;
  await loadEvents();
}

async function searchRules() {
  ruleFilters.pageNo = 1;
  await loadRules();
}

async function saveRule() {
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    if (editingRuleId.value) {
      await updateOpsRule(editingRuleId.value, ruleForm);
    } else {
      await createOpsRule(ruleForm);
    }
    closeRuleModal();
    await loadRules();
    successMessage.value = '告警规则已保存';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '告警规则保存失败';
  } finally {
    loading.value = false;
  }
}

async function removeRule(rule: OpsAlertRule) {
  loading.value = true;
  errorMessage.value = '';
  try {
    await deleteOpsRule(rule.id);
    await loadRules();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '告警规则删除失败';
  } finally {
    loading.value = false;
  }
}

async function submitHandleEvent() {
  if (!selectedEvent.value) {
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    selectedEvent.value = await handleOpsEvent(selectedEvent.value.id, handleForm);
    await Promise.all([loadEvents(), refreshOverview()]);
    successMessage.value = '告警事件已处理';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '告警事件处理失败';
  } finally {
    loading.value = false;
  }
}

function editRule(rule: OpsAlertRule) {
  editingRuleId.value = rule.id;
  ruleForm.ruleCode = rule.ruleCode;
  ruleForm.ruleName = rule.ruleName;
  ruleForm.metricCode = rule.metricCode;
  ruleForm.metricSource = rule.metricSource;
  ruleForm.operator = rule.operator;
  ruleForm.thresholdValue = Number(rule.thresholdValue || 0);
  ruleForm.severity = rule.severity;
  ruleForm.windowMinutes = rule.windowMinutes;
  ruleForm.cooldownMinutes = rule.cooldownMinutes;
  ruleForm.enabled = rule.enabled;
  ruleForm.notifyChannels = rule.notifyChannels || 'station';
  ruleForm.description = rule.description || '';
  ruleModalOpen.value = true;
}

function resetRuleForm() {
  editingRuleId.value = '';
  ruleForm.ruleCode = `custom-alert-${Date.now()}`;
  ruleForm.ruleName = '自定义告警规则';
  ruleForm.metricCode = 'api_failure_rate';
  ruleForm.metricSource = 'audit';
  ruleForm.operator = '>=';
  ruleForm.thresholdValue = 10;
  ruleForm.severity = 'warning';
  ruleForm.windowMinutes = 60;
  ruleForm.cooldownMinutes = 30;
  ruleForm.enabled = true;
  ruleForm.notifyChannels = 'station';
  ruleForm.description = '';
}

function openCreateRuleModal() {
  resetRuleForm();
  ruleModalOpen.value = true;
}

function closeRuleModal() {
  ruleModalOpen.value = false;
  resetRuleForm();
}

function onMetricChange() {
  const option = metricOptions.find((item) => item.value === ruleForm.metricCode);
  if (option) {
    ruleForm.metricSource = option.source;
  }
}

function eventQuery() {
  return {
    status: eventFilters.status,
    severity: eventFilters.severity,
    keyword: eventFilters.keyword,
    pageNo: eventFilters.pageNo,
    pageSize: eventFilters.pageSize,
  };
}

function ruleQuery() {
  return {
    enabled: ruleFilters.enabled === 'all' ? undefined : ruleFilters.enabled === 'true',
    keyword: ruleFilters.keyword,
    pageNo: ruleFilters.pageNo,
    pageSize: ruleFilters.pageSize,
  };
}

function tone(status?: string) {
  if (['healthy', 'success', 'resolved'].includes(status || '')) return 'success';
  if (['warning', 'acknowledged'].includes(status || '')) return 'warning';
  if (['unhealthy', 'critical', 'open'].includes(status || '')) return 'danger';
  return 'neutral';
}

function severityLabel(value?: string) {
  const map: Record<string, string> = { info: '信息', warning: '警告', critical: '严重' };
  return map[value || ''] || value || '-';
}

function statusLabel(value?: string) {
  const map: Record<string, string> = {
    open: '待处理',
    acknowledged: '已确认',
    resolved: '已解决',
    ignored: '已忽略',
    healthy: '健康',
    warning: '警告',
    unhealthy: '异常',
    unknown: '未知',
    sent: '已发送',
    pending: '待发送',
    failed: '失败',
  };
  return map[value || ''] || value || '-';
}

function formatTime(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
}

function formatNumber(value?: number) {
  return Number(value || 0).toFixed(2);
}
</script>

<template>
  <PageHeader title="运营监控" description="统一查看平台健康、告警规则、告警事件、巡检任务和通知渠道">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadAll">
        <RefreshCw :size="16" /> 刷新
      </button>
      <button class="primary-button" type="button" :disabled="loading" @click="inspectNow">
        <RefreshCw :size="16" /> 立即巡检
      </button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
  <p v-if="successMessage" class="form-success">{{ successMessage }}</p>

  <section class="metric-grid">
    <StatCard label="打开告警" :value="String(openEventCount)" detail="待确认或待处理" icon="ShieldAlert" tone="danger" />
    <StatCard label="异常组件" :value="String(overview?.unhealthyComponentCount || 0)" detail="warning / unhealthy" icon="Server" tone="warning" />
    <StatCard label="任务积压" :value="String(overview?.taskBacklogCount || 0)" detail="pending + running" icon="Timer" tone="info" />
    <StatCard label="今日成本" :value="`¥${formatNumber(overview?.todayCost)}`" :detail="`${overview?.todayRunCount || 0} 次运行`" icon="Coins" tone="neutral" />
  </section>

  <section class="governance-card-tabs ops-card-tabs">
    <button class="governance-tab-card" :class="{ active: activePanel === 'health' }" type="button" @click="activePanel = 'health'">
      <span>平台健康矩阵</span>
      <b>{{ healthItems.length }}</b>
      <small>组件状态、耗时和巡检说明</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'events' }" type="button" @click="activePanel = 'events'">
      <span>告警事件</span>
      <b>{{ eventTotal }}</b>
      <small>筛选、选择和查看告警</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'handle' }" type="button" @click="activePanel = 'handle'">
      <span>告警处理</span>
      <b>{{ selectedEvent ? 1 : 0 }}</b>
      <small>确认、解决或忽略当前告警</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'rules' }" type="button" @click="activePanel = 'rules'">
      <span>告警规则</span>
      <b>{{ ruleTotal }}</b>
      <small>指标阈值、冷却和通知</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'checks' }" type="button" @click="activePanel = 'checks'">
      <span>巡检项</span>
      <b>{{ checks.length }}</b>
      <small>巡检目标、状态和下次时间</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'channels' }" type="button" @click="activePanel = 'channels'">
      <span>通知渠道</span>
      <b>{{ channels.length }}</b>
      <small>站内通知和 Webhook 渠道</small>
    </button>
  </section>

  <section class="section-block ops-monitor-panel">
    <template v-if="activePanel === 'health'">
      <div class="section-title">
        <h2>平台健康矩阵</h2>
        <span>最近巡检：{{ formatTime(overview?.lastInspectionAt) }}</span>
      </div>
      <table class="data-table">
        <thead><tr><th>组件</th><th>类型</th><th>状态</th><th>耗时</th><th>说明</th><th>检测时间</th></tr></thead>
        <tbody>
          <tr v-for="item in pagedHealthItems" :key="item.code">
            <td><b>{{ item.name }}</b><span class="muted block mono">{{ item.code }}</span></td>
            <td>{{ item.type }}</td>
            <td><StatusBadge :label="statusLabel(item.status)" :tone="tone(item.status)" /></td>
            <td>{{ item.latencyMs || 0 }}ms</td>
            <td>{{ item.message || '-' }}</td>
            <td>{{ formatTime(item.checkedAt) }}</td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="healthPage" :total="healthItems.length" />
    </template>

    <template v-else-if="activePanel === 'events'">
      <div class="section-title">
        <h2>告警事件</h2>
        <span>共 {{ eventTotal }} 条</span>
      </div>
      <div class="filter-row">
        <select v-model="eventFilters.status" @change="searchEvents">
          <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <select v-model="eventFilters.severity" @change="searchEvents">
          <option v-for="item in severityOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <label class="search-input">
          <Search :size="16" />
          <input v-model="eventFilters.keyword" placeholder="搜索标题、规则、指标" @keyup.enter="searchEvents" />
        </label>
        <button class="secondary-button" type="button" @click="searchEvents"><Search :size="16" /> 查询</button>
      </div>
      <table class="data-table">
        <thead><tr><th>告警</th><th>级别</th><th>指标</th><th>当前/阈值</th><th>状态</th><th>最近触发</th></tr></thead>
        <tbody>
          <tr v-for="event in alertEvents" :key="event.id" :class="{ selected: selectedEvent?.id === event.id }" @click="selectedEvent = event">
            <td><b>{{ event.alertTitle }}</b><span class="muted block">{{ event.eventCode }}</span></td>
            <td><StatusBadge :label="severityLabel(event.severity)" :tone="tone(event.severity)" /></td>
            <td>{{ event.metricCode }}</td>
            <td>{{ event.metricValue }} / {{ event.thresholdValue }}</td>
            <td><StatusBadge :label="statusLabel(event.status)" :tone="tone(event.status)" /></td>
            <td>{{ formatTime(event.lastTriggeredAt) }}</td>
          </tr>
        </tbody>
      </table>
      <PaginationBar :page="eventFilters.pageNo" :total="eventTotal" @update:page="changeEventPage" />
    </template>

    <template v-else-if="activePanel === 'handle'">
      <div class="section-title">
        <h2>告警处理</h2>
        <span>{{ selectedEvent?.eventCode || '请选择告警' }}</span>
      </div>
      <template v-if="selectedEvent">
        <div class="trace-meta">
          <span>规则</span><b>{{ selectedEvent.ruleCode || '-' }}</b>
          <span>指标</span><b>{{ selectedEvent.metricCode }}</b>
          <span>通知</span><b>{{ statusLabel(selectedEvent.notifyStatus) }}</b>
          <span>触发次数</span><b>{{ selectedEvent.triggerCount || 0 }}</b>
        </div>
        <p>{{ selectedEvent.alertDetail || '-' }}</p>
        <label>处理状态
          <select v-model="handleForm.status">
            <option value="acknowledged">已确认</option>
            <option value="resolved">已解决</option>
            <option value="ignored">已忽略</option>
          </select>
        </label>
        <label>处理备注<textarea v-model="handleForm.handleNote" placeholder="记录处理动作、原因或后续计划" /></label>
        <button class="primary-button full" type="button" :disabled="loading" @click="submitHandleEvent">
          <Save :size="16" /> 保存处理
        </button>
      </template>
      <div v-else class="empty-state">暂无告警事件</div>
    </template>

    <template v-else-if="activePanel === 'rules'">
      <div class="section-title">
        <h2>告警规则</h2>
        <div class="title-actions">
          <span>共 {{ ruleTotal }} 条</span>
          <button class="primary-button slim" type="button" @click="openCreateRuleModal">
            <Plus :size="14" /> 新建告警规则
          </button>
        </div>
      </div>
      <div class="filter-row">
        <select v-model="ruleFilters.enabled" @change="searchRules">
          <option value="all">全部规则</option>
          <option value="true">仅启用</option>
          <option value="false">仅停用</option>
        </select>
        <label class="search-input">
          <Search :size="16" />
          <input v-model="ruleFilters.keyword" placeholder="搜索规则名称、编码、指标" @keyup.enter="searchRules" />
        </label>
        <button class="secondary-button" type="button" @click="searchRules"><Search :size="16" /> 查询</button>
      </div>
      <table class="data-table">
        <thead><tr><th>规则</th><th>指标</th><th>条件</th><th>级别</th><th>窗口/冷却</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="rule in alertRules" :key="rule.id">
            <td><b>{{ rule.ruleName }}</b><span class="muted block mono">{{ rule.ruleCode }}</span></td>
            <td>{{ rule.metricCode }}<span class="muted block">{{ rule.metricSource }}</span></td>
            <td>{{ rule.operator }} {{ rule.thresholdValue }}</td>
            <td><StatusBadge :label="severityLabel(rule.severity)" :tone="tone(rule.severity)" /></td>
            <td>{{ rule.windowMinutes }}m / {{ rule.cooldownMinutes }}m</td>
            <td><StatusBadge :label="rule.enabled ? '启用' : '停用'" :tone="rule.enabled ? 'success' : 'neutral'" /></td>
            <td>
              <div class="table-actions">
                <button class="icon-button" type="button" title="编辑" @click="editRule(rule)"><Pencil :size="16" /></button>
                <button class="icon-button" type="button" title="删除" @click="removeRule(rule)"><Trash2 :size="16" /></button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <PaginationBar :page="ruleFilters.pageNo" :total="ruleTotal" @update:page="changeRulePage" />
    </template>

    <template v-else-if="activePanel === 'checks'">
      <div class="section-title"><h2>巡检项</h2><span>{{ checks.length }} 项</span></div>
      <table class="data-table">
        <thead><tr><th>巡检项</th><th>目标</th><th>状态</th><th>下次巡检</th></tr></thead>
        <tbody>
          <tr v-for="item in pagedChecks" :key="item.id">
            <td><b>{{ item.checkName }}</b><span class="muted block mono">{{ item.checkCode }}</span></td>
            <td>{{ item.targetType }} / {{ item.targetCode }}</td>
            <td><StatusBadge :label="statusLabel(item.status)" :tone="tone(item.status)" /></td>
            <td>{{ formatTime(item.nextCheckAt) }}</td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="checkPage" :total="checks.length" />
    </template>

    <template v-else>
      <div class="section-title"><h2>通知渠道</h2><span>{{ channels.length }} 个</span></div>
      <table class="data-table">
        <thead><tr><th>渠道</th><th>类型</th><th>状态</th><th>最近测试</th></tr></thead>
        <tbody>
          <tr v-for="item in pagedChannels" :key="item.id">
            <td><b>{{ item.channelName }}</b><span class="muted block mono">{{ item.channelCode }}</span></td>
            <td>{{ item.channelType }}</td>
            <td><StatusBadge :label="item.enabled ? '启用' : '停用'" :tone="item.enabled ? 'success' : 'neutral'" /></td>
            <td>{{ item.lastTestMessage || '-' }}</td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="channelPage" :total="channels.length" />
    </template>
  </section>

  <div v-if="ruleModalOpen" class="overlay-backdrop" @click.self="closeRuleModal">
    <section class="modal-panel ops-rule-modal">
      <header class="overlay-header">
        <div>
          <h2>{{ editingRuleId ? '编辑告警规则' : '新建告警规则' }}</h2>
          <p class="muted">配置指标、阈值、冷却时间和通知渠道，巡检时会自动评估触发。</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closeRuleModal"><X :size="18" /></button>
      </header>
      <div class="settings-form">
        <label>规则编码<input v-model="ruleForm.ruleCode" /></label>
        <label>规则名称<input v-model="ruleForm.ruleName" /></label>
        <label>指标
          <select v-model="ruleForm.metricCode" @change="onMetricChange">
            <option v-for="item in metricOptions" :key="item.value" :value="item.value">{{ item.label }} / {{ item.value }}</option>
          </select>
        </label>
        <label>来源<input v-model="ruleForm.metricSource" /></label>
        <label>操作符
          <select v-model="ruleForm.operator">
            <option value=">=">&gt;=</option>
            <option value=">">&gt;</option>
            <option value="<=">&lt;=</option>
            <option value="<">&lt;</option>
            <option value="==">==</option>
          </select>
        </label>
        <label>阈值<input v-model.number="ruleForm.thresholdValue" type="number" min="0" step="0.01" /></label>
        <label>级别
          <select v-model="ruleForm.severity">
            <option value="info">信息</option>
            <option value="warning">警告</option>
            <option value="critical">严重</option>
          </select>
        </label>
        <label>统计窗口分钟<input v-model.number="ruleForm.windowMinutes" type="number" min="1" /></label>
        <label>冷却分钟<input v-model.number="ruleForm.cooldownMinutes" type="number" min="1" /></label>
        <label>通知渠道<input v-model="ruleForm.notifyChannels" /></label>
        <label class="check-line"><input v-model="ruleForm.enabled" type="checkbox" /> 启用规则</label>
      </div>
      <label>规则说明<textarea v-model="ruleForm.description" /></label>
      <div class="toolbar compact">
        <button class="secondary-button" type="button" @click="closeRuleModal">取消</button>
        <button class="primary-button" type="button" :disabled="loading" @click="saveRule"><Save :size="16" /> 保存规则</button>
      </div>
    </section>
  </div>
</template>
