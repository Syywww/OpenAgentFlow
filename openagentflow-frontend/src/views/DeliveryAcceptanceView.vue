<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ClipboardCheck, Eye, FileCheck2, Play, RefreshCw, X } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import StatCard from '../components/StatCard.vue';
import StatusBadge from '../components/StatusBadge.vue';
import {
  fetchDeliveryChecks,
  fetchDeliveryOverview,
  fetchDeliveryReport,
  fetchDeliveryReports,
  runDeliveryAcceptance,
  type DeliveryCheckItem,
  type DeliveryManifest,
  type DeliveryOverview,
  type DeliveryReportDetail,
  type DeliveryReportSummary,
  type DeliveryRiskItem,
} from '../api/delivery';
import type { StatusTone } from '../types';
import { usePagination } from '../composables/usePagination';

const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const overview = ref<DeliveryOverview | null>(null);
const checks = ref<DeliveryCheckItem[]>([]);
const reports = ref<DeliveryReportSummary[]>([]);
const reportTotal = ref(0);
const reportPage = ref(1);
const activePanel = ref<'checks' | 'risks' | 'manifest' | 'reports'>('checks');
const selectedReport = ref<DeliveryReportDetail | null>(null);
const reportModalOpen = ref(false);

const risks = computed<DeliveryRiskItem[]>(() => buildRisks(checks.value));
const manifest = computed<DeliveryManifest | null>(() => overview.value?.manifest || null);
const { currentPage: checkPage, pagedItems: pagedChecks } = usePagination(checks);
const { currentPage: riskPage, pagedItems: pagedRisks } = usePagination(risks);

const statusLabelMap: Record<string, string> = {
  ready: '可交付',
  warning: '有警告',
  failed: '有阻断',
  passed: '通过',
};

const checkStatusLabelMap: Record<string, string> = {
  passed: '通过',
  warning: '警告',
  failed: '失败',
};

function statusTone(status?: string): StatusTone {
  if (status === 'ready' || status === 'passed') return 'success';
  if (status === 'failed') return 'danger';
  if (status === 'warning') return 'warning';
  return 'neutral';
}

function riskTone(level?: string): StatusTone {
  if (level === 'high') return 'danger';
  if (level === 'medium') return 'warning';
  if (level === 'low') return 'info';
  return 'neutral';
}

function riskLabel(level?: string) {
  if (level === 'high') return '高风险';
  if (level === 'medium') return '中风险';
  if (level === 'low') return '低风险';
  return '提示';
}

function displayValue(value: unknown) {
  if (value === null || value === undefined || value === '') return '-';
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}

function formatTime(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
}

function manifestRows(data: DeliveryManifest | null) {
  if (!data) return [];
  return [
    { label: '应用名称', value: data.appName },
    { label: '后端版本', value: data.backendVersion },
    { label: '前端版本', value: data.frontendVersion },
    { label: 'Java 版本', value: data.javaVersion },
    { label: '数据库', value: data.databaseName },
    { label: '最新 SQL', value: data.latestSqlVersion },
    { label: '数据组件', value: data.dataComponents },
    { label: '模块数量', value: data.moduleCounts },
  ];
}

function buildRisks(items: DeliveryCheckItem[]) {
  return items
    .filter((item) => item.status !== 'passed')
    .map((item) => ({
      riskLevel: item.blocking || item.status === 'failed' ? 'high' : 'medium',
      title: item.checkName,
      description: item.message,
      suggestion: item.suggestion,
      sourceCheckCode: item.checkCode,
    }));
}

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [overviewResult, checksResult, reportPageResult] = await Promise.all([
      fetchDeliveryOverview(),
      fetchDeliveryChecks(),
      fetchDeliveryReports(reportPage.value, 10),
    ]);
    overview.value = overviewResult;
    checks.value = checksResult;
    reports.value = reportPageResult.records;
    reportTotal.value = reportPageResult.total;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '交付验收数据加载失败';
  } finally {
    loading.value = false;
  }
}

async function changeReportPage(page: number) {
  reportPage.value = page;
  await loadData();
}

async function runChecks() {
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    selectedReport.value = await runDeliveryAcceptance();
    successMessage.value = `交付验收已生成报告：${selectedReport.value.reportCode}`;
    reportModalOpen.value = true;
    activePanel.value = selectedReport.value.risks.length > 0 ? 'risks' : 'reports';
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '交付验收执行失败';
  } finally {
    loading.value = false;
  }
}

async function openReport(report: DeliveryReportSummary) {
  loading.value = true;
  errorMessage.value = '';
  try {
    selectedReport.value = await fetchDeliveryReport(report.id);
    reportModalOpen.value = true;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '交付报告加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <PageHeader title="交付验收中心" description="面向开源交付、部署上线和客户验收，统一检查环境、核心链路、风险提示与交付清单">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadData">
        <RefreshCw :size="16" /> 刷新
      </button>
      <button class="primary-button" type="button" :disabled="loading" @click="runChecks">
        <Play :size="16" /> 一键验收
      </button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
  <p v-if="successMessage" class="form-success">{{ successMessage }}</p>

  <section class="metric-grid">
    <StatCard label="交付状态" :value="statusLabelMap[overview?.overallStatus || ''] || '-'" :detail="`最新报告 ${overview?.latestReportCode || '-'}`" icon="ShieldCheck" :tone="statusTone(overview?.overallStatus)" />
    <StatCard label="验收分数" :value="String(overview?.score ?? 0)" :detail="`${overview?.passedCount || 0} 项通过`" icon="Gauge" :tone="statusTone(overview?.overallStatus)" />
    <StatCard label="警告项" :value="String(overview?.warningCount || 0)" detail="需要上线前关注" icon="ShieldAlert" tone="warning" />
    <StatCard label="阻断项" :value="String(overview?.failedCount || 0)" detail="存在则不建议交付" icon="ClipboardList" :tone="overview?.failedCount ? 'danger' : 'success'" />
  </section>

  <section class="governance-card-tabs delivery-card-tabs">
    <button class="governance-tab-card" :class="{ active: activePanel === 'checks' }" type="button" @click="activePanel = 'checks'">
      <span>检查项</span>
      <b>{{ checks.length }}</b>
      <small>环境、链路、权限和配置</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'risks' }" type="button" @click="activePanel = 'risks'">
      <span>风险提示</span>
      <b>{{ risks.length }}</b>
      <small>按阻断与警告聚合</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'manifest' }" type="button" @click="activePanel = 'manifest'">
      <span>交付清单</span>
      <b>{{ overview?.moduleCount || 0 }}</b>
      <small>版本、组件和模块数量</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'reports' }" type="button" @click="activePanel = 'reports'">
      <span>报告列表</span>
      <b>{{ reportTotal }}</b>
      <small>每次验收生成一份报告</small>
    </button>
  </section>

  <section class="section-block delivery-panel">
    <template v-if="activePanel === 'checks'">
      <div class="section-title">
        <h2>检查项</h2>
        <span>覆盖 {{ overview?.componentCount || 0 }} 个关键组件</span>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>检查名称</th><th>分类</th><th>状态</th><th>实际值</th><th>建议</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in pagedChecks" :key="item.checkCode">
            <td><b :title="item.checkName">{{ item.checkName }}</b><small class="block mono" :title="item.checkCode">{{ item.checkCode }}</small></td>
            <td>{{ item.category }}</td>
            <td><StatusBadge :label="checkStatusLabelMap[item.status] || item.status" :tone="statusTone(item.status)" /></td>
            <td :title="displayValue(item.actualValue)">{{ displayValue(item.actualValue) }}</td>
            <td :title="item.suggestion || item.message">{{ item.suggestion || item.message }}</td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="checkPage" :total="checks.length" />
    </template>

    <template v-else-if="activePanel === 'risks'">
      <div class="section-title">
        <h2>风险提示</h2>
        <span>{{ risks.length }} 项需要关注</span>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>等级</th><th>来源</th><th>描述</th><th>建议动作</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in pagedRisks" :key="`${item.sourceCheckCode}-${item.title}`">
            <td><StatusBadge :label="riskLabel(item.riskLevel)" :tone="riskTone(item.riskLevel)" /></td>
            <td><b :title="item.title">{{ item.title }}</b><small class="block mono" :title="item.sourceCheckCode">{{ item.sourceCheckCode }}</small></td>
            <td :title="item.description">{{ item.description }}</td>
            <td :title="item.suggestion">{{ item.suggestion }}</td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="riskPage" :total="risks.length" />
      <div v-if="risks.length === 0" class="empty-state">当前未发现阻断或警告项</div>
    </template>

    <template v-else-if="activePanel === 'manifest'">
      <div class="section-title">
        <h2>交付清单</h2>
        <span>{{ manifest?.appName || 'OpenAgentFlow' }}</span>
      </div>
      <div class="delivery-manifest-grid">
        <article v-for="item in manifestRows(manifest)" :key="item.label" class="delivery-manifest-item">
          <span>{{ item.label }}</span>
          <b :title="displayValue(item.value)">{{ displayValue(item.value) }}</b>
        </article>
      </div>
    </template>

    <template v-else>
      <div class="section-title">
        <h2>报告列表</h2>
        <div class="title-actions">
          <span>共 {{ reportTotal }} 份</span>
          <button class="primary-button slim" type="button" :disabled="loading" @click="runChecks">
            <ClipboardCheck :size="14" /> 生成报告
          </button>
        </div>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>报告</th><th>状态</th><th>分数</th><th>通过/警告/阻断</th><th>生成时间</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in reports" :key="item.id">
            <td><b :title="item.reportName">{{ item.reportName }}</b><small class="block mono" :title="item.reportCode">{{ item.reportCode }}</small></td>
            <td><StatusBadge :label="statusLabelMap[item.overallStatus] || item.overallStatus" :tone="statusTone(item.overallStatus)" /></td>
            <td>{{ item.score }}</td>
            <td>{{ item.passedCount }} / {{ item.warningCount }} / {{ item.failedCount }}</td>
            <td>{{ formatTime(item.createdAt) }}</td>
            <td>
              <button class="secondary-button slim" type="button" @click="openReport(item)">
                <Eye :size="14" /> 详情
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <PaginationBar :page="reportPage" :total="reportTotal" @update:page="changeReportPage" />
      <div v-if="reports.length === 0" class="empty-state">暂无交付报告</div>
    </template>
  </section>

  <div v-if="reportModalOpen && selectedReport" class="overlay-backdrop" @click.self="reportModalOpen = false">
    <section class="modal-panel delivery-report-modal">
      <header class="overlay-header">
        <div>
          <h2>{{ selectedReport.reportName }}</h2>
          <p class="muted mono">{{ selectedReport.reportCode }} / {{ formatTime(selectedReport.createdAt) }}</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="reportModalOpen = false"><X :size="18" /></button>
      </header>

      <div class="metric-grid compact">
        <StatCard label="状态" :value="statusLabelMap[selectedReport.overallStatus] || selectedReport.overallStatus" detail="报告结论" icon="ShieldCheck" :tone="statusTone(selectedReport.overallStatus)" />
        <StatCard label="分数" :value="String(selectedReport.score)" detail="百分制" icon="Gauge" :tone="statusTone(selectedReport.overallStatus)" />
        <StatCard label="风险" :value="String(selectedReport.risks.length)" detail="报告内风险提示" icon="ShieldAlert" :tone="selectedReport.risks.length ? 'warning' : 'success'" />
      </div>

      <div class="delivery-report-scroll">
        <h3>检查明细</h3>
        <table class="data-table">
          <thead><tr><th>检查项</th><th>状态</th><th>实际值</th><th>建议</th></tr></thead>
          <tbody>
            <tr v-for="item in selectedReport.checks" :key="item.checkCode">
              <td><b :title="item.checkName">{{ item.checkName }}</b></td>
              <td><StatusBadge :label="checkStatusLabelMap[item.status] || item.status" :tone="statusTone(item.status)" /></td>
              <td :title="displayValue(item.actualValue)">{{ displayValue(item.actualValue) }}</td>
              <td :title="item.suggestion">{{ item.suggestion }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="toolbar compact">
        <button class="secondary-button" type="button" @click="reportModalOpen = false">关闭</button>
        <button class="primary-button" type="button" @click="activePanel = 'reports'; reportModalOpen = false">
          <FileCheck2 :size="16" /> 查看报告列表
        </button>
      </div>
    </section>
  </div>
</template>
