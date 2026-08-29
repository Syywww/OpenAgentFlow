<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Plus, RefreshCw, Save, ShieldCheck, Trash2, X } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import StatCard from '../components/StatCard.vue';
import StatusBadge from '../components/StatusBadge.vue';
import {
  createModelRoutePolicy,
  deleteModelRoutePolicy,
  fetchModelGatewayCalls,
  fetchModelGatewayOverview,
  fetchModelHealth,
  fetchModelProviders,
  fetchModelRoutePolicies,
  updateModelRoutePolicy,
  type ModelConfigSummary,
  type ModelGatewayCallSummary,
  type ModelGatewayOverview,
  type ModelHealthSummary,
  type ModelProviderSummary,
  type ModelRouteCandidateRequest,
  type ModelRoutePolicyRequest,
  type ModelRoutePolicySummary,
} from '../api/models';
import type { StatusTone } from '../types';
import { usePagination } from '../composables/usePagination';
import { fetchWorkspaces, type WorkspaceSummary } from '../api/workspaces';

const loading = ref(false);
const errorMessage = ref('');
const overview = ref<ModelGatewayOverview | null>(null);
const policies = ref<ModelRoutePolicySummary[]>([]);
const providers = ref<ModelProviderSummary[]>([]);
const healthRows = ref<ModelHealthSummary[]>([]);
const calls = ref<ModelGatewayCallSummary[]>([]);
const workspaces = ref<WorkspaceSummary[]>([]);
const editingPolicyId = ref('');
const activePanel = ref<'policies' | 'health' | 'calls'>('policies');
const policyModalOpen = ref(false);
const { currentPage: policyPage, pagedItems: pagedPolicies } = usePagination(policies);
const { currentPage: healthPage, pagedItems: pagedHealthRows } = usePagination(healthRows);
const { currentPage: callPage, pagedItems: pagedCalls } = usePagination(calls);

const allModels = computed<ModelConfigSummary[]>(() => providers.value.flatMap((provider) => provider.models));

const policyForm = reactive<ModelRoutePolicyRequest>({
  policyCode: 'default-agent-chat',
  policyName: '默认 Agent 对话模型路由',
  sceneType: 'AGENT_CHAT',
  matchRule: '{\n  "scope": "GLOBAL"\n}',
  matchScope: 'GLOBAL',
  workspaceIds: [],
  fallbackEnabled: true,
  breakerFailureThreshold: 5,
  breakerTimeoutSeconds: 60,
  status: 'enabled',
  candidates: [],
});

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [overviewResult, policyResult, providerResult, healthResult, callResult, workspaceResult] = await Promise.all([
      fetchModelGatewayOverview(),
      fetchModelRoutePolicies(),
      fetchModelProviders(),
      fetchModelHealth(),
      fetchModelGatewayCalls(30),
      fetchWorkspaces(),
    ]);
    overview.value = overviewResult;
    policies.value = policyResult;
    providers.value = providerResult;
    healthRows.value = healthResult;
    calls.value = callResult;
    workspaces.value = workspaceResult;
    if (policyForm.candidates.length === 0 && providerResult.length > 0) {
      resetForm();
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '模型网关数据加载失败';
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  editingPolicyId.value = '';
  policyForm.policyCode = 'default-agent-chat';
  policyForm.policyName = '默认 Agent 对话模型路由';
  policyForm.sceneType = 'AGENT_CHAT';
  policyForm.matchRule = '{\n  "scope": "GLOBAL"\n}';
  policyForm.matchScope = 'GLOBAL';
  policyForm.workspaceIds = [];
  policyForm.fallbackEnabled = true;
  policyForm.breakerFailureThreshold = 5;
  policyForm.breakerTimeoutSeconds = 60;
  policyForm.status = 'enabled';
  policyForm.candidates = allModels.value
    .filter((model) => model.modelType === 'chat' && model.status === 'enabled')
    .slice(0, 3)
    .map((model, index) => ({
      modelId: model.id,
      priority: index + 1,
      weight: 1,
      enabled: true,
    }));
}

function editPolicy(policy: ModelRoutePolicySummary) {
  editingPolicyId.value = policy.id;
  policyForm.policyCode = policy.policyCode;
  policyForm.policyName = policy.policyName;
  policyForm.sceneType = policy.sceneType;
  policyForm.matchRule = policy.matchRule || '{}';
  policyForm.matchScope = policy.matchScope || 'GLOBAL';
  policyForm.workspaceIds = policy.workspaceIds || [];
  policyForm.fallbackEnabled = policy.fallbackEnabled;
  policyForm.breakerFailureThreshold = policy.breakerFailureThreshold ?? 5;
  policyForm.breakerTimeoutSeconds = policy.breakerTimeoutSeconds ?? 60;
  policyForm.status = policy.status;
  policyForm.candidates = policy.candidates.map((candidate) => ({
    modelId: candidate.modelId,
    priority: candidate.priority,
    weight: candidate.weight || 1,
    maxLatencyMs: candidate.maxLatencyMs,
    maxCostPer1k: candidate.maxCostPer1k,
    enabled: candidate.enabled,
  }));
  policyModalOpen.value = true;
}

function openCreatePolicyModal() {
  resetForm();
  policyModalOpen.value = true;
}

function closePolicyModal() {
  policyModalOpen.value = false;
  resetForm();
}

function addCandidate() {
  const selected = allModels.value.find((model) => model.modelType === 'chat' && !policyForm.candidates.some((item) => item.modelId === model.id));
  if (!selected) return;
  policyForm.candidates.push({
    modelId: selected.id,
    priority: policyForm.candidates.length + 1,
    weight: 1,
    enabled: true,
  });
}

function removeCandidate(index: number) {
  policyForm.candidates.splice(index, 1);
  policyForm.candidates.forEach((candidate, idx) => {
    candidate.priority = idx + 1;
  });
}

async function savePolicy() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const payload = normalizePayload();
    if (editingPolicyId.value) {
      await updateModelRoutePolicy(editingPolicyId.value, payload);
    } else {
      await createModelRoutePolicy(payload);
    }
    closePolicyModal();
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '模型路由策略保存失败';
  } finally {
    loading.value = false;
  }
}

async function removePolicy(policy: ModelRoutePolicySummary) {
  if (!window.confirm(`确认删除模型路由策略「${policy.policyName}」吗？`)) return;
  loading.value = true;
  try {
    await deleteModelRoutePolicy(policy.id);
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '模型路由策略删除失败';
  } finally {
    loading.value = false;
  }
}

function normalizePayload(): ModelRoutePolicyRequest {
  const scope = policyForm.matchScope === 'WORKSPACE' ? 'WORKSPACE' : 'GLOBAL';
  const workspaceIds = (policyForm.workspaceIds || []).filter((id) => Boolean(id && id.trim()));
  return {
    ...policyForm,
    matchRule: JSON.stringify(
      scope === 'WORKSPACE' ? { scope: 'WORKSPACE', workspaceIds } : { scope: 'GLOBAL' },
    ),
    sceneType: policyForm.sceneType.trim().toUpperCase(),
    candidates: policyForm.candidates
      .filter((candidate) => candidate.modelId)
      .map((candidate, index) => ({
        ...candidate,
        priority: Number(candidate.priority || index + 1),
        weight: Number(candidate.weight || 1),
        enabled: candidate.enabled !== false,
      })),
  };
}

function candidateModelName(candidate: ModelRouteCandidateRequest) {
  const model = allModels.value.find((item) => item.id === candidate.modelId);
  return model ? `${model.providerName} / ${model.modelName}` : candidate.modelId || '选择模型';
}

function statusTone(value?: string, failureRate?: number): StatusTone {
  if (failureRate !== undefined && Number(failureRate) >= 80) return 'danger';
  if (value === 'healthy' || value === 'enabled') return 'success';
  if (value === 'unhealthy' || value === 'disabled') return 'danger';
  return 'warning';
}

function formatNumber(value?: number | null) {
  return Number(value || 0).toLocaleString('zh-CN');
}

function formatPercent(value?: number | null) {
  return `${Number(value || 0).toFixed(2)}%`;
}

function weightPercent(candidate: ModelRouteCandidateRequest) {
  const total = policyForm.candidates.reduce((sum, item) => sum + Number(item.weight || 0), 0);
  if (total <= 0) return '0%';
  return `${((Number(candidate.weight || 0) / total) * 100).toFixed(1)}%`;
}

function formatMoney(value?: number | null) {
  return `¥${Number(value || 0).toFixed(6)}`;
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <PageHeader title="模型网关" description="统一治理模型路由、健康状态、失败回退、调用审计和成本用量">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadData"><RefreshCw :size="16" /> 刷新</button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

  <section class="metric-grid">
    <StatCard label="启用策略" :value="formatNumber(overview?.enabledPolicyCount)" detail="参与模型网关路由" icon="Workflow" tone="info" />
    <StatCard label="启用模型" :value="formatNumber(overview?.enabledModelCount)" detail="可被路由或直连调用" icon="Bot" tone="success" />
    <StatCard label="24h 失败率" :value="formatPercent(overview?.failureRate24h)" :detail="`${formatNumber(overview?.failureCount24h)} 次失败`" icon="ShieldAlert" tone="warning" />
    <StatCard label="24h 回退" :value="formatNumber(overview?.fallbackCount24h)" :detail="`${formatNumber(overview?.avgLatencyMs24h)} ms 平均耗时`" icon="Server" tone="danger" />
  </section>

  <section class="governance-card-tabs">
    <button class="governance-tab-card" :class="{ active: activePanel === 'policies' }" type="button" @click="activePanel = 'policies'">
      <span>路由策略</span>
      <b>{{ policies.length }}</b>
      <small>场景策略、候选模型和失败回退</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'health' }" type="button" @click="activePanel = 'health'">
      <span>模型健康</span>
      <b>{{ healthRows.length }}</b>
      <small>近 24 小时调用、失败率和耗时</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'calls' }" type="button" @click="activePanel = 'calls'">
      <span>最近网关调用</span>
      <b>{{ calls.length }}</b>
      <small>路由决策、Token 和回退状态</small>
    </button>
  </section>

  <section class="section-block model-gateway-panel">
    <template v-if="activePanel === 'policies'">
      <div class="section-title">
        <h2>路由策略</h2>
        <div class="title-actions">
          <span>{{ policies.length }} 条</span>
          <button class="primary-button slim" type="button" @click="openCreatePolicyModal">
            <Plus :size="14" /> 新增策略
          </button>
        </div>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>策略</th><th>场景</th><th>候选</th><th>回退</th><th>状态</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="policy in pagedPolicies" :key="policy.id">
            <td><b>{{ policy.policyName }}</b><br /><span class="muted mono">{{ policy.policyCode }}</span></td>
            <td>{{ policy.sceneType }}</td>
            <td>{{ policy.candidates.length }}</td>
            <td><StatusBadge :label="policy.fallbackEnabled ? '启用' : '关闭'" :tone="policy.fallbackEnabled ? 'success' : 'neutral'" /></td>
            <td><StatusBadge :label="policy.status" :tone="statusTone(policy.status)" /></td>
            <td>
              <div class="table-actions">
                <button class="secondary-button slim" type="button" @click="editPolicy(policy)">编辑</button>
                <button class="secondary-button slim danger-text" type="button" @click="removePolicy(policy)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="policyPage" :total="policies.length" />
      <div v-if="!loading && policies.length === 0" class="empty-state">暂无模型路由策略</div>
    </template>

    <template v-else-if="activePanel === 'health'">
      <div class="section-title"><h2>模型健康</h2><span>近 24 小时</span></div>
      <table class="data-table">
        <thead><tr><th>模型</th><th>调用</th><th>失败率</th><th>耗时</th><th>成本</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="item in pagedHealthRows" :key="item.modelId">
            <td><b>{{ item.modelName }}</b><br /><span class="muted">{{ item.providerName }} / {{ item.modelCode }}</span></td>
            <td>{{ formatNumber(item.recentCallCount) }}</td>
            <td>{{ formatPercent(item.recentFailureRate) }}</td>
            <td>{{ formatNumber(item.recentAvgLatencyMs) }} ms</td>
            <td>{{ formatMoney(item.recentCost) }}</td>
            <td><StatusBadge :label="item.healthStatus || item.status" :tone="statusTone(item.healthStatus || item.status, item.recentFailureRate)" /></td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="healthPage" :total="healthRows.length" />
    </template>

    <template v-else>
      <div class="section-title"><h2>最近网关调用</h2><span>{{ calls.length }} 条</span></div>
      <table class="data-table">
        <thead><tr><th>模型</th><th>策略</th><th>Token</th><th>回退</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="item in pagedCalls" :key="item.id">
            <td><b>{{ item.modelName || '-' }}</b><br /><span class="muted">{{ item.providerName || '-' }}</span></td>
            <td>{{ item.policyName || item.gatewaySceneType || '直连' }}</td>
            <td>{{ formatNumber(item.totalTokens) }}</td>
            <td><StatusBadge :label="item.fallbackUsed ? '是' : '否'" :tone="item.fallbackUsed ? 'warning' : 'neutral'" /></td>
            <td><StatusBadge :label="item.success ? '成功' : '失败'" :tone="item.success ? 'success' : 'danger'" /></td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="callPage" :total="calls.length" />
    </template>
  </section>

  <div class="insight-strip">
    <ShieldCheck :size="18" />
    <p>显式选择模型或 Agent 已绑定模型时会优先直连；未指定模型时由模型网关按场景策略选择候选模型，并在失败时自动回退到下一个健康候选。</p>
  </div>

  <div v-if="policyModalOpen" class="overlay-backdrop" @click.self="closePolicyModal">
    <section class="modal-panel model-route-modal">
      <header class="overlay-header">
        <div>
          <h2>{{ editingPolicyId ? '编辑策略' : '新增策略' }}</h2>
          <p class="muted">未指定模型时，模型网关会按场景策略选择候选模型并在失败时回退。</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closePolicyModal"><X :size="18" /></button>
      </header>
      <div class="form-layout">
        <div class="form-grid">
          <label>策略名称<input v-model="policyForm.policyName" /></label>
          <label>策略编码<input v-model="policyForm.policyCode" class="mono" /></label>
          <label>场景类型
            <select v-model="policyForm.sceneType">
              <option value="AGENT_CHAT">AGENT_CHAT</option>
              <option value="WORKFLOW_LLM">WORKFLOW_LLM</option>
            </select>
          </label>
          <label>状态
            <select v-model="policyForm.status">
              <option value="enabled">enabled</option>
              <option value="disabled">disabled</option>
            </select>
          </label>
          <label class="checkbox-line"><input v-model="policyForm.fallbackEnabled" type="checkbox" /> 启用失败回退</label>
          <label>熔断阈值(连续失败次数)
            <input v-model.number="policyForm.breakerFailureThreshold" type="number" min="1" placeholder="5" />
          </label>
          <label>熔断时长(秒)
            <input v-model.number="policyForm.breakerTimeoutSeconds" type="number" min="1" placeholder="60" />
          </label>
          <label>匹配范围
            <select v-model="policyForm.matchScope">
              <option value="GLOBAL">GLOBAL（全部空间）</option>
              <option value="WORKSPACE">WORKSPACE（指定空间）</option>
            </select>
          </label>
          <label v-if="policyForm.matchScope === 'WORKSPACE'" class="wide">指定空间
            <div class="candidate-list" style="max-height: 180px; overflow-y: auto;">
              <label v-for="workspace in workspaces" :key="workspace.id" class="checkbox-line">
                <input v-model="policyForm.workspaceIds" type="checkbox" :value="workspace.id" /> {{ workspace.workspaceName }}
              </label>
              <p v-if="workspaces.length === 0" class="muted">暂无可用空间</p>
            </div>
          </label>
        </div>

        <div class="section-title compact-title">
          <h2>候选模型</h2>
          <button class="secondary-button slim" type="button" @click="addCandidate"><Plus :size="14" /> 添加</button>
        </div>
        <div class="candidate-list">
          <div v-for="(candidate, index) in policyForm.candidates" :key="`${candidate.modelId}-${index}`" class="candidate-row">
            <label>模型
              <select v-model="candidate.modelId">
                <option v-for="model in allModels.filter((item) => item.modelType === 'chat')" :key="model.id" :value="model.id">
                  {{ model.providerName }} / {{ model.modelName }}
                </option>
              </select>
            </label>
            <label>优先级<input v-model.number="candidate.priority" type="number" min="1" /></label>
            <label>权重<input v-model.number="candidate.weight" type="number" min="0" step="0.1" /><span class="muted">{{ weightPercent(candidate) }}</span></label>
            <label>最大耗时<input v-model.number="candidate.maxLatencyMs" type="number" min="0" placeholder="可空" /></label>
            <label class="checkbox-line"><input v-model="candidate.enabled" type="checkbox" /> 启用</label>
            <button class="icon-button danger" type="button" :title="candidateModelName(candidate)" @click="removeCandidate(index)"><Trash2 :size="16" /></button>
          </div>
        </div>
        <div class="action-row end">
          <button class="secondary-button" type="button" @click="closePolicyModal">取消</button>
          <button class="primary-button" type="button" :disabled="loading" @click="savePolicy"><Save :size="16" /> 保存策略</button>
        </div>
      </div>
    </section>
  </div>
</template>
