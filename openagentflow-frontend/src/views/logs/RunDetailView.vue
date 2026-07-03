<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, RefreshCw, Share2 } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import PaginationBar from '../../components/PaginationBar.vue';
import RuntimeInterpreter from '../../components/RuntimeInterpreter.vue';
import StatCard from '../../components/StatCard.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import { fetchRunDetail, type RunDetail, type TraceStepDetail } from '../../api/traces';
import { usePagination } from '../../composables/usePagination';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const run = ref<RunDetail | null>(null);
const selectedStepId = ref('');
const activeTab = ref('运行概览');
const steps = computed(() => run.value?.steps || []);
const { currentPage: stepPage, pagedItems: pagedSteps, pageSize: stepPageSize, resetPage: resetStepPage } = usePagination(steps);

const selectedStep = computed(() => run.value?.steps.find((step) => step.id === selectedStepId.value) || run.value?.steps[0]);
const successStepCount = computed(() => run.value?.steps.filter((step) => step.status === 'SUCCESS').length ?? 0);
const runtimePhases = computed(() => {
  const currentRun = run.value;
  const currentSteps = currentRun?.steps || [];
  const ragStep = currentSteps.find((step) => step.stepType === 'RAG');
  const toolSteps = currentSteps.filter((step) => step.stepType === 'TOOL');
  const llmSteps = currentSteps.filter((step) => step.stepType === 'LLM');
  const trusted = findTrustedAnswer(currentRun?.outputPayload) || findTrustedAnswer(ragStep?.outputPayload);
  const runFailed = currentRun?.status === 'FAILED';
  return [
    {
      id: 'input',
      label: '输入接收',
      status: currentRun?.inputText ? 'success' : 'neutral',
      summary: currentRun?.inputText ? '已接收' : '无文本输入',
      reason: currentRun?.inputText || '本次运行没有可展示的输入文本。',
      metric: currentRun?.runNo || '',
    },
    {
      id: 'context',
      label: '上下文装配',
      status: currentRun ? 'success' : 'pending',
      summary: currentRun?.agentName || '默认 Agent',
      reason: `Runtime 将 Agent、模型、历史会话、Memory 与用户输入装配成模型上下文。`,
      metric: `步骤 ${currentSteps.length}`,
    },
    {
      id: 'rag',
      label: 'RAG 证据',
      status: trusted?.enabled && trusted.answerable === false ? 'warning' : ragStep ? 'success' : 'neutral',
      summary: ragStep ? `${ragStep.retrievalLogs?.length || currentRun?.retrievalLogs?.length || 0} 条检索` : '未触发',
      reason: trusted?.enabled && trusted.answerable === false
        ? trusted.rejectReason || '可信回答模式拦截'
        : ragStep
          ? '已执行知识库检索，引用来源可在步骤详情中展开。'
          : '本次运行没有进入 RAG 检索步骤。',
      metric: trusted?.enabled ? `可信模式 · 置信 ${(trusted.confidenceScore || 0).toFixed(4)}` : '普通模式',
    },
    {
      id: 'tool',
      label: '工具动作',
      status: toolSteps.length || currentRun?.toolInvocations?.length ? 'success' : 'neutral',
      summary: `${toolSteps.length || currentRun?.toolInvocations?.length || 0} 次调用`,
      reason: toolSteps.length || currentRun?.toolInvocations?.length ? 'Runtime 执行了外部工具并把结果回填给模型。' : '本次运行未触发工具。',
      evidence: (currentRun?.toolInvocations || []).slice(0, 3).map((tool) => String(tool.toolName || tool.toolCode || tool.id || 'tool')),
    },
    {
      id: 'llm',
      label: '模型生成',
      status: runFailed ? 'danger' : llmSteps.length ? 'success' : 'neutral',
      summary: `${llmSteps.length || currentRun?.llmCalls?.length || 0} 次 LLM`,
      reason: runFailed ? currentRun?.errorMessage || '模型或运行步骤失败。' : '模型调用、Prompt 与 Token 明细可在步骤详情中查看。',
      metric: `Token ${currentRun?.totalTokens || 0} · ${formatMs(currentRun?.latencyMs)}`,
    },
    {
      id: 'governance',
      label: '治理判定',
      status: trusted?.enabled && trusted.answerable === false ? 'warning' : runFailed ? 'danger' : 'success',
      summary: trusted?.enabled ? (trusted.answerable === false ? '可信拒答' : '可信通过') : '常规通过',
      reason: trusted?.rejectReason || currentRun?.errorMessage || '未发现阻断策略。',
      metric: trusted?.citationRequired ? '要求引用' : '不强制引用',
    },
    {
      id: 'output',
      label: '结果交付',
      status: runFailed ? 'danger' : currentRun?.outputText ? 'success' : 'pending',
      summary: currentRun?.statusLabel || '待完成',
      reason: currentRun?.outputText || currentRun?.errorMessage || '等待最终输出。',
      metric: formatCost(currentRun?.totalCost),
    },
  ] as const;
});

onMounted(() => {
  void loadRun();
});

async function loadRun() {
  loading.value = true;
  try {
    run.value = await fetchRunDetail(String(route.params.id));
    selectedStepId.value = run.value.steps[0]?.id || '';
    resetStepPage();
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

function parsePayload(value: unknown): Record<string, unknown> {
  if (!value) return {};
  if (typeof value === 'object') return value as Record<string, unknown>;
  if (typeof value === 'string') {
    try {
      return JSON.parse(value) as Record<string, unknown>;
    } catch {
      return {};
    }
  }
  return {};
}

function findTrustedAnswer(value: unknown) {
  const payload = parsePayload(value);
  const trusted = payload.trustedAnswer;
  if (!trusted || typeof trusted !== 'object') {
    return undefined;
  }
  const item = trusted as Record<string, unknown>;
  return {
    enabled: item.enabled === true,
    answerable: item.answerable !== false,
    citationRequired: item.citationRequired === true,
    confidenceScore: Number(item.confidenceScore || 0),
    rejectReason: String(item.rejectReason || ''),
  };
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

  <section v-if="!loading && run" class="run-detail-layout" :class="{ 'single-panel': activeTab !== '步骤详情' }">
    <div v-if="activeTab === '步骤详情'" class="section-block run-timeline-panel">
      <div class="section-title"><h2>步骤时间线</h2><StatusBadge :label="run.statusLabel" /></div>
      <div class="timeline-list">
        <button
          v-for="(step, index) in pagedSteps"
          :key="step.id"
          type="button"
          :class="{ active: selectedStepId === step.id }"
          @click="selectedStepId = step.id"
        >
          <b>{{ (stepPage - 1) * stepPageSize + index + 1 }}. {{ step.stepName }} <small>{{ stepBadge(step) }}</small></b>
          <span>{{ step.startedAt || '-' }} · 耗时 {{ formatMs(step.latencyMs) }} · {{ step.status }}</span>
        </button>
      </div>
      <div v-if="run.steps.length === 0" class="empty-state">暂无 Trace 步骤</div>
      <PaginationBar v-model:page="stepPage" :total="run.steps.length" />
    </div>

    <div class="run-side">
      <template v-if="activeTab === '运行概览'">
        <RuntimeInterpreter
          title="Agent Runtime 可视化解释器"
          :subtitle="`${run.runType} · ${run.statusLabel}`"
          :phases="runtimePhases"
          compact
        />
        <div class="section-block">
          <div class="section-title"><h2>输入信息</h2></div>
          <p>{{ run.inputText || '-' }}</p>
        </div>
        <div class="section-block">
          <div class="section-title"><h2>输出信息</h2></div>
          <p>{{ run.outputText || '-' }}</p>
          <p v-if="run.errorMessage" class="form-error">{{ run.errorMessage }}</p>
        </div>
      </template>

      <template v-else-if="activeTab === '步骤详情'">
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
      </template>

      <template v-else-if="activeTab === '性能分析'">
        <div class="section-block run-analysis-grid">
          <div><span>总耗时</span><b>{{ formatMs(run.latencyMs) }}</b></div>
          <div><span>总 Tokens</span><b>{{ run.totalTokens || 0 }}</b></div>
          <div><span>Prompt</span><b>{{ run.promptTokens || 0 }}</b></div>
          <div><span>Completion</span><b>{{ run.completionTokens || 0 }}</b></div>
          <div><span>总成本</span><b>{{ formatCost(run.totalCost) }}</b></div>
          <div><span>步骤数</span><b>{{ run.steps.length }}</b></div>
        </div>
        <div class="section-block">
          <div class="section-title"><h2>LLM 调用</h2><span>{{ run.llmCalls?.length || 0 }} 次</span></div>
          <pre class="code-block light">{{ toJson(run.llmCalls || []) }}</pre>
        </div>
        <div class="section-block">
          <div class="section-title"><h2>工具耗时</h2><span>{{ run.toolInvocations?.length || 0 }} 次</span></div>
          <pre class="code-block light">{{ toJson(run.toolInvocations || []) }}</pre>
        </div>
      </template>

      <template v-else>
        <div class="section-block">
          <div class="section-title"><h2>运行元数据</h2></div>
          <pre class="code-block light">{{ toJson(run.metadata) }}</pre>
        </div>
        <div class="section-block">
          <div class="section-title"><h2>运行输入载荷</h2></div>
          <pre class="code-block light">{{ toJson(run.inputPayload) }}</pre>
        </div>
        <div class="section-block">
          <div class="section-title"><h2>运行输出载荷</h2></div>
          <pre class="code-block light">{{ toJson(run.outputPayload) }}</pre>
        </div>
        <div class="section-block">
          <div class="section-title"><h2>RAG 检索日志</h2><span>{{ run.retrievalLogs?.length || 0 }} 条</span></div>
          <pre class="code-block light">{{ toJson(run.retrievalLogs || []) }}</pre>
        </div>
        <div class="section-block">
          <div class="section-title"><h2>工具调用日志</h2><span>{{ run.toolInvocations?.length || 0 }} 次</span></div>
          <pre class="code-block light">{{ toJson(run.toolInvocations || []) }}</pre>
        </div>
      </template>
    </div>
  </section>
</template>
