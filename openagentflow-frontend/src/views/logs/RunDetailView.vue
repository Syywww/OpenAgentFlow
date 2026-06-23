<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, RefreshCw, Share2 } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import StatCard from '../../components/StatCard.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import { fetchRunDetail, type RunDetail, type TraceStepDetail } from '../../api/traces';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const run = ref<RunDetail | null>(null);
const selectedStepId = ref('');
const activeTab = ref('运行概览');

const selectedStep = computed(() => run.value?.steps.find((step) => step.id === selectedStepId.value) || run.value?.steps[0]);
const successStepCount = computed(() => run.value?.steps.filter((step) => step.status === 'SUCCESS').length ?? 0);

onMounted(() => {
  void loadRun();
});

async function loadRun() {
  loading.value = true;
  try {
    run.value = await fetchRunDetail(String(route.params.id));
    selectedStepId.value = run.value.steps[0]?.id || '';
  } finally {
    loading.value = false;
  }
}

function formatMs(value?: number) {
  if (!value) return '0ms';
  if (value < 1000) return `${value}ms`;
  return `${(value / 1000).toFixed(2)}s`;
}

function formatCost(value?: number) {
  return `¥${Number(value || 0).toFixed(4)}`;
}

function toJson(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  if (typeof value === 'string') {
    return value;
  }
  return JSON.stringify(value, null, 2);
}

function stepBadge(step?: TraceStepDetail) {
  if (!step) return '未知';
  if (step.stepType === 'LLM') return 'LLM';
  if (step.stepType === 'RAG') return 'RAG';
  if (step.stepType === 'TOOL') return '工具';
  return step.stepType;
}
</script>

<template>
  <PageHeader
    :title="run?.runNo || String(route.params.id)"
    :description="`Agent · ${run?.agentName || '默认 Agent'}｜发起人：${run?.userName || '-'}｜开始时间：${run?.startedAt || '-'}`"
  >
    <template #actions>
      <button class="secondary-button" type="button" @click="router.push('/logs')"><ArrowLeft :size="16" /> 返回</button>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadRun"><RefreshCw :size="16" /> 刷新</button>
      <button class="secondary-button" type="button"><Share2 :size="16" /> 分享</button>
    </template>
  </PageHeader>

  <section class="metric-grid">
    <StatCard label="总耗时" :value="formatMs(run?.latencyMs)" :detail="`${run?.stepCount || 0} 个步骤`" icon="Timer" tone="warning" />
    <StatCard label="总成本" :value="formatCost(run?.totalCost)" detail="当前为估算字段" icon="Coins" tone="info" />
    <StatCard label="总 Tokens" :value="String(run?.totalTokens || 0)" :detail="`输入 ${run?.promptTokens || 0}`" icon="Activity" tone="neutral" />
    <StatCard label="步骤成功" :value="`${successStepCount}/${run?.steps.length || 0}`" :detail="run?.statusLabel || '待加载'" icon="Workflow" tone="success" />
  </section>

  <div class="tabs">
    <button v-for="tab in ['运行概览', '步骤详情', '性能分析', '日志']" :key="tab" class="tab" :class="{ active: activeTab === tab }" type="button" @click="activeTab = tab">{{ tab }}</button>
  </div>

  <section v-if="loading" class="section-block">
    <div class="empty-state">正在加载 Trace...</div>
  </section>

  <section v-else-if="run" class="run-detail-layout">
    <div class="section-block">
      <div class="section-title"><h2>步骤时间线</h2><StatusBadge :label="run.statusLabel" /></div>
      <div class="timeline-list">
        <button
          v-for="(step, index) in run.steps"
          :key="step.id"
          type="button"
          :class="{ active: selectedStepId === step.id }"
          @click="selectedStepId = step.id"
        >
          <b>{{ index + 1 }}. {{ step.stepName }} <small>{{ stepBadge(step) }}</small></b>
          <span>{{ step.startedAt || '-' }} · 耗时 {{ formatMs(step.latencyMs) }} · {{ step.status }}</span>
        </button>
      </div>
      <div v-if="run.steps.length === 0" class="empty-state">暂无 Trace 步骤</div>
    </div>

    <div class="run-side">
      <div class="section-block">
        <div class="section-title"><h2>输入信息</h2></div>
        <p>{{ run.inputText || '-' }}</p>
      </div>
      <div class="section-block">
        <div class="section-title"><h2>输出信息</h2></div>
        <p>{{ run.outputText || '-' }}</p>
        <p v-if="run.errorMessage" class="form-error">{{ run.errorMessage }}</p>
      </div>
      <div class="section-block">
        <div class="section-title"><h2>当前步骤</h2><StatusBadge v-if="selectedStep" :label="selectedStep.status" /></div>
        <p><b>{{ selectedStep?.stepName || '-' }}</b></p>
        <p>类型：{{ selectedStep?.stepType || '-' }} · 耗时：{{ formatMs(selectedStep?.latencyMs) }}</p>
        <p v-if="selectedStep?.errorMessage" class="form-error">{{ selectedStep.errorMessage }}</p>
      </div>
      <div class="section-block">
        <div class="section-title"><h2>输入载荷</h2></div>
        <pre class="code-block light">{{ toJson(selectedStep?.inputPayload) }}</pre>
      </div>
      <div class="section-block">
        <div class="section-title"><h2>输出载荷</h2></div>
        <pre class="code-block light">{{ toJson(selectedStep?.outputPayload) }}</pre>
      </div>
      <div v-if="selectedStep?.stepType === 'LLM'" class="section-block">
        <div class="section-title"><h2>Prompt / Messages</h2></div>
        <pre class="code-block light">{{ toJson(selectedStep.prompt || selectedStep.llmCall?.requestMessages) }}</pre>
      </div>
      <div v-if="selectedStep?.stepType === 'RAG'" class="section-block">
        <div class="section-title"><h2>RAG 检索</h2><span>{{ selectedStep.retrievalLogs?.length || 0 }} 条</span></div>
        <pre class="code-block light">{{ toJson(selectedStep.retrievalLogs || run.retrievalLogs) }}</pre>
      </div>
      <div v-if="selectedStep?.stepType === 'TOOL'" class="section-block">
        <div class="section-title"><h2>工具调用</h2></div>
        <pre class="code-block light">{{ toJson(selectedStep.toolInvocation) }}</pre>
      </div>
      <div class="section-block">
        <div class="section-title"><h2>Token 使用</h2></div>
        <pre class="code-block light">{{ toJson(selectedStep?.tokenUsage) }}</pre>
      </div>
    </div>
  </section>
</template>
