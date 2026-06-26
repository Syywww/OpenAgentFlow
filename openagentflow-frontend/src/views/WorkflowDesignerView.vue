<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Handle, Position, VueFlow, type Connection } from '@vue-flow/core';
import { Bug, Check, GitCompare, Link2, MessageSquare, Play, Plus, Rocket, Save, Settings2, Trash2, X, XCircle } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import StatCard from '../components/StatCard.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { fetchAgents, type AgentSummary } from '../api/agents';
import { fetchChatModels, type ModelConfigSummary } from '../api/models';
import { fetchTools, type ToolDefinitionSummary } from '../api/tools';
import {
  createWorkflow,
  decideWorkflowHumanTask,
  fetchWorkflow,
  fetchWorkflowAdvancedOverview,
  fetchWorkflowApiEndpoints,
  fetchWorkflowHumanTasks,
  fetchWorkflowTemplates,
  fetchWorkflowVersionDiff,
  fetchWorkflows,
  publishWorkflow,
  publishWorkflowApiEndpoint,
  runWorkflow,
  updateWorkflow,
  type WorkflowAdvancedOverview,
  type WorkflowApiEndpointSummary,
  type WorkflowDetail,
  type WorkflowEdgeDto,
  type WorkflowHumanTaskSummary,
  type WorkflowNodeDto,
  type WorkflowRequest,
  type WorkflowSummary,
  type WorkflowTemplateSummary,
  type WorkflowVersionDiff,
} from '../api/workflows';
import { useOverlay } from '../composables/useOverlay';
import { usePagination } from '../composables/usePagination';

type FlowNode = {
  id: string;
  type?: string;
  label?: string;
  position: { x: number; y: number };
  class?: string;
  data?: {
    label?: string;
    nodeType?: string;
    config?: Record<string, unknown>;
    retryPolicy?: Record<string, unknown>;
  };
};

type FlowEdge = {
  id: string;
  source: string;
  target: string;
  label?: string;
  data?: Record<string, unknown>;
};

type WorkflowRunStepView = {
  nodeKey: string;
  nodeName: string;
  nodeType: string;
  status: string;
  output?: unknown;
  errorMessage?: string;
  tokenCount?: number;
  latencyMs?: number;
  attemptNo?: number;
};

const { toast } = useOverlay();
const workflows = ref<WorkflowSummary[]>([]);
const currentWorkflow = ref<WorkflowDetail | null>(null);
const nodes = ref<FlowNode[]>([]);
const edges = ref<FlowEdge[]>([]);
const selectedNodeId = ref('start');
const loading = ref(false);
const agents = ref<AgentSummary[]>([]);
const models = ref<ModelConfigSummary[]>([]);
const tools = ref<ToolDefinitionSummary[]>([]);
const overview = ref<WorkflowAdvancedOverview | null>(null);
const templates = ref<WorkflowTemplateSummary[]>([]);
const apiEndpoints = ref<WorkflowApiEndpointSummary[]>([]);
const humanTasks = ref<WorkflowHumanTaskSummary[]>([]);
const versionDiff = ref<WorkflowVersionDiff | null>(null);
const activePanel = ref<'node' | 'debug' | 'templates' | 'api' | 'governance' | 'versions'>('node');
const runInput = ref('请基于工作流回答：OpenAgentFlow 现在支持哪些核心能力？');
const selectedAgentId = ref('');
const runResult = ref('');
const traceRunId = ref('');
const runSteps = ref<WorkflowRunStepView[]>([]);
const runningNodeId = ref('');
const createModalOpen = ref(false);
const nodePickerOpen = ref(false);
const nodePickerPosition = ref({ x: 24, y: 80 });
const pendingNodePosition = ref({ x: 120, y: 120 });
const chatPanelNodeId = ref('');
let runAnimationTimer: ReturnType<typeof window.setInterval> | null = null;
const { currentPage: workflowPage, pagedItems: pagedWorkflows } = usePagination(workflows);
const { currentPage: templatePage, pagedItems: pagedTemplates } = usePagination(templates);
const { currentPage: taskPage, pagedItems: pagedHumanTasks } = usePagination(humanTasks);

const workflowForm = reactive({
  workflowCode: '',
  workflowName: '企业问答工作流',
  description: '开始 -> RAG 检索 -> LLM 生成 -> 结束',
  workflowType: 'agent_workflow',
  visibility: 'private',
  status: 'draft',
  budgetTokens: 8000,
  budgetCost: 1,
  grayPercent: 100,
  releaseStrategy: 'standard',
});

const createForm = reactive({
  workflowCode: '',
  workflowName: '',
  description: '',
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
  retryCount: 1,
  retryIntervalMs: 500,
  timeoutMs: 60000,
  failureStrategy: 'STOP',
  failureTargetNodeKey: '',
  fallbackOutput: '',
  sandboxLevel: 'low',
  budgetTokens: 0,
  taskName: '人工确认',
  expireMinutes: 60,
  itemPath: '',
  itemTemplate: '{{item}}',
  maxLoops: 20,
  subWorkflowId: '',
  pluginCode: '',
  joinStrategy: 'all',
});

const debugForm = reactive({
  debugMode: true,
  dryRun: false,
  startNodeKey: '',
  maxSteps: 100,
});

const apiForm = reactive({
  endpointCode: '',
  endpointName: '',
  authType: 'jwt',
  rateLimitPerMinute: 60,
  enabled: true,
});

const diffForm = reactive({
  leftVersion: '',
  rightVersion: '',
});

const palette = [
  { type: 'START', label: '开始' },
  { type: 'LLM', label: 'LLM' },
  { type: 'RAG', label: 'RAG 检索' },
  { type: 'TOOL', label: '工具调用' },
  { type: 'CONDITION', label: '条件' },
  { type: 'HUMAN', label: '人工确认' },
  { type: 'PARALLEL', label: '并行' },
  { type: 'JOIN', label: '汇聚' },
  { type: 'LOOP', label: '循环' },
  { type: 'SUBFLOW', label: '子流程' },
  { type: 'PLUGIN', label: '插件' },
  { type: 'API', label: 'API/Webhook' },
  { type: 'NOTIFY', label: '通知' },
  { type: 'END', label: '结束' },
];

const selectedNode = computed(() => nodes.value.find((node) => node.id === selectedNodeId.value));
const currentStatus = computed(() => currentWorkflow.value?.statusLabel || '草稿');
const selectedEndpoint = computed(() => apiEndpoints.value.find((item) => item.workflowId === currentWorkflow.value?.id));
const capabilityRows = computed(() => overview.value?.capabilities || []);
const versions = computed(() => currentWorkflow.value?.versions || []);
const nodeTypeOptions = [
  { type: 'START', label: '开始' },
  { type: 'LLM', label: '对话 / LLM' },
  { type: 'RAG', label: 'RAG 检索' },
  { type: 'TOOL', label: '工具调用' },
  { type: 'CONDITION', label: '条件判断' },
  { type: 'HUMAN', label: '人工确认' },
  { type: 'PARALLEL', label: '并行' },
  { type: 'JOIN', label: '汇聚' },
  { type: 'LOOP', label: '循环' },
  { type: 'SUBFLOW', label: '子流程' },
  { type: 'PLUGIN', label: '插件' },
  { type: 'API', label: 'API/Webhook' },
  { type: 'NOTIFY', label: '通知' },
  { type: 'END', label: '结束' },
];
const stepByNodeKey = computed(() => Object.fromEntries(runSteps.value.map((step) => [step.nodeKey, step])));
const activeChatNode = computed(() => nodes.value.find((node) => node.id === chatPanelNodeId.value));
const activeChatStep = computed(() => (chatPanelNodeId.value ? stepByNodeKey.value[chatPanelNodeId.value] : undefined));

onMounted(async () => {
  await Promise.all([loadOptions(), loadWorkflowList(), loadAdvancedData()]);
});

async function loadOptions() {
  const [agentRows, modelRows, toolRows] = await Promise.all([fetchAgents(), fetchChatModels(), fetchTools()]);
  agents.value = agentRows;
  models.value = modelRows;
  tools.value = toolRows;
  selectedAgentId.value = agentRows[0]?.id || '';
}

async function loadAdvancedData() {
  try {
    const [overviewResult, templateRows, endpointRows, taskRows] = await Promise.all([
      fetchWorkflowAdvancedOverview(),
      fetchWorkflowTemplates(),
      fetchWorkflowApiEndpoints(),
      fetchWorkflowHumanTasks(),
    ]);
    overview.value = overviewResult;
    templates.value = templateRows;
    apiEndpoints.value = endpointRows;
    humanTasks.value = taskRows;
  } catch (error) {
    toast(error instanceof Error ? error.message : '工作流增强数据加载失败');
  }
}

async function loadWorkflowList() {
  workflows.value = await fetchWorkflows();
  if (workflows.value.length > 0) {
    await openWorkflow(workflows.value[0].id);
    return;
  }
  resetEmptyCanvas();
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
    const graphNodes = Array.isArray(detail.graphJson?.nodes) ? detail.graphJson.nodes : null;
    const graphEdges = Array.isArray(detail.graphJson?.edges) ? detail.graphJson.edges : null;
    nodes.value = graphNodes && graphNodes.length === 0 ? [] : detail.nodes.map(toFlowNode);
    edges.value = graphEdges && graphEdges.length === 0 ? [] : detail.edges.map(toFlowEdge);
    selectedNodeId.value = nodes.value[0]?.id || '';
    chatPanelNodeId.value = '';
    runSteps.value = [];
    runningNodeId.value = '';
    const policy = readExecutionPolicy(detail.graphJson);
    workflowForm.budgetTokens = Number(policy.budgetTokens ?? 8000);
    workflowForm.budgetCost = Number(policy.budgetCost ?? 1);
    workflowForm.grayPercent = Number(policy.grayPercent ?? 100);
    workflowForm.releaseStrategy = String(policy.releaseStrategy || 'standard');
    fillNodeForm();
    fillApiForm();
    resetDiffForm();
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
  workflowForm.budgetTokens = 8000;
  workflowForm.budgetCost = 1;
  workflowForm.grayPercent = 100;
  workflowForm.releaseStrategy = 'standard';
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

function resetEmptyCanvas() {
  currentWorkflow.value = null;
  workflowForm.workflowCode = '';
  workflowForm.workflowName = '未选择工作流';
  workflowForm.description = '请先新建工作流，创建成功后再双击画布添加节点';
  workflowForm.workflowType = 'agent_workflow';
  workflowForm.visibility = 'private';
  workflowForm.status = 'draft';
  workflowForm.budgetTokens = 8000;
  workflowForm.budgetCost = 1;
  workflowForm.grayPercent = 100;
  workflowForm.releaseStrategy = 'standard';
  nodes.value = [];
  edges.value = [];
  selectedNodeId.value = '';
  chatPanelNodeId.value = '';
  runSteps.value = [];
}

function openCreateModal() {
  createForm.workflowCode = '';
  createForm.workflowName = `工作流${workflows.value.length + 1}`;
  createForm.workflowName = `工作流${workflows.value.length + 1}`;
  createForm.description = '';
  createForm.workflowType = 'agent_workflow';
  createForm.visibility = 'private';
  createForm.status = 'draft';
  createModalOpen.value = true;
}

function closeCreateModal() {
  createModalOpen.value = false;
}

async function handleCreateWorkflow() {
  if (!createForm.workflowName.trim()) {
    toast('请先填写工作流名称');
    return;
  }
  loading.value = true;
  try {
    const created = await createWorkflow({
      workflowCode: createForm.workflowCode.trim(),
      workflowName: createForm.workflowName.trim(),
      description: createForm.description.trim(),
      workflowType: createForm.workflowType,
      visibility: createForm.visibility,
      status: createForm.status,
      nodes: [],
      edges: [],
      graphJson: { nodes: [], edges: [], executionPolicy: { budgetTokens: 8000, budgetCost: 1, grayPercent: 100, releaseStrategy: 'standard' } },
      variableSchema: { input: { type: 'string', title: '用户输入' } },
    });
    workflows.value = await fetchWorkflows();
    await openWorkflow(created.id);
    currentWorkflow.value = { ...created, nodes: [], edges: [], graphJson: { nodes: [], edges: [] } };
    workflowForm.workflowCode = created.workflowCode || '';
    workflowForm.workflowName = created.workflowName;
    workflowForm.description = created.description || '';
    workflowForm.workflowType = created.workflowType || 'agent_workflow';
    workflowForm.visibility = created.visibility || 'private';
    workflowForm.status = created.status || 'draft';
    workflowForm.budgetTokens = 8000;
    workflowForm.budgetCost = 1;
    workflowForm.grayPercent = 100;
    workflowForm.releaseStrategy = 'standard';
    nodes.value = [];
    edges.value = [];
    selectedNodeId.value = '';
    chatPanelNodeId.value = '';
    runSteps.value = [];
    runningNodeId.value = '';
    workflows.value = workflows.value.map((item) => (item.id === created.id ? { ...item, nodeCount: 0 } : item));
    createModalOpen.value = false;
    toast('工作流已创建，请在画布上双击添加节点');
  } finally {
    loading.value = false;
  }
}

function applyTemplate(template: WorkflowTemplateSummary) {
  currentWorkflow.value = null;
  workflowForm.workflowCode = '';
  workflowForm.workflowName = template.templateName;
  workflowForm.description = template.description || '';
  const graphNodes = Array.isArray(template.graphJson.nodes) ? template.graphJson.nodes : [];
  const graphEdges = Array.isArray(template.graphJson.edges) ? template.graphJson.edges : [];
  nodes.value = graphNodes.map((item, index) => graphNodeToFlowNode(item, index));
  edges.value = graphEdges.map((item, index) => graphEdgeToFlowEdge(item, index));
  const policy = template.defaultPolicy || {};
  workflowForm.budgetTokens = Number(policy.budgetTokens ?? 8000);
  workflowForm.grayPercent = 100;
  selectedNodeId.value = nodes.value[0]?.id || '';
  activePanel.value = 'node';
  fillNodeForm();
  toast('模板已应用到新画布');
}

function buildNode(id: string, label: string, nodeType: string, x: number, y: number, config: Record<string, unknown>, retryPolicy: Record<string, unknown> = {}): FlowNode {
  return {
    id,
    type: 'workflowNode',
    label,
    position: { x, y },
    class: nodeClass(nodeType),
    data: { label, nodeType, config, retryPolicy },
  };
}

function graphNodeToFlowNode(raw: unknown, index: number): FlowNode {
  const item = raw as Record<string, unknown>;
  const data = (item.data || {}) as Record<string, unknown>;
  const position = (item.position || {}) as Record<string, unknown>;
  const id = String(item.id || `node_${Date.now()}_${index}`);
  const nodeType = String(data.nodeType || 'LLM');
  return buildNode(
    id,
    String(data.label || item.label || id),
    nodeType,
    Number(position.x || 120 + index * 140),
    Number(position.y || 120),
    (data.config || {}) as Record<string, unknown>,
    (data.retryPolicy || {}) as Record<string, unknown>,
  );
}

function graphEdgeToFlowEdge(raw: unknown, index: number): FlowEdge {
  const item = raw as Record<string, unknown>;
  return {
    id: String(item.id || `edge_${Date.now()}_${index}`),
    source: String(item.source || ''),
    target: String(item.target || ''),
    label: String(item.label || ''),
    data: (item.data || {}) as Record<string, unknown>,
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
    (node.retryPolicy || {}) as Record<string, unknown>,
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
  if (!connection.source || !connection.target) return;
  edges.value = [...edges.value, { id: `e_${connection.source}_${connection.target}_${Date.now()}`, source: connection.source, target: connection.target }];
}

function nodeTypeLabel(type: string) {
  return nodeTypeOptions.find((item) => item.type === type)?.label || type;
}

function handleFlowShellDblClick(event: MouseEvent) {
  const target = event.target as HTMLElement | null;
  if (!target?.closest('.flow-canvas')) return;
  if (target.closest('.vue-flow__node') || target.closest('.node-picker-popover') || target.closest('.workflow-node-dialog-panel')) return;
  openNodePicker(event);
}

function openNodePicker(payload: MouseEvent | { event?: MouseEvent }) {
  if (!currentWorkflow.value) {
    openCreateModal();
    toast('请先新建工作流基础信息');
    return;
  }
  const event = payload instanceof MouseEvent ? payload : payload.event;
  if (!event) return;
  const shell = (event.target as HTMLElement | null)?.closest('.flow-shell') as HTMLElement | null;
  const canvas = shell?.querySelector('.flow-canvas') as HTMLElement | null;
  const shellRect = shell?.getBoundingClientRect();
  const canvasRect = canvas?.getBoundingClientRect();
  if (shellRect) {
    nodePickerPosition.value = {
      x: Math.max(12, Math.min(event.clientX - shellRect.left, shellRect.width - 260)),
      y: Math.max(52, Math.min(event.clientY - shellRect.top, shellRect.height - 240)),
    };
  }
  if (canvasRect) {
    pendingNodePosition.value = {
      x: Math.max(20, event.clientX - canvasRect.left - 66),
      y: Math.max(20, event.clientY - canvasRect.top - 32),
    };
  }
  nodePickerOpen.value = true;
}

function closeNodePicker() {
  nodePickerOpen.value = false;
}

function addNode(type: string, label: string) {
  if (!currentWorkflow.value) {
    openCreateModal();
    toast('请先新建工作流基础信息');
    return;
  }
  const id = `${type.toLowerCase()}_${Date.now()}`;
  const position = pendingNodePosition.value;
  nodes.value.push(buildNode(id, label, type, position.x, position.y, defaultConfig(type), defaultRetryPolicy(type)));
  selectedNodeId.value = id;
  activePanel.value = 'node';
  nodePickerOpen.value = false;
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
  activePanel.value = 'node';
  fillNodeForm();
}

function fillNodeForm() {
  const node = selectedNode.value;
  if (!node) return;
  const config = node.data?.config || {};
  const retryPolicy = node.data?.retryPolicy || {};
  nodeForm.nodeName = String(node.label || node.data?.label || node.id);
  nodeForm.nodeType = String(node.data?.nodeType || 'LLM');
  nodeForm.promptTemplate = String(config.promptTemplate || '{{input}}');
  nodeForm.systemPrompt = String(config.systemPrompt || '');
  nodeForm.queryTemplate = String(config.queryTemplate || '{{input}}');
  nodeForm.toolName = String(config.toolName || config.toolCode || '');
  nodeForm.conditionExpr = String(config.conditionExpr || 'success');
  nodeForm.temperature = Number(config.temperature ?? 0.3);
  nodeForm.maxTokens = Number(config.maxTokens ?? 2048);
  nodeForm.retryCount = Number(config.retryCount ?? retryPolicy.retryCount ?? 1);
  nodeForm.retryIntervalMs = Number(config.retryIntervalMs ?? retryPolicy.retryIntervalMs ?? 500);
  nodeForm.timeoutMs = Number(config.timeoutMs ?? retryPolicy.timeoutMs ?? 60000);
  nodeForm.failureStrategy = String(config.failureStrategy || retryPolicy.failureStrategy || 'STOP');
  nodeForm.failureTargetNodeKey = String(config.failureTargetNodeKey || '');
  nodeForm.fallbackOutput = String(config.fallbackOutput || '');
  nodeForm.sandboxLevel = String(config.sandboxLevel || 'low');
  nodeForm.budgetTokens = Number(config.budgetTokens ?? 0);
  nodeForm.taskName = String(config.taskName || '人工确认');
  nodeForm.expireMinutes = Number(config.expireMinutes ?? 60);
  nodeForm.itemPath = String(config.itemPath || '');
  nodeForm.itemTemplate = String(config.itemTemplate || '{{item}}');
  nodeForm.maxLoops = Number(config.maxLoops ?? 20);
  nodeForm.subWorkflowId = String(config.workflowId || '');
  nodeForm.pluginCode = String(config.pluginCode || '');
  nodeForm.joinStrategy = String(config.joinStrategy || 'all');
}

function applyNodeForm() {
  nodes.value = nodes.value.map((node) => {
    if (node.id !== selectedNodeId.value) return node;
    const config = buildNodeConfig();
    const retryPolicy = {
      retryCount: Number(nodeForm.retryCount),
      retryIntervalMs: Number(nodeForm.retryIntervalMs),
      timeoutMs: Number(nodeForm.timeoutMs),
      failureStrategy: nodeForm.failureStrategy,
    };
    return {
      ...node,
      label: nodeForm.nodeName,
      class: nodeClass(nodeForm.nodeType),
      data: { ...node.data, label: nodeForm.nodeName, nodeType: nodeForm.nodeType, config, retryPolicy },
    };
  });
}

function buildNodeConfig() {
  const common = {
    retryCount: Number(nodeForm.retryCount),
    retryIntervalMs: Number(nodeForm.retryIntervalMs),
    timeoutMs: Number(nodeForm.timeoutMs),
    failureStrategy: nodeForm.failureStrategy,
    failureTargetNodeKey: nodeForm.failureTargetNodeKey,
    fallbackOutput: nodeForm.fallbackOutput,
    sandboxLevel: nodeForm.sandboxLevel,
    budgetTokens: Number(nodeForm.budgetTokens),
  };
  if (nodeForm.nodeType === 'RAG') return { ...common, queryTemplate: nodeForm.queryTemplate };
  if (nodeForm.nodeType === 'TOOL' || nodeForm.nodeType === 'API' || nodeForm.nodeType === 'NOTIFY') return { ...common, toolName: nodeForm.toolName, arguments: { input: '{{input}}', lastOutput: '{{lastOutput}}' } };
  if (nodeForm.nodeType === 'CONDITION') return { ...common, conditionExpr: nodeForm.conditionExpr };
  if (nodeForm.nodeType === 'HUMAN') return { ...common, taskName: nodeForm.taskName, expireMinutes: Number(nodeForm.expireMinutes), suggestion: '{{lastOutput}}' };
  if (nodeForm.nodeType === 'LOOP') return { ...common, itemPath: nodeForm.itemPath, itemTemplate: nodeForm.itemTemplate, maxLoops: Number(nodeForm.maxLoops) };
  if (nodeForm.nodeType === 'SUBFLOW') return { ...common, workflowId: nodeForm.subWorkflowId, inputTemplate: '{{lastOutput}}' };
  if (nodeForm.nodeType === 'PLUGIN') return { ...common, pluginCode: nodeForm.pluginCode, toolName: nodeForm.toolName };
  if (nodeForm.nodeType === 'PARALLEL' || nodeForm.nodeType === 'JOIN') return { ...common, joinStrategy: nodeForm.joinStrategy };
  if (nodeForm.nodeType === 'LLM') return { ...common, promptTemplate: nodeForm.promptTemplate, systemPrompt: nodeForm.systemPrompt, temperature: Number(nodeForm.temperature), maxTokens: Number(nodeForm.maxTokens) };
  return common;
}

async function handleSave() {
  if (!currentWorkflow.value) {
    openCreateModal();
    toast('请先新建工作流基础信息');
    return;
  }
  applyNodeForm();
  const payload = toWorkflowRequest();
  currentWorkflow.value = await updateWorkflow(currentWorkflow.value.id, payload);
  workflows.value = await fetchWorkflows();
  await openWorkflow(currentWorkflow.value.id);
  toast('工作流已保存');
}

async function handlePublish() {
  if (!currentWorkflow.value) await handleSave();
  if (!currentWorkflow.value) return;
  currentWorkflow.value = await publishWorkflow(currentWorkflow.value.id, '工作流生产增强配置发布');
  workflows.value = await fetchWorkflows();
  toast('工作流已发布');
}

async function handleRun() {
  if (!currentWorkflow.value) await handleSave();
  if (!currentWorkflow.value) return;
  if (nodes.value.length === 0) {
    toast('当前工作流还没有节点，请双击画布添加节点');
    return;
  }
  await handleSave();
  if (!currentWorkflow.value) return;
  runResult.value = '';
  traceRunId.value = '';
  runSteps.value = [];
  startRunAnimation();
  try {
  const result = await runWorkflow(currentWorkflow.value.id, selectedAgentId.value || undefined, runInput.value, {
    debugMode: debugForm.debugMode,
    dryRun: debugForm.dryRun,
    startNodeKey: debugForm.startNodeKey || undefined,
    maxSteps: debugForm.maxSteps,
  });
  runSteps.value = normalizeRunSteps(result.steps);
  const lastStep = runSteps.value[runSteps.value.length - 1];
  if (lastStep?.nodeKey) runningNodeId.value = lastStep.nodeKey;
  runResult.value = result.outputText || result.errorMessage || '';
  traceRunId.value = result.runtimeRunId;
  toast(result.status === 'SUCCESS' ? '工作流运行成功' : result.status === 'WAITING' ? '工作流等待人工确认' : '工作流运行失败');
  await loadAdvancedData();
  } finally {
    stopRunAnimation(1200);
  }
}

function normalizeRunSteps(steps?: Array<Record<string, unknown>>): WorkflowRunStepView[] {
  return (steps || []).map((step) => ({
    nodeKey: String(step.nodeKey || ''),
    nodeName: String(step.nodeName || step.nodeKey || ''),
    nodeType: String(step.nodeType || ''),
    status: String(step.status || ''),
    output: step.output,
    errorMessage: step.errorMessage ? String(step.errorMessage) : '',
    tokenCount: Number(step.tokenCount || 0),
    latencyMs: Number(step.latencyMs || 0),
    attemptNo: Number(step.attemptNo || 1),
  })).filter((step) => step.nodeKey);
}

function buildRunPath() {
  const startId = debugForm.startNodeKey || nodes.value.find((node) => String(node.data?.nodeType || '').toUpperCase() === 'START')?.id || nodes.value[0]?.id || '';
  const path: string[] = [];
  const visited = new Set<string>();
  let cursor = startId;
  while (cursor && !visited.has(cursor) && path.length <= nodes.value.length + 5) {
    visited.add(cursor);
    path.push(cursor);
    cursor = edges.value.find((edge) => edge.source === cursor)?.target || '';
  }
  return path.length > 0 ? path : nodes.value.map((node) => node.id);
}

function startRunAnimation() {
  stopRunAnimation();
  const path = buildRunPath();
  if (path.length === 0) return;
  let index = 0;
  runningNodeId.value = path[index];
  runAnimationTimer = window.setInterval(() => {
    index = Math.min(index + 1, path.length - 1);
    runningNodeId.value = path[index];
  }, 900);
}

function stopRunAnimation(delay = 0) {
  if (runAnimationTimer) {
    window.clearInterval(runAnimationTimer);
    runAnimationTimer = null;
  }
  if (delay > 0) {
    window.setTimeout(() => {
      runningNodeId.value = '';
    }, delay);
    return;
  }
  runningNodeId.value = '';
}

function isChatNode(nodeType: unknown) {
  return String(nodeType || '').toUpperCase() === 'LLM';
}

function toggleNodeDialog(nodeId: string) {
  chatPanelNodeId.value = chatPanelNodeId.value === nodeId ? '' : nodeId;
}

function closeNodeDialog() {
  chatPanelNodeId.value = '';
}

function formatNodeOutput(step?: WorkflowRunStepView) {
  if (!step) return '该节点还没有运行输出。';
  if (step.errorMessage) return step.errorMessage;
  if (typeof step.output === 'string') return step.output;
  if (step.output == null) return '该节点没有返回内容。';
  return JSON.stringify(step.output, null, 2);
}

async function handlePublishApi() {
  if (!currentWorkflow.value) await handleSave();
  if (!currentWorkflow.value) return;
  const endpoint = await publishWorkflowApiEndpoint(currentWorkflow.value.id, { ...apiForm });
  apiForm.endpointCode = endpoint.endpointCode;
  apiForm.endpointName = endpoint.endpointName;
  await loadAdvancedData();
  toast('工作流 API 端点已发布');
}

async function handleHumanTask(id: string, decision: string) {
  await decideWorkflowHumanTask(id, { decision, comment: decision === 'approved' ? '同意继续' : '拒绝执行' });
  await loadAdvancedData();
  toast('人工确认任务已处理');
}

async function handleDiff() {
  if (!currentWorkflow.value || !diffForm.leftVersion || !diffForm.rightVersion) return;
  versionDiff.value = await fetchWorkflowVersionDiff(currentWorkflow.value.id, diffForm.leftVersion, diffForm.rightVersion);
}

function toWorkflowRequest(): WorkflowRequest {
  const executionPolicy = {
    budgetTokens: Number(workflowForm.budgetTokens),
    budgetCost: Number(workflowForm.budgetCost),
    grayPercent: Number(workflowForm.grayPercent),
    releaseStrategy: workflowForm.releaseStrategy,
  };
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
      retryPolicy: (node.data?.retryPolicy || {}) as Record<string, unknown>,
      enabled: true,
    })),
    edges: edges.value.map((edge) => ({
      edgeKey: edge.id,
      sourceNodeKey: edge.source,
      targetNodeKey: edge.target,
      conditionExpr: String(edge.data?.conditionExpr || edge.label || ''),
      label: String(edge.label || ''),
      metadata: edge.data || {},
    })),
    graphJson: { nodes: nodes.value, edges: edges.value, executionPolicy },
    variableSchema: { input: { type: 'string', title: '用户输入' } },
  };
}

function defaultConfig(type: string) {
  if (type === 'RAG') return { queryTemplate: '{{input}}' };
  if (type === 'LLM') return { promptTemplate: '{{input}}', temperature: 0.3, maxTokens: 2048 };
  if (type === 'TOOL' || type === 'API' || type === 'NOTIFY') return { toolName: tools.value[0]?.toolCode || '', arguments: { input: '{{input}}' } };
  if (type === 'CONDITION') return { conditionExpr: 'success' };
  if (type === 'HUMAN') return { taskName: '人工确认', expireMinutes: 60, suggestion: '{{lastOutput}}' };
  if (type === 'LOOP') return { itemPath: 'items', itemTemplate: '{{item}}', maxLoops: 20 };
  if (type === 'SUBFLOW') return { workflowId: workflows.value[0]?.id || '', inputTemplate: '{{lastOutput}}' };
  if (type === 'PLUGIN') return { pluginCode: 'custom-plugin' };
  if (type === 'PARALLEL' || type === 'JOIN') return { joinStrategy: 'all' };
  return {};
}

function defaultRetryPolicy(type: string) {
  return { retryCount: ['LLM', 'RAG', 'TOOL'].includes(type) ? 1 : 0, retryIntervalMs: 500, timeoutMs: type === 'HUMAN' ? 0 : 60000, failureStrategy: 'STOP' };
}

function nodeClass(type: string) {
  if (type === 'START' || type === 'END') return 'flow-node start';
  if (type === 'CONDITION') return 'flow-node decision';
  if (type === 'HUMAN') return 'flow-node human';
  if (['PARALLEL', 'JOIN', 'LOOP', 'SUBFLOW'].includes(type)) return 'flow-node orchestration';
  return 'flow-node';
}

function canReceive(nodeType: unknown) {
  return String(nodeType || '') !== 'START';
}

function canSend(nodeType: unknown) {
  return String(nodeType || '') !== 'END';
}

function readExecutionPolicy(graphJson?: Record<string, unknown>) {
  return ((graphJson || {}).executionPolicy || {}) as Record<string, unknown>;
}

function fillApiForm() {
  const endpoint = selectedEndpoint.value;
  apiForm.endpointCode = endpoint?.endpointCode || (currentWorkflow.value ? `wf-${currentWorkflow.value.workflowCode || currentWorkflow.value.id.slice(0, 8)}` : '');
  apiForm.endpointName = endpoint?.endpointName || `${workflowForm.workflowName} API`;
  apiForm.authType = endpoint?.authType || 'jwt';
  apiForm.rateLimitPerMinute = endpoint?.rateLimitPerMinute || 60;
  apiForm.enabled = endpoint?.enabled ?? true;
}

function resetDiffForm() {
  diffForm.leftVersion = versions.value[1]?.versionNo || '';
  diffForm.rightVersion = versions.value[0]?.versionNo || '';
  versionDiff.value = null;
}
</script>

<template>
  <PageHeader :title="`工作流编排 / ${workflowForm.workflowName}`" :description="workflowForm.description || '编排 Agent、RAG、LLM、工具和条件分支'">
    <template #actions>
      <button class="secondary-button" type="button" @click="openCreateModal"><Plus :size="16" /> 新建</button>
      <button class="secondary-button" type="button" :disabled="loading" @click="handleSave"><Save :size="16" /> 保存</button>
      <button class="secondary-button" type="button" :disabled="loading" @click="handleRun"><Play :size="16" /> 运行</button>
      <button class="secondary-button" type="button" :disabled="!traceRunId" @click="$router.push(`/logs/${traceRunId}`)"><Bug :size="16" /> Trace</button>
      <button class="primary-button" type="button" :disabled="loading" @click="handlePublish"><Rocket :size="16" /> 发布</button>
    </template>
  </PageHeader>

  <section class="metric-grid">
    <StatCard label="工作流" :value="String(overview?.workflowCount || workflows.length)" detail="已纳管流程" icon="Workflow" tone="info" />
    <StatCard label="API端点" :value="String(overview?.apiEndpointCount || 0)" detail="可被外部调用" icon="Braces" tone="success" />
    <StatCard label="人工任务" :value="String(overview?.pendingHumanTaskCount || 0)" detail="待确认" icon="ShieldAlert" tone="warning" />
    <StatCard label="今日失败" :value="String(overview?.todayFailedCount || 0)" detail="运行质量" icon="Gauge" :tone="overview?.todayFailedCount ? 'danger' : 'success'" />
  </section>

  <section class="workflow-layout production-workflow-layout">
    <aside class="node-palette workflow-sidebar">
      <h2>节点库</h2>
      <div class="workflow-palette-grid">
        <button v-for="item in palette" :key="item.type" type="button" @click="addNode(item.type, item.label)">{{ item.label }}</button>
      </div>

      <div class="workflow-list">
        <h2>工作流</h2>
        <button v-for="item in pagedWorkflows" :key="item.id" type="button" :class="{ active: currentWorkflow?.id === item.id }" @click="openWorkflow(item.id)">
          {{ item.workflowName }}
          <small>{{ item.statusLabel }} / {{ item.nodeCount }} 节点</small>
        </button>
        <PaginationBar v-model:page="workflowPage" :total="workflows.length" compact />
      </div>
    </aside>

    <div class="flow-shell" @dblclick="handleFlowShellDblClick">
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
        @pane-click="closeNodePicker"
        @pane-dblclick="openNodePicker"
        @node-click="selectNode"
      >
        <template #node-workflowNode="{ id, data }">
          <Handle v-if="canReceive(data.nodeType)" type="target" :position="Position.Left" />
          <button v-if="isChatNode(data.nodeType)" class="node-dialog-toggle" type="button" title="查看节点返回内容" @click.stop="toggleNodeDialog(id)">
            <MessageSquare :size="13" />
          </button>
          <div class="workflow-node-content">
            <strong>{{ data.label }}</strong>
            <small>{{ data.nodeType }}</small>
          </div>
          <span v-if="runningNodeId === id" class="workflow-node-spinner" aria-label="正在运行"></span>
          <Handle v-if="canSend(data.nodeType)" type="source" :position="Position.Right" />
        </template>
      </VueFlow>
      <div
        v-if="nodePickerOpen"
        class="node-picker-popover"
        :style="{ left: `${nodePickerPosition.x}px`, top: `${nodePickerPosition.y}px` }"
      >
        <div class="node-picker-header">
          <span>添加节点</span>
          <button class="icon-button" type="button" title="关闭" @click="closeNodePicker"><X :size="14" /></button>
        </div>
        <div class="node-picker-title-row">
          <b>选择节点类型</b>
          <span>双击画布后选择要添加的节点类型</span>
        </div>
        <div class="node-picker-grid">
          <button v-for="item in nodeTypeOptions" :key="item.type" type="button" @click="addNode(item.type, item.label)">
            <b>{{ item.label }}</b>
            <small>{{ item.type }}</small>
          </button>
        </div>
      </div>
      <aside v-if="chatPanelNodeId" class="workflow-node-dialog-panel">
        <div class="node-dialog-header">
          <div>
            <b>{{ activeChatNode?.data?.label || activeChatNode?.label || '对话节点' }}</b>
            <span>{{ activeChatStep?.status || '未运行' }}</span>
          </div>
          <button class="icon-button" type="button" title="关闭" @click="closeNodeDialog"><X :size="14" /></button>
        </div>
        <pre>{{ formatNodeOutput(activeChatStep) }}</pre>
        <div class="node-dialog-meta">
          <span>Token {{ activeChatStep?.tokenCount || 0 }}</span>
          <span>耗时 {{ activeChatStep?.latencyMs || 0 }} ms</span>
          <span>尝试 {{ activeChatStep?.attemptNo || 0 }}</span>
        </div>
      </aside>
    </div>

    <aside class="node-config production-node-config">
      <section class="governance-card-tabs workflow-card-tabs">
        <button class="governance-tab-card" :class="{ active: activePanel === 'node' }" type="button" @click="activePanel = 'node'"><span>节点配置</span><b><Settings2 :size="18" /></b><small>{{ selectedNode?.id || '-' }}</small></button>
        <button class="governance-tab-card" :class="{ active: activePanel === 'debug' }" type="button" @click="activePanel = 'debug'"><span>调试</span><b><Play :size="18" /></b><small>{{ traceRunId || '未运行' }}</small></button>
        <button class="governance-tab-card" :class="{ active: activePanel === 'templates' }" type="button" @click="activePanel = 'templates'"><span>模板</span><b>{{ templates.length }}</b><small>复用流程</small></button>
        <button class="governance-tab-card" :class="{ active: activePanel === 'api' }" type="button" @click="activePanel = 'api'"><span>API</span><b>{{ apiEndpoints.length }}</b><small>对外发布</small></button>
        <button class="governance-tab-card" :class="{ active: activePanel === 'governance' }" type="button" @click="activePanel = 'governance'"><span>治理</span><b>{{ capabilityRows.length }}</b><small>1-20</small></button>
        <button class="governance-tab-card" :class="{ active: activePanel === 'versions' }" type="button" @click="activePanel = 'versions'"><span>版本</span><b>{{ versions.length }}</b><small>差异</small></button>
      </section>

      <div v-if="activePanel === 'node'" class="form-stack workflow-scroll-panel">
        <div class="section-title"><h2>节点配置</h2><span>{{ selectedNode?.id || '未选择' }}</span></div>
        <label>节点名称<input v-model="nodeForm.nodeName" @blur="applyNodeForm" /></label>
        <label>节点类型<select v-model="nodeForm.nodeType" @change="applyNodeForm">
          <option v-for="item in nodeTypeOptions" :key="item.type" :value="item.type">{{ item.label }}</option>
        </select></label>

        <template v-if="nodeForm.nodeType === 'LLM'">
          <label>模型<select><option v-for="model in models" :key="model.id">{{ model.providerName }} / {{ model.modelName }}</option></select></label>
          <label>Temperature<input v-model.number="nodeForm.temperature" type="range" min="0" max="2" step="0.01" @change="applyNodeForm" /></label>
          <label>最大 Tokens<input v-model.number="nodeForm.maxTokens" type="number" min="256" step="128" @blur="applyNodeForm" /></label>
          <label>System Prompt<textarea v-model="nodeForm.systemPrompt" @blur="applyNodeForm" /></label>
          <label>Prompt 模板<textarea v-model="nodeForm.promptTemplate" @blur="applyNodeForm" /></label>
        </template>

        <label v-if="nodeForm.nodeType === 'RAG'">检索问题模板<textarea v-model="nodeForm.queryTemplate" @blur="applyNodeForm" /></label>

        <template v-if="['TOOL', 'PLUGIN', 'API', 'NOTIFY'].includes(nodeForm.nodeType)">
          <label>工具<select v-model="nodeForm.toolName" @change="applyNodeForm">
            <option value="">不绑定工具</option>
            <option v-for="tool in tools" :key="tool.id" :value="tool.toolCode">{{ tool.toolName }} / {{ tool.toolCode }}</option>
          </select></label>
          <label v-if="nodeForm.nodeType === 'PLUGIN'">插件编码<input v-model="nodeForm.pluginCode" @blur="applyNodeForm" /></label>
        </template>

        <label v-if="nodeForm.nodeType === 'CONDITION'">条件表达式<input v-model="nodeForm.conditionExpr" placeholder="success / contains:文本 / score>=80 / a&&b" @blur="applyNodeForm" /></label>

        <template v-if="nodeForm.nodeType === 'HUMAN'">
          <label>任务名称<input v-model="nodeForm.taskName" @blur="applyNodeForm" /></label>
          <label>过期分钟<input v-model.number="nodeForm.expireMinutes" type="number" @blur="applyNodeForm" /></label>
        </template>

        <template v-if="nodeForm.nodeType === 'LOOP'">
          <label>列表路径<input v-model="nodeForm.itemPath" placeholder="variables.items" @blur="applyNodeForm" /></label>
          <label>单项模板<input v-model="nodeForm.itemTemplate" @blur="applyNodeForm" /></label>
          <label>最大循环<input v-model.number="nodeForm.maxLoops" type="number" @blur="applyNodeForm" /></label>
        </template>

        <label v-if="nodeForm.nodeType === 'SUBFLOW'">子工作流<select v-model="nodeForm.subWorkflowId" @change="applyNodeForm"><option value="">请选择</option><option v-for="item in workflows" :key="item.id" :value="item.id">{{ item.workflowName }}</option></select></label>

        <div class="section-title compact-title"><h2>生产策略</h2><span>重试 / 超时 / 沙箱</span></div>
        <label>重试次数<input v-model.number="nodeForm.retryCount" type="number" min="0" @blur="applyNodeForm" /></label>
        <label>重试间隔(ms)<input v-model.number="nodeForm.retryIntervalMs" type="number" min="0" @blur="applyNodeForm" /></label>
        <label>超时(ms)<input v-model.number="nodeForm.timeoutMs" type="number" min="0" @blur="applyNodeForm" /></label>
        <label>失败策略<select v-model="nodeForm.failureStrategy" @change="applyNodeForm"><option value="STOP">终止</option><option value="CONTINUE">继续</option><option value="GOTO">跳转</option></select></label>
        <label v-if="nodeForm.failureStrategy === 'GOTO'">失败跳转节点<select v-model="nodeForm.failureTargetNodeKey" @change="applyNodeForm"><option value="">请选择</option><option v-for="node in nodes" :key="node.id" :value="node.id">{{ node.label }}</option></select></label>
        <label>兜底输出<input v-model="nodeForm.fallbackOutput" @blur="applyNodeForm" /></label>
        <label>沙箱等级<select v-model="nodeForm.sandboxLevel" @change="applyNodeForm"><option value="low">低</option><option value="medium">中</option><option value="high">高</option></select></label>
        <label>节点 Token 预算<input v-model.number="nodeForm.budgetTokens" type="number" min="0" @blur="applyNodeForm" /></label>

        <button class="secondary-button full" type="button" @click="applyNodeForm"><Save :size="16" /> 保存节点配置</button>
        <button class="danger-button full" type="button" @click="removeSelectedNode"><Trash2 :size="16" /> 删除节点</button>
      </div>

      <div v-else-if="activePanel === 'debug'" class="form-stack workflow-scroll-panel">
        <div class="section-title"><h2>调试运行</h2><span>{{ selectedAgentId ? '绑定 Agent' : '直接运行' }}</span></div>
        <label>Agent<select v-model="selectedAgentId"><option value="">不绑定 Agent</option><option v-for="agent in agents" :key="agent.id" :value="agent.id">{{ agent.agentName }}</option></select></label>
        <label>起始节点<select v-model="debugForm.startNodeKey"><option value="">START</option><option v-for="node in nodes" :key="node.id" :value="node.id">{{ node.label }}</option></select></label>
        <label>最大步数<input v-model.number="debugForm.maxSteps" type="number" min="1" /></label>
        <label class="check-line"><input v-model="debugForm.debugMode" type="checkbox" /> 调试模式</label>
        <label class="check-line"><input v-model="debugForm.dryRun" type="checkbox" /> 空跑外部调用</label>
        <label>输入<textarea v-model="runInput" /></label>
        <button class="primary-button full" type="button" @click="handleRun"><Play :size="16" /> 运行工作流</button>
        <div v-if="runResult" class="run-result">{{ runResult }}</div>
      </div>

      <div v-else-if="activePanel === 'templates'" class="workflow-scroll-panel">
        <div class="section-title"><h2>工作流模板</h2><span>{{ templates.length }} 个</span></div>
        <div v-for="item in pagedTemplates" :key="item.id" class="list-row workflow-template-row">
          <div><b>{{ item.templateName }}</b><span>{{ item.description }}</span></div>
          <StatusBadge :label="item.templateCategory" tone="info" />
          <button class="secondary-button slim" type="button" @click="applyTemplate(item)">应用</button>
        </div>
        <PaginationBar v-model:page="templatePage" :total="templates.length" />
      </div>

      <div v-else-if="activePanel === 'api'" class="form-stack workflow-scroll-panel">
        <div class="section-title"><h2>API 发布</h2><span>{{ selectedEndpoint?.endpointCode || '未发布' }}</span></div>
        <label>端点编码<input v-model="apiForm.endpointCode" /></label>
        <label>端点名称<input v-model="apiForm.endpointName" /></label>
        <label>认证方式<select v-model="apiForm.authType"><option value="jwt">JWT</option><option value="secret">Secret</option><option value="none">无认证</option></select></label>
        <label>每分钟限流<input v-model.number="apiForm.rateLimitPerMinute" type="number" min="1" /></label>
        <label class="check-line"><input v-model="apiForm.enabled" type="checkbox" /> 启用端点</label>
        <button class="primary-button full" type="button" @click="handlePublishApi"><Link2 :size="16" /> 发布 API</button>
        <div class="section-title compact-title"><h2>已发布端点</h2><span>{{ apiEndpoints.length }} 个</span></div>
        <div v-for="item in apiEndpoints" :key="item.id" class="list-row">
          <div><b>{{ item.endpointName }}</b><span class="mono">/api/workflow-api/{{ item.endpointCode }}</span></div>
          <StatusBadge :label="item.enabled ? '启用' : '停用'" :tone="item.enabled ? 'success' : 'neutral'" />
        </div>
      </div>

      <div v-else-if="activePanel === 'governance'" class="workflow-scroll-panel">
        <div class="section-title"><h2>治理能力</h2><span>覆盖 1-20</span></div>
        <div class="form-grid compact-workflow-policy">
          <label>流程 Token 预算<input v-model.number="workflowForm.budgetTokens" type="number" /></label>
          <label>流程成本预算<input v-model.number="workflowForm.budgetCost" type="number" step="0.01" /></label>
          <label>灰度比例<input v-model.number="workflowForm.grayPercent" type="number" min="0" max="100" /></label>
          <label>发布策略<select v-model="workflowForm.releaseStrategy"><option value="standard">标准</option><option value="gray">灰度</option><option value="manual">人工确认</option></select></label>
        </div>
        <div v-for="item in capabilityRows" :key="item.code" class="list-row workflow-capability-row">
          <div><b>{{ item.name }}</b><span>{{ item.description }}</span></div>
          <StatusBadge :label="item.status === 'ready' ? '已接入' : '已配置'" :tone="item.status === 'ready' ? 'success' : 'info'" />
        </div>
        <div class="section-title compact-title"><h2>人工确认任务</h2><span>{{ humanTasks.length }} 个</span></div>
        <div v-for="task in pagedHumanTasks" :key="task.id" class="list-row">
          <div><b>{{ task.taskName }}</b><span class="mono">{{ task.workflowRunId }}</span></div>
          <StatusBadge :label="task.status" />
          <div class="table-actions">
            <button class="secondary-button slim" type="button" @click="handleHumanTask(task.id, 'approved')"><Check :size="14" /> 同意</button>
            <button class="secondary-button slim danger-text" type="button" @click="handleHumanTask(task.id, 'rejected')"><XCircle :size="14" /> 拒绝</button>
          </div>
        </div>
        <PaginationBar v-model:page="taskPage" :total="humanTasks.length" />
      </div>

      <div v-else class="form-stack workflow-scroll-panel">
        <div class="section-title"><h2>版本差异</h2><span>{{ versions.length }} 个版本</span></div>
        <label>左侧版本<select v-model="diffForm.leftVersion"><option value="">请选择</option><option v-for="item in versions" :key="item.id" :value="item.versionNo">{{ item.versionNo }}</option></select></label>
        <label>右侧版本<select v-model="diffForm.rightVersion"><option value="">请选择</option><option v-for="item in versions" :key="item.id" :value="item.versionNo">{{ item.versionNo }}</option></select></label>
        <button class="secondary-button full" type="button" @click="handleDiff"><GitCompare :size="16" /> 对比版本</button>
        <div v-if="versionDiff" class="trace-meta">
          <span>新增节点</span><b>{{ versionDiff.addedNodes }}</b>
          <span>删除节点</span><b>{{ versionDiff.removedNodes }}</b>
          <span>连线变化</span><b>{{ versionDiff.changedEdges }}</b>
        </div>
        <div v-for="item in versionDiff?.changes || []" :key="item" class="list-row"><span>{{ item }}</span></div>
      </div>
    </aside>
  </section>
  <div v-if="createModalOpen" class="overlay-backdrop" @click.self="closeCreateModal">
    <section class="modal-panel compact workflow-create-modal">
      <header class="overlay-header">
        <div>
          <h2>新建工作流</h2>
          <p class="muted">先填写基础信息并创建空画布，创建成功后会自动选中新工作流。</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closeCreateModal"><X :size="18" /></button>
      </header>

      <div class="form-stack">
        <label>工作流名称<input v-model="createForm.workflowName" placeholder="例如：客户问题分流流程" /></label>
        <label>工作流编码<input v-model="createForm.workflowCode" placeholder="不填则由后端自动生成" /></label>
        <label>工作流类型
          <select v-model="createForm.workflowType">
            <option value="agent_workflow">Agent 工作流</option>
            <option value="business_workflow">业务工作流</option>
            <option value="evaluation_workflow">评测工作流</option>
          </select>
        </label>
        <label>可见范围
          <select v-model="createForm.visibility">
            <option value="private">私有</option>
            <option value="workspace">工作空间</option>
            <option value="public">公开</option>
          </select>
        </label>
        <label>状态
          <select v-model="createForm.status">
            <option value="draft">草稿</option>
            <option value="disabled">停用</option>
          </select>
        </label>
        <label>描述<textarea v-model="createForm.description" rows="4" placeholder="描述流程用途、输入来源和预期输出" /></label>
      </div>

      <div class="form-actions modal-actions">
        <button class="secondary-button" type="button" @click="closeCreateModal">取消</button>
        <button class="primary-button" type="button" :disabled="loading" @click="handleCreateWorkflow"><Plus :size="16" /> 创建空工作流</button>
      </div>
    </section>
  </div>
</template>
