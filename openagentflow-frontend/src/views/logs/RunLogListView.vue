<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Download, RefreshCw, Search } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import StatCard from '../../components/StatCard.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import { fetchAgents, type AgentSummary } from '../../api/agents';
import { fetchRuns, fetchRunStats, type RunStats, type RunSummary } from '../../api/traces';

const router = useRouter();
const runs = ref<RunSummary[]>([]);
const agents = ref<AgentSummary[]>([]);
const stats = ref<RunStats | null>(null);
const loading = ref(false);
const statusFilter = ref('all');
const agentFilter = ref('all');
const keyword = ref('');

const successRate = computed(() => {
  if (!stats.value || stats.value.totalRuns === 0) return '100%';
  return `${((stats.value.successRuns * 100) / stats.value.totalRuns).toFixed(1)}%`;
});

onMounted(() => {
  void loadData();
});

async function loadData() {
  loading.value = true;
  try {
    const [runResult, statsResult, agentResult] = await Promise.all([
      fetchRuns({ pageNo: 1, pageSize: 50, status: statusFilter.value, agentId: agentFilter.value, keyword: keyword.value }),
      fetchRunStats(),
      fetchAgents(),
    ]);
    runs.value = runResult.records;
    stats.value = statsResult;
    agents.value = agentResult;
  } finally {
    loading.value = false;
  }
}

function formatMs(value?: number) {
  if (!value) return '0ms';
  if (value < 1000) return `${value}ms`;
  return `${(value / 1000).toFixed(2)}s`;
}

function formatCost(value?: number) {
  return `¥${Number(value || 0).toFixed(4)}`;
}
</script>

<template>
  <PageHeader title="运行日志" description="查看和分析智能体与工作流的真实运行情况，支持检索、筛选与追踪">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadData"><RefreshCw :size="16" /> 刷新</button>
      <button class="secondary-button" type="button"><Download :size="16" /> 导出</button>
    </template>
  </PageHeader>

  <section class="filter-row">
    <select v-model="agentFilter">
      <option value="all">Agent 全部</option>
      <option v-for="agent in agents" :key="agent.id" :value="agent.id">{{ agent.agentName }}</option>
    </select>
    <select>
      <option>Workflow 全部</option>
    </select>
    <select v-model="statusFilter">
      <option value="all">全部状态</option>
      <option value="SUCCESS">成功</option>
      <option value="FAILED">失败</option>
      <option value="RUNNING">运行中</option>
    </select>
    <input v-model="keyword" placeholder="搜索 Run ID、输入或输出内容" @keydown.enter="loadData" />
    <button class="primary-button" type="button" @click="loadData"><Search :size="16" /> 搜索</button>
  </section>

  <section class="metric-grid">
    <StatCard label="全部运行" :value="String(stats?.totalRuns ?? 0)" detail="累计运行" icon="Activity" tone="info" />
    <StatCard label="成功" :value="String(stats?.successRuns ?? 0)" :detail="`成功率 ${successRate}`" icon="ShieldCheck" tone="success" />
    <StatCard label="失败" :value="String(stats?.failedRuns ?? 0)" detail="可进入详情排查" icon="ShieldAlert" tone="danger" />
    <StatCard label="平均耗时" :value="formatMs(stats?.avgLatencyMs)" :detail="`Tokens ${stats?.totalTokens ?? 0}`" icon="Timer" tone="warning" />
  </section>

  <section class="section-block">
    <div class="section-title"><h2>运行记录</h2><span>{{ runs.length }} 条</span></div>
    <div v-if="loading" class="empty-state">正在加载运行记录...</div>
    <div v-else-if="runs.length === 0" class="empty-state">暂无运行记录</div>
    <table v-else class="data-table">
      <thead>
        <tr><th>运行 ID</th><th>类型</th><th>名称</th><th>状态</th><th>耗时</th><th>成本</th><th>Tokens</th><th>发起人</th><th>开始时间</th></tr>
      </thead>
      <tbody>
        <tr v-for="run in runs" :key="run.id" @click="router.push(`/logs/${run.id}`)">
          <td class="mono">{{ run.runNo || run.id }}</td>
          <td>{{ run.runType }}</td>
          <td>{{ run.agentName || '默认 Agent' }}</td>
          <td><StatusBadge :label="run.statusLabel" /></td>
          <td>{{ formatMs(run.latencyMs) }}</td>
          <td>{{ formatCost(run.totalCost) }}</td>
          <td>{{ run.totalTokens }}</td>
          <td>{{ run.userName || '-' }}</td>
          <td>{{ run.startedAt || '-' }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
