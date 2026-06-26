<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, Download, RefreshCw } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import PaginationBar from '../../components/PaginationBar.vue';
import StatCard from '../../components/StatCard.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import { fetchEvaluationTask, fetchEvaluationTasks, type EvaluationTaskDetail, type EvaluationTaskSummary } from '../../api/evaluations';
import { usePagination } from '../../composables/usePagination';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const errorMessage = ref('');
const tasks = ref<EvaluationTaskSummary[]>([]);
const task = ref<EvaluationTaskDetail | null>(null);
const activePanel = ref<'compare' | 'runs' | 'lowScore'>('compare');

const summary = computed(() => task.value?.summary ?? {});
const runs = computed(() => task.value?.runs ?? []);
const modelCompare = computed(() => task.value?.modelCompare ?? []);
const lowScoreRuns = computed(() => runs.value.slice().sort((a, b) => judgeScore(a) - judgeScore(b)));
const { currentPage: comparePage, pagedItems: pagedModelCompare } = usePagination(modelCompare);
const { currentPage: runPage, pagedItems: pagedRuns } = usePagination(runs);
const { currentPage: lowScorePage, pagedItems: pagedLowScoreRuns } = usePagination(lowScoreRuns);

function numberValue(value: unknown) {
  return typeof value === 'number' ? value : Number(value ?? 0);
}

function percent(value: unknown) {
  return `${numberValue(value).toFixed(2)}%`;
}

function metricScore(run: EvaluationTaskDetail['runs'][number], code: string) {
  return run.scores.find((score) => score.metricCode === code)?.score ?? 0;
}

function judgeScore(run: EvaluationTaskDetail['runs'][number]) {
  return metricScore(run, 'llm_judge_overall') || metricScore(run, 'accuracy');
}

function parseJudgeDetail(raw?: string) {
  if (!raw) {
    return {};
  }
  try {
    return JSON.parse(raw) as Record<string, unknown>;
  } catch {
    return {};
  }
}

function judgeReason(run: EvaluationTaskDetail['runs'][number]) {
  const score = run.scores.find((item) => item.metricCode === 'llm_judge_overall')
    || run.scores.find((item) => item.judgeType === 'llm_as_judge');
  const detail = parseJudgeDetail(score?.judgeDetail);
  const judgeDetail = detail.judgeDetail as Record<string, unknown> | undefined;
  return String(judgeDetail?.reason || detail.judgeErrorMessage || detail.reason || '暂无 Judge 理由');
}

function judgeTypeLabel(run: EvaluationTaskDetail['runs'][number]) {
  const score = run.scores.find((item) => item.metricCode === 'llm_judge_overall');
  return score?.judgeType === 'llm_as_judge' ? 'LLM Judge' : '规则兜底';
}

function statusLabel(status?: string) {
  const map: Record<string, string> = {
    running: '运行中',
    success: '成功',
    failed: '失败',
  };
  return map[status ?? ''] ?? status ?? '-';
}

function exportJson() {
  if (!task.value) {
    return;
  }
  const blob = new Blob([JSON.stringify(task.value, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${task.value.taskCode || task.value.id}.json`;
  link.click();
  URL.revokeObjectURL(url);
}

async function loadTask() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const taskId = route.params.id ? String(route.params.id) : '';
    if (taskId) {
      task.value = await fetchEvaluationTask(taskId);
    } else {
      tasks.value = await fetchEvaluationTasks();
      if (tasks.value.length > 0) {
        await router.replace(`/eval/result/${tasks.value[0].id}`);
      }
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载评测结果失败';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadTask();
});

watch(() => route.params.id, () => {
  void loadTask();
});
</script>

<template>
  <PageHeader :title="task?.taskName || '评测结果'" :description="task ? `任务 ID：${task.taskCode}｜评测集：${task.datasetName}` : '选择一次真实评测任务查看明细'">
    <template #actions>
      <button class="secondary-button" type="button" @click="router.push('/eval')"><ArrowLeft :size="16" /> 返回</button>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadTask"><RefreshCw :size="16" /> 刷新</button>
      <button class="primary-button" type="button" :disabled="!task" @click="exportJson"><Download :size="16" /> 导出 JSON</button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

  <template v-if="task">
    <section class="metric-grid">
      <StatCard label="综合得分" :value="String(summary.overallScore ?? task.overallScore ?? 0)" detail="优先采用 LLM-as-Judge" icon="ShieldCheck" tone="success" />
      <StatCard label="准确率" :value="percent(summary.accuracy)" detail="标准答案覆盖" icon="Activity" tone="info" />
      <StatCard label="Judge 得分" :value="String(summary.judgeScore ?? 0)" detail="裁判模型综合分" icon="Sparkles" tone="success" />
      <StatCard label="幻觉率" :value="percent(summary.hallucinationRate)" detail="100 - 幻觉控制分" icon="ShieldAlert" tone="warning" />
      <StatCard label="Token 消耗" :value="String(summary.totalTokens ?? task.totalTokens ?? 0)" detail="本次评测累计" icon="Coins" tone="neutral" />
    </section>

    <section class="metric-grid compact">
      <StatCard label="相关性" :value="percent(summary.relevance)" detail="问题与上下文匹配" icon="Library" tone="info" />
      <StatCard label="完整性" :value="percent(summary.completeness)" detail="评分点覆盖" icon="Braces" tone="success" />
      <StatCard label="引用正确率" :value="percent(summary.citationCorrectness)" detail="RAG 来源追溯" icon="Server" tone="info" />
      <StatCard label="平均耗时" :value="`${summary.averageLatencyMs ?? task.averageLatencyMs ?? 0}ms`" detail="样本平均" icon="Timer" tone="neutral" />
    </section>

    <section class="governance-card-tabs">
      <button class="governance-tab-card" :class="{ active: activePanel === 'compare' }" type="button" @click="activePanel = 'compare'">
        <span>模型策略对比</span>
        <b>{{ modelCompare.length }}</b>
        <small>模型、Prompt 和知识库策略</small>
      </button>
      <button class="governance-tab-card" :class="{ active: activePanel === 'runs' }" type="button" @click="activePanel = 'runs'">
        <span>样本结果</span>
        <b>{{ runs.length }}</b>
        <small>逐样本评分与 Trace</small>
      </button>
      <button class="governance-tab-card" :class="{ active: activePanel === 'lowScore' }" type="button" @click="activePanel = 'lowScore'">
        <span>低分样本详情</span>
        <b>{{ lowScoreRuns.length }}</b>
        <small>定位效果偏低的样本</small>
      </button>
    </section>

    <section class="section-block evaluation-result-panel">
      <template v-if="activePanel === 'compare'">
        <div class="section-title"><h2>模型 / Prompt / 知识库策略对比</h2><span>{{ modelCompare.length }} 组</span></div>
        <table class="data-table">
          <thead>
            <tr><th>模型</th><th>综合</th><th>Judge</th><th>准确率</th><th>相关性</th><th>完整性</th><th>幻觉率</th><th>引用</th><th>工具</th><th>耗时</th><th>Token</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in pagedModelCompare" :key="String(row.modelId)">
              <td><b>{{ row.modelName }}</b><span class="block muted">{{ row.modelId }}</span></td>
              <td>{{ row.overallScore }}</td>
              <td>{{ row.judgeScore ?? 0 }}</td>
              <td>{{ percent(row.accuracy) }}</td>
              <td>{{ percent(row.relevance) }}</td>
              <td>{{ percent(row.completeness) }}</td>
              <td>{{ percent(row.hallucinationRate) }}</td>
              <td>{{ percent(row.citationCorrectness) }}</td>
              <td>{{ percent(row.toolSuccessRate) }}</td>
              <td>{{ row.averageLatencyMs }}ms</td>
              <td>{{ row.totalTokens }}</td>
            </tr>
          </tbody>
        </table>
        <PaginationBar v-model:page="comparePage" :total="modelCompare.length" />
      </template>

      <template v-else-if="activePanel === 'runs'">
        <div class="section-title"><h2>样本结果</h2><span>{{ runs.length }} 条</span></div>
        <table class="data-table">
          <thead>
            <tr><th>样本</th><th>模型</th><th>状态</th><th>Judge</th><th>准确</th><th>完整</th><th>耗时</th><th>Trace</th></tr>
          </thead>
          <tbody>
            <tr v-for="run in pagedRuns" :key="run.id">
              <td><b>#{{ run.sampleNo }}</b><span class="block muted">{{ run.question }}</span></td>
              <td>{{ run.modelName || run.modelId }}</td>
              <td><StatusBadge :label="statusLabel(run.status)" /></td>
              <td><StatusBadge :label="`${judgeTypeLabel(run)} ${judgeScore(run).toFixed(2)}`" /></td>
              <td>{{ percent(metricScore(run, 'accuracy')) }}</td>
              <td>{{ percent(metricScore(run, 'completeness')) }}</td>
              <td>{{ run.latencyMs || 0 }}ms</td>
              <td>
                <button class="secondary-button slim" type="button" :disabled="!run.runId" @click="router.push(`/logs/${run.runId}`)">查看</button>
              </td>
            </tr>
          </tbody>
        </table>
        <PaginationBar v-model:page="runPage" :total="runs.length" />
      </template>

      <template v-else>
        <div class="section-title"><h2>低分样本详情</h2><span>可追溯</span></div>
        <div v-if="runs.length === 0" class="empty-state">暂无样本结果</div>
        <article v-for="run in pagedLowScoreRuns" :key="`${run.id}-detail`" class="list-row">
          <div>
            <b>{{ run.modelName || run.modelId }} / #{{ run.sampleNo }}</b>
            <span>{{ run.errorMessage || run.answerText || '无回答内容' }}</span>
            <small class="block muted">标准答案：{{ run.expectedAnswer || '-' }}</small>
            <small class="block muted">Judge：{{ judgeScore(run).toFixed(2) }}，{{ judgeReason(run) }}</small>
          </div>
          <StatusBadge :label="statusLabel(run.status)" />
          <button class="secondary-button slim" type="button" :disabled="!run.runId" @click="router.push(`/logs/${run.runId}`)">Trace</button>
        </article>
        <PaginationBar v-model:page="lowScorePage" :total="lowScoreRuns.length" />
      </template>
    </section>
  </template>

  <section v-else class="empty-state">暂无评测结果</section>
</template>
