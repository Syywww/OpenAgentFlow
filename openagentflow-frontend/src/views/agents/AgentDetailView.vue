<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Copy, Eye, Rocket, Save, TestTube2, Trash2 } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import {
  copyAgent,
  createAgent,
  deleteAgent,
  fetchAgent,
  publishAgent,
  updateAgent,
  type AgentDetail,
  type AgentRequest,
} from '../../api/agents';
import {
  fetchAgentKnowledgeBindings,
  fetchKnowledgeBases,
  saveAgentKnowledgeBindings,
  type AgentKnowledgeBindingSummary,
  type KnowledgeBaseSummary,
} from '../../api/knowledge';
import {
  fetchAgentToolBindings,
  fetchTools,
  saveAgentToolBindings,
  type AgentToolBindingSummary,
  type ToolDefinitionSummary,
} from '../../api/tools';
import {
  fetchAgentWorkflowBindings,
  fetchWorkflows,
  saveAgentWorkflowBindings,
  type AgentWorkflowBindingSummary,
  type WorkflowSummary,
} from '../../api/workflows';
import { fetchChatModels, type ModelConfigSummary } from '../../api/models';
import { useOverlay } from '../../composables/useOverlay';

const route = useRoute();
const router = useRouter();
const { toast } = useOverlay();
const tabs = ['基础信息', '模型参数', 'Prompt 配置', '知识库绑定', '工具绑定', '工作流绑定', '安全策略'];
const activeTab = ref('基础信息');
const loading = ref(false);
const models = ref<ModelConfigSummary[]>([]);
const currentAgent = ref<AgentDetail | null>(null);
const knowledgeBases = ref<KnowledgeBaseSummary[]>([]);
const knowledgeBindings = ref<AgentKnowledgeBindingSummary[]>([]);
const selectedKnowledgeBaseIds = ref<string[]>([]);
const tools = ref<ToolDefinitionSummary[]>([]);
const toolBindings = ref<AgentToolBindingSummary[]>([]);
const selectedToolIds = ref<string[]>([]);
const workflows = ref<WorkflowSummary[]>([]);
const workflowBindings = ref<AgentWorkflowBindingSummary[]>([]);
const selectedWorkflowIds = ref<string[]>([]);

const form = reactive({
  agentCode: '',
  agentName: '',
  category: '通用',
  description: '',
  agentType: 'chat_agent',
  modelId: '',
  systemPrompt: '你是 OpenAgentFlow-Java 的智能体，请使用清晰、准确的中文回答用户。',
  temperature: 0.3,
  maxTokens: 2048,
  memoryStrategy: 'none',
  visibility: 'private',
  status: 'draft',
});

const isNew = computed(() => route.params.id === 'new');
const pageTitle = computed(() => (isNew.value ? '新建智能体' : form.agentName || '智能体详情'));
const pageDescription = computed(() => form.description || '配置 Prompt、模型参数、知识库、工具、工作流与运行权限');
const statusLabel = computed(() => currentAgent.value?.statusLabel || statusText(form.status));

onMounted(async () => {
  await Promise.all([loadModels(), loadKnowledgeBases(), loadTools(), loadWorkflows(), loadAgent()]);
});

async function loadModels() {
  models.value = await fetchChatModels();
  if (!form.modelId && models.value.length > 0) {
    form.modelId = models.value[0].id;
  }
}

async function loadAgent() {
  if (isNew.value) {
    return;
  }
  loading.value = true;
  try {
    const detail = await fetchAgent(String(route.params.id));
    currentAgent.value = detail;
    fillForm(detail);
    await Promise.all([loadKnowledgeBindings(detail.id), loadToolBindings(detail.id), loadWorkflowBindings(detail.id)]);
  } finally {
    loading.value = false;
  }
}

async function loadKnowledgeBases() {
  knowledgeBases.value = await fetchKnowledgeBases();
}

async function loadKnowledgeBindings(agentId: string) {
  knowledgeBindings.value = await fetchAgentKnowledgeBindings(agentId);
  selectedKnowledgeBaseIds.value = knowledgeBindings.value.map((binding) => binding.knowledgeBaseId);
}

async function loadTools() {
  tools.value = await fetchTools();
}

async function loadToolBindings(agentId: string) {
  toolBindings.value = await fetchAgentToolBindings(agentId);
  selectedToolIds.value = toolBindings.value.map((binding) => binding.toolId);
}

async function loadWorkflows() {
  workflows.value = await fetchWorkflows();
}

async function loadWorkflowBindings(agentId: string) {
  workflowBindings.value = await fetchAgentWorkflowBindings(agentId);
  selectedWorkflowIds.value = workflowBindings.value.map((binding) => binding.workflowId);
}

function fillForm(detail: AgentDetail) {
  form.agentCode = detail.agentCode;
  form.agentName = detail.agentName;
  form.category = detail.category || '通用';
  form.description = detail.description || '';
  form.agentType = detail.agentType || 'chat_agent';
  form.modelId = detail.modelId || form.modelId;
  form.systemPrompt = detail.systemPrompt || form.systemPrompt;
  form.memoryStrategy = detail.memoryStrategy || 'none';
  form.visibility = detail.visibility || 'private';
  form.status = detail.status || 'draft';

  try {
    const params = JSON.parse(detail.modelParams || '{}');
    form.temperature = Number(params.temperature ?? form.temperature);
    form.maxTokens = Number(params.maxTokens ?? params.max_tokens ?? form.maxTokens);
  } catch {
    form.temperature = 0.3;
    form.maxTokens = 2048;
  }
}

async function handleSave() {
  const payload = toRequest();
  if (isNew.value) {
    const created = await createAgent(payload);
    await saveBindings(created.id);
    toast('智能体已创建');
    router.replace(`/agents/${created.id}`);
    currentAgent.value = created;
    fillForm(created);
    await Promise.all([loadKnowledgeBindings(created.id), loadToolBindings(created.id), loadWorkflowBindings(created.id)]);
    return;
  }
  const updated = await updateAgent(String(route.params.id), payload);
  await saveBindings(updated.id);
  currentAgent.value = updated;
  fillForm(updated);
  await Promise.all([loadKnowledgeBindings(updated.id), loadToolBindings(updated.id), loadWorkflowBindings(updated.id)]);
  toast('智能体配置已保存');
}

async function saveBindings(agentId: string) {
  await Promise.all([
    saveAgentKnowledgeBindings(agentId, selectedKnowledgeBaseIds.value, 5, 0.65),
    saveAgentToolBindings(agentId, selectedToolIds.value),
    saveAgentWorkflowBindings(agentId, selectedWorkflowIds.value),
  ]);
}

async function handlePublish() {
  if (isNew.value) {
    await handleSave();
  }
  const id = String(route.params.id === 'new' ? currentAgent.value?.id : route.params.id);
  if (!id || id === 'undefined') {
    return;
  }
  const published = await publishAgent(id, {
    versionNo: `v${new Date().toISOString().slice(0, 19).replace(/[-:T]/g, '')}`,
    publishNote: '通过管理台发布',
  });
  currentAgent.value = published;
  fillForm(published);
  toast('智能体已发布');
}

async function handleCopy() {
  if (!currentAgent.value) {
    return;
  }
  const copied = await copyAgent(currentAgent.value.id);
  toast('智能体已复制');
  router.push(`/agents/${copied.id}`);
}

async function handleDelete() {
  if (!currentAgent.value || !window.confirm('确认删除该智能体？')) {
    return;
  }
  await deleteAgent(currentAgent.value.id);
  toast('智能体已删除');
  router.push('/agents');
}

function goDebug() {
  const id = currentAgent.value?.id || String(route.params.id);
  router.push({ path: '/debug', query: { agentId: id, modelId: form.modelId } });
}

function toRequest(): AgentRequest {
  return {
    agentCode: form.agentCode,
    agentName: form.agentName,
    category: form.category,
    description: form.description,
    agentType: form.agentType,
    modelId: form.modelId,
    systemPrompt: form.systemPrompt,
    modelParams: JSON.stringify({
      temperature: Number(form.temperature),
      maxTokens: Number(form.maxTokens),
    }),
    memoryStrategy: form.memoryStrategy,
    visibility: form.visibility,
    status: form.status,
  };
}

function statusText(status: string) {
  if (status === 'published') return '运行中';
  if (status === 'draft') return '开发中';
  if (status === 'disabled') return '已暂停';
  return status || '未知';
}
</script>

<template>
  <PageHeader :title="pageTitle" :description="pageDescription">
    <template #actions>
      <button class="secondary-button" type="button"><Eye :size="16" /> 预览</button>
      <button class="secondary-button" type="button" :disabled="isNew" @click="goDebug"><TestTube2 :size="16" /> 调试</button>
      <button class="secondary-button" type="button" :disabled="isNew" @click="handleCopy"><Copy :size="16" /> 复制</button>
      <button class="secondary-button" type="button" @click="handlePublish"><Rocket :size="16" /> 发布</button>
      <button class="primary-button" type="button" @click="handleSave"><Save :size="16" /> 保存</button>
      <button v-if="!isNew" class="danger-button" type="button" @click="handleDelete"><Trash2 :size="16" /> 删除</button>
    </template>
  </PageHeader>

  <div class="tabs">
    <button v-for="tab in tabs" :key="tab" class="tab" :class="{ active: activeTab === tab }" type="button" @click="activeTab = tab">{{ tab }}</button>
  </div>

  <section class="form-layout">
    <div class="section-block">
      <div class="section-title"><h2>基础信息</h2><StatusBadge :label="statusLabel" /></div>
      <div class="form-grid">
        <label>智能体名称<input v-model="form.agentName" /></label>
        <label>分类<select v-model="form.category"><option>通用</option><option>客服</option><option>知识问答</option><option>数据分析</option><option>运维</option></select></label>
        <label class="wide">描述<textarea v-model="form.description" /></label>
        <label>编码<input v-model="form.agentCode" placeholder="不填则自动生成" /></label>
        <label>状态<select v-model="form.status"><option value="draft">开发中</option><option value="published">运行中</option><option value="disabled">已暂停</option></select></label>
      </div>
    </div>

    <div class="section-block">
      <div class="section-title"><h2>模型配置</h2><span>OpenAI-compatible / Ollama / Qwen / DeepSeek</span></div>
      <div class="form-grid">
        <label>Agent 类型<select v-model="form.agentType"><option value="chat_agent">对话 Agent</option><option value="rag_tool_agent">RAG 工具 Agent</option><option value="workflow_agent">工作流 Agent</option></select></label>
        <label>基础模型<select v-model="form.modelId"><option v-for="model in models" :key="model.id" :value="model.id">{{ model.providerName }} / {{ model.modelName }}</option></select></label>
        <label>Temperature<input v-model.number="form.temperature" type="range" min="0" max="2" step="0.01" /></label>
        <label>最大 Tokens<input v-model.number="form.maxTokens" type="range" min="256" max="8192" step="128" /></label>
        <label class="wide">System Prompt<textarea v-model="form.systemPrompt" /></label>
      </div>
    </div>
  </section>

  <section class="detail-columns">
    <div class="section-block">
      <div class="section-title"><h2>已绑定知识库</h2><span>{{ selectedKnowledgeBaseIds.length }} 个</span></div>
      <div v-if="knowledgeBases.length === 0" class="empty-state">暂无知识库，请先在知识库模块创建并上传文档</div>
      <template v-else>
        <div v-for="kb in knowledgeBases" :key="kb.id" class="list-row">
          <label class="checkbox-row">
            <input v-model="selectedKnowledgeBaseIds" type="checkbox" :value="kb.id" />
            <b>{{ kb.kbName }}</b>
          </label>
          <span>{{ kb.documentCount }} 文档 · {{ kb.chunkCount }} 分片</span>
          <StatusBadge :label="selectedKnowledgeBaseIds.includes(kb.id) ? '已启用' : '未绑定'" />
        </div>
      </template>
    </div>

    <div class="section-block">
      <div class="section-title"><h2>已绑定工具</h2><span>{{ selectedToolIds.length }} 个</span></div>
      <div v-if="tools.length === 0" class="empty-state">暂无工具，请先在工具中心创建 REST API、Webhook 或数据库查询工具</div>
      <template v-else>
        <div v-for="tool in tools" :key="tool.id" class="list-row">
          <label class="checkbox-row">
            <input v-model="selectedToolIds" type="checkbox" :value="tool.id" />
            <b>{{ tool.toolName }}</b>
          </label>
          <span class="mono">{{ tool.toolCode }}</span>
          <StatusBadge :label="selectedToolIds.includes(tool.id) ? '已启用' : tool.riskLabel" :tone="tool.riskLevel === 'high' ? 'danger' : tool.riskLevel === 'medium' ? 'warning' : undefined" />
        </div>
      </template>
    </div>

    <div class="section-block">
      <div class="section-title"><h2>已绑定工作流</h2><span>{{ selectedWorkflowIds.length }} 个</span></div>
      <div v-if="workflows.length === 0" class="empty-state">暂无工作流，请先在工作流编排中创建并发布</div>
      <template v-else>
        <div v-for="workflow in workflows" :key="workflow.id" class="list-row">
          <label class="checkbox-row">
            <input v-model="selectedWorkflowIds" type="checkbox" :value="workflow.id" />
            <b>{{ workflow.workflowName }}</b>
          </label>
          <span class="mono">{{ workflow.workflowCode }}</span>
          <StatusBadge :label="selectedWorkflowIds.includes(workflow.id) ? '调试时优先运行' : workflow.statusLabel" />
        </div>
      </template>
    </div>
  </section>
</template>
