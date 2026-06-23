<script setup lang="ts">
import { computed } from 'vue';
import type { StatusTone } from '../types';

const props = defineProps<{
  label: string;
  tone?: StatusTone;
}>();

const resolvedTone = computed<StatusTone>(() => {
  if (props.tone) return props.tone;
  if (['成功', '正常', '运行中', '启用', '已发布', '健康', '在线'].some((key) => props.label.includes(key))) return 'success';
  if (['失败', '异常', '停用', '高风险', '连接异常'].some((key) => props.label.includes(key))) return 'danger';
  if (['中风险', '索引中', '已暂停', '开发中', '警告', '草稿'].some((key) => props.label.includes(key))) return 'warning';
  if (['低风险', 'RAG', '问答', '客服', '知识管理'].some((key) => props.label.includes(key))) return 'info';
  return 'neutral';
});
</script>

<template>
  <span class="status-badge" :class="`status-${resolvedTone}`">{{ label }}</span>
</template>
