<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Handle, Position, VueFlow, type Connection } from '@vue-flow/core';
import { Bug, Plus, Play, Rocket, Save, Trash2 } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import { fetchAgents, type AgentSummary } from '../api/agents';
import { fetchChatModels, type ModelConfigSummary } from '../api/models';
import { fetchTools, type ToolDefinitionSummary } from '../api/tools';
import {
  createWorkflow,
  fetchWorkflow,
  fetchWorkflows,
  publishWorkflow,
  runWorkflow,
  updateWorkflow,
  type WorkflowDetail,
  type WorkflowEdgeDto,
  type WorkflowNodeDto,
  type WorkflowRequest,
  type WorkflowSummary,
} from '../api/workflows';
import { useOverlay } from '../composables/useOverlay';
import { usePagination } from '../composables/usePagination';

const { toast } = useOverlay();
const workflows = ref<WorkflowSummary[]>([]);
const currentWorkflow = ref<WorkflowDetail | null>(null);
type FlowNode = { id: string; type?: string; label?: string; position: { x: number; y: number }; class?: string; data?: Record<string, unknown> };
type FlowEdge = { id: string; source: string; target: string; label?: string; data?: Record<string, unknown> };
const nodes = ref<FlowNode[]>([]);
const edges = ref<FlowEdge[]>([]);
const selectedNodeId = ref<string>('start');
const loading = ref(false);
const agents = ref<AgentSummary[]>([]);
const models = ref<ModelConfigSummary[]>([]);
const tools = ref<ToolDefinitionSummary[]>([]);
const runInput = ref('请基于工作流回答：OpenAgentFlow 现在支持哪些能力？');
const selectedAgentId = ref('');
const runResult = ref('');
const traceRunId = ref('');
const { currentPage: workflowPage, pagedItems: pagedWorkflows } = usePagination(workflows);

const workflowForm = reactive({
  workflowCode: '',
  workflowName: '企业问答工作流',
  description: '开始 -> RAG 检索 -> LLM 生成 -> 结束',
  workflowType: 'agent_workflow',
  visibility: 'private',
  status: 'draft',
});

const nodeForm = reactive({
  nodeName: '',
  nodeType: 'LLM',
  promptTemplate: '{{input}}',
  systemPrompt: '',
  queryTemplate: '{{input}}',
  toolName: '',
  conditionExpr: 'success',
  temperature: 0.3,
  maxTokens: 2048,
});

const palette = [
  { type: 'START', label: '开始' },
  { type: 'LLM', label: 'LLM' },
  { type: 'RAG', label: 'RAG 检索' },
  { type: 'TOOL', label: '工具调用' },
  { type: 'CONDITION', label: '条件' },
  { type: 'END', label: '结束' },
];

const selectedNode = computed(() => nodes.value.find((node) => node.id === selectedNodeId.value));
const currentStatus = computed(() => currentWorkflow.value?.statusLabel || '草稿');

onMounted(async () => {
  await Promise.all([loadOptions(), loadWorkflowList()]);
});

async function loadOptions() {
  const [agentRows, modelRows, toolRows] = await Promise.all([fetchAgents(), fetchChatModels(), fetchTools()]);
  agents.value = agentRows;
  models.value = modelRows;
  tools.value = toolRows;
  selectedAgentId.value = agentRows[0]?.id || '';
}

async function loadWorkflowList() {
  workflows.value = await fetchWorkflows();
  if (workflows.value.length > 0) {
    await openWorkflow(workflows.value[0].id);
    return;
  }
  createDraftCanvas();
}

async function openWorkflow(id: string) {
  loading.value = true;
  try {
    const detail = await fetchWorkflow(id);
    currentWorkflow.value = detail;
    workflowForm.workflowCode = detail.workflowCode || '';
    workflowForm.workflowName = detail.workflowName;
    workflowForm.description = detail.description || '';
    workflowForm.workflowType = detail.workflowType || 'agent_workflow';
    workflowForm.visibility = detail.visibility || 'private';
    workflowForm.status = detail.status || 'draft';
    nodes.value = detail.nodes.map(toFlowNode);
    edges.value = detail.edges.map(toFlowEdge);
    selectedNodeId.value = nodes.value[0]?.id || '';
    fillNodeForm();
  } finally {
    loading.value = false;
  }
}

function createDraftCanvas() {
  currentWorkflow.value = null;
  workflowForm.workflowCode = '';
  workflowForm.workflowName = '企业问答工作流';
  workflowForm.description = '开始 -> RAG 检索 -> LLM 生成 -> 结束';
  workflowForm.workflowType = 'agent_workflow';
  workflowForm.visibility = 'private';
  workflowForm.status = 'draft';
  nodes.value = [
    buildNode('start', '开始', 'START', 40, 140, {}),
    buildNode('rag', 'RAG 检索', 'RAG', 280, 80, { queryTemplate: '{{input}}' }),
    buildNode('llm', 'LLM 生成', 'LLM', 520, 140, { promptTemplate: '{{input}}', temperature: 0.3, maxTokens: 2048 }),
    buildNode('end', '结束', 'END', 760, 140, {}),
  ];
  edges.value = [
    { id: 'e_start_rag', source: 'start', target: 'rag' },
    { id: 'e_rag_llm', source: 'rag', target: 'llm' },
    { id: 'e_llm_end', source: 'llm', target: 'end' },
  ];
  selectedNodeId.value = 'llm';
  fillNodeForm();
}

function buildNode(id: string, label: string, nodeType: string, x: number, y: number, config: Record<string, unknown>): FlowNode {
  return {
    id,
    type: 'workflowNode',
    label,
    position: { x, y },
    class: nodeClass(nodeType),
    data: { label, nodeType, config },
  };
}

function toFlowNode(node: WorkflowNodeDto): FlowNode {
  return buildNode(
    node.nodeKey,
    node.nodeName,
    node.nodeType,
    node.positionX || 0,
    node.positionY || 0,
    (node.configJson || {}) as Record<string, unknown>,
  );
}

function toFlowEdge(edge: WorkflowEdgeDto): FlowEdge {
  return {
    id: edge.edgeKey,
    source: edge.sourceNodeKey,
    target: edge.targetNodeKey,
    label: edge.label || edge.conditionExpr || '',
    data: { conditionExpr: edge.conditionExpr || '' },
  };
}

function onConnect(connection: Connection) {
  if (!connection.source || !connection.target) {
    return;
  }
  edges.value = [
    ...edges.value,
    {
      id: `e_${connection.source}_${connection.target}_${Date.now()}`,
      source: connection.source,
      target: connection.target,
    },
  ];
}

function addNode(type: string, label: string) {
  const id = `${type.toLowerCase()}_${Date.now()}`;
  nodes.value.push(buildNode(id, label, type, 120 + nodes.value.length * 60, 120 + nodes.value.length * 24, defaultConfig(type)));
  selectedNodeId.value = id;
  fillNodeForm();
}

function removeSelectedNode() {
  if (!selectedNode.value || ['START', 'END'].includes(String(selectedNode.value.data?.nodeType))) {
    toast('开始和结束节点不能删除');
    return;
  }
  nodes.value = nodes.value.filter((node) => node.id !== selectedNodeId.value);
  edges.value = edges.value.filter((edge) => edge.source !== selectedNodeId.value && edge.target !== selectedNodeId.value);
  selectedNodeId.value = nodes.value[0]?.id || '';
  fillNodeForm();
}

function selectNode(event: { node: { id: string } }) {
  selectedNodeId.value = event.node.id;
  fillNodeForm();
}

function fillNodeForm() {
  const node = selectedNode.value;
  if (!node) {
    return;
  }
  const config = (node.data?.config || {}) as Record<string, unknown>;
  nodeForm.nodeName = String(node.label || node.data?.label || node.id);
  nodeForm.nodeType = String(node.data?.nodeType || 'LLM');
  nodeForm.promptTemplate = String(config.promptTemplate || '{{input}}');
  nodeForm.systemPrompt = String(config.systemPrompt || '');
  nodeForm.queryTemplate = String(config.queryTemplate || '{{input}}');
  nodeForm.toolName = String(config.toolName || '');
  nodeForm.conditionExpr = String(config.conditionExpr || 'success');
  nodeForm.temperature = Number(config.temperature ?? 0.3);
  nodeForm.maxTokens = Number(config.maxTokens ?? 2048);
}

function applyNodeForm() {
  nodes.value = nodes.value.map((node) => {
    if (node.id !== selectedNodeId.value) {
      return node;
    }
    const config = buildNodeConfig();
    return {
      ...node,
      label: nodeForm.nodeName,
      class: nodeClass(nodeForm.nodeType),
      data: {
        ...node.data,
        label: nodeForm.nodeName,
        nodeType: nodeForm.nodeType,
        config,
      },
    };
  });
}

function buildNodeConfig() {
  if (nodeForm.nodeType === 'RAG') {
    return { queryTemplate: nodeForm.queryTemplate };
  }
  if (nodeForm.nodeType === 'TOOL') {
    return { toolName: nodeForm.toolName, arguments: { input: '{{input}}', lastOutput: '{{lastOutput}}' } };
  }
  if (nodeForm.nodeType === 'CONDITION') {
    return { conditionExpr: nodeForm.conditionExpr };
  }
  if (nodeForm.nodeType === 'LLM') {
    return {
      promptTemplate: nodeForm.promptTemplate,
      systemPrompt: nodeForm.systemPrompt,
      temperature: Number(nodeForm.temperature),
      maxTokens: Number(nodeForm.maxTokens),
    };
  }
  return {};
}

async function handleSave() {
  applyNodeForm();
  const payload = toWorkflowRequest();
  currentWorkflow.value = currentWorkflow.value
    ? await updateWorkflow(currentWorkflow.value.id, payload)
    : await createWorkflow(payload);
  workflows.value = await fetchWorkflows();
  await openWorkflow(currentWorkflow.value.id);
  toast('工作流已保存');
}

async function handlePublish() {
  if (!currentWorkflow.value) {
    await handleSave();
  }
  if (!currentWorkflow.value) {
    return;
  }
  currentWorkflow.value = await publishWorkflow(currentWorkflow.value.id);
  workflows.value = await fetchWorkflows();
  toast('工作流已发布');
}

async function handleRun() {
  if (!currentWorkflow.value) {
    await handleSave();
  }
  if (!currentWorkflow.value) {
    return;
  }
  const result = await runWorkflow(currentWorkflow.value.id, selectedAgentId.value || undefined, runInput.value);
  runResult.value = result.outputText || result.errorMessage || '';
  traceRunId.value = result.runtimeRunId;
  toast(result.status === 'SUCCESS' ? '工作流运行成功' : '工作流运行失败');
}

function toWorkflowRequest(): WorkflowRequest {
  return {
    workflowCode: workflowForm.workflowCode,
    workflowName: workflowForm.workflowName,
    description: workflowForm.description,
    workflowType: workflowForm.workflowType,
    visibility: workflowForm.visibility,
    status: workflowForm.status,
    nodes: nodes.value.map((node) => ({
      nodeKey: node.id,
      nodeName: String(node.label || node.data?.label || node.id),
      nodeType: String(node.data?.nodeType || 'LLM'),
      positionX: node.position.x,
      positionY: node.position.y,
      configJson: (node.data?.config || {}) as Record<string, unknown>,
      enabled: true,
    })),
    edges: edges.value.map((edge) => ({
      edgeKey: edge.id,
      sourceNodeKey: edge.source,
      targetNodeKey: edge.target,
      conditionExpr: String(edge.data?.conditionExpr || edge.label || ''),
      label: String(edge.label || ''),
      metadata: {},
    })),
    graphJson: { nodes: nodes.value, edges: edges.value },
    variableSchema: {},
  };
}

function defaultConfig(type: string) {
  if (type === 'RAG') return { queryTemplate: '{{input}}' };
  if (type === 'LLM') return { promptTemplate: '{{input}}', temperature: 0.3, maxTokens: 2048 };
  if (type === 'TOOL') return { toolName: tools.value[0]?.toolCode || '', arguments: { input: '{{input}}' } };
  if (type === 'CONDITION') return { conditionExpr: 'success' };
  return {};
}

function nodeClass(type: string) {
  if (type === 'START' || type === 'END') return 'flow-node start';
  if (type === 'CONDITION') return 'flow-node decision';
  return 'flow-node';
}

function canReceive(nodeType: unknown) {
  return String(nodeType || '') !== 'START';
}

function canSend(nodeType: unknown) {
  return String(nodeType || '') !== 'END';
}
</script>

<template>
  <PageHeader :title="`工作流编排 / ${workflowForm.workflowName}`" :description="workflowForm.description || '编排 Agent、RAG、LLM、工具和条件分支'">
    <template #actions>
      <button class="secondary-button" type="button" @click="createDraftCanvas"><Plus :size="16" /> 新建</button>
      <button class="secondary-button" type="button" :disabled="loading" @click="handleSave"><Save :size="16" /> 保存</button>
      <button class="secondary-button" type="button" :disabled="loading" @click="handleRun"><Play :size="16" /> 运行</button>
      <button class="secondary-button" type="button" :disabled="!traceRunId" @click="$router.push(`/logs/${traceRunId}`)"><Bug :size="16" /> Trace</button>
      <button class="primary-button" type="button" :disabled="loading" @click="handlePublish"><Rocket :size="16" /> 发布</button>
    </template>
  </PageHeader>

  <section class="workflow-layout">
    <aside class="node-palette">
      <h2>节点库</h2>
      <button v-for="item in palette" :key="item.type" type="button" @click="addNode(item.type, item.label)">{{ item.label }}</button>

      <div class="workflow-list">
        <h2>工作流</h2>
        <button v-for="item in pagedWorkflows" :key="item.id" type="button" :class="{ active: currentWorkflow?.id === item.id }" @click="openWorkflow(item.id)">
          {{ item.workflowName }}
          <small>{{ item.statusLabel }}</small>
        </button>
        <PaginationBar v-model:page="workflowPage" :total="workflows.length" />
      </div>
    </aside>

    <div class="flow-shell">
      <div class="workflow-toolbar">
        <label>名称<input v-model="workflowForm.workflowName" /></label>
        <label>编码<input v-model="workflowForm.workflowCode" placeholder="不填自动生成" /></label>
        <label>状态<select v-model="workflowForm.status"><option value="draft">草稿</option><option value="published">已发布</option><option value="disabled">停用</option></select></label>
        <span>{{ currentStatus }}</span>
      </div>
      <VueFlow
        v-model:nodes="nodes"
        v-model:edges="edges"
        fit-view-on-init
        class="flow-canvas"
        @connect="onConnect"
        @node-click="selectNode"
      >
        <template #node-workflowNode="{ data }">
          <Handle v-if="canReceive(data.nodeType)" type="target" :position="Position.Left" />
          <div class="workflow-node-content">
            <strong>{{ data.label }}</strong>
            <small>{{ data.nodeType }}</small>
          </div>
          <Handle v-if="canSend(data.nodeType)" type="source" :position="Position.Right" />
        </template>
      </VueFlow>
    </div>

    <aside class="node-config">
      <div class="section-title">
        <h2>节点配置</h2>
        <span>{{ selectedNode?.id || '未选择' }}</span>
      </div>
      <div class="form-stack">
        <label>节点名称<input v-model="nodeForm.nodeName" @blur="applyNodeForm" /></label>
        <label>节点类型<select v-model="nodeForm.nodeType" @change="applyNodeForm">
          <option value="START">开始</option>
          <option value="LLM">LLM</option>
          <option value="RAG">RAG</option>
          <option value="TOOL">工具</option>
          <option value="CONDITION">条件</option>
          <option value="END">结束</option>
        </select></label>

        <template v-if="nodeForm.nodeType === 'LLM'">
          <label>模型<select><option v-for="model in models" :key="model.id">{{ model.providerName }} / {{ model.modelName }}</option></select></label>
          <label>Temperature<input v-model.number="nodeForm.temperature" type="range" min="0" max="2" step="0.01" @change="applyNodeForm" /></label>
          <label>最大 Tokens<input v-model.number="nodeForm.maxTokens" type="number" min="256" step="128" @blur="applyNodeForm" /></label>
          <label>System Prompt<textarea v-model="nodeForm.systemPrompt" @blur="applyNodeForm" /></label>
          <label>Prompt 模板<textarea v-model="nodeForm.promptTemplate" @blur="applyNodeForm" /></label>
        </template>

        <template v-if="nodeForm.nodeType === 'RAG'">
          <label>检索问题模板<textarea v-model="nodeForm.queryTemplate" @blur="applyNodeForm" /></label>
        </template>

        <template v-if="nodeForm.nodeType === 'TOOL'">
          <label>工具<select v-model="nodeForm.toolName" @change="applyNodeForm">
            <option value="">请选择</option>
            <option v-for="tool in tools" :key="tool.id" :value="tool.toolCode">{{ tool.toolName }} / {{ tool.toolCode }}</option>
          </select></label>
        </template>

        <template v-if="nodeForm.nodeType === 'CONDITION'">
          <label>条件表达式<input v-model="nodeForm.conditionExpr" placeholder="success / contains:文本 / equals:文本" @blur="applyNodeForm" /></label>
        </template>

        <button class="secondary-button full" type="button" @click="applyNodeForm"><Save :size="16" /> 保存节点配置</button>
        <button class="danger-button full" type="button" @click="removeSelectedNode"><Trash2 :size="16" /> 删除节点</button>

        <div class="section-title"><h2>运行测试</h2><span>{{ selectedAgentId ? '已选 Agent' : '可直接运行 LLM' }}</span></div>
        <label>Agent<select v-model="selectedAgentId"><option value="">不绑定 Agent</option><option v-for="agent in agents" :key="agent.id" :value="agent.id">{{ agent.agentName }}</option></select></label>
        <label>输入<textarea v-model="runInput" /></label>
        <div v-if="runResult" class="run-result">{{ runResult }}</div>
      </div>
    </aside>
  </section>
</template>
