<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { CheckCircle2, Plus, RefreshCw, Save, SearchCheck, ShieldCheck, Trash2, X } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import PaginationBar from '../../components/PaginationBar.vue';
import StatCard from '../../components/StatCard.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import {
  createKnowledgeGovernancePolicy,
  deleteKnowledgeGovernancePolicy,
  fetchKnowledgeGovernanceIssues,
  fetchKnowledgeGovernanceOverview,
  fetchKnowledgeGovernancePolicies,
  fetchKnowledgeQualityRows,
  handleKnowledgeGovernanceIssue,
  scanKnowledgeGovernanceIssues,
  updateKnowledgeGovernancePolicy,
  type KnowledgeGovernanceIssueSummary,
  type KnowledgeGovernanceOverview,
  type KnowledgeGovernancePolicyRequest,
  type KnowledgeGovernancePolicySummary,
  type KnowledgeQualityRow,
} from '../../api/knowledge';
import type { StatusTone } from '../../types';
import { usePagination } from '../../composables/usePagination';

const loading = ref(false);
const scanning = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const overview = ref<KnowledgeGovernanceOverview | null>(null);
const qualityRows = ref<KnowledgeQualityRow[]>([]);
const issues = ref<KnowledgeGovernanceIssueSummary[]>([]);
const policies = ref<KnowledgeGovernancePolicySummary[]>([]);
const editingPolicyId = ref('');
const activePanel = ref<'issues' | 'policies' | 'quality'>('issues');
const policyModalOpen = ref(false);
const { currentPage: issuePage, pagedItems: pagedIssues, resetPage: resetIssuePage } = usePagination(issues);
const { currentPage: policyPage, pagedItems: pagedPolicies } = usePagination(policies);
const { currentPage: qualityPage, pagedItems: pagedQualityRows } = usePagination(qualityRows);

const filters = reactive({
  status: 'open',
  severity: '',
  issueType: '',
});

const policyForm = reactive<KnowledgeGovernancePolicyRequest>({
  policyCode: 'default-knowledge-governance',
  policyName: '默认知识库治理策略',
  staleDays: 90,
  minChunkTokens: 20,
  maxChunkTokens: 1200,
  maxFailedDocuments: 0,
  requireAgentBinding: true,
  requireMilvusSync: true,
  autoIssueEnabled: true,
  status: 'enabled',
});

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [overviewResult, qualityResult, issueResult, policyResult] = await Promise.all([
      fetchKnowledgeGovernanceOverview(),
      fetchKnowledgeQualityRows(),
      fetchKnowledgeGovernanceIssues({ ...filters, limit: 100 }),
      fetchKnowledgeGovernancePolicies(),
    ]);
    overview.value = overviewResult;
    qualityRows.value = qualityResult;
    issues.value = issueResult;
    policies.value = policyResult;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '知识库治理数据加载失败';
  } finally {
    loading.value = false;
  }
}

async function loadIssues() {
  loading.value = true;
  errorMessage.value = '';
  try {
    resetIssuePage();
    issues.value = await fetchKnowledgeGovernanceIssues({ ...filters, limit: 100 });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '治理问题加载失败';
  } finally {
    loading.value = false;
  }
}

async function scanIssues() {
  scanning.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    const result = await scanKnowledgeGovernanceIssues();
    successMessage.value = `扫描完成，新生成 ${result.createdIssueCount} 个问题，当前打开 ${result.openIssueCount} 个问题`;
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '知识库治理扫描失败';
  } finally {
    scanning.value = false;
  }
}

function resetPolicyForm() {
  editingPolicyId.value = '';
  policyForm.policyCode = `knowledge-governance-${Date.now()}`;
  policyForm.policyName = '知识库治理策略';
  policyForm.kbId = '';
  policyForm.staleDays = 90;
  policyForm.minChunkTokens = 20;
  policyForm.maxChunkTokens = 1200;
  policyForm.maxFailedDocuments = 0;
  policyForm.requireAgentBinding = true;
  policyForm.requireMilvusSync = true;
  policyForm.autoIssueEnabled = true;
  policyForm.status = 'enabled';
}

function openCreatePolicyModal() {
  resetPolicyForm();
  policyModalOpen.value = true;
}

function closePolicyModal() {
  policyModalOpen.value = false;
  resetPolicyForm();
}

function editPolicy(policy: KnowledgeGovernancePolicySummary) {
  editingPolicyId.value = policy.id;
  policyForm.policyCode = policy.policyCode;
  policyForm.policyName = policy.policyName;
  policyForm.kbId = policy.kbId || '';
  policyForm.staleDays = policy.staleDays;
  policyForm.minChunkTokens = policy.minChunkTokens;
  policyForm.maxChunkTokens = policy.maxChunkTokens;
  policyForm.maxFailedDocuments = policy.maxFailedDocuments;
  policyForm.requireAgentBinding = policy.requireAgentBinding;
  policyForm.requireMilvusSync = policy.requireMilvusSync;
  policyForm.autoIssueEnabled = policy.autoIssueEnabled;
  policyForm.status = policy.status;
  policyModalOpen.value = true;
}

async function savePolicy() {
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    const payload = normalizePolicy();
    if (editingPolicyId.value) {
      await updateKnowledgeGovernancePolicy(editingPolicyId.value, payload);
    } else {
      await createKnowledgeGovernancePolicy(payload);
    }
    successMessage.value = '治理策略已保存';
    closePolicyModal();
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '治理策略保存失败';
  } finally {
    loading.value = false;
  }
}

async function removePolicy(policy: KnowledgeGovernancePolicySummary) {
  if (!window.confirm(`确认删除治理策略「${policy.policyName}」吗？`)) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    await deleteKnowledgeGovernancePolicy(policy.id);
    successMessage.value = '治理策略已删除';
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '治理策略删除失败';
  } finally {
    loading.value = false;
  }
}

async function resolveIssue(issue: KnowledgeGovernanceIssueSummary, status: 'resolved' | 'ignored') {
  loading.value = true;
  errorMessage.value = '';
  try {
    await handleKnowledgeGovernanceIssue(issue.id, status, status === 'resolved' ? '已在治理中心标记解决' : '已在治理中心忽略');
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '治理问题处理失败';
  } finally {
    loading.value = false;
  }
}

function normalizePolicy(): KnowledgeGovernancePolicyRequest {
  return {
    ...policyForm,
    kbId: policyForm.kbId || undefined,
    staleDays: Number(policyForm.staleDays || 90),
    minChunkTokens: Number(policyForm.minChunkTokens || 20),
    maxChunkTokens: Number(policyForm.maxChunkTokens || 1200),
    maxFailedDocuments: Number(policyForm.maxFailedDocuments || 0),
    requireAgentBinding: policyForm.requireAgentBinding !== false,
    requireMilvusSync: policyForm.requireMilvusSync !== false,
    autoIssueEnabled: policyForm.autoIssueEnabled !== false,
    status: policyForm.status || 'enabled',
  };
}

function issueTypeLabel(value?: string) {
  const labels: Record<string, string> = {
    FAILED_DOCUMENT: '解析失败',
    PROCESSING_STUCK: '处理卡住',
    STALE_DOCUMENT: '长期未更新',
    MISSING_EMBEDDING: '缺少向量',
    MILVUS_FALLBACK: 'Milvus同步异常',
    LOW_CHUNK_QUALITY: '切片质量',
    UNBOUND_KNOWLEDGE_BASE: '未绑定智能体',
    EMPTY_KNOWLEDGE_BASE: '空知识库',
  };
  return labels[value || ''] || value || '-';
}

function severityTone(value?: string): StatusTone {
  if (value === 'critical' || value === 'high') return 'danger';
  if (value === 'medium') return 'warning';
  if (value === 'low') return 'info';
  return 'neutral';
}

function statusTone(value?: string): StatusTone {
  if (value === 'enabled' || value === 'resolved') return 'success';
  if (value === 'open') return 'warning';
  if (value === 'ignored' || value === 'disabled') return 'neutral';
  return 'info';
}

function riskTone(value?: string): StatusTone {
  if (value === 'high') return 'danger';
  if (value === 'medium') return 'warning';
  return 'success';
}

function riskLabel(value?: string) {
  if (value === 'high') return '高风险';
  if (value === 'medium') return '中风险';
  return '低风险';
}

function formatNumber(value?: number | null) {
  return Number(value || 0).toLocaleString('zh-CN');
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-';
}

function evidenceText(issue: KnowledgeGovernanceIssueSummary) {
  return issue.evidence ? JSON.stringify(issue.evidence) : '';
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <PageHeader title="知识库治理" description="集中治理知识库质量、解析状态、切片质量、Milvus向量同步、Agent绑定和可交付风险">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading || scanning" @click="loadData">
        <RefreshCw :size="16" /> 刷新
      </button>
      <button class="primary-button" type="button" :disabled="scanning" @click="scanIssues">
        <SearchCheck :size="16" /> 扫描问题
      </button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
  <p v-if="successMessage" class="form-success">{{ successMessage }}</p>

  <section class="metric-grid">
    <StatCard label="知识库" :value="formatNumber(overview?.knowledgeBaseCount)" detail="当前可治理资产" icon="Library" tone="info" />
    <StatCard label="打开问题" :value="formatNumber(overview?.openIssueCount)" :detail="`${formatNumber(overview?.highRiskIssueCount)} 个高风险`" icon="ShieldAlert" tone="warning" />
    <StatCard label="解析失败" :value="formatNumber(overview?.failedDocumentCount)" :detail="`${formatNumber(overview?.processingDocumentCount)} 个处理中`" icon="Activity" tone="danger" />
    <StatCard label="向量异常" :value="formatNumber(overview?.milvusFallbackCount)" :detail="`${formatNumber(overview?.embeddingCount)} 条向量`" icon="Server" tone="warning" />
  </section>

  <section class="governance-card-tabs">
    <button class="governance-tab-card" :class="{ active: activePanel === 'issues' }" type="button" @click="activePanel = 'issues'">
      <span>治理问题列表</span>
      <b>{{ issues.length }}</b>
      <small>待处理、已解决、已忽略问题</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'policies' }" type="button" @click="activePanel = 'policies'">
      <span>治理策略列表</span>
      <b>{{ policies.length }}</b>
      <small>规则阈值、开关和适用范围</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'quality' }" type="button" @click="activePanel = 'quality'">
      <span>知识库质量列表</span>
      <b>{{ qualityRows.length }}</b>
      <small>质量分、风险级别和同步状态</small>
    </button>
  </section>

  <section class="section-block knowledge-governance-panel">
    <template v-if="activePanel === 'issues'">
      <div class="section-title">
        <h2>治理问题列表</h2>
        <span>{{ issues.length }} 条</span>
      </div>
      <div class="filter-row">
        <select v-model="filters.status" @change="loadIssues">
          <option value="open">待处理</option>
          <option value="resolved">已解决</option>
          <option value="ignored">已忽略</option>
          <option value="">全部状态</option>
        </select>
        <select v-model="filters.severity" @change="loadIssues">
          <option value="">全部级别</option>
          <option value="critical">严重</option>
          <option value="high">高风险</option>
          <option value="medium">中风险</option>
          <option value="low">低风险</option>
        </select>
        <select v-model="filters.issueType" @change="loadIssues">
          <option value="">全部类型</option>
          <option value="FAILED_DOCUMENT">解析失败</option>
          <option value="PROCESSING_STUCK">处理卡住</option>
          <option value="STALE_DOCUMENT">长期未更新</option>
          <option value="MISSING_EMBEDDING">缺少向量</option>
          <option value="MILVUS_FALLBACK">Milvus同步异常</option>
          <option value="LOW_CHUNK_QUALITY">切片质量</option>
          <option value="UNBOUND_KNOWLEDGE_BASE">未绑定智能体</option>
          <option value="EMPTY_KNOWLEDGE_BASE">空知识库</option>
        </select>
      </div>
      <div class="table-scroll">
        <table class="data-table knowledge-issues-table">
          <thead>
            <tr><th>问题</th><th>知识库</th><th>对象</th><th>级别</th><th>状态</th><th>时间</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="issue in pagedIssues" :key="issue.id">
              <td>
                <b>{{ issue.issueTitle }}</b>
                <small>{{ issueTypeLabel(issue.issueType) }} · {{ issue.issueDetail }}</small>
                <small v-if="evidenceText(issue)" class="mono evidence-snippet">{{ evidenceText(issue) }}</small>
              </td>
              <td>{{ issue.kbName || issue.kbId }}</td>
              <td>{{ issue.documentName || issue.chunkId || '-' }}</td>
              <td><StatusBadge :label="issue.severity" :tone="severityTone(issue.severity)" /></td>
              <td><StatusBadge :label="issue.status" :tone="statusTone(issue.status)" /></td>
              <td>{{ formatDate(issue.createdAt) }}</td>
              <td>
                <div class="table-actions">
                  <button class="secondary-button slim" type="button" :disabled="issue.status !== 'open'" @click="resolveIssue(issue, 'resolved')">
                    <CheckCircle2 :size="14" /> 解决
                  </button>
                  <button class="secondary-button slim" type="button" :disabled="issue.status !== 'open'" @click="resolveIssue(issue, 'ignored')">忽略</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar v-model:page="issuePage" :total="issues.length" />
      <div v-if="!loading && issues.length === 0" class="empty-state">暂无治理问题</div>
    </template>

    <template v-else-if="activePanel === 'policies'">
      <div class="section-title">
        <h2>治理策略列表</h2>
        <div class="title-actions">
          <span>{{ policies.length }} 条</span>
          <button class="primary-button slim" type="button" @click="openCreatePolicyModal">
            <Plus :size="14" /> 新增治理策略
          </button>
        </div>
      </div>
      <div class="policy-list">
        <div v-for="policy in pagedPolicies" :key="policy.id" class="policy-row">
          <div>
            <b>{{ policy.policyName }}</b>
            <small class="mono">{{ policy.policyCode }}</small>
          </div>
          <StatusBadge :label="policy.status" :tone="statusTone(policy.status)" />
          <button class="secondary-button slim" type="button" @click="editPolicy(policy)"><ShieldCheck :size="14" /> 编辑</button>
          <button class="secondary-button slim danger-text" type="button" @click="removePolicy(policy)"><Trash2 :size="14" /> 删除</button>
        </div>
      </div>
      <PaginationBar v-model:page="policyPage" :total="policies.length" />
      <div v-if="!loading && policies.length === 0" class="empty-state">暂无治理策略</div>
    </template>

    <template v-else>
      <div class="section-title">
        <h2>知识库质量列表</h2>
        <span>{{ qualityRows.length }} 个知识库</span>
      </div>
      <div class="table-scroll">
        <table class="data-table knowledge-quality-table">
          <thead>
            <tr><th>知识库</th><th>质量分</th><th>文档</th><th>分片</th><th>向量</th><th>失败</th><th>向量异常</th><th>绑定Agent</th><th>最后上传</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in pagedQualityRows" :key="row.kbId">
              <td><b>{{ row.kbName }}</b><br /><span class="muted mono">{{ row.kbId }}</span></td>
              <td><strong>{{ row.qualityScore }}</strong> <StatusBadge :label="riskLabel(row.riskLevel)" :tone="riskTone(row.riskLevel)" /></td>
              <td>{{ formatNumber(row.documentCount) }}</td>
              <td>{{ formatNumber(row.chunkCount) }}</td>
              <td>{{ formatNumber(row.embeddingCount) }}</td>
              <td>{{ formatNumber(row.failedDocumentCount) }}</td>
              <td>{{ formatNumber(row.fallbackEmbeddingCount) }}</td>
              <td>{{ formatNumber(row.agentBindingCount) }}</td>
              <td>{{ formatDate(row.lastUploadedAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar v-model:page="qualityPage" :total="qualityRows.length" />
      <div v-if="!loading && qualityRows.length === 0" class="empty-state">暂无知识库质量数据</div>
    </template>
  </section>

  <div v-if="policyModalOpen" class="overlay-backdrop" @click.self="closePolicyModal">
    <section class="modal-panel knowledge-policy-modal">
      <header class="overlay-header">
        <div>
          <h2>{{ editingPolicyId ? '编辑治理策略' : '新增治理策略' }}</h2>
          <p class="muted">配置知识库治理阈值、自动问题生成和交付检查要求。</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closePolicyModal"><X :size="18" /></button>
      </header>
      <div class="form-layout compact-form">
        <div class="form-grid">
          <label>策略名称<input v-model="policyForm.policyName" /></label>
          <label>策略编码<input v-model="policyForm.policyCode" class="mono" /></label>
          <label>指定知识库ID<input v-model="policyForm.kbId" class="mono" placeholder="为空表示全局策略" /></label>
          <label>状态
            <select v-model="policyForm.status">
              <option value="enabled">启用</option>
              <option value="disabled">停用</option>
            </select>
          </label>
          <label>陈旧天数<input v-model.number="policyForm.staleDays" type="number" min="1" /></label>
          <label>最小Token<input v-model.number="policyForm.minChunkTokens" type="number" min="1" /></label>
          <label>最大Token<input v-model.number="policyForm.maxChunkTokens" type="number" min="1" /></label>
          <label>失败文档阈值<input v-model.number="policyForm.maxFailedDocuments" type="number" min="0" /></label>
        </div>
        <div class="toggle-row">
          <label><input v-model="policyForm.requireAgentBinding" type="checkbox" /> 要求绑定智能体</label>
          <label><input v-model="policyForm.requireMilvusSync" type="checkbox" /> 要求Milvus同步</label>
          <label><input v-model="policyForm.autoIssueEnabled" type="checkbox" /> 自动生成问题</label>
        </div>
        <div class="form-actions">
          <button class="secondary-button" type="button" @click="closePolicyModal">取消</button>
          <button class="primary-button" type="button" :disabled="loading" @click="savePolicy">
            <Save :size="16" /> 保存策略
          </button>
        </div>
      </div>
    </section>
  </div>
</template>
