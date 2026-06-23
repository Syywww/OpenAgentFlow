<script setup lang="ts">
import type { Component } from 'vue';
import { Activity, Bot, Braces, Coins, Gauge, Library, Server, ShieldAlert, ShieldCheck, Timer, Workflow } from 'lucide-vue-next';
import type { StatusTone } from '../types';

const props = defineProps<{
  label: string;
  value: string;
  detail: string;
  icon: string;
  tone?: StatusTone;
}>();

const iconMap: Record<string, Component> = {
  Activity,
  Bot,
  Braces,
  Coins,
  Gauge,
  Library,
  Server,
  ShieldAlert,
  ShieldCheck,
  Timer,
  Workflow,
};
</script>

<template>
  <article class="stat-card" :class="`tone-${props.tone ?? 'neutral'}`">
    <div>
      <span>{{ label }}</span>
      <strong>{{ value }}</strong>
      <small>{{ detail }}</small>
    </div>
    <div class="stat-icon">
      <component :is="iconMap[icon] ?? Activity" :size="22" />
    </div>
  </article>
</template>
