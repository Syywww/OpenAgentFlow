<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Copy, Edit3, Plus, Save, X } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import PaginationBar from '../../components/PaginationBar.vue';
import StatCard from '../../components/StatCard.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import { copyAgent, createAgent, fetchAgents, type AgentRequest, type AgentSummary } from '../../api/agents';
import { fetchKnowledgeBases, saveAgentKnowledgeBindings, type KnowledgeBaseSummary } from '../../api/knowledge';
import { fetchChatModels, type ModelConfigSummary } from '../../api/models';
import { fetchPromptTemplates, type PromptTemplateSummary } from '../../api/prompts';
import { fetchTools, saveAgentToolBindings, type ToolDefinitionSummary } from '../../api/tools';
import { fetchWorkflows, saveAgentWorkflowBindings, type WorkflowSummary } from '../../api/workflows';
import { useOverlay } from '../../composables/useOverlay';
import { usePagination } from '../../composables/usePagination';

const router = useRouter();
const { toast } = useOverlay();
const agents = ref<AgentSummary[]>([]);
const loading = ref(false);
const keyword = ref('');
const createModalOpen = ref(false);
const createActivePanel = ref<'base' | 'model' | 'prompt' | 'knowledge' | 'tools' | 'workflow' | 'security'>('base');
const models = ref<ModelConfigSummary[]>([]);
const promptTemplates = ref<PromptTemplateSummary[]>([]);
const knowledgeBases = ref<KnowledgeBaseSummary[]>([]);
const selectedKnowledgeBaseIds = ref<string[]>([]);
const tools = ref<ToolDefinitionSummary[]>([]);
const selectedToolIds = ref<string[]>([]);
const workflows = ref<WorkflowSummary[]>([]);
const selectedWorkflowIds = ref<string[]>([]);

const createForm = reactive({
  agentCode: '',
  agentName: '',
  category: '通用',
  description: '',
  agentType: 'chat_agent',
  modelId: '',
  systemPromptTemplateId: '',
  systemPrompt: '你是 OpenAgentFlow-Java 的智能体，请使用清晰、准确的中文回答用户。',
  temperature: 0.3,
  maxTokens: 2048,
  visibility: 'private',
  status: 'draft',
});

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
const { currentPage: kbPage, pagedItems: pagedKnowledgeBases, resetPage: resetKbPage } = usePagination(knowledgeBases);
const { currentPage: toolPage, pagedItems: pagedTools, resetPage: resetToolPage } = usePagination(tools);
const { currentPage: workflowPage, pagedItems: pagedWorkflows, resetPage: resetWorkflowPage } = usePagination(workflows);

onMounted(() => {
  void Promise.all([loadAgents(), loadCreateOptions()]);
});

async function loadAgents() {
  loading.value = true;
  try {
    agents.value = await fetchAgents();
  } finally {
    loading.value = false;
  }
}

async function loadCreateOptions() {
  const [modelList, promptResult, kbList, toolList, workflowList] = await Promise.all([
    fetchChatModels(),
    fetchPromptTemplates({ promptType: 'system', status: 'published', pageNo: 1, pageSize: 100 }),
    fetchKnowledgeBases(),
    fetchTools(),
    fetchWorkflows(),
  ]);
  models.value = modelList;
  promptTemplates.value = promptResult.records;
  knowledgeBases.value = kbList;
  tools.value = toolList;
  workflows.value = workflowList;
  if (!createForm.modelId && modelList[0]) {
    createForm.modelId = modelList[0].id;
  }
}

function resetCreateForm() {
  createForm.agentCode = '';
  createForm.agentName = '';
  createForm.category = '通用';
  createForm.description = '';
  createForm.agentType = 'chat_agent';
  createForm.modelId = models.value[0]?.id || '';
  createForm.systemPromptTemplateId = '';
  createForm.systemPrompt = '你是 OpenAgentFlow-Java 的智能体，请使用清晰、准确的中文回答用户。';
  createForm.temperature = 0.3;
  createForm.maxTokens = 2048;
  createForm.visibility = 'private';
  createForm.status = 'draft';
  selectedKnowledgeBaseIds.value = [];
  selectedToolIds.value = [];
  selectedWorkflowIds.value = [];
  resetKbPage();
  resetToolPage();
  resetWorkflowPage();
}

function openCreateModal() {
  resetCreateForm();
  createActivePanel.value = 'base';
  createModalOpen.value = true;
}

function closeCreateModal() {
  createModalOpen.value = false;
  resetCreateForm();
}

function applySystemPromptTemplate() {
  const template = promptTemplates.value.find((item) => item.id === createForm.systemPromptTemplateId);
  if (template) {
    createForm.systemPrompt = template.content;
  }
}

function toCreateRequest(): AgentRequest {
  return {
    agentCode: createForm.agentCode || undefined,
    agentName: createForm.agentName,
    category: createForm.category,
    description: createForm.description,
    agentType: createForm.agentType,
    modelId: createForm.modelId || undefined,
    systemPromptTemplateId: createForm.systemPromptTemplateId || undefined,
    systemPrompt: createForm.systemPrompt,
    modelParams: JSON.stringify({
      temperature: Number(createForm.temperature),
      maxTokens: Number(createForm.maxTokens),
    }),
    memoryStrategy: 'none',
    visibility: createForm.visibility,
    status: createForm.status,
  };
}

async function handleCreateAgent() {
  loading.value = true;
  try {
    const created = await createAgent(toCreateRequest());
    await Promise.all([
      saveAgentKnowledgeBindings(created.id, selectedKnowledgeBaseIds.value, 5, 0.65),
      saveAgentToolBindings(created.id, selectedToolIds.value),
      saveAgentWorkflowBindings(created.id, selectedWorkflowIds.value),
    ]);
    toast('智能体已创建');
    closeCreateModal();
    await loadAgents();
    router.push(`/agents/${created.id}`);
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
      <button class="primary-button" type="button" @click="openCreateModal"><Plus :size="16" /> 新建智能体</button>
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

  <div v-if="createModalOpen" class="overlay-backdrop" @click.self="closeCreateModal">
    <section class="modal-panel agent-create-modal">
      <header class="overlay-header">
        <div>
          <h2>新建智能体</h2>
          <p class="muted">先创建基础配置，保存后进入详情页继续绑定知识库、工具和工作流。</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closeCreateModal"><X :size="18" /></button>
      </header>

      <section class="governance-card-tabs compact-modal-tabs">
        <button class="governance-tab-card" :class="{ active: createActivePanel === 'base' }" type="button" @click="createActivePanel = 'base'">
          <span>基础信息</span>
          <b>1</b>
          <small>名称、编码、分类和描述</small>
        </button>
        <button class="governance-tab-card" :class="{ active: createActivePanel === 'model' }" type="button" @click="createActivePanel = 'model'">
          <span>模型参数</span>
          <b>{{ createForm.modelId ? 1 : 0 }}</b>
          <small>模型、类型和生成参数</small>
        </button>
        <button class="governance-tab-card" :class="{ active: createActivePanel === 'prompt' }" type="button" @click="createActivePanel = 'prompt'">
          <span>Prompt 配置</span>
          <b>{{ createForm.systemPromptTemplateId ? 1 : 0 }}</b>
          <small>模板与 System Prompt</small>
        </button>
        <button class="governance-tab-card" :class="{ active: createActivePanel === 'knowledge' }" type="button" @click="createActivePanel = 'knowledge'">
          <span>知识库绑定</span>
          <b>{{ selectedKnowledgeBaseIds.length }}</b>
          <small>选择可检索的知识库</small>
        </button>
        <button class="governance-tab-card" :class="{ active: createActivePanel === 'tools' }" type="button" @click="createActivePanel = 'tools'">
          <span>工具绑定</span>
          <b>{{ selectedToolIds.length }}</b>
          <small>选择可调用的工具</small>
        </button>
        <button class="governance-tab-card" :class="{ active: createActivePanel === 'workflow' }" type="button" @click="createActivePanel = 'workflow'">
          <span>工作流绑定</span>
          <b>{{ selectedWorkflowIds.length }}</b>
          <small>选择优先运行的工作流</small>
        </button>
        <button class="governance-tab-card" :class="{ active: createActivePanel === 'security' }" type="button" @click="createActivePanel = 'security'">
          <span>安全策略</span>
          <b>{{ createForm.status === 'published' ? '启用' : '未发布' }}</b>
          <small>可见范围和运行状态</small>
        </button>
      </section>

      <div class="create-agent-panel">
        <div v-if="createActivePanel === 'base'" class="form-grid">
          <label>智能体名称<input v-model="createForm.agentName" placeholder="如：客服助手" /></label>
          <label>编码<input v-model="createForm.agentCode" placeholder="不填则后端自动生成" /></label>
          <label>分类
            <select v-model="createForm.category">
              <option>通用</option>
              <option>客服</option>
              <option>知识问答</option>
              <option>数据分析</option>
              <option>运维</option>
            </select>
          </label>
          <label class="wide">描述<textarea v-model="createForm.description" rows="4" placeholder="描述智能体的目标、使用场景和边界" /></label>
        </div>

        <div v-else-if="createActivePanel === 'model'" class="form-grid">
          <label>Agent 类型
            <select v-model="createForm.agentType">
              <option value="chat_agent">对话 Agent</option>
              <option value="rag_tool_agent">RAG 工具 Agent</option>
              <option value="workflow_agent">工作流 Agent</option>
            </select>
          </label>
          <label>基础模型
            <select v-model="createForm.modelId">
              <option value="">暂不绑定</option>
              <option v-for="model in models" :key="model.id" :value="model.id">{{ model.providerName }} / {{ model.modelName }}</option>
            </select>
          </label>
          <label>Temperature<input v-model.number="createForm.temperature" type="range" min="0" max="2" step="0.01" /></label>
          <label>最大 Tokens<input v-model.number="createForm.maxTokens" type="range" min="256" max="8192" step="128" /></label>
        </div>

        <div v-else-if="createActivePanel === 'prompt'" class="form-grid">
          <label>System Prompt 模板
            <select v-model="createForm.systemPromptTemplateId" @change="applySystemPromptTemplate">
              <option value="">不使用模板</option>
              <option v-for="template in promptTemplates" :key="template.id" :value="template.id">
                {{ template.templateName }} / {{ template.latestVersionNo || '未发布版本' }}
              </option>
            </select>
          </label>
          <label class="wide">System Prompt<textarea v-model="createForm.systemPrompt" rows="8" /></label>
        </div>

        <div v-else-if="createActivePanel === 'knowledge'">
          <div v-if="knowledgeBases.length === 0" class="empty-state">暂无知识库，请先在知识库模块创建并上传文档</div>
          <template v-else>
            <div class="agent-binding-list modal-binding-list">
              <div v-for="kb in pagedKnowledgeBases" :key="kb.id" class="list-row agent-binding-row">
                <label class="checkbox-row">
                  <input v-model="selectedKnowledgeBaseIds" type="checkbox" :value="kb.id" />
                  <b>{{ kb.kbName }}</b>
                </label>
                <span>{{ kb.documentCount }} 文档 · {{ kb.chunkCount }} 分片</span>
                <StatusBadge :label="selectedKnowledgeBaseIds.includes(kb.id) ? '已启用' : '未绑定'" />
              </div>
            </div>
            <PaginationBar v-model:page="kbPage" :total="knowledgeBases.length" />
          </template>
        </div>

        <div v-else-if="createActivePanel === 'tools'">
          <div v-if="tools.length === 0" class="empty-state">暂无工具，请先在工具中心创建 REST API、Webhook 或数据库查询工具</div>
          <template v-else>
            <div class="agent-binding-list modal-binding-list">
              <div v-for="tool in pagedTools" :key="tool.id" class="list-row agent-binding-row">
                <label class="checkbox-row">
                  <input v-model="selectedToolIds" type="checkbox" :value="tool.id" />
                  <b>{{ tool.toolName }}</b>
                </label>
                <span class="mono">{{ tool.toolCode }}</span>
                <StatusBadge :label="selectedToolIds.includes(tool.id) ? '已启用' : tool.riskLabel" :tone="tool.riskLevel === 'high' ? 'danger' : tool.riskLevel === 'medium' ? 'warning' : undefined" />
              </div>
            </div>
            <PaginationBar v-model:page="toolPage" :total="tools.length" />
          </template>
        </div>

        <div v-else-if="createActivePanel === 'workflow'">
          <div v-if="workflows.length === 0" class="empty-state">暂无工作流，请先在工作流编排中创建并发布</div>
          <template v-else>
            <div class="agent-binding-list modal-binding-list">
              <div v-for="workflow in pagedWorkflows" :key="workflow.id" class="list-row agent-binding-row">
                <label class="checkbox-row">
                  <input v-model="selectedWorkflowIds" type="checkbox" :value="workflow.id" />
                  <b>{{ workflow.workflowName }}</b>
                </label>
                <span class="mono">{{ workflow.workflowCode }}</span>
                <StatusBadge :label="selectedWorkflowIds.includes(workflow.id) ? '调试时优先运行' : workflow.statusLabel" />
              </div>
            </div>
            <PaginationBar v-model:page="workflowPage" :total="workflows.length" />
          </template>
        </div>

        <div v-else class="form-grid">
          <label>可见范围
            <select v-model="createForm.visibility">
              <option value="private">仅自己可见</option>
              <option value="team">团队可见</option>
              <option value="public">公开可见</option>
            </select>
          </label>
          <label>运行状态
            <select v-model="createForm.status">
              <option value="draft">开发中</option>
              <option value="published">运行中</option>
              <option value="disabled">已暂停</option>
            </select>
          </label>
        </div>
      </div>
      <div class="toolbar compact">
        <button class="secondary-button" type="button" @click="closeCreateModal">取消</button>
        <button class="primary-button" type="button" :disabled="loading || !createForm.agentName" @click="handleCreateAgent">
          <Save :size="16" /> 创建智能体
        </button>
      </div>
    </section>
  </div>
</template>
