<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, Edit3, PlayCircle, RefreshCw } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import PaginationBar from '../../components/PaginationBar.vue';
import StatCard from '../../components/StatCard.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import {
  discoverMcpServer,
  fetchMcpCapabilities,
  fetchMcpServers,
  type McpCapabilitySummary,
  type McpServerSummary,
} from '../../api/mcp';
import { fetchTools, testTool, type ToolDefinitionSummary, type ToolExecutionResult } from '../../api/tools';
import { useOverlay } from '../../composables/useOverlay';
import { usePagination } from '../../composables/usePagination';

const route = useRoute();
const router = useRouter();
const { toast } = useOverlay();
const servers = ref<McpServerSummary[]>([]);
const capabilities = ref<McpCapabilitySummary[]>([]);
const tools = ref<ToolDefinitionSummary[]>([]);
const selectedServerId = ref('');
const loading = ref(false);
const discovering = ref(false);
const testingToolId = ref('');
const testResult = ref<ToolExecutionResult | null>(null);

const selectedServer = computed(() => servers.value.find((server) => server.id === selectedServerId.value));
const serverTools = computed(() => tools.value.filter((tool) => tool.toolType === 'MCP' && tool.mcpServerId === selectedServerId.value));
const enabledToolCount = computed(() => serverTools.value.filter((tool) => tool.enabled).length);
const highRiskCount = computed(() => serverTools.value.filter((tool) => tool.riskLevel === 'high').length);
const { currentPage: serverPage, pagedItems: pagedServers } = usePagination(servers);
const { currentPage: toolPage, pagedItems: pagedServerTools } = usePagination(serverTools);
const { currentPage: capabilityPage, pagedItems: pagedCapabilities } = usePagination(capabilities);

onMounted(async () => {
  await loadData();
});

watch(selectedServerId, async () => {
  await loadCapabilities();
});

async function loadData() {
  loading.value = true;
  try {
    const [serverResult, toolResult] = await Promise.all([fetchMcpServers(), fetchTools()]);
    servers.value = serverResult;
    tools.value = toolResult;
    const queryServerId = String(route.query.serverId || '');
    selectedServerId.value = serverResult.some((server) => server.id === queryServerId)
      ? queryServerId
      : serverResult[0]?.id || '';
    await loadCapabilities();
  } finally {
    loading.value = false;
  }
}

async function loadCapabilities() {
  if (!selectedServerId.value) {
    capabilities.value = [];
    return;
  }
  capabilities.value = await fetchMcpCapabilities(selectedServerId.value);
}

async function rediscover() {
  if (!selectedServerId.value) return;
  discovering.value = true;
  testResult.value = null;
  try {
    const result = await discoverMcpServer(selectedServerId.value);
    if (result.status === 'pending' || result.status === 'running') {
      toast('MCP 能力发现任务已提交，可在异步任务中心查看进度');
    } else {
      toast(result.status === 'success' ? 'MCP 能力已重新发现' : 'MCP 能力发现失败');
      await loadData();
    }
  } finally {
    discovering.value = false;
  }
}

async function handleTest(tool: ToolDefinitionSummary) {
  testingToolId.value = tool.id;
  testResult.value = null;
  try {
    testResult.value = await testTool(tool.id, {});
    toast(testResult.value.success ? 'MCP 工具测试成功' : 'MCP 工具测试失败');
  } finally {
    testingToolId.value = '';
  }
}

function formatEndpoint(server?: McpServerSummary) {
  if (!server) return '-';
  return server.endpointUrl || server.command || '-';
}
</script>

<template>
  <PageHeader title="MCP / 工具发现与管理" description="从 MCP Server 发现可用工具，并纳入工具中心统一启停和审计">
    <template #actions>
      <button class="secondary-button" type="button" @click="router.push('/mcp')"><ArrowLeft :size="16" /> 返回</button>
      <button class="primary-button" type="button" :disabled="discovering || !selectedServerId" @click="rediscover"><RefreshCw :size="16" /> {{ discovering ? '发现中' : '重新发现' }}</button>
    </template>
  </PageHeader>

  <section class="mcp-tools-layout">
    <aside class="document-list">
      <button
        v-for="server in pagedServers"
        :key="server.id"
        class="document-item"
        :class="{ active: selectedServerId === server.id }"
        type="button"
        @click="selectedServerId = server.id"
      >
        <b>{{ server.serverName }}</b>
        <span class="mono">{{ server.serverCode }}</span>
        <StatusBadge :label="server.status" :tone="server.status === 'running' ? 'success' : server.status === 'error' ? 'danger' : 'warning'" />
      </button>
      <PaginationBar v-model:page="serverPage" :total="servers.length" />
    </aside>

    <div class="section-block">
      <div class="section-title">
        <div><h2>{{ selectedServer?.serverName || '暂无 MCP Server' }}</h2><span class="mono">{{ formatEndpoint(selectedServer) }}</span></div>
      </div>
      <div class="metric-grid compact">
        <StatCard label="发现能力" :value="String(capabilities.length)" detail="Tools / Prompts / Resources" icon="Braces" tone="info" />
        <StatCard label="已同步工具" :value="String(serverTools.length)" detail="工具中心 MCP 类型" icon="ShieldCheck" tone="success" />
        <StatCard label="启用中" :value="String(enabledToolCount)" detail="可被 Agent/工作流调用" icon="Activity" tone="warning" />
        <StatCard label="高风险" :value="String(highRiskCount)" detail="默认停用并确认" icon="ShieldAlert" tone="danger" />
      </div>

      <div class="section-title"><h2>工具中心 MCP 工具</h2><span>{{ serverTools.length }} 个</span></div>
      <div v-if="loading" class="empty-state">正在加载 MCP 工具...</div>
      <div v-else-if="serverTools.length === 0" class="empty-state">暂无已同步 MCP 工具</div>
      <table v-else class="data-table">
        <thead>
          <tr><th>工具名称</th><th>MCP 名称</th><th>Code</th><th>风险</th><th>状态</th><th>调用</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="tool in pagedServerTools" :key="tool.id">
            <td><b>{{ tool.toolName }}</b></td>
            <td class="mono">{{ tool.mcpToolName }}</td>
            <td class="mono">{{ tool.toolCode }}</td>
            <td><StatusBadge :label="tool.riskLabel" :tone="tool.riskLevel === 'high' ? 'danger' : tool.riskLevel === 'medium' ? 'warning' : 'success'" /></td>
            <td><span class="switch" :class="{ on: tool.enabled }" /></td>
            <td>{{ tool.invocationCount }} 次 / {{ tool.successRate }}%</td>
            <td>
              <div class="table-actions">
                <button class="icon-button" type="button" title="测试" :disabled="testingToolId === tool.id" @click="handleTest(tool)"><PlayCircle :size="16" /></button>
                <button class="icon-button" type="button" title="编辑" @click="router.push(`/tools/${tool.id}`)"><Edit3 :size="16" /></button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-if="serverTools.length > 0" v-model:page="toolPage" :total="serverTools.length" />

      <div class="section-title"><h2>发现能力</h2><span>{{ capabilities.length }} 个</span></div>
      <table class="data-table">
        <thead>
          <tr><th>类型</th><th>名称</th><th>描述</th><th>风险</th><th>状态</th></tr>
        </thead>
        <tbody>
          <tr v-for="capability in pagedCapabilities" :key="capability.id">
            <td>{{ capability.capabilityType }}</td>
            <td class="mono">{{ capability.capabilityName }}</td>
            <td>{{ capability.description || '-' }}</td>
            <td><StatusBadge :label="capability.riskLabel" :tone="capability.riskLevel === 'high' ? 'danger' : capability.riskLevel === 'medium' ? 'warning' : 'success'" /></td>
            <td><span class="switch" :class="{ on: capability.enabled }" /></td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="capabilityPage" :total="capabilities.length" />

      <div v-if="testResult" class="process-panel">
        <div class="section-title"><h2>测试结果</h2><StatusBadge :label="testResult.success ? '成功' : '失败'" /></div>
        <pre class="code-block light">statusCode: {{ testResult.statusCode }}
latencyMs: {{ testResult.latencyMs }}
error: {{ testResult.errorMessage || '-' }}

{{ testResult.responseBody || '' }}</pre>
      </div>
    </div>
  </section>
</template>
