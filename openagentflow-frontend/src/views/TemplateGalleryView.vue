<script setup lang="ts">
import { Plus } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { useOverlay } from '../composables/useOverlay';

const { showModal } = useOverlay();
const templates = ['知识库问答', 'SQL 查询', '日报助手', '合同审查', '客服助手', '创建空白 Agent'];
</script>

<template>
  <PageHeader title="模板广场" description="选择合适的 Agent 模板，快速生成 Prompt、工具绑定和推荐参数">
    <template #actions>
      <button class="primary-button" type="button" @click="showModal('new-agent')"><Plus :size="16" /> 创建空白 Agent</button>
    </template>
  </PageHeader>

  <section class="filter-row">
    <input placeholder="搜索模板名称或场景" />
    <button class="primary-button" type="button">全部</button>
    <button class="secondary-button" type="button">办公助手</button>
    <button class="secondary-button" type="button">数据分析</button>
    <button class="secondary-button" type="button">客服服务</button>
    <button class="secondary-button" type="button">开发运维</button>
  </section>

  <section class="template-gallery">
    <article v-for="(item, index) in templates" :key="item" class="template-card">
      <div class="template-mark">{{ index + 1 }}</div>
      <h2>{{ item }}</h2>
      <p>快速创建 {{ item }} 类型智能体，内置 Prompt、工具与推荐配置。</p>
      <div class="badge-row"><StatusBadge :label="index % 2 ? '自动化' : '知识管理'" /><StatusBadge label="推荐" /></div>
      <button class="primary-button full" type="button" @click="showModal('new-agent')">使用模板</button>
    </article>
  </section>
</template>
