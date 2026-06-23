<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { Bell, FileClock, Pencil, Save, TestTube2, Trash2, UserPlus } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import StatusBadge from '../components/StatusBadge.vue';
import {
  createModelProvider,
  deleteModelProvider,
  fetchModelProviders,
  testModelProvider,
  updateModelProvider,
  type ModelProviderRequest,
  type ModelProviderSummary,
} from '../api/models';
import { useOverlay } from '../composables/useOverlay';

const { showDrawer, showModal } = useOverlay();
const users = ['admin', '张三', '李四', '王五', '赵六'];

const providers = ref<ModelProviderSummary[]>([]);
const editingProviderId = ref('');
const loading = ref(false);
const errorMessage = ref('');
const testMessage = ref('');

const providerForm = reactive({
  providerCode: 'doubao',
  providerName: '豆包 Doubao',
  providerType: 'openai_compatible',
  baseUrl: 'https://ark.cn-beijing.volces.com/api/v3',
  authType: 'api_key',
  apiKey: '',
  status: 'enabled',
  modelId: '',
  modelCode: 'ep-20260605102340-bwv2d',
  modelName: '豆包接入点 ep-20260605102340-bwv2d',
  modelType: 'chat',
  contextWindow: 32768,
  maxOutputTokens: 4096,
  inputPricePer1k: 0,
  outputPricePer1k: 0,
  supportStream: true,
  supportFunctionCalling: false,
  supportVision: false,
  isDefault: true,
});

async function loadProviders() {
  errorMessage.value = '';
  try {
    providers.value = await fetchModelProviders();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '模型服务商加载失败';
  }
}

function resetForm() {
  editingProviderId.value = '';
  providerForm.providerCode = 'doubao';
  providerForm.providerName = '豆包 Doubao';
  providerForm.providerType = 'openai_compatible';
  providerForm.baseUrl = 'https://ark.cn-beijing.volces.com/api/v3';
  providerForm.authType = 'api_key';
  providerForm.apiKey = '';
  providerForm.status = 'enabled';
  providerForm.modelId = '';
  providerForm.modelCode = 'ep-20260605102340-bwv2d';
  providerForm.modelName = '豆包接入点 ep-20260605102340-bwv2d';
  providerForm.modelType = 'chat';
  providerForm.contextWindow = 32768;
  providerForm.maxOutputTokens = 4096;
  providerForm.inputPricePer1k = 0;
  providerForm.outputPricePer1k = 0;
  providerForm.supportStream = true;
  providerForm.supportFunctionCalling = false;
  providerForm.supportVision = false;
  providerForm.isDefault = true;
}

function editProvider(provider: ModelProviderSummary) {
  const firstChatModel = provider.models.find((model) => model.modelType === 'chat') ?? provider.models[0];
  editingProviderId.value = provider.id;
  providerForm.providerCode = provider.providerCode;
  providerForm.providerName = provider.providerName;
  providerForm.providerType = provider.providerType;
  providerForm.baseUrl = provider.baseUrl;
  providerForm.authType = provider.authType || 'api_key';
  providerForm.apiKey = '';
  providerForm.status = provider.status;
  providerForm.modelId = firstChatModel?.id ?? '';
  providerForm.modelCode = firstChatModel?.modelCode ?? '';
  providerForm.modelName = firstChatModel?.modelName ?? '';
  providerForm.modelType = firstChatModel?.modelType ?? 'chat';
  providerForm.contextWindow = firstChatModel?.contextWindow ?? 32768;
  providerForm.maxOutputTokens = firstChatModel?.maxOutputTokens ?? 4096;
  providerForm.inputPricePer1k = firstChatModel?.inputPricePer1k ?? 0;
  providerForm.outputPricePer1k = firstChatModel?.outputPricePer1k ?? 0;
  providerForm.supportStream = firstChatModel?.supportStream ?? true;
  providerForm.supportFunctionCalling = firstChatModel?.supportFunctionCalling ?? false;
  providerForm.supportVision = firstChatModel?.supportVision ?? false;
  providerForm.isDefault = firstChatModel?.isDefault ?? false;
}

function buildPayload(): ModelProviderRequest {
  return {
    providerCode: providerForm.providerCode,
    providerName: providerForm.providerName,
    providerType: providerForm.providerType,
    baseUrl: providerForm.baseUrl,
    authType: providerForm.authType,
    apiKey: providerForm.apiKey || undefined,
    status: providerForm.status,
    sortOrder: 0,
    models: [
      {
        id: providerForm.modelId || undefined,
        modelCode: providerForm.modelCode,
        modelName: providerForm.modelName,
        modelType: providerForm.modelType,
        contextWindow: providerForm.contextWindow,
        maxOutputTokens: providerForm.maxOutputTokens,
        inputPricePer1k: providerForm.inputPricePer1k,
        outputPricePer1k: providerForm.outputPricePer1k,
        supportStream: providerForm.supportStream,
        supportFunctionCalling: providerForm.supportFunctionCalling,
        supportVision: providerForm.supportVision,
        defaultParams: '{}',
        status: 'enabled',
        isDefault: providerForm.isDefault,
      },
    ],
  };
}

async function saveProvider() {
  loading.value = true;
  errorMessage.value = '';
  testMessage.value = '';
  try {
    if (editingProviderId.value) {
      await updateModelProvider(editingProviderId.value, buildPayload());
    } else {
      await createModelProvider(buildPayload());
    }
    resetForm();
    await loadProviders();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '模型服务商保存失败';
  } finally {
    loading.value = false;
  }
}

async function removeProvider(provider: ModelProviderSummary) {
  loading.value = true;
  errorMessage.value = '';
  try {
    await deleteModelProvider(provider.id);
    await loadProviders();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '模型服务商删除失败';
  } finally {
    loading.value = false;
  }
}

async function runProviderTest(provider: ModelProviderSummary) {
  loading.value = true;
  errorMessage.value = '';
  testMessage.value = '';
  try {
    const result = await testModelProvider(provider.id);
    testMessage.value = result.success
      ? `连通成功，耗时 ${result.latencyMs}ms：${result.responseText ?? ''}`
      : `连通失败：${result.errorMessage ?? '未知错误'}`;
    await loadProviders();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '模型连通性测试失败';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadProviders();
});
</script>

<template>
  <PageHeader title="系统设置" description="用户权限、模型供应商、通知中心与操作审计">
    <template #actions>
      <button class="secondary-button" type="button" @click="showDrawer('notices')"><Bell :size="16" /> 通知中心</button>
      <button class="secondary-button" type="button" @click="showModal('audit')"><FileClock :size="16" /> 操作日志详情</button>
    </template>
  </PageHeader>

  <section class="settings-layout">
    <div class="section-block">
      <div class="section-title">
        <h2>用户与角色权限</h2>
        <button class="primary-button" type="button"><UserPlus :size="16" /> 邀请用户</button>
      </div>
      <table class="data-table">
        <thead><tr><th>用户</th><th>角色</th><th>所属部门</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="(user, index) in users" :key="user">
            <td><b>{{ user }}</b><span class="muted block">{{ user }}@openagentflow.ai</span></td>
            <td><StatusBadge :label="index === 0 ? '超级管理员' : index === 1 ? '系统管理员' : '开发者'" /></td>
            <td>研发中心</td>
            <td><StatusBadge :label="index % 2 ? '离线' : '在线'" /></td>
            <td><button class="secondary-button slim" type="button">编辑</button></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="section-block">
      <div class="section-title"><h2>模型供应商配置</h2><span>OpenAI-compatible / Ollama / Qwen / DeepSeek / Doubao</span></div>
      <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
      <p v-if="testMessage" class="form-success">{{ testMessage }}</p>

      <div class="settings-form">
        <label>服务商编码<input v-model="providerForm.providerCode" /></label>
        <label>服务商名称<input v-model="providerForm.providerName" /></label>
        <label>服务商类型
          <select v-model="providerForm.providerType">
            <option value="openai_compatible">OpenAI-compatible</option>
            <option value="ollama">Ollama</option>
          </select>
        </label>
        <label>Base URL<input v-model="providerForm.baseUrl" /></label>
        <label>认证类型
          <select v-model="providerForm.authType">
            <option value="api_key">API Key</option>
            <option value="none">None</option>
          </select>
        </label>
        <label>API Key<input v-model="providerForm.apiKey" type="password" placeholder="编辑时留空表示不替换" /></label>
        <label>模型编码<input v-model="providerForm.modelCode" /></label>
        <label>模型名称<input v-model="providerForm.modelName" /></label>
        <label>上下文窗口<input v-model.number="providerForm.contextWindow" type="number" /></label>
        <label>最大输出 Token<input v-model.number="providerForm.maxOutputTokens" type="number" /></label>
        <label>输入每千 Token 单价<input v-model.number="providerForm.inputPricePer1k" type="number" min="0" step="0.000001" /></label>
        <label>输出每千 Token 单价<input v-model.number="providerForm.outputPricePer1k" type="number" min="0" step="0.000001" /></label>
        <label class="check-line"><input v-model="providerForm.supportStream" type="checkbox" /> 支持流式输出</label>
        <label class="check-line"><input v-model="providerForm.isDefault" type="checkbox" /> 设为默认聊天模型</label>
      </div>
      <div class="toolbar compact">
        <button class="primary-button" type="button" :disabled="loading" @click="saveProvider"><Save :size="16" /> {{ editingProviderId ? '保存修改' : '新增服务商' }}</button>
        <button class="secondary-button" type="button" @click="resetForm">重置</button>
      </div>

      <div class="provider-grid">
        <article v-for="provider in providers" :key="provider.id" class="provider-card">
          <div><h3>{{ provider.providerName }}</h3><span>{{ provider.providerType }}</span></div>
          <StatusBadge :label="provider.healthStatus" :tone="provider.healthStatus === 'healthy' ? 'success' : provider.healthStatus === 'unhealthy' ? 'danger' : 'warning'" />
          <p>{{ provider.models.length }} 个模型 · {{ provider.baseUrl }}</p>
          <p class="mono">{{ provider.keyMask || '未配置 API Key' }}</p>
          <div class="row-actions">
            <button class="secondary-button slim" type="button" @click="editProvider(provider)"><Pencil :size="14" /> 编辑</button>
            <button class="secondary-button slim" type="button" :disabled="loading" @click="runProviderTest(provider)"><TestTube2 :size="14" /> 测试</button>
            <button class="secondary-button slim danger-text" type="button" :disabled="loading" @click="removeProvider(provider)"><Trash2 :size="14" /> 删除</button>
          </div>
        </article>
      </div>

      <table class="data-table">
        <thead><tr><th>供应商</th><th>模型</th><th>类型</th><th>单价 / 1K Token</th><th>流式</th><th>状态</th></tr></thead>
        <tbody>
          <template v-for="provider in providers" :key="provider.id">
            <tr v-for="model in provider.models" :key="model.id">
              <td><b>{{ provider.providerName }}</b></td>
              <td>{{ model.modelName }}<span class="muted block mono">{{ model.modelCode }}</span></td>
              <td>{{ model.modelType }}</td>
              <td>输入 ¥{{ Number(model.inputPricePer1k || 0).toFixed(6) }}<span class="muted block">输出 ¥{{ Number(model.outputPricePer1k || 0).toFixed(6) }}</span></td>
              <td>{{ model.supportStream ? '支持' : '不支持' }}</td>
              <td><StatusBadge :label="model.status" /></td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>
  </section>
</template>
