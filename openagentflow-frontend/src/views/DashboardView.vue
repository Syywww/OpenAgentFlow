<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Bot, Braces, FileUp, GitBranch, Plug, RefreshCw, TestTube2 } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import StatCard from '../components/StatCard.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { fetchDashboardOverview, type DashboardOverview } from '../api/dashboard';
import { readCurrentUser } from '../api/permissions';
import { usePagination } from '../composables/usePagination';
import type { Metric, StatusTone } from '../types';

const router = useRouter();
const overview = ref<DashboardOverview | null>(null);
const loading = ref(false);
const errorMessage = ref('');
const currentUser = computed(() => readCurrentUser());
const recentRunRows = computed(() => overview.value?.recentRuns ?? []);
const { currentPage: recentRunPage, pagedItems: pagedRecentRuns } = usePagination(recentRunRows);

const headerDescription = computed(() => {
  const name = currentUser.value?.displayName || currentUser.value?.username || '管理员';
  return `您好，${name}，欢迎使用 OpenAgentFlow-Java`;
});

const metricCards = computed<Metric[]>(() => [
  {
    label: '智能体',
    value: formatNumber(overview.value?.agentCount),
    detail: `${formatNumber(overview.value?.publishedAgentCount)} 个已发布`,
    tone: 'info',
    icon: 'Bot',
  },
  {
    label: '知识库',
    value: formatNumber(overview.value?.knowledgeBaseCount),
    detail: `${formatNumber(overview.value?.knowledgeHealth?.documentCount)} 个文档`,
    tone: 'success',
    icon: 'Library',
  },
  {
    label: '工具/MCP',
    value: `${formatNumber(overview.value?.enabledToolCount)}/${formatNumber(overview.value?.toolCount)}`,
    detail: `${formatNumber(overview.value?.mcpServerCount)} 个 MCP 服务`,
    tone: 'neutral',
    icon: 'Braces',
  },
  {
    label: '今日运行',
    value: formatNumber(overview.value?.todayRunCount),
    detail: `成功率 ${formatPercent(overview.value?.todaySuccessRate)}`,
    tone: 'warning',
    icon: 'Activity',
  },
  {
    label: 'Token / 成本',
    value: formatCompact(overview.value?.todayTokenCount),
    detail: formatMoney(overview.value?.todayCost),
    tone: 'danger',
    icon: 'Coins',
  },
  {
    label: '待处理',
    value: `${formatNumber(overview.value?.openAlertCount)}/${formatNumber(overview.value?.taskBacklogCount)}`,
    detail: '告警 / 积压任务',
    tone: (overview.value?.openAlertCount || 0) > 0 ? 'danger' : 'info',
    icon: 'ShieldAlert',
  },
]);

const quickActions = computed(() => [
  { title: '智能体', desc: `${formatNumber(overview.value?.agentCount)} 个 Agent`, icon: Bot, action: () => router.push('/agents') },
  { title: '知识库', desc: `${formatNumber(overview.value?.knowledgeHealth?.chunkCount)} 个分片`, icon: FileUp, action: () => router.push('/knowledge') },
  { title: '调试台', desc: `${formatNumber(overview.value?.todayRunCount)} 次今日运行`, icon: TestTube2, action: () => router.push('/debug') },
  { title: '工作流', desc: `${formatNumber(overview.value?.workflowCount)} 个流程`, icon: GitBranch, action: () => router.push('/workflow') },
  { title: 'MCP', desc: `${formatNumber(overview.value?.mcpServerCount)} 个服务`, icon: Plug, action: () => router.push('/mcp') },
  { title: '工具中心', desc: `${formatNumber(overview.value?.enabledToolCount)} 个启用工具`, icon: Braces, action: () => router.push('/tools') },
]);

const maxTrendRunCount = computed(() => {
  const values = overview.value?.runTrend.map((item) => item.runCount) ?? [];
  return Math.max(1, ...values);
});

onMounted(() => {
  void loadDashboard();
});

async function loadDashboard() {
  loading.value = true;
  errorMessage.value = '';
  try {
    overview.value = await fetchDashboardOverview();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '工作台数据加载失败';
  } finally {
    loading.value = false;
  }
}

function formatNumber(value?: number | null) {
  return Number(value || 0).toLocaleString('zh-CN');
}

function formatCompact(value?: number | null) {
  const numeric = Number(value || 0);
  if (numeric >= 100000000) return `${(numeric / 100000000).toFixed(2)}亿`;
  if (numeric >= 10000) return `${(numeric / 10000).toFixed(2)}万`;
  return numeric.toLocaleString('zh-CN');
}

function formatMoney(value?: number | null) {
  return `¥${Number(value || 0).toFixed(4)}`;
}

function formatPercent(value?: number | null) {
  return `${Number(value || 0).toFixed(1)}%`;
}

function formatDuration(value?: number | null) {
  const ms = Number(value || 0);
  if (ms <= 0) return '-';
  if (ms >= 1000) return `${(ms / 1000).toFixed(2)}s`;
  return `${ms}ms`;
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', { hour12: false });
}

function shortDate(value?: string) {
  if (!value) return '-';
  return value.slice(5);
}

function trendWidth(value: number) {
  if (!value) return '0%';
  return `${Math.max(6, (value * 100) / maxTrendRunCount.value)}%`;
}

function statusTone(status?: string): StatusTone {
  const normalized = (status || '').toLowerCase();
  if (['success', 'healthy', 'resolved', 'synced', 'published'].includes(normalized)) return 'success';
  if (['failed', 'unhealthy', 'critical', 'high', 'open'].includes(normalized)) return 'danger';
  if (['warning', 'running', 'pending', 'acknowledged', 'medium'].includes(normalized)) return 'warning';
  return 'neutral';
}

function statusLabel(status?: string) {
  const map: Record<string, string> = {
    SUCCESS: '成功',
    FAILED: '失败',
    RUNNING: '运行中',
    PENDING: '排队中',
    success: '成功',
    failed: '失败',
    running: '运行中',
    pending: '排队中',
    warning: '警告',
    unhealthy: '异常',
    healthy: '健康',
    open: '打开',
    acknowledged: '处理中',
    critical: '严重',
    high: '高',
    medium: '中',
    low: '低',
  };
  return status ? map[status] || map[status.toUpperCase()] || status : '未知';
}
</script>

<template>
  <PageHeader title="工作台" :description="headerDescription">
    <template #actions>
      <button class="secondary-button" type="button">近 7 天</button>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadDashboard">
        <RefreshCw :size="16" /> {{ loading ? '刷新中' : '刷新' }}
      </button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

  <section class="metric-grid dashboard-metric-grid">
    <StatCard v-for="item in metricCards" :key="item.label" v-bind="item" />
  </section>

  <section class="section-block">
    <div class="section-title">
      <h2>快捷操作</h2>
      <span>Agent、RAG、Tool、Trace、Workflow、MCP</span>
    </div>
    <div class="quick-grid">
      <button v-for="item in quickActions" :key="item.title" class="quick-action" type="button" @click="item.action">
        <component :is="item.icon" :size="22" />
        <b>{{ item.title }}</b>
        <span>{{ item.desc }}</span>
      </button>
    </div>
  </section>

  <section class="dashboard-columns">
    <div class="section-block">
      <div class="section-title">
        <h2>最近运行记录</h2>
        <button class="link-button" type="button" @click="router.push('/logs')">查看全部</button>
      </div>
      <div class="table-scroll">
        <table class="data-table">
          <thead>
            <tr><th>运行 ID</th><th>类型</th><th>名称</th><th>状态</th><th>耗时</th><th>Tokens</th><th>成本</th></tr>
          </thead>
          <tbody>
            <tr v-for="run in pagedRecentRuns" :key="run.id" @click="router.push(`/logs/${run.id}`)">
              <td class="mono" :title="run.runNo || run.id">{{ run.runNo || run.id }}</td>
              <td>{{ run.runType }}</td>
              <td :title="run.targetName">{{ run.targetName }}</td>
              <td><StatusBadge :label="run.statusLabel || statusLabel(run.status)" /></td>
              <td>{{ formatDuration(run.latencyMs) }}</td>
              <td>{{ formatNumber(run.totalTokens) }}</td>
              <td>{{ formatMoney(run.totalCost) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar v-model:page="recentRunPage" :total="recentRunRows.length" />
      <div v-if="!loading && recentRunRows.length === 0" class="empty-state">暂无运行记录</div>
    </div>

    <div class="section-block dashboard-side-stack">
      <div>
        <div class="section-title">
          <h2>模型使用排行</h2>
          <button class="link-button" type="button" @click="router.push('/usage')">用量中心</button>
        </div>
        <div class="usage-bars">
          <div v-for="model in overview?.modelUsage || []" :key="model.modelId" class="bar-row">
            <div>
              <b :title="model.modelName">{{ model.modelName }}</b>
              <span>{{ formatNumber(model.callCount) }} 次 · {{ model.providerName }}</span>
            </div>
            <i><em :style="{ width: formatPercent(model.usagePercent) }" /></i>
          </div>
          <div v-if="!loading && (overview?.modelUsage || []).length === 0" class="empty-state">暂无模型调用数据</div>
        </div>
      </div>

      <div class="dashboard-insights">
        <div v-for="item in overview?.insights || []" :key="item.title" class="insight-strip" :class="`tone-${item.tone}`">
          <b>{{ item.title }}</b>
          <p>{{ item.content }}</p>
        </div>
      </div>
    </div>
  </section>

  <section class="dashboard-columns">
    <div class="section-block">
      <div class="section-title">
        <h2>近 7 天运行趋势</h2>
        <span>运行、失败、Token、成本</span>
      </div>
      <div class="dashboard-trend-list">
        <div v-for="item in overview?.runTrend || []" :key="item.statDate" class="dashboard-trend-row">
          <span>{{ shortDate(item.statDate) }}</span>
          <i><em :style="{ width: trendWidth(item.runCount) }" /></i>
          <b>{{ formatNumber(item.runCount) }}</b>
          <small>失败 {{ formatNumber(item.failureCount) }} · {{ formatCompact(item.tokenCount) }} tokens · {{ formatMoney(item.costAmount) }}</small>
        </div>
      </div>
    </div>

    <div class="section-block">
      <div class="section-title">
        <h2>知识库健康</h2>
        <button class="link-button" type="button" @click="router.push('/knowledge-governance')">知识治理</button>
      </div>
      <div class="dashboard-health-grid">
        <div><span>文档</span><b>{{ formatNumber(overview?.knowledgeHealth.documentCount) }}</b></div>
        <div><span>已解析</span><b>{{ formatNumber(overview?.knowledgeHealth.parsedDocumentCount) }}</b></div>
        <div><span>解析失败</span><b>{{ formatNumber(overview?.knowledgeHealth.failedDocumentCount) }}</b></div>
        <div><span>处理中</span><b>{{ formatNumber(overview?.knowledgeHealth.processingDocumentCount) }}</b></div>
        <div><span>分片</span><b>{{ formatNumber(overview?.knowledgeHealth.chunkCount) }}</b></div>
        <div><span>向量</span><b>{{ formatNumber(overview?.knowledgeHealth.embeddingCount) }}</b></div>
        <div><span>治理问题</span><b>{{ formatNumber(overview?.knowledgeHealth.openIssueCount) }}</b></div>
        <div><span>向量异常</span><b>{{ formatNumber(overview?.knowledgeHealth.unsyncedEmbeddingCount) }}</b></div>
      </div>
    </div>
  </section>

  <section class="dashboard-columns">
    <div class="section-block">
      <div class="section-title">
        <h2>任务队列</h2>
        <button class="link-button" type="button" @click="router.push('/tasks')">任务中心</button>
      </div>
      <div class="dashboard-list">
        <article v-for="task in overview?.taskQueue || []" :key="task.id" class="dashboard-list-item">
          <div>
            <b :title="task.taskName">{{ task.taskName }}</b>
            <StatusBadge :label="statusLabel(task.status)" :tone="statusTone(task.status)" />
          </div>
          <span>{{ task.taskType }} · {{ Number(task.progressPercent || 0).toFixed(0) }}% · {{ task.currentMessage || '-' }}</span>
        </article>
        <div v-if="!loading && (overview?.taskQueue || []).length === 0" class="empty-state">暂无积压任务</div>
      </div>
    </div>

    <div class="section-block">
      <div class="section-title">
        <h2>告警与健康</h2>
        <button class="link-button" type="button" @click="router.push('/ops')">运营监控</button>
      </div>
      <div class="dashboard-list compact">
        <article v-for="alert in overview?.openAlerts || []" :key="alert.id" class="dashboard-list-item">
          <div>
            <b :title="alert.alertTitle">{{ alert.alertTitle }}</b>
            <StatusBadge :label="statusLabel(alert.severity)" :tone="statusTone(alert.severity)" />
          </div>
          <span>{{ alert.metricSource }} · 当前 {{ alert.metricValue }} / 阈值 {{ alert.thresholdValue }} · {{ formatDateTime(alert.lastTriggeredAt) }}</span>
        </article>
        <article v-for="item in overview?.healthChecks || []" :key="item.id" class="dashboard-list-item">
          <div>
            <b :title="item.checkName">{{ item.checkName }}</b>
            <StatusBadge :label="statusLabel(item.status)" :tone="statusTone(item.status)" />
          </div>
          <span>{{ item.targetType }} · {{ item.targetCode }} · {{ item.message || '-' }}</span>
        </article>
        <div v-if="!loading && (overview?.openAlerts || []).length === 0 && (overview?.healthChecks || []).length === 0" class="empty-state">暂无告警和健康检查数据</div>
      </div>
    </div>
  </section>
</template>
