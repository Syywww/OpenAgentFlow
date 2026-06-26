<script setup lang="ts">
import { computed } from 'vue';
import { ChevronLeft, ChevronRight } from 'lucide-vue-next';

const props = withDefaults(defineProps<{
  page: number;
  total: number;
  pageSize?: number;
  compact?: boolean;
}>(), {
  pageSize: 10,
  compact: false,
});

const emit = defineEmits<{
  'update:page': [value: number];
}>();

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)));
const start = computed(() => (props.total === 0 ? 0 : (props.page - 1) * props.pageSize + 1));
const end = computed(() => Math.min(props.total, props.page * props.pageSize));

function go(page: number) {
  const next = Math.min(Math.max(page, 1), totalPages.value);
  emit('update:page', next);
}
</script>

<template>
  <div class="pagination-bar" :class="{ compact }">
    <span>共 {{ total }} 条，每页 {{ pageSize }} 条，当前 {{ start }}-{{ end }}</span>
    <div class="pagination-actions">
      <button class="secondary-button slim" type="button" :disabled="page <= 1" @click="go(page - 1)">
        <ChevronLeft :size="14" /> 上一页
      </button>
      <b>{{ page }} / {{ totalPages }}</b>
      <button class="secondary-button slim" type="button" :disabled="page >= totalPages" @click="go(page + 1)">
        下一页 <ChevronRight :size="14" />
      </button>
    </div>
  </div>
</template>
