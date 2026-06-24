<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Copy, Edit3, Plus } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import PaginationBar from '../../components/PaginationBar.vue';
import StatCard from '../../components/StatCard.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import { copyAgent, fetchAgents, type AgentSummary } from '../../api/agents';
import { useOverlay } from '../../composables/useOverlay';
import { usePagination } from '../../composables/usePagination';

const router = useRouter();
const { toast } = useOverlay();
const agents = ref<AgentSummary[]>([]);
const loading = ref(false);
const keyword = ref('');

const filteredAgents = computed(() => {
  const text = keyword.value.trim().toLowerCase();
  if (!text) {
    return agents.value;
  }
  return agents.value.filter((agent) => {
    return [agent.agentName, agent.agentCode, agent.description, agent.category, agent.ownerName]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(text));
  });
});

const publishedCount = computed(() => agents.value.filter((agent) => agent.status === 'published').length);
const draftCount = computed(() => agents.value.filter((agent) => agent.status === 'draft').length);
const disabledCount = computed(() => agents.value.filter((agent) => agent.status === 'disabled').length);
const { currentPage: agentPage, pagedItems: pagedAgents, resetPage: resetAgentPage } = usePagination(filteredAgents);

onMounted(() => {
  loadAgents();
});

async function loadAgents() {
  loading.value = true;
  try {
    agents.value = await fetchAgents();
  } finally {
    loading.value = false;
  }
}

async function handleCopy(agent: AgentSummary) {
  const copied = await copyAgent(agent.id);
  toast('智能体已复制');
  await loadAgents();
  router.push(`/agents/${copied.id}`);
}
</script>

<template>
  <PageHeader title="智能体管理" description="创建、管理和运营 AI 智能体，配置模型、知识库、工具与安全策略">
    <template #actions>
      <button class="primary-button" type="button" @click="router.push('/agents/new')"><Plus :size="16" /> 新建智能体</button>
    </template>
  </PageHeader>

  <section class="filter-row">
    <select><option>全部类型</option></select>
    <select><option>全部状态</option></select>
    <select><option>全部模型</option></select>
    <input v-model="keyword" placeholder="搜索智能体名称、描述、负责人" @input="resetAgentPage" />
  </section>

  <section class="metric-grid">
    <StatCard label="全部" :value="String(agents.length)" detail="当前用户可见智能体" icon="Bot" tone="info" />
    <StatCard label="运行中" :value="String(publishedCount)" detail="已发布可调试运行" icon="Activity" tone="success" />
    <StatCard label="开发中" :value="String(draftCount)" detail="待完善或待发布" icon="Workflow" tone="warning" />
    <StatCard label="已暂停" :value="String(disabledCount)" detail="暂不可对外使用" icon="ShieldCheck" tone="danger" />
  </section>

  <section class="section-block">
    <table class="data-table rich">
      <thead>
        <tr><th>智能体</th><th>类型</th><th>模型</th><th>知识库</th><th>工具</th><th>状态</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="agent in pagedAgents" :key="agent.id">
          <td>
            <div class="entity-cell">
              <div class="entity-icon">A</div>
              <div><b>{{ agent.agentName }}</b><span>{{ agent.description || agent.agentCode }}</span></div>
            </div>
          </td>
          <td>{{ agent.category }}</td>
          <td>{{ agent.modelName || '未绑定' }}</td>
          <td>{{ agent.knowledgeCount }} 个</td>
          <td>{{ agent.toolCount }} 个</td>
          <td><StatusBadge :label="agent.statusLabel || agent.status" /></td>
          <td>
            <div class="table-actions">
              <button class="icon-button" type="button" title="编辑" @click="router.push(`/agents/${agent.id}`)"><Edit3 :size="16" /></button>
              <button class="icon-button" type="button" title="复制" @click="handleCopy(agent)"><Copy :size="16" /></button>
            </div>
          </td>
        </tr>
        <tr v-if="!loading && filteredAgents.length === 0">
          <td colspan="7">
            <div class="empty-state">暂无可见智能体</div>
          </td>
        </tr>
      </tbody>
    </table>
    <PaginationBar v-model:page="agentPage" :total="filteredAgents.length" />
  </section>
</template>
