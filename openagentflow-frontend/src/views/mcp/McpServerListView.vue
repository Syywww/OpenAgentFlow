<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Edit3, Plug, RefreshCw, Search, ServerCog, Trash2 } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import PaginationBar from '../../components/PaginationBar.vue';
import StatCard from '../../components/StatCard.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import {
  createMcpServer,
  deleteMcpServer,
  discoverMcpServer,
  fetchMcpServer,
  fetchMcpServers,
  testMcpServer,
  updateMcpServer,
  type McpConnectionTestResult,
  type McpDiscoveryResult,
  type McpServerRequest,
  type McpServerSummary,
} from '../../api/mcp';
import { useOverlay } from '../../composables/useOverlay';
import { usePagination } from '../../composables/usePagination';

const router = useRouter();
const { toast } = useOverlay();
const servers = ref<McpServerSummary[]>([]);
const loading = ref(false);
const saving = ref(false);
const testingId = ref('');
const discoveringId = ref('');
const editingId = ref('');
const panelOpen = ref(false);
const operationLog = ref('');

const form = reactive({
  serverCode: '',
  serverName: '',
  description: '',
  transportType: 'http',
  command: '',
  args: '[]',
  endpointUrl: '',
  authType: 'none',
  authConfig: '{}',
  envVars: '{}',
  allowedPaths: '[]',
  riskPolicy: '{\n  "highRiskDefault": "disabled_and_confirm"\n}',
  status: 'stopped',
});

const runningCount = computed(() => servers.value.filter((server) => server.status === 'running').length);
const stoppedCount = computed(() => servers.value.filter((server) => server.status === 'stopped').length);
const errorCount = computed(() => servers.value.filter((server) => server.status === 'error').length);
const toolCount = computed(() => servers.value.reduce((sum, server) => sum + (server.toolsCount || 0), 0));
const { currentPage: serverPage, pagedItems: pagedServers } = usePagination(servers);

onMounted(() => {
  void loadServers();
});

async function loadServers() {
  loading.value = true;
  try {
    servers.value = await fetchMcpServers();
  } finally {
    loading.value = false;
  }
}

function startCreate() {
  editingId.value = '';
  panelOpen.value = true;
  form.serverCode = '';
  form.serverName = '企业 MCP Server';
  form.description = '';
  form.transportType = 'http';
  form.command = '';
  form.args = '[]';
  form.endpointUrl = 'http://localhost:7001/mcp';
  form.authType = 'none';
  form.authConfig = '{}';
  form.envVars = '{}';
  form.allowedPaths = '[]';
  form.riskPolicy = '{\n  "highRiskDefault": "disabled_and_confirm"\n}';
  form.status = 'stopped';
}

async function startEdit(server: McpServerSummary) {
  editingId.value = server.id;
  panelOpen.value = true;
  const detail = await fetchMcpServer(server.id);
  form.serverCode = detail.serverCode || '';
  form.serverName = detail.serverName || '';
  form.description = detail.description || '';
  form.transportType = detail.transportType || 'http';
  form.command = detail.command || '';
  form.args = detail.args || '[]';
  form.endpointUrl = detail.endpointUrl || '';
  form.authType = detail.authType || 'none';
  form.authConfig = detail.authConfig || '{}';
  form.envVars = detail.envVars || '{}';
  form.allowedPaths = detail.allowedPaths || '[]';
  form.riskPolicy = detail.riskPolicy || '{\n  "highRiskDefault": "disabled_and_confirm"\n}';
  form.status = detail.status || 'stopped';
}

async function saveServer() {
  saving.value = true;
  try {
    const payload = toRequest();
    const saved = editingId.value
      ? await updateMcpServer(editingId.value, payload)
      : await createMcpServer(payload);
    editingId.value = saved.id;
    toast('MCP Server 已保存');
    await loadServers();
  } finally {
    saving.value = false;
  }
}

async function removeServer(server: McpServerSummary) {
  if (!window.confirm(`确认删除 MCP Server「${server.serverName}」？同步出的 MCP 工具会被停用。`)) {
    return;
  }
  await deleteMcpServer(server.id);
  toast('MCP Server 已删除');
  if (editingId.value === server.id) {
    panelOpen.value = false;
    editingId.value = '';
  }
  await loadServers();
}

async function handleTest(server: McpServerSummary) {
  testingId.value = server.id;
  operationLog.value = '';
  try {
    const result = await testMcpServer(server.id);
    operationLog.value = formatConnectionResult(server.serverName, result);
    toast(result.success ? 'MCP 连接测试成功' : 'MCP 连接测试失败');
    await loadServers();
  } finally {
    testingId.value = '';
  }
}

async function handleDiscover(server: McpServerSummary) {
  discoveringId.value = server.id;
  operationLog.value = '';
  try {
    const result = await discoverMcpServer(server.id);
    operationLog.value = formatDiscoveryResult(server.serverName, result);
    if (result.status === 'pending' || result.status === 'running') {
      toast('MCP 能力发现任务已提交，可在异步任务中心查看进度');
    } else {
      toast(result.status === 'success' ? 'MCP 能力发现完成' : 'MCP 能力发现失败');
      await loadServers();
    }
  } finally {
    discoveringId.value = '';
  }
}

function toRequest(): McpServerRequest {
  return {
    serverCode: form.serverCode || undefined,
    serverName: form.serverName,
    description: form.description,
    transportType: form.transportType,
    command: form.command,
    args: normalizedJson(form.args, '[]'),
    endpointUrl: form.endpointUrl,
    authType: form.authType,
    authConfig: normalizedJson(form.authConfig, '{}'),
    envVars: normalizedJson(form.envVars, '{}'),
    allowedPaths: normalizedJson(form.allowedPaths, '[]'),
    riskPolicy: normalizedJson(form.riskPolicy, '{"highRiskDefault":"disabled_and_confirm"}'),
    status: form.status,
  };
}

function endpointOf(server: McpServerSummary) {
  return server.endpointUrl || server.command || '-';
}

function formatTime(value?: string) {
  if (!value) return '暂无';
  return value.replace('T', ' ').slice(0, 19);
}

function formatConnectionResult(name: string, result: McpConnectionTestResult) {
  return [
    `Server: ${name}`,
    `success: ${result.success}`,
    `latencyMs: ${result.latencyMs}`,
    `tools/prompts/resources: ${result.toolsCount}/${result.promptsCount}/${result.resourcesCount}`,
    `error: ${result.errorMessage || '-'}`,
    '',
    result.responsePayload || '{}',
  ].join('\n');
}

function formatDiscoveryResult(name: string, result: McpDiscoveryResult) {
  return [
    `Server: ${name}`,
    `taskId: ${result.taskId}`,
    `status: ${result.status}`,
    `message: ${result.status === 'pending' ? '任务已投递 Kafka，等待 Worker 执行' : '任务已处理'}`,
    `tools/prompts/resources: ${result.toolsCount}/${result.promptsCount}/${result.resourcesCount}`,
    `error: ${result.errorMessage || '-'}`,
  ].join('\n');
}

function normalizedJson(text: string, fallback: string) {
  try {
    return JSON.stringify(JSON.parse(text || fallback), null, 2);
  } catch {
    return fallback;
  }
}
</script>

<template>
  <PageHeader title="MCP Server 管理" description="管理 MCP Server 连接，发现 Tools、Prompts、Resources 并统一授权审计">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadServers"><RefreshCw :size="16" /> 刷新</button>
      <button class="primary-button" type="button" @click="startCreate"><Plug :size="16" /> 新建 Server</button>
    </template>
  </PageHeader>

  <section class="metric-grid">
    <StatCard label="全部 Server" :value="String(servers.length)" detail="HTTP JSON-RPC 已接入" icon="Server" tone="info" />
    <StatCard label="运行中" :value="String(runningCount)" detail="连接测试成功" icon="Activity" tone="success" />
    <StatCard label="已停止" :value="String(stoppedCount)" detail="待连接测试" icon="Timer" tone="warning" />
    <StatCard label="异常 / 工具" :value="`${errorCount} / ${toolCount}`" detail="发现后同步工具中心" icon="ShieldAlert" tone="danger" />
  </section>

  <section v-if="panelOpen" class="section-block">
    <div class="section-title">
      <h2>{{ editingId ? '编辑 MCP Server' : '新建 MCP Server' }}</h2>
      <StatusBadge :label="form.transportType.toUpperCase()" />
    </div>
    <div class="form-grid">
      <label>服务名称<input v-model="form.serverName" /></label>
      <label>服务编码<input v-model="form.serverCode" class="mono" placeholder="不填自动生成" /></label>
      <label>传输类型<select v-model="form.transportType"><option value="http">Streamable HTTP</option><option value="sse">传统 SSE</option><option value="stdio">stdio 子进程</option></select></label>
      <label>状态<select v-model="form.status"><option value="stopped">已停止</option><option value="running">运行中</option><option value="error">异常</option></select></label>
      <label class="wide">描述<textarea v-model="form.description" /></label>
      <label class="wide">端点 URL<input v-model="form.endpointUrl" class="mono" placeholder="http://localhost:7001/mcp" /></label>
      <label class="wide">stdio 命令<input v-model="form.command" class="mono" placeholder="npx @modelcontextprotocol/server-filesystem" /></label>
      <label class="wide">stdio 参数 JSON<textarea v-model="form.args" class="mono" /></label>
      <label>认证方式<select v-model="form.authType"><option value="none">无认证</option><option value="bearer">Bearer Token</option><option value="api_key">API Key</option><option value="basic">Basic</option></select></label>
      <label>允许路径 JSON<textarea v-model="form.allowedPaths" class="mono" /></label>
      <label class="wide">认证配置 JSON<textarea v-model="form.authConfig" class="mono" /></label>
      <label class="wide">环境变量 JSON<textarea v-model="form.envVars" class="mono" /></label>
      <label class="wide">风险策略 JSON<textarea v-model="form.riskPolicy" class="mono" /></label>
    </div>
    <div class="table-actions">
      <button class="secondary-button" type="button" @click="panelOpen = false">收起</button>
      <button class="primary-button" type="button" :disabled="saving" @click="saveServer">{{ saving ? '保存中' : '保存 Server' }}</button>
    </div>
  </section>

  <section class="section-block">
    <div class="section-title"><h2>Server 列表</h2><span>{{ loading ? '加载中' : `${servers.length} 个` }}</span></div>
    <div v-if="!loading && servers.length === 0" class="empty-state">暂无 MCP Server</div>
    <table v-else class="data-table">
      <thead>
        <tr><th>Server 名称</th><th>传输类型</th><th>地址 / 命令</th><th>认证方式</th><th>能力</th><th>状态</th><th>最近心跳</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="server in pagedServers" :key="server.id">
          <td><b>{{ server.serverName }}</b><br /><span class="mono">{{ server.serverCode }}</span></td>
          <td>{{ server.transportType }}</td>
          <td class="mono">{{ endpointOf(server) }}</td>
          <td>{{ server.authType || 'none' }}</td>
          <td>{{ server.toolsCount }} / {{ server.promptsCount }} / {{ server.resourcesCount }}</td>
          <td><StatusBadge :label="server.status" :tone="server.status === 'running' ? 'success' : server.status === 'error' ? 'danger' : 'warning'" /></td>
          <td>{{ formatTime(server.lastHeartbeatAt) }}</td>
          <td>
            <div class="table-actions">
              <button class="icon-button" type="button" title="编辑" @click="startEdit(server)"><Edit3 :size="16" /></button>
              <button class="icon-button" type="button" title="连接测试" :disabled="testingId === server.id" @click="handleTest(server)"><ServerCog :size="16" /></button>
              <button class="icon-button" type="button" title="发现能力" :disabled="discoveringId === server.id" @click="handleDiscover(server)"><Search :size="16" /></button>
              <button class="secondary-button slim" type="button" @click="router.push(`/mcp/tools?serverId=${server.id}`)">工具</button>
              <button class="icon-button danger" type="button" title="删除" @click="removeServer(server)"><Trash2 :size="16" /></button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <PaginationBar v-model:page="serverPage" :total="servers.length" />
  </section>

  <section v-if="operationLog" class="section-block">
    <div class="section-title"><h2>连接 / 发现日志</h2></div>
    <pre class="code-block light">{{ operationLog }}</pre>
  </section>
</template>
