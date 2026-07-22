<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Archive, CheckCheck, ExternalLink, RefreshCw, Save, Search } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import StatCard from '../components/StatCard.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { useOverlay } from '../composables/useOverlay';
import {
  archiveNotification,
  archiveNotifications,
  fetchNotificationOverview,
  fetchNotificationPreference,
  fetchNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  markNotificationsRead,
  notifyNotificationChanged,
  saveNotificationPreference,
  type NotificationItem,
  type NotificationOverview,
  type NotificationPreference,
} from '../api/notifications';

type Panel = 'all' | 'unread' | 'read' | 'archived' | 'preference';

const router = useRouter();
const { toast } = useOverlay();
const loading = ref(false);
const activePanel = ref<Panel>('all');
const notices = ref<NotificationItem[]>([]);
const overview = ref<NotificationOverview | null>(null);
const total = ref(0);
const selectedIds = ref<string[]>([]);
const filters = reactive({ keyword: '', notificationType: 'all', severity: 'all', pageNo: 1, pageSize: 10 });
const preference = reactive<NotificationPreference>({
  enabledTypes: [],
  minSeverity: 'info',
  stationEnabled: true,
  emailEnabled: false,
  webhookEnabled: false,
  quietStart: undefined,
  quietEnd: undefined,
  digestMode: 'realtime',
});

const tabs: Array<{ key: Panel; label: string; count: () => number }> = [
  { key: 'all', label: '全部通知', count: () => overview.value?.totalCount || 0 },
  { key: 'unread', label: '未读', count: () => overview.value?.unreadCount || 0 },
  { key: 'read', label: '已读', count: () => Math.max(0, (overview.value?.totalCount || 0) - (overview.value?.unreadCount || 0)) },
  { key: 'archived', label: '已归档', count: () => overview.value?.archivedCount || 0 },
  { key: 'preference', label: '接收偏好', count: () => 0 },
];

const allSelected = computed(() => notices.value.length > 0 && notices.value.every((item) => selectedIds.value.includes(item.id)));

onMounted(() => void loadAll());

async function loadAll() {
  loading.value = true;
  try {
    const [overviewResult, preferenceResult] = await Promise.all([
      fetchNotificationOverview(),
      fetchNotificationPreference(),
    ]);
    overview.value = overviewResult;
    Object.assign(preference, preferenceResult);
    await loadList();
  } catch (error) {
    toast(error instanceof Error ? error.message : '通知中心加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadList() {
  if (activePanel.value === 'preference') return;
  const result = await fetchNotifications({
    status: activePanel.value,
    notificationType: filters.notificationType,
    severity: filters.severity,
    keyword: filters.keyword,
    pageNo: filters.pageNo,
    pageSize: filters.pageSize,
  });
  notices.value = result.records;
  total.value = result.total;
  selectedIds.value = [];
}

async function switchPanel(panel: Panel) {
  activePanel.value = panel;
  filters.pageNo = 1;
  await loadList();
}

async function refresh() {
  await loadAll();
  notifyNotificationChanged();
}

async function search() {
  filters.pageNo = 1;
  await loadList();
}

async function changePage(page: number) {
  filters.pageNo = page;
  await loadList();
}

function toggleAll() {
  selectedIds.value = allSelected.value ? [] : notices.value.map((item) => item.id);
}

async function markSelectedRead() {
  if (!selectedIds.value.length) return;
  await markNotificationsRead(selectedIds.value);
  toast('已标记为已读');
  await refresh();
}

async function archiveSelected() {
  if (!selectedIds.value.length) return;
  await archiveNotifications(selectedIds.value);
  toast('通知已归档');
  await refresh();
}

async function markAllRead() {
  await markAllNotificationsRead();
  toast('全部通知已标记为已读');
  await refresh();
}

async function openNotice(item: NotificationItem) {
  if (!item.read) await markNotificationRead(item.id);
  notifyNotificationChanged();
  if (item.actionUrl) await router.push(item.actionUrl);
  else await refresh();
}

async function archiveOne(item: NotificationItem) {
  await archiveNotification(item.id);
  toast('通知已归档');
  await refresh();
}

async function savePreference() {
  const saved = await saveNotificationPreference({ ...preference, enabledTypes: [...preference.enabledTypes] });
  Object.assign(preference, saved);
  toast('通知偏好已保存');
}

function severityTone(severity: string) {
  return severity === 'critical' ? 'danger' : severity === 'warning' ? 'warning' : 'info';
}
</script>

<template>
  <PageHeader title="通知中心" description="集中处理平台告警、任务结果、治理风险和系统消息">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="refresh">
        <RefreshCw :size="16" /> 刷新
      </button>
      <button class="primary-button" type="button" :disabled="!overview?.unreadCount" @click="markAllRead">
        <CheckCheck :size="16" /> 全部已读
      </button>
    </template>
  </PageHeader>

  <section class="stats-grid notification-stats">
    <StatCard label="有效通知" :value="String(overview?.totalCount || 0)" detail="未归档且未失效" icon="Activity" />
    <StatCard label="未读" :value="String(overview?.unreadCount || 0)" detail="等待处理" icon="MessageSquareText" tone="info" />
    <StatCard label="严重未读" :value="String(overview?.criticalUnreadCount || 0)" detail="建议优先处理" icon="ShieldAlert" tone="danger" />
    <StatCard label="已归档" :value="String(overview?.archivedCount || 0)" detail="历史消息" icon="ClipboardList" />
  </section>

  <section class="governance-tab-grid notification-tabs">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      class="governance-tab-card"
      :class="{ active: activePanel === tab.key }"
      type="button"
      @click="switchPanel(tab.key)"
    >
      <span>{{ tab.label }}</span>
      <b v-if="tab.key !== 'preference'">{{ tab.count() }}</b>
      <b v-else>设置</b>
    </button>
  </section>

  <section v-if="activePanel !== 'preference'" class="panel notification-list-panel">
    <div class="toolbar notification-toolbar">
      <div class="search-field"><Search :size="16" /><input v-model="filters.keyword" placeholder="搜索标题或正文" @keyup.enter="search" /></div>
      <select v-model="filters.notificationType" @change="search">
        <option value="all">全部类型</option>
        <option value="ops_alert">运营告警</option>
        <option value="async_task">异步任务</option>
        <option value="governance">治理风险</option>
        <option value="system">系统消息</option>
      </select>
      <select v-model="filters.severity" @change="search">
        <option value="all">全部级别</option>
        <option value="critical">严重</option>
        <option value="warning">警告</option>
        <option value="info">信息</option>
      </select>
      <span class="toolbar-spacer" />
      <button class="secondary-button slim" type="button" :disabled="!selectedIds.length" @click="markSelectedRead"><CheckCheck :size="14" /> 标记已读</button>
      <button class="secondary-button slim" type="button" :disabled="!selectedIds.length" @click="archiveSelected"><Archive :size="14" /> 归档</button>
    </div>

    <div class="table-scroll notification-table-scroll">
      <table class="data-table notification-table">
        <thead><tr><th class="check-column"><input type="checkbox" :checked="allSelected" @change="toggleAll" /></th><th>通知</th><th>类型</th><th>级别</th><th>时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in notices" :key="item.id" :class="{ 'notification-unread-row': !item.read }">
            <td><input v-model="selectedIds" type="checkbox" :value="item.id" /></td>
            <td class="notification-main-cell">
              <button class="text-action notification-title" type="button" :title="item.title" @click="openNotice(item)">
                <span v-if="!item.read" class="unread-dot" />{{ item.title }}
              </button>
              <span class="muted truncate-cell" :title="item.content">{{ item.content }}</span>
            </td>
            <td><span class="mono truncate-cell" :title="item.notificationType">{{ item.notificationType }}</span></td>
            <td><StatusBadge :label="item.severity" :tone="severityTone(item.severity)" /></td>
            <td><span class="single-line">{{ new Date(item.createdAt).toLocaleString() }}</span></td>
            <td class="table-actions">
              <button v-if="item.actionUrl" class="icon-button" type="button" title="打开关联页面" @click="openNotice(item)"><ExternalLink :size="15" /></button>
              <button v-if="!item.archived" class="icon-button" type="button" title="归档" @click="archiveOne(item)"><Archive :size="15" /></button>
            </td>
          </tr>
          <tr v-if="!notices.length"><td colspan="6"><div class="empty-state">当前筛选下暂无通知</div></td></tr>
        </tbody>
      </table>
    </div>
    <PaginationBar :page="filters.pageNo" :page-size="filters.pageSize" :total="total" @update:page="changePage" />
  </section>

  <section v-else class="panel notification-preference-panel">
    <div class="section-title"><div><h2>接收偏好</h2><p class="muted">控制站内消息级别、类型和触达频率。</p></div></div>
    <div class="settings-form notification-preference-form">
      <label>最低接收级别
        <select v-model="preference.minSeverity"><option value="info">信息及以上</option><option value="warning">警告及以上</option><option value="critical">仅严重</option></select>
      </label>
      <label>消息频率
        <select v-model="preference.digestMode"><option value="realtime">实时</option><option value="hourly">每小时摘要</option><option value="daily">每日摘要</option></select>
      </label>
      <label>免打扰开始<input v-model="preference.quietStart" type="time" /></label>
      <label>免打扰结束<input v-model="preference.quietEnd" type="time" /></label>
    </div>
    <div class="switch-list notification-switches">
      <label><input v-model="preference.stationEnabled" type="checkbox" /> 站内通知</label>
      <label><input v-model="preference.emailEnabled" type="checkbox" /> 邮件触达</label>
      <label><input v-model="preference.webhookEnabled" type="checkbox" /> Webhook触达</label>
    </div>
    <div class="notification-type-options">
      <span class="muted">指定接收类型，全部不选表示接收所有类型</span>
      <label v-for="type in ['ops_alert', 'async_task', 'governance', 'system']" :key="type"><input v-model="preference.enabledTypes" type="checkbox" :value="type" /> {{ type }}</label>
    </div>
    <div class="toolbar compact"><span class="toolbar-spacer" /><button class="primary-button" type="button" @click="savePreference"><Save :size="16" /> 保存偏好</button></div>
  </section>
</template>

<style scoped>
.notification-stats { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.notification-tabs { grid-template-columns: repeat(5, minmax(0, 1fr)); }
.notification-list-panel { min-height: 0; }
.notification-toolbar { flex-wrap: nowrap; }
.notification-toolbar .search-field { min-width: 260px; }
.notification-table-scroll { max-height: calc(100vh - 410px); min-height: 260px; overflow: auto; }
.notification-table { table-layout: fixed; min-width: 920px; }
.notification-table th:nth-child(1) { width: 44px; }
.notification-table th:nth-child(2) { width: 44%; }
.notification-table th:nth-child(3) { width: 130px; }
.notification-table th:nth-child(4) { width: 90px; }
.notification-table th:nth-child(5) { width: 180px; }
.notification-table th:nth-child(6) { width: 96px; }
.notification-main-cell { display: grid; gap: 5px; min-width: 0; }
.notification-title { display: inline-flex; align-items: center; gap: 8px; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 700; }
.notification-main-cell > .truncate-cell { display: block; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notification-unread-row { background: color-mix(in srgb, var(--primary, #2563eb) 5%, white); }
.unread-dot { width: 7px; height: 7px; border-radius: 50%; flex: 0 0 auto; background: #dc2626; }
.single-line { white-space: nowrap; }
.notification-preference-panel { max-width: none; }
.notification-preference-form { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.notification-switches { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.notification-type-options { display: flex; align-items: center; gap: 22px; min-height: 56px; border-top: 1px solid var(--border-color, #e5e7eb); border-bottom: 1px solid var(--border-color, #e5e7eb); }
.notification-type-options label { display: inline-flex; align-items: center; gap: 7px; white-space: nowrap; }
@media (max-width: 1100px) {
  .notification-stats, .notification-tabs { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .notification-preference-form { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .notification-toolbar { flex-wrap: wrap; }
}
</style>
