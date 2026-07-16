<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { FlaskConical, Play, Rocket, Square, Trash2, X } from 'lucide-vue-next';
import StatusBadge from './StatusBadge.vue';
import {
  autoChoosePromptExperimentWinner,
  createPromptExperiment,
  deletePromptExperiment,
  diffPromptVersions,
  fetchPromptExperiments,
  fetchPromptImpacts,
  fetchPromptMetrics,
  fetchPromptReleases,
  previewPromptTemplate,
  promotePromptVersion,
  startPromptExperiment,
  stopPromptExperiment,
  type PromptCompileResult,
  type PromptEnvironmentRelease,
  type PromptExperimentSummary,
  type PromptImpactItem,
  type PromptTemplateDetail,
  type PromptVersionDiff,
  type PromptVersionMetric,
} from '../api/prompts';

const props = defineProps<{
  template: PromptTemplateDetail | null;
  mode: 'preview' | 'governance' | 'experiments' | 'metrics';
}>();

const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const compileResult = ref<PromptCompileResult | null>(null);
const impacts = ref<PromptImpactItem[]>([]);
const releases = ref<PromptEnvironmentRelease[]>([]);
const experiments = ref<PromptExperimentSummary[]>([]);
const metrics = ref<PromptVersionMetric[]>([]);
const versionDiff = ref<PromptVersionDiff | null>(null);
const releaseModalOpen = ref(false);
const experimentModalOpen = ref(false);

const previewForm = reactive({ versionId: '', variables: '{\n  "user_input": "请介绍企业退款政策"\n}' });
const diffForm = reactive({ fromVersionId: '', toVersionId: '' });
const releaseForm = reactive({ versionId: '', environment: 'testing', grayPercent: 100, releaseNote: '' });
const experimentForm = reactive({
  experimentName: '',
  metricKey: 'quality_score',
  minSampleSize: 30,
  autoWinnerEnabled: true,
  versionA: '',
  versionB: '',
  weightA: 50,
});

const versions = computed(() => props.template?.versions || []);

watch(() => [props.template?.id, props.mode], () => {
  resetVersionDefaults();
  void loadModeData();
}, { immediate: true });

function resetVersionDefaults() {
  const list = versions.value;
  previewForm.versionId ||= props.template?.stableVersionId || list[0]?.id || '';
  diffForm.fromVersionId ||= list[1]?.id || list[0]?.id || '';
  diffForm.toVersionId ||= list[0]?.id || '';
  releaseForm.versionId ||= list[0]?.id || '';
  experimentForm.versionA ||= list[0]?.id || '';
  experimentForm.versionB ||= list[1]?.id || '';
}

async function loadModeData() {
  if (!props.template) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    if (props.mode === 'governance') {
      [impacts.value, releases.value] = await Promise.all([
        fetchPromptImpacts(props.template.id),
        fetchPromptReleases(props.template.id),
      ]);
    } else if (props.mode === 'experiments') {
      experiments.value = await fetchPromptExperiments(props.template.id);
    } else if (props.mode === 'metrics') {
      metrics.value = await fetchPromptMetrics(props.template.id);
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'PromptOps 数据加载失败';
  } finally {
    loading.value = false;
  }
}

async function runPreview() {
  if (!props.template) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    const variables = JSON.parse(previewForm.variables || '{}') as Record<string, unknown>;
    compileResult.value = await previewPromptTemplate(props.template.id, {
      versionId: previewForm.versionId || undefined,
      content: previewForm.versionId ? undefined : props.template.content,
      variableSchema: props.template.variableSchema,
      variables,
      strict: true,
    });
    successMessage.value = 'Prompt 编译完成';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Prompt 编译失败';
  } finally {
    loading.value = false;
  }
}

async function runDiff() {
  if (!props.template || !diffForm.fromVersionId || !diffForm.toVersionId) return;
  loading.value = true;
  try {
    versionDiff.value = await diffPromptVersions(props.template.id, diffForm.fromVersionId, diffForm.toVersionId);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '版本差异加载失败';
  } finally {
    loading.value = false;
  }
}

async function submitRelease() {
  if (!props.template || !releaseForm.versionId) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    await promotePromptVersion(props.template.id, { ...releaseForm });
    releases.value = await fetchPromptReleases(props.template.id);
    successMessage.value = 'Prompt 版本已晋级';
    releaseModalOpen.value = false;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Prompt 版本晋级失败';
  } finally {
    loading.value = false;
  }
}

async function submitExperiment() {
  if (!props.template || !experimentForm.versionA || !experimentForm.versionB) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    await createPromptExperiment(props.template.id, {
      experimentName: experimentForm.experimentName,
      metricKey: experimentForm.metricKey,
      minSampleSize: experimentForm.minSampleSize,
      autoWinnerEnabled: experimentForm.autoWinnerEnabled,
      variants: [
        { variantCode: 'A', promptVersionId: experimentForm.versionA, trafficWeight: experimentForm.weightA },
        { variantCode: 'B', promptVersionId: experimentForm.versionB, trafficWeight: 100 - experimentForm.weightA },
      ],
    });
    experiments.value = await fetchPromptExperiments(props.template.id);
    successMessage.value = 'Prompt A/B 实验已创建';
    experimentModalOpen.value = false;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Prompt 实验创建失败';
  } finally {
    loading.value = false;
  }
}

async function changeExperiment(experiment: PromptExperimentSummary, action: 'start' | 'stop' | 'auto' | 'delete') {
  if (!props.template) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    if (action === 'start') await startPromptExperiment(props.template.id, experiment.id);
    if (action === 'stop') await stopPromptExperiment(props.template.id, experiment.id);
    if (action === 'auto') await autoChoosePromptExperimentWinner(props.template.id, experiment.id);
    if (action === 'delete') await deletePromptExperiment(props.template.id, experiment.id);
    experiments.value = await fetchPromptExperiments(props.template.id);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Prompt 实验操作失败';
  } finally {
    loading.value = false;
  }
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-';
}
</script>

<template>
  <div v-if="!template" class="empty-state">请先选择一个 Prompt 模板</div>
  <template v-else>
    <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="form-success">{{ successMessage }}</p>

    <template v-if="mode === 'preview'">
      <div class="section-title">
        <h2>统一编译预览</h2>
        <button class="primary-button slim" type="button" :disabled="loading" @click="runPreview"><Play :size="14" /> 编译 Prompt</button>
      </div>
      <div class="form-grid promptops-form">
        <label>预览版本<select v-model="previewForm.versionId"><option value="">当前草稿</option><option v-for="version in versions" :key="version.id" :value="version.id">{{ version.versionNo }}</option></select></label>
        <label class="wide">运行变量 JSON<textarea v-model="previewForm.variables" class="code-editor compact" rows="5" /></label>
      </div>
      <template v-if="compileResult">
        <div class="trace-meta">
          <span>实际版本</span><b>{{ compileResult.versionNo || '当前草稿' }}</b>
          <span>预估 Token</span><b>{{ compileResult.estimatedTokens }}</b>
          <span>内容哈希</span><b class="mono truncate-cell" :title="compileResult.contentHash">{{ compileResult.contentHash }}</b>
          <span>实验变体</span><b>{{ compileResult.variantCode || '-' }}</b>
        </div>
        <div v-if="compileResult.warnings.length" class="badge-row"><StatusBadge v-for="warning in compileResult.warnings" :key="warning" :label="warning" tone="warning" /></div>
        <div class="promptops-split">
          <pre class="json-preview promptops-preview">{{ compileResult.renderedPrompt }}</pre>
          <div class="promptops-layer-list">
            <div v-for="layer in compileResult.layers" :key="`${layer.layerCode}-${layer.orderNo}`" class="runtime-step">
              <b>{{ layer.layerName }}</b><span class="muted">{{ layer.layerCode }} · {{ layer.orderNo }}</span>
            </div>
          </div>
        </div>
      </template>
    </template>

    <template v-else-if="mode === 'governance'">
      <div class="section-title">
        <h2>发布与影响分析</h2>
        <button class="primary-button slim" type="button" :disabled="versions.length === 0" @click="releaseModalOpen = true"><Rocket :size="14" /> 晋级版本</button>
      </div>
      <div class="form-grid promptops-form">
        <label>来源版本<select v-model="diffForm.fromVersionId"><option v-for="version in versions" :key="version.id" :value="version.id">{{ version.versionNo }}</option></select></label>
        <label>目标版本<select v-model="diffForm.toVersionId"><option v-for="version in versions" :key="version.id" :value="version.id">{{ version.versionNo }}</option></select></label>
        <button class="secondary-button promptops-inline-action" type="button" @click="runDiff">查看差异</button>
      </div>
      <div v-if="versionDiff" class="promptops-split">
        <div><h3>新增内容</h3><pre class="json-preview diff-added">{{ versionDiff.addedLines.join('\n') || '无' }}</pre></div>
        <div><h3>移除内容</h3><pre class="json-preview diff-removed">{{ versionDiff.removedLines.join('\n') || '无' }}</pre></div>
      </div>
      <div class="section-title compact-title"><h2>资源影响面</h2><span>{{ impacts.length }} 项绑定</span></div>
      <table class="data-table"><thead><tr><th>资源类型</th><th>资源名称</th><th>绑定模式</th><th>锁定版本</th></tr></thead><tbody><tr v-for="item in impacts" :key="`${item.resourceType}-${item.resourceId}`"><td>{{ item.resourceType }}</td><td>{{ item.resourceName }}</td><td>{{ item.bindingMode }}</td><td class="mono">{{ item.versionId || '跟随稳定版' }}</td></tr></tbody></table>
      <div class="section-title compact-title"><h2>环境发布</h2><span>开发 / 测试 / 生产</span></div>
      <table class="data-table"><thead><tr><th>环境</th><th>版本</th><th>灰度</th><th>状态</th><th>晋级时间</th></tr></thead><tbody><tr v-for="release in releases" :key="release.id"><td>{{ release.environment }}</td><td class="mono">{{ release.versionId }}</td><td>{{ release.grayPercent }}%</td><td><StatusBadge :label="release.status" :tone="release.status === 'active' ? 'success' : 'neutral'" /></td><td>{{ formatTime(release.promotedAt) }}</td></tr></tbody></table>
    </template>

    <template v-else-if="mode === 'experiments'">
      <div class="section-title"><h2>Prompt A/B 实验</h2><button class="primary-button slim" type="button" :disabled="versions.length < 2" @click="experimentModalOpen = true"><FlaskConical :size="14" /> 新建实验</button></div>
      <table class="data-table"><thead><tr><th>实验</th><th>状态</th><th>指标</th><th>变体表现</th><th>胜出变体</th><th>操作</th></tr></thead><tbody><tr v-for="experiment in experiments" :key="experiment.id"><td><b>{{ experiment.experimentName }}</b><span class="muted block mono">{{ experiment.experimentCode }}</span></td><td><StatusBadge :label="experiment.status" :tone="experiment.status === 'running' ? 'success' : 'neutral'" /></td><td>{{ experiment.metricKey }}</td><td><span v-for="variant in experiment.variants" :key="variant.id" class="block">{{ variant.variantCode }}: {{ variant.sampleCount }} 次 / {{ variant.successRate }}%</span></td><td>{{ experiment.variants.find((item) => item.id === experiment.winnerVariantId)?.variantCode || '-' }}</td><td><div class="table-actions"><button v-if="experiment.status !== 'running'" class="icon-button" type="button" title="启动" @click="changeExperiment(experiment, 'start')"><Play :size="15" /></button><button v-else class="icon-button" type="button" title="停止" @click="changeExperiment(experiment, 'stop')"><Square :size="15" /></button><button class="secondary-button slim" type="button" @click="changeExperiment(experiment, 'auto')">自动选优</button><button class="icon-button danger-text" type="button" title="删除" @click="changeExperiment(experiment, 'delete')"><Trash2 :size="15" /></button></div></td></tr></tbody></table>
      <div v-if="experiments.length === 0" class="empty-state">暂无 Prompt 实验</div>
    </template>

    <template v-else>
      <div class="section-title"><h2>版本运行指标</h2><span>真实 Runtime 聚合</span></div>
      <table class="data-table"><thead><tr><th>版本</th><th>调用次数</th><th>成功率</th><th>质量分</th><th>平均耗时</th><th>Token</th><th>成本</th></tr></thead><tbody><tr v-for="item in metrics" :key="item.versionId || item.versionNo"><td><b>{{ item.versionNo }}</b></td><td>{{ item.callCount }}</td><td>{{ item.successRate }}%</td><td>{{ item.avgQualityScore.toFixed(2) }}</td><td>{{ item.avgLatencyMs.toFixed(0) }} ms</td><td>{{ item.totalTokens }}</td><td>¥{{ item.totalCost.toFixed(4) }}</td></tr></tbody></table>
      <div v-if="metrics.length === 0" class="empty-state">当前版本尚无真实调用数据</div>
    </template>
  </template>

  <div v-if="releaseModalOpen" class="overlay-backdrop" @click.self="releaseModalOpen = false">
    <section class="modal-panel compact">
      <header class="overlay-header"><div><h2>Prompt 环境晋级</h2><p class="muted">生产环境会执行发布门禁，灰度比例用于稳定分流。</p></div><button class="icon-button" type="button" title="关闭" @click="releaseModalOpen = false"><X :size="18" /></button></header>
      <div class="form-grid"><label>版本<select v-model="releaseForm.versionId"><option v-for="version in versions" :key="version.id" :value="version.id">{{ version.versionNo }}</option></select></label><label>目标环境<select v-model="releaseForm.environment"><option value="development">开发环境</option><option value="testing">测试环境</option><option value="production">生产环境</option></select></label><label>灰度比例<input v-model.number="releaseForm.grayPercent" type="number" min="0" max="100" /></label><label class="wide">晋级说明<textarea v-model="releaseForm.releaseNote" rows="3" /></label></div>
      <div class="toolbar compact"><button class="secondary-button" type="button" @click="releaseModalOpen = false">取消</button><button class="primary-button" type="button" :disabled="loading" @click="submitRelease"><Rocket :size="16" /> 确认晋级</button></div>
    </section>
  </div>

  <div v-if="experimentModalOpen" class="overlay-backdrop" @click.self="experimentModalOpen = false">
    <section class="modal-panel compact">
      <header class="overlay-header"><div><h2>新建 Prompt A/B 实验</h2><p class="muted">相同会话会稳定命中同一变体，避免多轮对话跳组。</p></div><button class="icon-button" type="button" title="关闭" @click="experimentModalOpen = false"><X :size="18" /></button></header>
      <div class="form-grid"><label>实验名称<input v-model="experimentForm.experimentName" /></label><label>主要指标<select v-model="experimentForm.metricKey"><option value="quality_score">质量分</option><option value="success_rate">成功率</option><option value="latency">响应耗时</option><option value="cost">调用成本</option></select></label><label>A 版本<select v-model="experimentForm.versionA"><option v-for="version in versions" :key="version.id" :value="version.id">{{ version.versionNo }}</option></select></label><label>B 版本<select v-model="experimentForm.versionB"><option v-for="version in versions" :key="version.id" :value="version.id">{{ version.versionNo }}</option></select></label><label>A 流量权重<input v-model.number="experimentForm.weightA" type="number" min="1" max="99" /></label><label>B 流量权重<input :value="100 - experimentForm.weightA" disabled /></label><label>最小样本量<input v-model.number="experimentForm.minSampleSize" type="number" min="1" /></label><label class="checkbox-row"><input v-model="experimentForm.autoWinnerEnabled" type="checkbox" /> 达标后允许自动选优</label></div>
      <div class="toolbar compact"><button class="secondary-button" type="button" @click="experimentModalOpen = false">取消</button><button class="primary-button" type="button" :disabled="loading || !experimentForm.experimentName" @click="submitExperiment"><FlaskConical :size="16" /> 创建实验</button></div>
    </section>
  </div>
</template>

<style scoped>
.promptops-form { align-items: end; margin-bottom: 16px; }
.promptops-inline-action { align-self: end; width: max-content; }
.promptops-split { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(260px, .6fr); gap: 16px; margin: 16px 0; }
.promptops-preview { min-height: 240px; max-height: 420px; overflow: auto; }
.promptops-layer-list { max-height: 420px; overflow: auto; }
.promptops-layer-list .runtime-step { display: flex; justify-content: space-between; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--border-color); }
.diff-added { border-left: 3px solid #16a34a; }
.diff-removed { border-left: 3px solid #dc2626; }
@media (max-width: 900px) { .promptops-split { grid-template-columns: 1fr; } }
</style>
