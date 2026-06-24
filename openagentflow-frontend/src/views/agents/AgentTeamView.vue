<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Edit3, Eye, Play, Plus, RefreshCw, Trash2, UsersRound, X } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import PaginationBar from '../../components/PaginationBar.vue';
import StatCard from '../../components/StatCard.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import {
  createAgentTeam,
  deleteAgentTeam,
  fetchAgentTeam,
  fetchAgentTeams,
  publishAgentTeam,
  runAgentTeam,
  updateAgentTeam,
  type AgentTeamDetail,
  type AgentTeamMemberRequest,
  type AgentTeamRunResult,
  type AgentTeamSummary,
} from '../../api/agentTeams';
import { fetchAgents, type AgentSummary } from '../../api/agents';
import { usePagination } from '../../composables/usePagination';

const router = useRouter();
const teams = ref<AgentTeamSummary[]>([]);
const agents = ref<AgentSummary[]>([]);
const selectedTeam = ref<AgentTeamDetail | null>(null);
const runResult = ref<AgentTeamRunResult | null>(null);
const loading = ref(false);
const saving = ref(false);
const running = ref(false);
const keyword = ref('');
const modeFilter = ref('all');
const statusFilter = ref('all');
const showEditor = ref(false);
const showDetail = ref(false);
const editingId = ref('');

const form = reactive({
  teamCode: '',
  teamName: '',
  description: '',
  collaborationMode: 'sequential',
  coordinatorAgentId: '',
  status: 'draft',
  members: [] as AgentTeamMemberRequest[],
});

const runForm = reactive({
  objective: '请由团队协作完成一次企业知识问答方案设计，并给出最终结论。',
  sharedContext: '{}',
  continueOnError: false,
});

const filteredTeams = computed(() => teams.value.filter((team) => {
  const keywordMatched = !keyword.value
    || team.teamName.toLowerCase().includes(keyword.value.toLowerCase())
    || team.teamCode.toLowerCase().includes(keyword.value.toLowerCase());
  const modeMatched = modeFilter.value === 'all' || team.collaborationMode === modeFilter.value;
  const statusMatched = statusFilter.value === 'all' || team.status === statusFilter.value;
  return keywordMatched && modeMatched && statusMatched;
}));

const publishedCount = computed(() => teams.value.filter((team) => team.status === 'published').length);
const memberCount = computed(() => teams.value.reduce((sum, team) => sum + (team.memberCount || 0), 0));
const runs7d = computed(() => teams.value.reduce((sum, team) => sum + (team.runs7d || 0), 0));
const { currentPage: teamPage, pagedItems: pagedTeams, resetPage: resetTeamPage } = usePagination(filteredTeams);

onMounted(() => {
  void loadData();
});

async function loadData() {
  loading.value = true;
  try {
    const [teamList, agentList] = await Promise.all([fetchAgentTeams(), fetchAgents()]);
    teams.value = teamList;
    agents.value = agentList;
    if (selectedTeam.value) {
      selectedTeam.value = await fetchAgentTeam(selectedTeam.value.id);
    }
  } finally {
    loading.value = false;
  }
}

async function openTeamDetail(id: string) {
  selectedTeam.value = await fetchAgentTeam(id);
  runResult.value = null;
  showDetail.value = true;
}

function closeTeamDetail() {
  showDetail.value = false;
}

function openCreate() {
  editingId.value = '';
  form.teamCode = '';
  form.teamName = '企业问答协作团队';
  form.description = '由规划、检索、工具执行和复核 Agent 组成的协作团队';
  form.collaborationMode = 'sequential';
  form.coordinatorAgentId = '';
  form.status = 'draft';
  form.members = [];
  showEditor.value = true;
}

async function openEdit(team: AgentTeamSummary) {
  const detail = await fetchAgentTeam(team.id);
  editingId.value = detail.id;
  form.teamCode = detail.teamCode;
  form.teamName = detail.teamName;
  form.description = detail.description || '';
  form.collaborationMode = detail.collaborationMode || 'sequential';
  form.coordinatorAgentId = detail.coordinatorAgentId || '';
  form.status = detail.status || 'draft';
  form.members = detail.members.map((member) => ({
    agentId: member.agentId,
    memberRole: member.memberRole,
    handoffPolicy: member.handoffPolicy || '{}',
    sortOrder: member.sortOrder,
    enabled: member.enabled,
  }));
  showEditor.value = true;
}

function addMember() {
  form.members.push({
    agentId: agents.value[0]?.id || '',
    memberRole: form.members.length === 0 ? 'coordinator' : 'worker',
    handoffPolicy: '{}',
    sortOrder: form.members.length * 10,
    enabled: true,
  });
}

function removeMember(index: number) {
  form.members.splice(index, 1);
}

async function saveTeam() {
  saving.value = true;
  try {
    const payload = {
      teamCode: form.teamCode,
      teamName: form.teamName,
      description: form.description,
      collaborationMode: form.collaborationMode,
      coordinatorAgentId: form.coordinatorAgentId || undefined,
      status: form.status,
      members: form.members,
    };
    const detail = editingId.value
      ? await updateAgentTeam(editingId.value, payload)
      : await createAgentTeam(payload);
    showEditor.value = false;
    await loadData();
    await openTeamDetail(detail.id);
  } finally {
    saving.value = false;
  }
}

async function handlePublish(team: AgentTeamSummary) {
  await publishAgentTeam(team.id);
  await loadData();
  await openTeamDetail(team.id);
}

async function handleDelete(team: AgentTeamSummary) {
  if (!window.confirm(`确认删除协作团队「${team.teamName}」？`)) {
    return;
  }
  await deleteAgentTeam(team.id);
  if (selectedTeam.value?.id === team.id) {
    selectedTeam.value = null;
    showDetail.value = false;
  }
  await loadData();
}

async function handleRun() {
  if (!selectedTeam.value) {
    return;
  }
  running.value = true;
  try {
    runResult.value = await runAgentTeam(selectedTeam.value.id, {
      objective: runForm.objective,
      sharedContext: parseJson(runForm.sharedContext),
      continueOnError: runForm.continueOnError,
    });
    await loadData();
  } finally {
    running.value = false;
  }
}

function parseJson(text: string) {
  try {
    return JSON.parse(text || '{}') as Record<string, unknown>;
  } catch {
    return {};
  }
}

function agentName(id?: string) {
  return agents.value.find((agent) => agent.id === id)?.agentName || '请选择 Agent';
}
</script>

<template>
  <PageHeader title="多 Agent 协作" description="编排多个专业 Agent 分工处理任务，并将协作过程写入 Trace">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadData"><RefreshCw :size="16" /> 刷新</button>
      <button class="primary-button" type="button" @click="openCreate"><Plus :size="16" /> 新建团队</button>
    </template>
  </PageHeader>

  <section class="metric-grid">
    <StatCard label="协作团队" :value="String(teams.length)" detail="多 Agent 编排单元" icon="Bot" tone="info" />
    <StatCard label="已发布" :value="String(publishedCount)" detail="可直接运行验证" icon="ShieldCheck" tone="success" />
    <StatCard label="成员数量" :value="String(memberCount)" detail="绑定 Agent 总数" icon="Workflow" tone="warning" />
    <StatCard label="7日运行" :value="String(runs7d)" detail="真实协作调用" icon="Activity" tone="neutral" />
  </section>

  <section class="filter-row">
    <select v-model="modeFilter" @change="resetTeamPage">
      <option value="all">全部模式</option>
      <option value="sequential">顺序协作</option>
      <option value="parallel">并行协作</option>
      <option value="router">路由分派</option>
      <option value="supervisor">主控规划</option>
      <option value="reviewer">产出复核</option>
    </select>
    <select v-model="statusFilter" @change="resetTeamPage">
      <option value="all">全部状态</option>
      <option value="draft">草稿</option>
      <option value="published">已发布</option>
      <option value="disabled">已停用</option>
    </select>
    <input v-model="keyword" placeholder="搜索团队名称或编码" @input="resetTeamPage" />
  </section>

  <section class="section-block agent-team-list">
    <div class="section-title"><h2>协作团队列表</h2><span>{{ filteredTeams.length }} 个</span></div>
    <div v-if="loading" class="empty-state">正在加载协作团队...</div>
    <div v-else-if="filteredTeams.length === 0" class="empty-state">暂无协作团队</div>
    <table v-else class="data-table">
      <thead>
        <tr><th>团队</th><th>模式</th><th>成员</th><th>状态</th><th>7日运行</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="team in pagedTeams" :key="team.id" :class="{ selected: selectedTeam?.id === team.id }" @click="openTeamDetail(team.id)">
          <td><b :title="team.teamName">{{ team.teamName }}</b><small class="mono" :title="team.teamCode">{{ team.teamCode }}</small></td>
          <td>{{ team.collaborationModeLabel }}</td>
          <td>{{ team.memberCount }}</td>
          <td><StatusBadge :label="team.statusLabel" :tone="team.status === 'published' ? 'success' : 'warning'" /></td>
          <td>{{ team.runs7d }} / 成功 {{ team.success7d }}</td>
          <td>
            <div class="table-actions" @click.stop>
              <button class="icon-button" type="button" title="详情" @click="openTeamDetail(team.id)"><Eye :size="16" /></button>
              <button class="icon-button" type="button" title="编辑" @click="openEdit(team)"><Edit3 :size="16" /></button>
              <button class="icon-button" type="button" title="发布" @click="handlePublish(team)"><UsersRound :size="16" /></button>
              <button class="icon-button danger" type="button" title="删除" @click="handleDelete(team)"><Trash2 :size="16" /></button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <PaginationBar v-if="!loading && filteredTeams.length > 0" v-model:page="teamPage" :total="filteredTeams.length" />
  </section>

  <div v-if="showDetail && selectedTeam" class="overlay-backdrop">
    <section class="modal-panel agent-team-detail-modal">
      <div class="overlay-header">
        <div>
          <h2>{{ selectedTeam.teamName }}</h2>
          <p>{{ selectedTeam.description || '协作团队详情、成员分工、运行历史和调试验证' }}</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closeTeamDetail"><X :size="18" /></button>
      </div>

      <div class="agent-team-detail-grid">
        <div class="form-stack">
          <div class="team-profile">
            <div><span>编码</span><b class="mono">{{ selectedTeam.teamCode }}</b></div>
            <div><span>协作模式</span><b>{{ selectedTeam.collaborationModeLabel }}</b></div>
            <div><span>状态</span><b>{{ selectedTeam.statusLabel }}</b></div>
            <div><span>主控 Agent</span><b>{{ selectedTeam.coordinatorAgentName || '-' }}</b></div>
            <div><span>成员数量</span><b>{{ selectedTeam.memberCount }}</b></div>
            <div><span>7日运行</span><b>{{ selectedTeam.runs7d }}</b></div>
          </div>

          <div class="section-title slim"><h2>成员分工</h2><span>{{ selectedTeam.members.length }} 个</span></div>
          <div class="team-member-scroll">
            <div v-for="member in selectedTeam.members" :key="member.agentId" class="list-row agent-team-member-row">
              <div>
                <b>{{ member.agentName }}</b>
                <small>{{ member.memberRole }} · {{ member.modelName || '未绑定模型' }}</small>
              </div>
              <StatusBadge :label="member.enabled ? '启用' : '停用'" :tone="member.enabled ? 'success' : 'neutral'" />
            </div>
          </div>

          <div class="section-title slim"><h2>最近运行</h2><span>{{ selectedTeam.recentRuns?.length || 0 }} 条</span></div>
          <div class="team-member-scroll">
            <div v-if="!selectedTeam.recentRuns || selectedTeam.recentRuns.length === 0" class="empty-state">暂无协作运行历史</div>
            <div v-for="run in selectedTeam.recentRuns" :key="run.collaborationRunId" class="list-row agent-team-member-row">
              <div>
                <b :title="run.objective">{{ run.objective }}</b>
                <small :title="run.finalResult || ''">{{ run.totalTokens || 0 }} Token · {{ run.latencyMs || 0 }}ms</small>
              </div>
              <button class="secondary-button slim" type="button" :disabled="!run.runtimeRunId" @click="router.push(`/logs/${run.runtimeRunId}`)">Trace</button>
            </div>
          </div>
        </div>

        <div class="form-stack">
          <div class="section-title slim"><h2>运行验证</h2><span>{{ runResult?.status || '待运行' }}</span></div>
          <label>协作目标<textarea v-model="runForm.objective" rows="4" /></label>
          <label>共享上下文 JSON<textarea v-model="runForm.sharedContext" rows="3" /></label>
          <label class="check-line"><input v-model="runForm.continueOnError" type="checkbox" /> 成员失败后继续执行</label>
          <div class="button-row">
            <button class="primary-button" type="button" :disabled="running" @click="handleRun"><Play :size="16" /> {{ running ? '运行中...' : '运行协作' }}</button>
            <button v-if="runResult?.runtimeRunId" class="secondary-button" type="button" @click="router.push(`/logs/${runResult.runtimeRunId}`)">查看 Trace</button>
          </div>

          <div v-if="runResult" class="agent-team-result">
            <div class="team-profile">
              <div><span>状态</span><b>{{ runResult.status }}</b></div>
              <div><span>Token</span><b>{{ runResult.totalTokens }}</b></div>
              <div><span>耗时</span><b>{{ runResult.latencyMs }}ms</b></div>
            </div>
            <div class="team-member-scroll">
              <div v-for="step in runResult.steps" :key="step.traceStepId" class="list-row agent-team-member-row">
                <div>
                  <b>{{ step.stepName }} · {{ step.agentName }}</b>
                  <small :title="step.output || step.errorMessage">{{ step.output || step.errorMessage || '-' }}</small>
                </div>
                <StatusBadge :label="step.status" :tone="step.status === 'SUCCESS' ? 'success' : 'danger'" />
              </div>
            </div>
            <label>最终结果<textarea :value="runResult.finalResult || runResult.errorMessage || ''" rows="5" readonly /></label>
          </div>
        </div>
      </div>
    </section>
  </div>

  <div v-if="showEditor" class="overlay-backdrop">
    <section class="modal-panel agent-team-modal">
      <div class="overlay-header">
        <div>
          <h2>{{ editingId ? '编辑协作团队' : '新建协作团队' }}</h2>
          <p>配置团队模式、主控 Agent 和成员执行顺序</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="showEditor = false"><X :size="18" /></button>
      </div>

      <div class="overlay-grid">
        <div class="form-stack">
          <label>团队名称<input v-model="form.teamName" /></label>
          <label>团队编码<input v-model="form.teamCode" placeholder="不填自动生成" /></label>
          <label>团队说明<textarea v-model="form.description" rows="3" /></label>
          <div class="two-cols">
            <label>协作模式
              <select v-model="form.collaborationMode">
                <option value="sequential">顺序协作</option>
                <option value="parallel">并行协作</option>
                <option value="router">路由分派</option>
                <option value="supervisor">主控规划</option>
                <option value="reviewer">产出复核</option>
              </select>
            </label>
            <label>状态
              <select v-model="form.status">
                <option value="draft">草稿</option>
                <option value="published">已发布</option>
                <option value="disabled">已停用</option>
              </select>
            </label>
          </div>
          <label>主控 Agent
            <select v-model="form.coordinatorAgentId">
              <option value="">不指定</option>
              <option v-for="agent in agents" :key="agent.id" :value="agent.id">{{ agent.agentName }}</option>
            </select>
          </label>
        </div>

        <div class="form-stack">
          <div class="section-title slim">
            <h2>成员配置</h2>
            <button class="secondary-button slim" type="button" @click="addMember"><Plus :size="14" /> 添加成员</button>
          </div>
          <div v-if="form.members.length === 0" class="empty-state">请添加至少一个 Agent 成员</div>
          <div v-for="(member, index) in form.members" :key="index" class="member-editor-row">
            <label>Agent
              <select v-model="member.agentId">
                <option v-for="agent in agents" :key="agent.id" :value="agent.id">{{ agent.agentName }}</option>
              </select>
            </label>
            <div class="two-cols">
              <label>职责<input v-model="member.memberRole" placeholder="coordinator / worker / reviewer" /></label>
              <label>排序<input v-model.number="member.sortOrder" type="number" /></label>
            </div>
            <label>交接策略<textarea v-model="member.handoffPolicy" rows="2" /></label>
            <div class="button-row">
              <label class="check-line"><input v-model="member.enabled" type="checkbox" /> 启用</label>
              <span>{{ agentName(member.agentId) }}</span>
              <button class="icon-button danger" type="button" title="移除" @click="removeMember(index)"><Trash2 :size="16" /></button>
            </div>
          </div>
        </div>
      </div>

      <div class="button-row right">
        <button class="secondary-button" type="button" @click="showEditor = false">取消</button>
        <button class="primary-button" type="button" :disabled="saving" @click="saveTeam">{{ saving ? '保存中...' : '保存团队' }}</button>
      </div>
    </section>
  </div>
</template>
