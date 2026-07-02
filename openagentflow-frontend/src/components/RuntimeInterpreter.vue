<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import StatusBadge from './StatusBadge.vue';
import type { StatusTone } from '../types';

type RuntimePhaseStatus = 'pending' | 'running' | 'success' | 'warning' | 'danger' | 'neutral';

interface RuntimePhase {
  id: string;
  label: string;
  status: RuntimePhaseStatus;
  summary: string;
  reason?: string;
  detail?: string;
  metric?: string;
  evidence?: string[];
}

const props = defineProps<{
  title: string;
  subtitle?: string;
  phases: readonly RuntimePhase[];
  compact?: boolean;
}>();

const selectedId = ref('');

const selectedPhase = computed(() => props.phases.find((phase) => phase.id === selectedId.value) || props.phases[0]);
const completedCount = computed(() => props.phases.filter((phase) => ['success', 'warning', 'danger'].includes(phase.status)).length);
const activePhase = computed(() => props.phases.find((phase) => phase.status === 'running') || selectedPhase.value);

watch(
  () => props.phases.map((phase) => `${phase.id}:${phase.status}`).join('|'),
  () => {
    if (!props.phases.some((phase) => phase.id === selectedId.value)) {
      selectedId.value = activePhase.value?.id || props.phases[0]?.id || '';
    }
    if (activePhase.value?.status === 'running') {
      selectedId.value = activePhase.value.id;
    }
  },
  { immediate: true },
);

function badgeLabel(status: RuntimePhaseStatus) {
  if (status === 'running') return '运行中';
  if (status === 'success') return '通过';
  if (status === 'warning') return '需关注';
  if (status === 'danger') return '异常';
  if (status === 'neutral') return '跳过';
  return '待执行';
}

function badgeTone(status: RuntimePhaseStatus): StatusTone {
  if (status === 'success') return 'success';
  if (status === 'warning') return 'warning';
  if (status === 'danger') return 'danger';
  if (status === 'running') return 'info';
  return 'neutral';
}
</script>

<template>
  <section class="runtime-interpreter" :class="{ compact }">
    <div class="runtime-interpreter-head">
      <div>
        <h2>{{ title }}</h2>
        <span v-if="subtitle">{{ subtitle }}</span>
      </div>
      <StatusBadge :label="`${completedCount}/${phases.length}`" tone="info" />
    </div>

    <div class="runtime-chain">
      <button
        v-for="(phase, index) in phases"
        :key="phase.id"
        class="runtime-node"
        :class="[phase.status, { active: selectedPhase?.id === phase.id }]"
        type="button"
        @click="selectedId = phase.id"
      >
        <i>{{ index + 1 }}</i>
        <b>{{ phase.label }}</b>
        <small>{{ phase.summary }}</small>
      </button>
    </div>

    <div v-if="selectedPhase" class="runtime-phase-detail">
      <div class="runtime-phase-title">
        <b>{{ selectedPhase.label }}</b>
        <StatusBadge :label="badgeLabel(selectedPhase.status)" :tone="badgeTone(selectedPhase.status)" />
      </div>
      <p>{{ selectedPhase.reason || selectedPhase.detail || selectedPhase.summary }}</p>
      <div v-if="selectedPhase.metric" class="runtime-phase-metric">{{ selectedPhase.metric }}</div>
      <div v-if="selectedPhase.evidence?.length" class="runtime-evidence">
        <span v-for="item in selectedPhase.evidence" :key="item" :title="item">{{ item }}</span>
      </div>
    </div>
  </section>
</template>
