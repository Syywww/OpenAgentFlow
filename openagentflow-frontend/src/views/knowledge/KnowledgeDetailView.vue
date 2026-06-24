<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, ClipboardList, RefreshCw, Search, Upload } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import PaginationBar from '../../components/PaginationBar.vue';
import StatCard from '../../components/StatCard.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import {
  fetchKnowledgeBase,
  fetchKnowledgeDocumentStatus,
  retrievalTest,
  uploadKnowledgeDocument,
  type KnowledgeBaseDetail,
  type KnowledgeDocumentSummary,
  type KnowledgeSource,
} from '../../api/knowledge';
import { useOverlay } from '../../composables/useOverlay';
import { usePagination } from '../../composables/usePagination';

const route = useRoute();
const router = useRouter();
const { toast } = useOverlay();
const detail = ref<KnowledgeBaseDetail | null>(null);
const selectedDocumentId = ref('');
const fileInput = ref<HTMLInputElement | null>(null);
const loading = ref(false);
const uploading = ref(false);
const pollingDocumentId = ref('');
const pollTimer = ref<number | null>(null);
const query = ref('请根据知识库总结核心内容');
const sources = ref<KnowledgeSource[]>([]);
const retrievalLatency = ref(0);

const documents = computed(() => detail.value?.documents ?? []);
const selectedDocument = computed(() => detail.value?.documents.find((doc) => doc.id === selectedDocumentId.value));
const visibleChunks = computed(() => {
  if (!detail.value) return [];
  if (!selectedDocumentId.value) return detail.value.chunks;
  return detail.value.chunks.filter((chunk) => chunk.documentId === selectedDocumentId.value);
});
const { currentPage: documentPage, pagedItems: pagedDocuments } = usePagination(documents);
const { currentPage: sourcePage, pagedItems: pagedSources } = usePagination(sources);
const { currentPage: chunkPage, pagedItems: pagedChunks } = usePagination(visibleChunks);
const processing = computed(() => selectedDocument.value?.parseStatus === 'processing');

onMounted(() => {
  void loadDetail();
});

onUnmounted(() => {
  stopPolling();
});

async function loadDetail() {
  loading.value = true;
  try {
    detail.value = await fetchKnowledgeBase(String(route.params.id));
    selectedDocumentId.value = selectedDocumentId.value || detail.value.documents[0]?.id || '';
    const processingDoc = detail.value.documents.find((doc) => doc.parseStatus === 'processing');
    if (processingDoc) {
      startPolling(processingDoc.id);
    }
  } finally {
    loading.value = false;
  }
}

function chooseFile() {
  fileInput.value?.click();
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  uploading.value = true;
  try {
    const result = await uploadKnowledgeDocument(String(route.params.id), file);
    upsertDocument(result.document);
    selectedDocumentId.value = result.document.id;
    toast(result.message || '文件已上传，后台开始处理');
    startPolling(result.document.id);
  } finally {
    uploading.value = false;
    input.value = '';
  }
}

async function handleRetrievalTest() {
  if (!query.value.trim()) {
    return;
  }
  const result = await retrievalTest(String(route.params.id), query.value.trim(), 5, 0.55);
  sources.value = result.sources;
  retrievalLatency.value = result.latencyMs;
  toast(`检索完成，命中 ${result.sources.length} 条来源`);
}

function startPolling(documentId: string) {
  stopPolling();
  pollingDocumentId.value = documentId;
  void pollDocument();
  pollTimer.value = window.setInterval(() => {
    void pollDocument();
  }, 1500);
}

function stopPolling() {
  if (pollTimer.value) {
    window.clearInterval(pollTimer.value);
    pollTimer.value = null;
  }
}

async function pollDocument() {
  if (!pollingDocumentId.value) {
    return;
  }
  const doc = await fetchKnowledgeDocumentStatus(String(route.params.id), pollingDocumentId.value);
  upsertDocument(doc);
  if (doc.parseStatus === 'parsed' || doc.parseStatus === 'failed') {
    stopPolling();
    await loadDetail();
    upsertDocument(doc);
  }
}

function upsertDocument(doc: KnowledgeDocumentSummary) {
  if (!detail.value) {
    return;
  }
  const index = detail.value.documents.findIndex((item) => item.id === doc.id);
  if (index >= 0) {
    detail.value.documents[index] = doc;
  } else {
    detail.value.documents.unshift(doc);
  }
}

function formatSize(size?: number) {
  if (!size) return '-';
  if (size > 1024 * 1024) return `${(size / 1024 / 1024).toFixed(2)} MB`;
  if (size > 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${size} B`;
}

function statusLabel(status?: string) {
  if (status === 'parsed') return '已完成';
  if (status === 'processing') return '处理中';
  if (status === 'failed') return '失败';
  if (status === 'active') return '启用';
  return status || '未知';
}

function syncLabel(doc?: KnowledgeDocumentSummary) {
  if (!doc) return '未开始';
  if (doc.embeddingFallbackUsed) return '本地兜底';
  if (doc.embeddingDimension) return `真实模型 ${doc.embeddingDimension} 维`;
  if (doc.parseStatus === 'processing') return '等待模型返回';
  return '未生成';
}
</script>

<template>
  <PageHeader
    :title="detail ? detail.kbName : '知识库详情'"
    :description="detail ? `${detail.description || '暂无描述'} · ${detail.milvusCollectionName || '未绑定集合'}` : '加载中'"
  >
    <template #actions>
      <button class="secondary-button" type="button" @click="router.push('/knowledge')"><ArrowLeft :size="16" /> 返回</button>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadDetail"><RefreshCw :size="16" /> 刷新</button>
      <button class="primary-button" type="button" :disabled="uploading" @click="chooseFile"><Upload :size="16" /> {{ uploading ? '上传中' : '上传文档' }}</button>
      <input ref="fileInput" type="file" hidden @change="handleFileChange" />
    </template>
  </PageHeader>

  <section v-if="detail" class="knowledge-detail">
    <aside class="document-list">
      <button
        v-for="doc in pagedDocuments"
        :key="doc.id"
        class="document-item"
        :class="{ active: selectedDocumentId === doc.id }"
        type="button"
        @click="selectedDocumentId = doc.id"
      >
        <b>{{ doc.docName }}</b>
        <span>{{ formatSize(doc.fileSize) }} · {{ statusLabel(doc.parseStatus) }} · {{ doc.progressPercent || 0 }}%</span>
      </button>
      <PaginationBar v-model:page="documentPage" :total="documents.length" />
      <div v-if="detail.documents.length === 0" class="empty-state">暂无文档，上传后会自动解析和向量化</div>
    </aside>

    <div class="section-block">
      <div class="tabs">
        <button class="tab active" type="button">切片预览</button>
        <button class="tab" type="button">检索测试</button>
        <button class="tab" type="button">引用来源</button>
      </div>

      <div v-if="selectedDocument" class="process-panel">
        <div class="process-header">
          <div>
            <b>{{ selectedDocument.processStageLabel || statusLabel(selectedDocument.parseStatus) }}</b>
            <span>{{ selectedDocument.lastMessage || '等待处理日志' }}</span>
          </div>
          <StatusBadge :label="syncLabel(selectedDocument)" />
        </div>
        <div class="progress-track">
          <div class="progress-bar" :style="{ width: `${selectedDocument.progressPercent || 0}%` }"></div>
        </div>
        <div class="process-meta">
          <span>状态：{{ statusLabel(selectedDocument.parseStatus) }}</span>
          <span>接口：{{ selectedDocument.embeddingApi || '-' }}</span>
          <span>模型：{{ selectedDocument.embeddingModelCode || '-' }}</span>
          <span>Milvus：{{ selectedDocument.milvusSynced ? '已同步' : processing ? '同步中' : '未同步' }}</span>
        </div>
        <button
          v-if="selectedDocument.asyncTaskId"
          class="secondary-button"
          type="button"
          @click="router.push('/tasks')"
        >
          <ClipboardList :size="16" /> 查看任务中心日志
        </button>
        <ul class="process-log">
          <li v-for="line in selectedDocument.processLogs || []" :key="line">{{ line }}</li>
        </ul>
        <p v-if="selectedDocument.parseError" class="error-text">{{ selectedDocument.parseError }}</p>
      </div>

      <div class="metric-grid compact">
        <StatCard label="文档总数" :value="String(detail.documentCount)" detail="已上传" icon="Library" tone="info" />
        <StatCard label="切片总数" :value="String(detail.chunkCount)" detail="已入库" icon="Braces" tone="success" />
        <StatCard label="向量总数" :value="String(detail.embeddingCount)" detail="MySQL + Milvus" icon="Activity" tone="neutral" />
        <StatCard label="检索耗时" :value="`${retrievalLatency}ms`" detail="最近一次" icon="ShieldCheck" tone="warning" />
      </div>

      <div class="filter-row">
        <input v-model="query" placeholder="输入检索测试问题" />
        <button class="primary-button" type="button" @click="handleRetrievalTest"><Search :size="16" /> 检索测试</button>
      </div>

      <article v-for="source in pagedSources" :key="source.chunkId" class="chunk-item">
        <div>
          <b>{{ source.documentName }} / 分片 {{ source.chunkNo }}</b>
          <StatusBadge :label="`相似度 ${source.score.toFixed(4)}`" />
        </div>
        <p>{{ source.quoteText }}</p>
      </article>
      <PaginationBar v-model:page="sourcePage" :total="sources.length" />

      <article v-for="chunk in pagedChunks" :key="chunk.id" class="chunk-item">
        <div>
          <b>{{ chunk.title || `分片 ${chunk.chunkNo}` }}</b>
          <StatusBadge :label="chunk.syncStatus === 'synced' ? '已写入 Milvus' : chunk.syncStatus || '待同步'" />
        </div>
        <p>{{ chunk.content }}</p>
        <span>{{ chunk.tokenCount }} tokens</span>
      </article>
      <PaginationBar v-model:page="chunkPage" :total="visibleChunks.length" />
      <div v-if="visibleChunks.length === 0 && sources.length === 0" class="empty-state">
        {{ selectedDocument ? '当前文档暂无切片' : '暂无可预览分片' }}
      </div>
    </div>
  </section>
</template>
