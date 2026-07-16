<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ArrowLeft, ArrowRight, CheckCircle2, PackageCheck, X } from 'lucide-vue-next';
import type { ModelConfigSummary } from '../api/models';
import type { TemplateDetail, TemplateInstallRequest, TemplateInstallSummary } from '../api/templates';
import type { WorkspaceSummary } from '../api/workspaces';
import StatusBadge from './StatusBadge.vue';

const props = defineProps<{
  template: TemplateDetail;
  workspaces: WorkspaceSummary[];
  models: ModelConfigSummary[];
  submitting: boolean;
  install?: TemplateInstallSummary | null;
}>();

const emit = defineEmits<{
  close: [];
  submit: [payload: TemplateInstallRequest];
}>();

const step = ref(1);
const form = reactive({
  templateVersionId: '',
  workspaceId: '',
  namePrefix: '',
  defaultChatModelId: '',
  embeddingModelId: '',
  modelMappingText: '{}',
  credentialsReady: false,
});

const chatModels = computed(() => props.models.filter((item) => item.modelType === 'chat'));
const embeddingModels = computed(() => props.models.filter((item) => item.modelType === 'embedding'));
const selectedVersion = computed(() => props.template.versions.find((item) => item.id === form.templateVersionId));
const canNext = computed(() => step.value === 1 ? Boolean(form.workspaceId && form.templateVersionId) : true);

watch(() => props.template.id, () => {
  step.value = 1;
  form.templateVersionId = props.template.currentVersionId || props.template.versions[0]?.id || '';
  form.workspaceId = props.workspaces.find((item) => item.defaultFlag)?.id || props.workspaces[0]?.id || '';
  form.namePrefix = '';
  form.defaultChatModelId = chatModels.value[0]?.id || '';
  form.embeddingModelId = embeddingModels.value[0]?.id || '';
  form.modelMappingText = '{}';
  form.credentialsReady = false;
}, { immediate: true });

function submit() {
  let mapping: Record<string, string> = {};
  try {
    mapping = JSON.parse(form.modelMappingText || '{}') as Record<string, string>;
  } catch {
    return;
  }
  if (form.defaultChatModelId) mapping.default = form.defaultChatModelId;
  emit('submit', {
    templateVersionId: form.templateVersionId,
    workspaceId: form.workspaceId,
    namePrefix: form.namePrefix,
    modelMapping: mapping,
    embeddingModelId: form.embeddingModelId || undefined,
    credentialsReady: form.credentialsReady,
    idempotencyKey: `${props.template.id}:${form.workspaceId}:${Date.now()}`,
  });
}
</script>

<template>
  <div class="overlay-backdrop" @click.self="emit('close')">
    <section class="modal-panel template-install-modal">
      <header class="overlay-header">
        <div><h2>安装 {{ template.templateName }}</h2><p class="muted">资源会复制为目标工作空间的独立副本</p></div>
        <button class="icon-button" type="button" title="关闭" @click="emit('close')"><X :size="18" /></button>
      </header>

      <div v-if="!install" class="install-stepper">
        <span v-for="item in 3" :key="item" :class="{ active: step === item, done: step > item }">{{ item }}</span>
      </div>

      <template v-if="install">
        <div class="install-progress-state">
          <PackageCheck :size="38" />
          <h3>{{ install.installStatus === 'success' ? '安装完成' : install.currentMessage || '正在安装解决方案' }}</h3>
          <div class="progress-track"><span :style="{ width: `${install.progressPercent}%` }" /></div>
          <b>{{ install.progressPercent }}%</b>
          <StatusBadge :label="install.installStatus" :tone="install.installStatus === 'success' ? 'success' : install.installStatus === 'failed' || install.installStatus === 'rollback' ? 'danger' : 'info'" />
          <p v-if="install.errorMessage" class="form-error">{{ install.errorMessage }}</p>
          <p v-if="install.installStatus === 'success' && !form.credentialsReady" class="muted">外部凭证尚未补齐，已安装 Agent 保持草稿状态。</p>
        </div>
      </template>

      <template v-else-if="step === 1">
        <div class="form-grid">
          <label>模板版本<select v-model="form.templateVersionId"><option v-for="version in template.versions.filter((item) => item.status === 'published')" :key="version.id" :value="version.id">{{ version.versionNo }} · {{ version.versionName || '正式版本' }}</option></select></label>
          <label>目标工作空间<select v-model="form.workspaceId"><option v-for="workspace in workspaces.filter((item) => item.canManage)" :key="workspace.id" :value="workspace.id">{{ workspace.workspaceName }}</option></select></label>
          <label class="wide">资源名称前缀<input v-model="form.namePrefix" placeholder="例如：客服中心-（可选）" /></label>
        </div>
        <div class="install-summary-band"><span>版本</span><b>{{ selectedVersion?.versionNo }}</b><span>资源数</span><b>{{ template.resources.length }}</b><span>包大小</span><b>{{ Math.ceil((selectedVersion?.packageSize || 0) / 1024) }} KB</b></div>
      </template>

      <template v-else-if="step === 2">
        <div class="form-grid">
          <label>默认对话模型<select v-model="form.defaultChatModelId"><option value="">安装后手动选择</option><option v-for="model in chatModels" :key="model.id" :value="model.id">{{ model.providerName }} / {{ model.modelName }}</option></select></label>
          <label>Embedding 模型<select v-model="form.embeddingModelId"><option value="">稍后重新向量化</option><option v-for="model in embeddingModels" :key="model.id" :value="model.id">{{ model.providerName }} / {{ model.modelName }}</option></select></label>
          <label class="wide">精确模型替代映射 JSON<textarea v-model="form.modelMappingText" class="code-editor compact" rows="5" placeholder='{"来源模型ID":"目标模型ID"}' /></label>
          <label class="checkbox-row wide"><input v-model="form.credentialsReady" type="checkbox" /> 已确认外部工具和 MCP 凭证可以在安装后补充</label>
        </div>
      </template>

      <template v-else>
        <div class="install-confirm-list">
          <div><CheckCircle2 :size="18" /><span>独立复制 Agent、团队、Prompt、工具、工作流和 Memory</span></div>
          <div><CheckCircle2 :size="18" /><span>完整复制知识文档、切片、MinIO 对象和兼容 Milvus 向量</span></div>
          <div><CheckCircle2 :size="18" /><span>API Key、认证头、MCP 密钥不会从模板带入</span></div>
          <div><CheckCircle2 :size="18" /><span>失败后自动补偿本次创建的全部资源</span></div>
        </div>
      </template>

      <div class="toolbar compact">
        <button v-if="!install && step > 1" class="secondary-button" type="button" @click="step--"><ArrowLeft :size="15" /> 上一步</button>
        <span class="toolbar-spacer" />
        <button v-if="install" class="primary-button" type="button" @click="emit('close')">完成</button>
        <button v-else-if="step < 3" class="primary-button" type="button" :disabled="!canNext" @click="step++">下一步 <ArrowRight :size="15" /></button>
        <button v-else class="primary-button" type="button" :disabled="submitting" @click="submit"><PackageCheck :size="16" /> 开始异步安装</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.template-install-modal { width: min(760px, calc(100vw - 32px)); }
.install-stepper { display: flex; justify-content: center; gap: 90px; margin: 4px 0 22px; }
.install-stepper span { display: grid; place-items: center; width: 28px; height: 28px; border: 1px solid var(--border-color); border-radius: 50%; color: var(--text-muted); }
.install-stepper span.active, .install-stepper span.done { color: white; background: var(--primary-color); border-color: var(--primary-color); }
.install-summary-band { display: grid; grid-template-columns: repeat(6, max-content); gap: 8px 18px; padding: 12px; margin-top: 16px; border-block: 1px solid var(--border-color); }
.install-progress-state { display: grid; justify-items: center; gap: 12px; padding: 36px 12px; text-align: center; }
.progress-track { width: min(480px, 90%); height: 8px; overflow: hidden; background: var(--surface-subtle); border-radius: 4px; }
.progress-track span { display: block; height: 100%; background: var(--primary-color); transition: width .25s ease; }
.install-confirm-list { display: grid; gap: 12px; padding: 14px 0; }
.install-confirm-list div { display: flex; align-items: center; gap: 10px; }
.toolbar-spacer { flex: 1; }
@media (max-width: 680px) { .install-stepper { gap: 44px; } .install-summary-band { grid-template-columns: auto 1fr; } }
</style>
