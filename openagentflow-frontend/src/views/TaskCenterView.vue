<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Eye, PauseCircle, PlayCircle, RefreshCw, RotateCcw, Search, X } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import StatCard from '../components/StatCard.vue';
import StatusBadge from '../components/StatusBadge.vue';
import {
  cancelTask,
  fetchTask,
  fetchTaskOverview,
  fetchTasks,
  retryTask,
  type AsyncTaskDetail,
  type AsyncTaskOverview,
  type AsyncTaskSummary,
} from '../api/tasks';
import { fetchWorkspaces, type WorkspaceSummary } from '../api/workspaces';

const loading = ref(false);
const errorMessage = ref('');
const overview = ref<AsyncTaskOverview | null>(null);
const tasks = ref<AsyncTaskSummary[]>([]);
const selectedTask = ref<AsyncTaskDetail | null>(null);
const workspaces = ref<WorkspaceSummary[]>([]);
const total = ref(0);
const detailModalOpen = ref(false);

const filters = reactive({
  status: 'all',
  taskType: 'all',
  workspaceId: 'all',
  keyword: '',
  pageNo: 1,
  pageSize: 10,
});

const taskTypes = [
  { value: 'all', label: '全部类型' },
  { value: 'DOCUMENT_PROCESS', label: '知识文档处理' },
  { value: 'EVALUATION_RUN', label: '评测批量运行' },
  { value: 'MCP_DISCOVERY', label: 'MCP 能力发现' },
  { value: 'DATA_IMPORT', label: '数据导入' },
];

const statusOptions = [
  { value: 'all', label: '全部状态' },
  { value: 'pending', label: '排队中' },
  { value: 'running', label: '运行中' },
  { value: 'success', label: '成功' },
  { value: 'failed', label: '失败' },
  { value: 'canceled', label: '已取消' },
];

const selectedLogs = computed(() => selectedTask.value?.logs || []);

onMounted(() => {
  void loadData();
});

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [overviewResult, taskResult, workspaceResult] = await Promise.all([
      fetchTaskOverview(),
      fetchTasks(filters),
      fetchWorkspaces(),
    ]);
    overview.value = overviewResult;
    tasks.value = taskResult.records;
    total.value = taskResult.total;
    workspaces.value = workspaceResult;
    if (tasks.value[0]) {
      await selectTask(tasks.value[0]);
    } else {
      selectedTask.value = null;
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '异步任务数据加载失败';
  } finally {
    loading.value = false;
  }
}

async function reloadList() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [overviewResult, taskResult] = await Promise.all([fetchTaskOverview(), fetchTasks(filters)]);
    overview.value = overviewResult;
    tasks.value = taskResult.records;
    total.value = taskResult.total;
    if (selectedTask.value) {
      selectedTask.value = await fetchTask(selectedTask.value.id);
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '异步任务刷新失败';
  } finally {
    loading.value = false;
  }
}

async function changeTaskPage(page: number) {
  filters.pageNo = page;
  await reloadList();
}

async function searchTasks() {
  filters.pageNo = 1;
  await reloadList();
}

async function selectTask(task: AsyncTaskSummary) {
  selectedTask.value = await fetchTask(task.id);
}

async function openTaskDetail(task: AsyncTaskSummary) {
  await selectTask(task);
  detailModalOpen.value = true;
}

function closeTaskDetail() {
  detailModalOpen.value = false;
}

async function handleCancel(task: AsyncTaskSummary) {
  loading.value = true;
  errorMessage.value = '';
  try {
    selectedTask.value = await cancelTask(task.id);
    await reloadList();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '任务取消失败';
  } finally {
    loading.value = false;
  }
}

async function handleRetry(task: AsyncTaskSummary) {
  loading.value = true;
  errorMessage.value = '';
  try {
    selectedTask.value = await retryTask(task.id);
    await reloadList();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '任务重试失败';
  } finally {
    loading.value = false;
  }
}

function canCancel(task: AsyncTaskSummary) {
  return ['pending', 'running'].includes(task.status);
}

function canRetry(task: AsyncTaskSummary) {
  return ['failed', 'canceled'].includes(task.status) && task.retryCount < task.maxRetries;
}

function statusLabel(status?: string) {
  const map: Record<string, string> = {
    pending: '排队中',
    running: '运行中',
    success: '成功',
    failed: '失败',
    canceled: '已取消',
  };
  return map[status || ''] || status || '未知';
}

function statusTone(status?: string) {
  if (status === 'success') return 'success';
  if (status === 'failed') return 'danger';
  if (status === 'running') return 'info';
  if (status === 'pending') return 'warning';
  return 'neutral';
}

function formatTime(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
}

function formatJson(value?: Record<string, unknown>) {
  if (!value || Object.keys(value).length === 0) return '{}';
  return JSON.stringify(value, null, 2);
}
</script>

<template>
  <PageHeader title="异步任务中心" description="统一查看后台任务进度、日志、失败原因、取消与重试">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="reloadList">
        <RefreshCw :size="16" /> 刷新
      </button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

  <section class="metric-grid">
    <StatCard label="全部任务" :value="String(overview?.totalCount || 0)" detail="当前可见任务" icon="Gauge" tone="neutral" />
    <StatCard label="运行中" :value="String(overview?.runningCount || 0)" detail="正在执行" icon="Activity" tone="info" />
    <StatCard label="排队中" :value="String(overview?.pendingCount || 0)" detail="等待线程池调度" icon="Timer" tone="warning" />
    <StatCard label="失败任务" :value="String(overview?.failedCount || 0)" detail="可进入详情重试" icon="ShieldAlert" tone="danger" />
  </section>

  <section class="section-block">
    <div class="filter-row">
      <select v-model="filters.status" @change="searchTasks">
        <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
      <select v-model="filters.taskType" @change="searchTasks">
        <option v-for="item in taskTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
      <select v-model="filters.workspaceId" @change="searchTasks">
        <option value="all">全部空间</option>
        <option v-for="workspace in workspaces" :key="workspace.id" :value="workspace.id">{{ workspace.workspaceName }}</option>
      </select>
      <label class="search-input">
        <Search :size="16" />
        <input v-model="filters.keyword" placeholder="搜索任务名称、编码、业务ID" @keyup.enter="searchTasks" />
      </label>
      <button class="secondary-button" type="button" :disabled="loading" @click="searchTasks">
        <Search :size="16" /> 查询
      </button>
    </div>
  </section>

  <section class="section-block task-center-panel">
    <div class="section-title">
      <h2>任务队列</h2>
      <span>共 {{ total }} 条，点击任务行或详情按钮查看完整执行信息</span>
    </div>
    <table class="data-table">
      <thead>
        <tr>
          <th>任务</th>
          <th>类型</th>
          <th>状态</th>
          <th>进度</th>
          <th>空间</th>
          <th>创建时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="task in tasks"
          :key="task.id"
          :class="{ selected: selectedTask?.id === task.id }"
          @click="openTaskDetail(task)"
        >
          <td><b>{{ task.taskName }}</b><span class="muted block">{{ task.taskCode }}</span></td>
          <td>{{ task.taskTypeLabel }}</td>
          <td><StatusBadge :label="statusLabel(task.status)" :tone="statusTone(task.status)" /></td>
          <td>
            <div class="progress-track small">
              <div class="progress-bar" :style="{ width: `${task.progressPercent || 0}%` }"></div>
            </div>
            <span class="muted">{{ task.progressPercent || 0 }}%</span>
          </td>
          <td>{{ task.workspaceName || '-' }}</td>
          <td>{{ formatTime(task.createdAt) }}</td>
          <td>
            <div class="table-actions">
              <button class="icon-button" type="button" title="详情" @click.stop="openTaskDetail(task)">
                <Eye :size="16" />
              </button>
              <button class="icon-button" type="button" title="取消" :disabled="!canCancel(task)" @click.stop="handleCancel(task)">
                <PauseCircle :size="16" />
              </button>
              <button class="icon-button" type="button" title="重试" :disabled="!canRetry(task)" @click.stop="handleRetry(task)">
                <RotateCcw :size="16" />
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <PaginationBar :page="filters.pageNo" :total="total" @update:page="changeTaskPage" />
  </section>

  <div v-if="detailModalOpen" class="overlay-backdrop" @click.self="closeTaskDetail">
    <section class="modal-panel task-detail-modal">
      <header class="overlay-header">
        <div>
          <h2>任务详情</h2>
          <p class="muted">{{ selectedTask?.taskTypeLabel || '请选择任务' }}</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closeTaskDetail"><X :size="18" /></button>
      </header>

      <div v-if="selectedTask" class="task-detail-content">
        <div class="trace-meta">
          <span>状态</span><b>{{ statusLabel(selectedTask.status) }}</b>
          <span>当前阶段</span><b>{{ selectedTask.currentStage || '-' }}</b>
          <span>进度</span><b>{{ selectedTask.progressPercent || 0 }}%</b>
          <span>重试</span><b>{{ selectedTask.retryCount || 0 }} / {{ selectedTask.maxRetries || 0 }}</b>
          <span>开始</span><b>{{ formatTime(selectedTask.startedAt) }}</b>
          <span>结束</span><b>{{ formatTime(selectedTask.finishedAt) }}</b>
        </div>

        <div class="section-title compact-title">
          <h2>当前消息</h2>
        </div>
        <p class="muted">{{ selectedTask.currentMessage || '-' }}</p>
        <p v-if="selectedTask.errorMessage" class="form-error">{{ selectedTask.errorMessage }}</p>

        <div class="section-title compact-title">
          <h2>执行日志</h2>
        </div>
        <div class="mini-timeline task-log-list">
          <div v-for="log in selectedLogs" :key="log.id">
            <PlayCircle :size="15" />
            <span>{{ formatTime(log.createdAt) }}</span>
            <b>{{ log.stage || log.logLevel }}</b>
            <p>{{ log.message }}</p>
          </div>
        </div>

        <div class="section-title compact-title">
          <h2>请求参数</h2>
        </div>
        <pre class="json-preview">{{ formatJson(selectedTask.requestPayload) }}</pre>

        <div class="section-title compact-title">
          <h2>结果数据</h2>
        </div>
        <pre class="json-preview">{{ formatJson(selectedTask.resultPayload) }}</pre>
      </div>

      <div v-else class="empty-state">暂无任务</div>
    </section>
  </div>
</template>
