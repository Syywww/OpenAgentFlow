<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Edit3, Plus, RefreshCw, ShieldAlert, Trash2 } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import StatCard from '../../components/StatCard.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import { deleteTool, fetchTools, type ToolDefinitionSummary } from '../../api/tools';
import { useOverlay } from '../../composables/useOverlay';

const router = useRouter();
const { showModal, toast } = useOverlay();
const tools = ref<ToolDefinitionSummary[]>([]);
const loading = ref(false);
const keyword = ref('');
const typeFilter = ref('all');
const riskFilter = ref('all');
const statusFilter = ref('all');

const filteredTools = computed(() => tools.value.filter((tool) => {
  const keywordMatched = !keyword.value
    || tool.toolName.toLowerCase().includes(keyword.value.toLowerCase())
    || tool.toolCode.toLowerCase().includes(keyword.value.toLowerCase());
  const typeMatched = typeFilter.value === 'all' || tool.toolType === typeFilter.value;
  const riskMatched = riskFilter.value === 'all' || tool.riskLevel === riskFilter.value;
  const statusMatched = statusFilter.value === 'all' || (statusFilter.value === 'enabled' ? tool.enabled : !tool.enabled);
  return keywordMatched && typeMatched && riskMatched && statusMatched;
}));

const enabledCount = computed(() => tools.value.filter((tool) => tool.enabled).length);
const highRiskCount = computed(() => tools.value.filter((tool) => tool.riskLevel === 'high').length);
const invocationCount = computed(() => tools.value.reduce((sum, tool) => sum + (tool.invocationCount || 0), 0));

onMounted(() => {
  void loadTools();
});

async function loadTools() {
  loading.value = true;
  try {
    tools.value = await fetchTools();
  } finally {
    loading.value = false;
  }
}

async function handleDelete(tool: ToolDefinitionSummary) {
  if (!window.confirm(`确认删除工具「${tool.toolName}」？`)) {
    return;
  }
  await deleteTool(tool.id);
  toast('工具已删除');
  await loadTools();
}
</script>

<template>
  <PageHeader title="工具中心" description="管理 Agent 可调用的 REST API、数据库、Webhook 与 MCP 工具">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadTools"><RefreshCw :size="16" /> 刷新</button>
      <button class="primary-button" type="button" @click="router.push('/tools/new')"><Plus :size="16" /> 新建工具</button>
    </template>
  </PageHeader>

  <section class="filter-row">
    <select v-model="typeFilter">
      <option value="all">全部类型</option>
      <option value="REST_API">REST API</option>
      <option value="WEBHOOK">Webhook</option>
      <option value="DB_QUERY">数据库查询</option>
      <option value="MCP">MCP 工具</option>
    </select>
    <select v-model="riskFilter">
      <option value="all">全部风险</option>
      <option value="low">低风险</option>
      <option value="medium">中风险</option>
      <option value="high">高风险</option>
    </select>
    <select v-model="statusFilter">
      <option value="all">全部状态</option>
      <option value="enabled">启用中</option>
      <option value="disabled">已停用</option>
    </select>
    <input v-model="keyword" placeholder="搜索工具名称或 Code" />
  </section>

  <section class="metric-grid">
    <StatCard label="全部工具" :value="String(tools.length)" detail="REST / DB / Webhook / MCP" icon="Braces" tone="info" />
    <StatCard label="启用中" :value="String(enabledCount)" detail="可被 Agent 调用" icon="ShieldCheck" tone="success" />
    <StatCard label="高风险工具" :value="String(highRiskCount)" detail="需二次确认" icon="ShieldAlert" tone="danger" />
    <StatCard label="调用总次数" :value="String(invocationCount)" detail="累计真实调用" icon="Activity" tone="warning" />
  </section>

  <section class="section-block">
    <div class="section-title"><h2>工具列表</h2><span>{{ filteredTools.length }} 个</span></div>
    <div v-if="loading" class="empty-state">正在加载工具...</div>
    <div v-else-if="filteredTools.length === 0" class="empty-state">暂无符合条件的工具</div>
    <table v-else class="data-table">
      <thead>
        <tr><th>工具名称</th><th>类型</th><th>Code</th><th>风险等级</th><th>状态</th><th>调用次数</th><th>成功率</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="tool in filteredTools" :key="tool.id">
          <td><b>{{ tool.toolName }}</b></td>
          <td>{{ tool.toolType }}</td>
          <td class="mono">{{ tool.toolCode }}</td>
          <td><StatusBadge :label="tool.riskLabel" :tone="tool.riskLevel === 'high' ? 'danger' : tool.riskLevel === 'medium' ? 'warning' : 'success'" /></td>
          <td><span class="switch" :class="{ on: tool.enabled }" /></td>
          <td>{{ tool.invocationCount }}</td>
          <td>{{ tool.successRate }}%</td>
          <td>
            <div class="table-actions">
              <button class="icon-button" type="button" title="编辑" @click="router.push(`/tools/${tool.id}`)"><Edit3 :size="16" /></button>
              <button class="icon-button" type="button" title="风险确认" @click="showModal('risk')"><ShieldAlert :size="16" /></button>
              <button class="icon-button danger" type="button" title="删除" @click="handleDelete(tool)"><Trash2 :size="16" /></button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
