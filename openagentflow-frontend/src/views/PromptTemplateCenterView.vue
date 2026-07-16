<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Copy, GitBranch, Plus, RefreshCw, RotateCcw, Save, Send, Trash2, X } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import PromptOpsPanel from '../components/PromptOpsPanel.vue';
import StatCard from '../components/StatCard.vue';
import StatusBadge from '../components/StatusBadge.vue';
import {
  copyPromptTemplate,
  createPromptTemplate,
  deletePromptTemplate,
  fetchPromptOverview,
  fetchPromptTemplate,
  fetchPromptTemplates,
  publishPromptTemplate,
  rollbackPromptTemplate,
  updatePromptTemplate,
  type PromptOverview,
  type PromptTemplateDetail,
  type PromptTemplateRequest,
  type PromptTemplateSummary,
  type PromptTemplateVersionSummary,
} from '../api/prompts';
import { usePagination } from '../composables/usePagination';

const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const overview = ref<PromptOverview | null>(null);
const templates = ref<PromptTemplateSummary[]>([]);
const selectedTemplate = ref<PromptTemplateDetail | null>(null);
const total = ref(0);
const activePanel = ref<'templates' | 'versions' | 'preview' | 'governance' | 'experiments' | 'metrics'>('templates');
const templateModalOpen = ref(false);
const publishModalOpen = ref(false);
const editingTemplateId = ref('');

const filters = reactive({
  promptType: 'all',
  status: 'all',
  keyword: '',
  pageNo: 1,
  pageSize: 10,
});

const templateForm = reactive<PromptTemplateRequest>({
  templateCode: '',
  templateName: '',
  promptType: 'system',
  content: '',
  variables: '',
  variableSchema: '[]',
  description: '',
  status: 'draft',
  riskLevel: 'low',
});

const publishForm = reactive({
  versionNo: '',
  changeNote: '',
});

const variablePreview = computed(() => selectedTemplate.value?.variableNames || []);
const versions = computed(() => selectedTemplate.value?.versions || []);
const promptOpsMode = computed<'preview' | 'governance' | 'experiments' | 'metrics'>(() => {
  return ['preview', 'governance', 'experiments', 'metrics'].includes(activePanel.value)
    ? activePanel.value as 'preview' | 'governance' | 'experiments' | 'metrics'
    : 'preview';
});
const { currentPage: versionPage, pagedItems: pagedVersions } = usePagination(versions);

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [overviewResult, pageResult] = await Promise.all([
      fetchPromptOverview(),
      fetchPromptTemplates(filters),
    ]);
    overview.value = overviewResult;
    templates.value = pageResult.records;
    total.value = pageResult.total;
    if (templates.value.length > 0) {
      await selectTemplate(templates.value[0]);
    } else {
      selectedTemplate.value = null;
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Prompt 模板数据加载失败';
  } finally {
    loading.value = false;
  }
}

async function searchTemplates() {
  filters.pageNo = 1;
  await loadData();
}

async function changePage(page: number) {
  filters.pageNo = page;
  await loadData();
}

async function selectTemplate(template: PromptTemplateSummary) {
  selectedTemplate.value = await fetchPromptTemplate(template.id);
}

function resetTemplateForm() {
  editingTemplateId.value = '';
  templateForm.templateCode = '';
  templateForm.templateName = '';
  templateForm.promptType = 'system';
  templateForm.content = '你是专业的 AI 助手，请结合 {{user_input}} 输出可执行、可验证的回答。';
  templateForm.variables = '';
  templateForm.variableSchema = '[]';
  templateForm.description = '';
  templateForm.status = 'draft';
  templateForm.riskLevel = 'low';
}

function openCreateTemplateModal() {
  resetTemplateForm();
  templateModalOpen.value = true;
}

function openEditTemplateModal(template: PromptTemplateSummary) {
  editingTemplateId.value = template.id;
  templateForm.templateCode = template.templateCode;
  templateForm.templateName = template.templateName;
  templateForm.promptType = template.promptType;
  templateForm.content = template.content;
  templateForm.variables = template.variables;
  templateForm.variableSchema = template.variableSchema || template.variables || '[]';
  templateForm.description = template.description || '';
  templateForm.status = template.status;
  templateForm.riskLevel = template.riskLevel || 'low';
  templateModalOpen.value = true;
}

function closeTemplateModal() {
  templateModalOpen.value = false;
  resetTemplateForm();
}

function openPublishModal() {
  if (!selectedTemplate.value) return;
  publishForm.versionNo = '';
  publishForm.changeNote = selectedTemplate.value.latestVersionNo ? '更新 Prompt 模板版本' : '首次发布 Prompt 模板';
  publishModalOpen.value = true;
}

function closePublishModal() {
  publishModalOpen.value = false;
  publishForm.versionNo = '';
  publishForm.changeNote = '';
}

async function saveTemplate() {
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    const payload = { ...templateForm };
    const detail = editingTemplateId.value
      ? await updatePromptTemplate(editingTemplateId.value, payload)
      : await createPromptTemplate(payload);
    selectedTemplate.value = detail;
    successMessage.value = editingTemplateId.value ? 'Prompt 模板已更新' : 'Prompt 模板已创建';
    closeTemplateModal();
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Prompt 模板保存失败';
  } finally {
    loading.value = false;
  }
}

async function publishSelectedTemplate() {
  if (!selectedTemplate.value) return;
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    selectedTemplate.value = await publishPromptTemplate(selectedTemplate.value.id, { ...publishForm });
    successMessage.value = 'Prompt 模板版本已发布';
    closePublishModal();
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Prompt 模板发布失败';
  } finally {
    loading.value = false;
  }
}

async function copySelectedTemplate() {
  if (!selectedTemplate.value) return;
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    selectedTemplate.value = await copyPromptTemplate(selectedTemplate.value.id, {
      templateName: `${selectedTemplate.value.templateName} 副本`,
    });
    successMessage.value = 'Prompt 模板已复制';
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Prompt 模板复制失败';
  } finally {
    loading.value = false;
  }
}

async function removeTemplate(template: PromptTemplateSummary) {
  if (!window.confirm(`确认删除 Prompt 模板「${template.templateName}」吗？`)) return;
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    await deletePromptTemplate(template.id);
    successMessage.value = 'Prompt 模板已删除';
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Prompt 模板删除失败';
  } finally {
    loading.value = false;
  }
}

async function rollbackVersion(version: PromptTemplateVersionSummary) {
  if (!selectedTemplate.value) return;
  if (!window.confirm(`确认回滚到版本 ${version.versionNo} 吗？`)) return;
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    selectedTemplate.value = await rollbackPromptTemplate(selectedTemplate.value.id, version.id);
    successMessage.value = `已回滚到版本 ${version.versionNo}`;
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Prompt 模板回滚失败';
  } finally {
    loading.value = false;
  }
}

function statusTone(status?: string) {
  if (status === 'published') return 'success';
  if (status === 'archived') return 'neutral';
  return 'warning';
}

function formatTime(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <PageHeader title="Prompt 模板中心" description="统一管理 System、User、RAG、Tool、Evaluation、Workflow Prompt，支持版本发布、复制和回滚">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadData"><RefreshCw :size="16" /> 刷新</button>
      <button class="primary-button" type="button" @click="openCreateTemplateModal"><Plus :size="16" /> 新增 Prompt</button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
  <p v-if="successMessage" class="form-success">{{ successMessage }}</p>

  <section class="metric-grid">
    <StatCard label="模板总数" :value="String(overview?.templateCount || 0)" detail="Prompt 资产" icon="GalleryVerticalEnd" tone="info" />
    <StatCard label="已发布" :value="String(overview?.publishedCount || 0)" detail="可被 Agent / 工作流复用" icon="ShieldCheck" tone="success" />
    <StatCard label="草稿" :value="String(overview?.draftCount || 0)" detail="待调试或发布" icon="Timer" tone="warning" />
    <StatCard label="版本数" :value="String(overview?.versionCount || 0)" detail="历史快照" icon="GitBranch" tone="neutral" />
  </section>

  <section class="section-block">
    <div class="filter-row">
      <select v-model="filters.promptType" @change="searchTemplates">
        <option value="all">全部类型</option>
        <option value="system">System Prompt</option>
        <option value="user">User Prompt</option>
        <option value="rag">RAG Prompt</option>
        <option value="tool">Tool Prompt</option>
        <option value="evaluation">Evaluation Prompt</option>
        <option value="workflow">Workflow Prompt</option>
      </select>
      <select v-model="filters.status" @change="searchTemplates">
        <option value="all">全部状态</option>
        <option value="draft">草稿</option>
        <option value="published">已发布</option>
        <option value="archived">已归档</option>
      </select>
      <input v-model="filters.keyword" placeholder="搜索模板名称、编码、描述" @keyup.enter="searchTemplates" />
      <button class="secondary-button" type="button" :disabled="loading" @click="searchTemplates">查询</button>
    </div>
  </section>

  <section class="governance-card-tabs">
    <button class="governance-tab-card" :class="{ active: activePanel === 'templates' }" type="button" @click="activePanel = 'templates'">
      <span>Prompt 模板</span>
      <b>{{ total }}</b>
      <small>模板内容、变量和状态</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'versions' }" type="button" @click="activePanel = 'versions'">
      <span>版本治理</span>
      <b>{{ versions.length }}</b>
      <small>{{ selectedTemplate?.templateName || '请选择模板' }}</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'preview' }" type="button" @click="activePanel = 'preview'">
      <span>编译预览</span>
      <b>{{ selectedTemplate?.variableNames.length || 0 }}</b>
      <small>变量、分层与最终 Prompt</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'governance' }" type="button" @click="activePanel = 'governance'">
      <span>发布治理</span>
      <b>{{ selectedTemplate?.bindingCount || 0 }}</b>
      <small>影响面、差异、环境与灰度</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'experiments' }" type="button" @click="activePanel = 'experiments'">
      <span>A/B 实验</span>
      <b>{{ overview?.runningExperimentCount || 0 }}</b>
      <small>稳定分流与自动选优</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'metrics' }" type="button" @click="activePanel = 'metrics'">
      <span>运行指标</span>
      <b>{{ selectedTemplate?.versions.length || 0 }}</b>
      <small>质量、耗时、Token 与成本</small>
    </button>
  </section>

  <section class="section-block prompt-center-panel">
    <template v-if="activePanel === 'templates'">
      <div class="section-title">
        <h2>Prompt 模板</h2>
        <div class="title-actions">
          <span>共 {{ total }} 条</span>
          <button class="primary-button slim" type="button" @click="openCreateTemplateModal"><Plus :size="14" /> 新增 Prompt</button>
        </div>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>模板</th><th>类型</th><th>变量</th><th>版本</th><th>状态</th><th>更新时间</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in templates" :key="item.id" :class="{ selected: selectedTemplate?.id === item.id }" @click="selectTemplate(item)">
            <td><b>{{ item.templateName }}</b><span class="muted block mono">{{ item.templateCode }}</span></td>
            <td>{{ item.promptTypeLabel }}</td>
            <td>{{ item.variableNames.join(', ') || '-' }}</td>
            <td>{{ item.latestVersionNo || '-' }} / {{ item.versionCount }}</td>
            <td><StatusBadge :label="item.statusLabel" :tone="statusTone(item.status)" /></td>
            <td>{{ formatTime(item.updatedAt) }}</td>
            <td>
              <div class="table-actions">
                <button class="secondary-button slim" type="button" @click.stop="openEditTemplateModal(item)">编辑</button>
                <button class="secondary-button slim danger-text" type="button" @click.stop="removeTemplate(item)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <PaginationBar :page="filters.pageNo" :total="total" @update:page="changePage" />
      <div v-if="templates.length === 0" class="empty-state">暂无 Prompt 模板</div>
    </template>

    <template v-else-if="activePanel === 'versions'">
      <div class="section-title">
        <h2>版本治理</h2>
        <div class="title-actions">
          <span>{{ selectedTemplate?.templateName || '请选择模板' }}</span>
          <button class="secondary-button slim" type="button" :disabled="!selectedTemplate" @click="copySelectedTemplate"><Copy :size="14" /> 复制</button>
          <button class="primary-button slim" type="button" :disabled="!selectedTemplate" @click="openPublishModal"><Send :size="14" /> 发布版本</button>
        </div>
      </div>

      <template v-if="selectedTemplate">
        <div class="trace-meta">
          <span>模板编码</span><b>{{ selectedTemplate.templateCode }}</b>
          <span>类型</span><b>{{ selectedTemplate.promptTypeLabel }}</b>
          <span>状态</span><b>{{ selectedTemplate.statusLabel }}</b>
          <span>最新版本</span><b>{{ selectedTemplate.latestVersionNo || '-' }}</b>
        </div>
        <div class="badge-row">
          <StatusBadge v-for="name in variablePreview" :key="name" :label="`{{${name}}}`" />
          <StatusBadge v-if="variablePreview.length === 0" label="无变量" />
        </div>
        <pre class="json-preview prompt-preview">{{ selectedTemplate.content }}</pre>

        <div class="section-title compact-title"><h2>历史版本</h2><span>{{ versions.length }} 条</span></div>
        <table class="data-table">
          <thead><tr><th>版本</th><th>变量</th><th>说明</th><th>创建时间</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="version in pagedVersions" :key="version.id">
              <td><b>{{ version.versionNo }}</b></td>
              <td>{{ version.variableNames.join(', ') || '-' }}</td>
              <td>{{ version.changeNote || '-' }}</td>
              <td>{{ formatTime(version.createdAt) }}</td>
              <td>
                <button class="secondary-button slim" type="button" @click="rollbackVersion(version)">
                  <RotateCcw :size="14" /> 回滚
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <PaginationBar v-model:page="versionPage" :total="versions.length" />
      </template>
      <div v-else class="empty-state">请选择一个 Prompt 模板</div>
    </template>
    <PromptOpsPanel
      v-else
      :template="selectedTemplate"
      :mode="promptOpsMode"
    />
  </section>

  <div v-if="templateModalOpen" class="overlay-backdrop" @click.self="closeTemplateModal">
    <section class="modal-panel prompt-template-modal">
      <header class="overlay-header">
        <div>
          <h2>{{ editingTemplateId ? '编辑 Prompt 模板' : '新增 Prompt 模板' }}</h2>
          <p class="muted">内容支持 <code v-pre>{{user_input}}</code> 这类变量，占位符会自动解析为变量定义。</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closeTemplateModal"><X :size="18" /></button>
      </header>
      <div class="form-grid">
        <label>模板名称<input v-model="templateForm.templateName" /></label>
        <label>模板编码<input v-model="templateForm.templateCode" class="mono" placeholder="不填则自动生成" /></label>
        <label>Prompt 类型
          <select v-model="templateForm.promptType">
            <option value="system">System Prompt</option>
            <option value="user">User Prompt</option>
            <option value="rag">RAG Prompt</option>
            <option value="tool">Tool Prompt</option>
            <option value="evaluation">Evaluation Prompt</option>
            <option value="workflow">Workflow Prompt</option>
          </select>
        </label>
        <label>状态
          <select v-model="templateForm.status">
            <option value="draft">草稿</option>
            <option value="published">已发布</option>
            <option value="archived">已归档</option>
          </select>
        </label>
        <label>风险等级
          <select v-model="templateForm.riskLevel">
            <option value="low">低风险</option>
            <option value="medium">中风险</option>
            <option value="high">高风险</option>
          </select>
        </label>
        <label class="wide">描述<textarea v-model="templateForm.description" rows="2" /></label>
        <label class="wide">Prompt 内容<textarea v-model="templateForm.content" class="code-editor" rows="10" /></label>
        <label class="wide">变量定义 JSON<textarea v-model="templateForm.variables" class="code-editor compact" placeholder="留空时后端自动从 {{变量名}} 解析" /></label>
        <label class="wide">强类型变量 Schema<textarea v-model="templateForm.variableSchema" class="code-editor compact" rows="6" placeholder='[{"name":"user_input","type":"string","required":true,"sensitive":false}]' /></label>
      </div>
      <div class="toolbar compact">
        <button class="secondary-button" type="button" @click="closeTemplateModal">取消</button>
        <button class="primary-button" type="button" :disabled="loading || !templateForm.templateName || !templateForm.content" @click="saveTemplate">
          <Save :size="16" /> 保存模板
        </button>
      </div>
    </section>
  </div>

  <div v-if="publishModalOpen" class="overlay-backdrop" @click.self="closePublishModal">
    <section class="modal-panel compact">
      <header class="overlay-header">
        <div>
          <h2>发布 Prompt 版本</h2>
          <p class="muted">{{ selectedTemplate?.templateName }}</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closePublishModal"><X :size="18" /></button>
      </header>
      <div class="form-grid">
        <label>版本号<input v-model="publishForm.versionNo" placeholder="不填则自动生成 vN" /></label>
        <label class="wide">发布说明<textarea v-model="publishForm.changeNote" rows="3" /></label>
      </div>
      <div class="toolbar compact">
        <button class="secondary-button" type="button" @click="closePublishModal">取消</button>
        <button class="primary-button" type="button" :disabled="loading || !selectedTemplate" @click="publishSelectedTemplate">
          <GitBranch :size="16" /> 发布版本
        </button>
      </div>
    </section>
  </div>
</template>
