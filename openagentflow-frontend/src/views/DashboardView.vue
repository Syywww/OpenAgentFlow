<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { Bot, Braces, FileUp, GitBranch, Plug, TestTube2 } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import StatCard from '../components/StatCard.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { dashboardMetrics, runLogs } from '../data/mock';
import { useOverlay } from '../composables/useOverlay';
import { usePagination } from '../composables/usePagination';

const router = useRouter();
const { showModal } = useOverlay();
const recentRunRows = ref(runLogs);
const { currentPage: recentRunPage, pagedItems: pagedRecentRuns } = usePagination(recentRunRows);

const quickActions = [
  { title: '创建智能体', desc: '配置 Prompt、模型参数和能力边界', icon: Bot, action: () => showModal('new-agent') },
  { title: '上传知识库', desc: '解析文档、切片并生成向量索引', icon: FileUp, action: () => showModal('upload') },
  { title: '进入调试台', desc: '验证 RAG、工具调用和完整 Trace', icon: TestTube2, action: () => router.push('/debug') },
  { title: '编排工作流', desc: '拖拽节点构建 Agent 执行链路', icon: GitBranch, action: () => router.push('/workflow') },
  { title: '接入 MCP', desc: '发现并统一管理外部工具服务', icon: Plug, action: () => router.push('/mcp') },
  { title: '注册工具', desc: '配置 REST API、DB、Webhook 工具', icon: Braces, action: () => router.push('/tools/new') },
];
</script>

<template>
  <PageHeader title="工作台" description="您好，admin，欢迎使用 OpenAgentFlow-Java">
    <template #actions>
      <button class="secondary-button" type="button">2024-05-20 ~ 2024-05-26</button>
    </template>
  </PageHeader>

  <section class="metric-grid">
    <StatCard v-for="item in dashboardMetrics" :key="item.label" v-bind="item" />
  </section>

  <section class="section-block">
    <div class="section-title">
      <h2>快捷操作</h2>
      <span>围绕 MVP 的 Agent、RAG、Tool Calling、Trace 主链路</span>
    </div>
    <div class="quick-grid">
      <button v-for="item in quickActions" :key="item.title" class="quick-action" type="button" @click="item.action">
        <component :is="item.icon" :size="22" />
        <b>{{ item.title }}</b>
        <span>{{ item.desc }}</span>
      </button>
    </div>
  </section>

  <section class="dashboard-columns">
    <div class="section-block">
      <div class="section-title">
        <h2>最近运行记录</h2>
        <button class="link-button" type="button" @click="router.push('/logs')">查看全部</button>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>运行 ID</th><th>类型</th><th>名称</th><th>状态</th><th>耗时</th><th>Tokens</th></tr>
        </thead>
        <tbody>
          <tr v-for="run in pagedRecentRuns" :key="run.id" @click="router.push(`/logs/${run.id}`)">
            <td class="mono">{{ run.id }}</td>
            <td>{{ run.type }}</td>
            <td>{{ run.name }}</td>
            <td><StatusBadge :label="run.status" /></td>
            <td>{{ run.duration }}</td>
            <td>{{ run.tokens }}</td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="recentRunPage" :total="recentRunRows.length" />
    </div>

    <div class="section-block">
      <div class="section-title">
        <h2>模型使用情况</h2>
        <span>近 7 天调用分布</span>
      </div>
      <div class="usage-bars">
        <div v-for="model in [
          ['GPT-4o', '8742', '78%'],
          ['Qwen2.5-72B', '6521', '58%'],
          ['Claude 3.5', '4231', '42%'],
          ['GLM-4', '2985', '31%']
        ]" :key="model[0]" class="bar-row">
          <div><b>{{ model[0] }}</b><span>{{ model[1] }} 次</span></div>
          <i><em :style="{ width: model[2] }" /></i>
        </div>
      </div>
      <div class="insight-strip">
        <b>AI 业务洞察</b>
        <p>知识库命中率 78.3%，较上周提升 6.2%。可将非核心任务路由到成本更低的模型。</p>
      </div>
    </div>
  </section>
</template>
