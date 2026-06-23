<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Play, Plus, RefreshCw, Save, Trash2, Upload } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import { fetchAgents, type AgentSummary } from '../../api/agents';
import { fetchChatModels, type ModelConfigSummary } from '../../api/models';
import {
  createEvaluationDataset,
  deleteEvaluationDataset,
  fetchEvaluationDataset,
  fetchEvaluationDatasets,
  fetchEvaluationTasks,
  importEvaluationSamples,
  runEvaluationTask,
  updateEvaluationDataset,
  type EvaluationDatasetDetail,
  type EvaluationDatasetRequest,
  type EvaluationDatasetSummary,
  type EvaluationTaskSummary,
} from '../../api/evaluations';

const router = useRouter();
const loading = ref(false);
const running = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const datasets = ref<EvaluationDatasetSummary[]>([]);
const selectedDataset = ref<EvaluationDatasetDetail | null>(null);
const tasks = ref<EvaluationTaskSummary[]>([]);
const agents = ref<AgentSummary[]>([]);
const models = ref<ModelConfigSummary[]>([]);
const compareModelIds = ref<string[]>([]);
const replaceExisting = ref(true);
const sampleText = ref(`[
  {
    "question": "请回答：1+1 等于几？",
    "expectedAnswer": "2",
    "scoringPoints": "2",
    "metadata": "{\\"tag\\":\\"smoke\\"}"
  }
]`);

const datasetForm = reactive<EvaluationDatasetRequest>({
  datasetName: '',
  datasetCode: '',
  description: '',
  domain: '',
  tags: '',
  visibility: 'private',
  status: 'active',
});

const runForm = reactive({
  taskName: '',
  agentId: '',
  baselineModelId: '',
  promptStrategy: '默认策略',
  promptVariantText: '',
  knowledgeStrategy: 'Agent 当前绑定知识库',
  temperature: 0.2,
  maxTokens: 512,
  maxSamples: 20,
});

const latestTasks = computed(() => tasks.value.slice(0, 8));

function statusLabel(status?: string) {
  const map: Record<string, string> = {
    active: '启用',
    enabled: '启用',
    disabled: '停用',
    running: '运行中',
    success: '成功',
    failed: '失败',
    deleted: '已删除',
  };
  return map[status ?? ''] ?? status ?? '-';
}

function resetDatasetForm() {
  datasetForm.datasetName = '';
  datasetForm.datasetCode = '';
  datasetForm.description = '';
  datasetForm.domain = '';
  datasetForm.tags = '';
  datasetForm.visibility = 'private';
  datasetForm.status = 'active';
  selectedDataset.value = null;
}

function fillDatasetForm(dataset: EvaluationDatasetDetail | EvaluationDatasetSummary) {
  datasetForm.datasetName = dataset.datasetName;
  datasetForm.datasetCode = dataset.datasetCode;
  datasetForm.description = dataset.description ?? '';
  datasetForm.domain = dataset.domain ?? '';
  datasetForm.tags = dataset.tags ?? '';
  datasetForm.visibility = dataset.visibility ?? 'private';
  datasetForm.status = dataset.status ?? 'active';
}

async function loadAll() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [datasetList, taskList, agentList, modelList] = await Promise.all([
      fetchEvaluationDatasets(),
      fetchEvaluationTasks(),
      fetchAgents(),
      fetchChatModels(),
    ]);
    datasets.value = datasetList;
    tasks.value = taskList;
    agents.value = agentList;
    models.value = modelList;
    if (!selectedDataset.value && datasetList.length > 0) {
      await selectDataset(datasetList[0].id);
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载评测数据失败';
  } finally {
    loading.value = false;
  }
}

async function selectDataset(id: string) {
  const detail = await fetchEvaluationDataset(id);
  selectedDataset.value = detail;
  fillDatasetForm(detail);
  if (!runForm.taskName) {
    runForm.taskName = `${detail.datasetName} 评测`;
  }
}

async function saveDataset() {
  if (!datasetForm.datasetName.trim()) {
    errorMessage.value = '请填写评测集名称';
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    const saved = selectedDataset.value
      ? await updateEvaluationDataset(selectedDataset.value.id, datasetForm)
      : await createEvaluationDataset(datasetForm);
    selectedDataset.value = saved;
    successMessage.value = '评测集已保存';
    await loadAll();
    await selectDataset(saved.id);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存评测集失败';
  } finally {
    loading.value = false;
  }
}

async function removeDataset() {
  if (!selectedDataset.value) {
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    await deleteEvaluationDataset(selectedDataset.value.id);
    resetDatasetForm();
    successMessage.value = '评测集已删除';
    await loadAll();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '删除评测集失败';
  } finally {
    loading.value = false;
  }
}

async function importSamples() {
  if (!selectedDataset.value) {
    errorMessage.value = '请先选择或创建评测集';
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    const samples = JSON.parse(sampleText.value);
    if (!Array.isArray(samples)) {
      throw new Error('样本 JSON 必须是数组');
    }
    const detail = await importEvaluationSamples(selectedDataset.value.id, {
      replaceExisting: replaceExisting.value,
      samples,
    });
    selectedDataset.value = detail;
    successMessage.value = `已导入 ${detail.samples.length} 条样本`;
    await loadAll();
    await selectDataset(detail.id);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '导入样本失败';
  } finally {
    loading.value = false;
  }
}

async function runTask() {
  if (!selectedDataset.value || !runForm.agentId) {
    errorMessage.value = '请选择评测集和 Agent';
    return;
  }
  running.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    const detail = await runEvaluationTask({
      taskName: runForm.taskName || `${selectedDataset.value.datasetName} 评测`,
      datasetId: selectedDataset.value.id,
      agentId: runForm.agentId,
      baselineModelId: runForm.baselineModelId || undefined,
      compareModelIds: compareModelIds.value,
      promptStrategy: runForm.promptStrategy,
      promptVariantText: runForm.promptVariantText,
      knowledgeStrategy: runForm.knowledgeStrategy,
      temperature: runForm.temperature,
      maxTokens: runForm.maxTokens,
      maxSamples: runForm.maxSamples,
    });
    successMessage.value = '评测任务已完成';
    await router.push(`/eval/result/${detail.id}`);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '运行评测失败';
  } finally {
    running.value = false;
  }
}

function toggleCompareModel(id: string) {
  compareModelIds.value = compareModelIds.value.includes(id)
    ? compareModelIds.value.filter((item) => item !== id)
    : [...compareModelIds.value, id];
}

onMounted(() => {
  void loadAll();
});
</script>

<template>
  <PageHeader title="评测集管理" description="管理测试样本，支持 Agent、模型、Prompt 与知识库策略对比评测">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadAll"><RefreshCw :size="16" /> 刷新</button>
      <button class="secondary-button" type="button" @click="resetDatasetForm"><Plus :size="16" /> 新建评测集</button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
  <p v-if="successMessage" class="form-success">{{ successMessage }}</p>

  <section class="dashboard-columns">
    <div class="section-block">
      <div class="section-title"><h2>评测集</h2><span>{{ datasets.length }} 个</span></div>
      <table class="data-table rich">
        <thead>
          <tr><th>名称</th><th>样本</th><th>标签</th><th>状态</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in datasets" :key="item.id">
            <td>
              <b>{{ item.datasetName }}</b>
              <span class="block muted">{{ item.description || item.datasetCode }}</span>
            </td>
            <td>{{ item.sampleCount }}</td>
            <td><StatusBadge :label="item.domain || '通用'" /></td>
            <td><StatusBadge :label="statusLabel(item.status)" /></td>
            <td>
              <button class="secondary-button slim" type="button" @click="selectDataset(item.id)">编辑</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="datasets.length === 0" class="empty-state">暂无评测集</div>
    </div>

    <div class="section-block">
      <div class="section-title"><h2>{{ selectedDataset ? '编辑评测集' : '新建评测集' }}</h2><span>{{ selectedDataset?.datasetCode || '自动生成编码' }}</span></div>
      <div class="form-stack">
        <label>评测集名称<input v-model="datasetForm.datasetName" placeholder="例如：企业知识库问答基准集" /></label>
        <label>评测集编码<input v-model="datasetForm.datasetCode" placeholder="不填自动生成" /></label>
        <label>业务领域<input v-model="datasetForm.domain" placeholder="客服 / 金融 / 法务" /></label>
        <label>标签<textarea v-model="datasetForm.tags" class="code-editor compact" placeholder="可以填写 JSON 数组或逗号分隔标签" /></label>
        <label>描述<textarea v-model="datasetForm.description" placeholder="说明评测集覆盖范围" /></label>
        <div class="two-cols">
          <label>可见性<select v-model="datasetForm.visibility"><option value="private">私有</option><option value="public">公开</option></select></label>
          <label>状态<select v-model="datasetForm.status"><option value="active">启用</option><option value="disabled">停用</option></select></label>
        </div>
        <div class="action-row end">
          <button class="danger-button" type="button" :disabled="!selectedDataset || loading" @click="removeDataset"><Trash2 :size="16" /> 删除</button>
          <button class="primary-button" type="button" :disabled="loading" @click="saveDataset"><Save :size="16" /> 保存</button>
        </div>
      </div>
    </div>
  </section>

  <section class="dashboard-columns">
    <div class="section-block">
      <div class="section-title"><h2>样本导入</h2><span>{{ selectedDataset?.samples.length || 0 }} 条</span></div>
      <div class="form-stack">
        <label class="checkbox-row"><input v-model="replaceExisting" type="checkbox" /> 替换已有样本</label>
        <label>样本 JSON<textarea v-model="sampleText" class="code-editor" /></label>
        <div class="action-row end">
          <button class="primary-button" type="button" :disabled="!selectedDataset || loading" @click="importSamples"><Upload :size="16" /> 导入样本</button>
        </div>
      </div>
    </div>

    <div class="section-block">
      <div class="section-title"><h2>运行评测</h2><span>同步批量执行</span></div>
      <div class="form-stack">
        <label>任务名称<input v-model="runForm.taskName" placeholder="例如：知识库问答回归评测" /></label>
        <label>Agent<select v-model="runForm.agentId"><option value="">请选择 Agent</option><option v-for="agent in agents" :key="agent.id" :value="agent.id">{{ agent.agentName }}</option></select></label>
        <label>基线模型<select v-model="runForm.baselineModelId"><option value="">使用 Agent 默认模型</option><option v-for="model in models" :key="model.id" :value="model.id">{{ model.modelName }}</option></select></label>
        <div>
          <b>对比模型</b>
          <div class="badge-row">
            <label v-for="model in models" :key="model.id" class="checkbox-row">
              <input type="checkbox" :checked="compareModelIds.includes(model.id)" @change="toggleCompareModel(model.id)" />
              {{ model.modelName }}
            </label>
          </div>
        </div>
        <div class="two-cols">
          <label>温度<input v-model.number="runForm.temperature" type="number" step="0.1" min="0" max="2" /></label>
          <label>最大 Token<input v-model.number="runForm.maxTokens" type="number" min="1" /></label>
        </div>
        <label>最大样本数<input v-model.number="runForm.maxSamples" type="number" min="1" max="500" /></label>
        <label>Prompt 策略<input v-model="runForm.promptStrategy" placeholder="默认策略 / 简洁回答 / 带引用回答" /></label>
        <label>Prompt 补充<textarea v-model="runForm.promptVariantText" placeholder="用于 Prompt A/B 对比的补充指令" /></label>
        <label>知识库切片策略<input v-model="runForm.knowledgeStrategy" placeholder="记录本次对比的切片策略名称" /></label>
        <div class="action-row end">
          <button class="primary-button" type="button" :disabled="running || !selectedDataset" @click="runTask"><Play :size="16" /> {{ running ? '评测中' : '运行评测' }}</button>
        </div>
      </div>
    </div>
  </section>

  <section class="section-block">
    <div class="section-title"><h2>最近评测任务</h2><span>{{ latestTasks.length }} 条</span></div>
    <table class="data-table">
      <thead>
        <tr><th>任务</th><th>评测集</th><th>Agent</th><th>进度</th><th>得分</th><th>Token</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="task in latestTasks" :key="task.id">
          <td><b>{{ task.taskName }}</b><span class="block muted">{{ task.taskCode }}</span></td>
          <td>{{ task.datasetName }}</td>
          <td>{{ task.agentName }}</td>
          <td>{{ task.finishedSamples }}/{{ task.totalSamples }}</td>
          <td>{{ task.overallScore ?? 0 }}</td>
          <td>{{ task.totalTokens ?? 0 }}</td>
          <td><button class="secondary-button slim" type="button" @click="router.push(`/eval/result/${task.id}`)">结果</button></td>
        </tr>
      </tbody>
    </table>
    <div v-if="latestTasks.length === 0" class="empty-state">暂无评测任务</div>
  </section>
</template>
