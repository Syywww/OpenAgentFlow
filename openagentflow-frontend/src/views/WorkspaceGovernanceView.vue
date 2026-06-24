<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Pencil, Plus, RefreshCw, Save, ShieldCheck, Trash2, UserPlus, X } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import StatCard from '../components/StatCard.vue';
import StatusBadge from '../components/StatusBadge.vue';
import {
  createOrganization,
  createWorkspace,
  fetchOrganizations,
  fetchWorkspace,
  fetchWorkspaces,
  removeWorkspaceMember,
  saveWorkspaceMember,
  updateWorkspace,
  type OrganizationSummary,
  type WorkspaceDetail,
  type WorkspaceSummary,
} from '../api/workspaces';
import { usePagination } from '../composables/usePagination';

const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const organizations = ref<OrganizationSummary[]>([]);
const workspaces = ref<WorkspaceSummary[]>([]);
const selectedWorkspace = ref<WorkspaceDetail | null>(null);
const activePanel = ref<'organizations' | 'workspaces' | 'members'>('organizations');
const orgModalOpen = ref(false);
const workspaceModalOpen = ref(false);
const memberModalOpen = ref(false);
const members = computed(() => selectedWorkspace.value?.members || []);
const { currentPage: orgPage, pagedItems: pagedOrganizations } = usePagination(organizations);
const { currentPage: workspacePage, pagedItems: pagedWorkspaces } = usePagination(workspaces);
const { currentPage: memberPage, pagedItems: pagedMembers } = usePagination(members);

const orgForm = reactive({
  orgCode: '',
  orgName: '',
  description: '',
});

const workspaceForm = reactive({
  id: '',
  organizationId: '',
  workspaceCode: '',
  workspaceName: '',
  description: '',
  workspaceType: 'team',
  defaultFlag: false,
});

const memberForm = reactive({
  userId: '',
  memberRole: 'member',
});

const totals = computed(() => {
  const memberCount = workspaces.value.reduce((sum, item) => sum + Number(item.memberCount || 0), 0);
  const resourceCount = workspaces.value.reduce(
    (sum, item) => sum + item.agentCount + item.knowledgeBaseCount + item.toolCount + item.workflowCount,
    0,
  );
  return {
    orgCount: organizations.value.length,
    workspaceCount: workspaces.value.length,
    memberCount,
    resourceCount,
  };
});

onMounted(() => {
  void loadData();
});

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [orgResult, workspaceResult] = await Promise.all([fetchOrganizations(), fetchWorkspaces()]);
    organizations.value = orgResult;
    workspaces.value = workspaceResult;
    if (!workspaceForm.organizationId && orgResult[0]) {
      workspaceForm.organizationId = orgResult[0].id;
    }
    if (!selectedWorkspace.value && workspaceResult[0]) {
      await selectWorkspace(workspaceResult[0]);
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '组织空间数据加载失败';
  } finally {
    loading.value = false;
  }
}

async function selectWorkspace(workspace: WorkspaceSummary) {
  selectedWorkspace.value = await fetchWorkspace(workspace.id);
  workspaceForm.id = workspace.id;
  workspaceForm.organizationId = workspace.organizationId;
  workspaceForm.workspaceCode = workspace.workspaceCode;
  workspaceForm.workspaceName = workspace.workspaceName;
  workspaceForm.description = workspace.description || '';
  workspaceForm.workspaceType = workspace.workspaceType || 'team';
  workspaceForm.defaultFlag = Boolean(workspace.defaultFlag);
}

function resetWorkspaceForm() {
  workspaceForm.id = '';
  workspaceForm.workspaceCode = '';
  workspaceForm.workspaceName = '';
  workspaceForm.description = '';
  workspaceForm.workspaceType = 'team';
  workspaceForm.defaultFlag = false;
  workspaceForm.organizationId = organizations.value[0]?.id || '';
}

function openCreateWorkspaceModal() {
  resetWorkspaceForm();
  workspaceModalOpen.value = true;
}

async function openEditWorkspaceModal(workspace: WorkspaceSummary) {
  await selectWorkspace(workspace);
  workspaceModalOpen.value = true;
}

function closeWorkspaceModal() {
  workspaceModalOpen.value = false;
  resetWorkspaceForm();
}

function resetOrgForm() {
  orgForm.orgCode = '';
  orgForm.orgName = '';
  orgForm.description = '';
}

function openCreateOrgModal() {
  resetOrgForm();
  orgModalOpen.value = true;
}

function closeOrgModal() {
  orgModalOpen.value = false;
  resetOrgForm();
}

function resetMemberForm() {
  memberForm.userId = '';
  memberForm.memberRole = 'member';
}

function openCreateMemberModal() {
  resetMemberForm();
  memberModalOpen.value = true;
}

function closeMemberModal() {
  memberModalOpen.value = false;
  resetMemberForm();
}

async function saveOrganization() {
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    await createOrganization({
      orgCode: orgForm.orgCode || undefined,
      orgName: orgForm.orgName,
      description: orgForm.description || undefined,
    });
    closeOrgModal();
    successMessage.value = '组织已创建';
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '组织保存失败';
  } finally {
    loading.value = false;
  }
}

async function saveWorkspace() {
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  const payload = {
    organizationId: workspaceForm.organizationId || undefined,
    workspaceCode: workspaceForm.workspaceCode || undefined,
    workspaceName: workspaceForm.workspaceName,
    description: workspaceForm.description || undefined,
    workspaceType: workspaceForm.workspaceType,
    defaultFlag: workspaceForm.defaultFlag,
  };
  try {
    const detail = workspaceForm.id
      ? await updateWorkspace(workspaceForm.id, payload)
      : await createWorkspace(payload);
    selectedWorkspace.value = detail;
    successMessage.value = workspaceForm.id ? '工作空间已更新' : '工作空间已创建';
    closeWorkspaceModal();
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '工作空间保存失败';
  } finally {
    loading.value = false;
  }
}

async function addMember() {
  if (!selectedWorkspace.value) return;
  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    selectedWorkspace.value = await saveWorkspaceMember(selectedWorkspace.value.id, {
      userId: memberForm.userId,
      memberRole: memberForm.memberRole,
    });
    closeMemberModal();
    successMessage.value = '成员已保存';
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '成员保存失败';
  } finally {
    loading.value = false;
  }
}

async function removeMember(userId: string) {
  if (!selectedWorkspace.value) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    selectedWorkspace.value = await removeWorkspaceMember(selectedWorkspace.value.id, userId);
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '成员移除失败';
  } finally {
    loading.value = false;
  }
}

function roleLabel(role?: string) {
  const map: Record<string, string> = {
    owner: '所有者',
    admin: '管理员',
    member: '成员',
    viewer: '只读',
  };
  return map[role || 'member'] || role || '成员';
}
</script>

<template>
  <PageHeader title="组织空间" description="组织、工作空间、成员与核心资源归属治理">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadData">
        <RefreshCw :size="16" /> 刷新
      </button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
  <p v-if="successMessage" class="form-success">{{ successMessage }}</p>

  <section class="metric-grid">
    <StatCard label="组织数" :value="String(totals.orgCount)" detail="当前可见组织" icon="ShieldCheck" tone="info" />
    <StatCard label="工作空间" :value="String(totals.workspaceCount)" detail="资源隔离边界" icon="Workflow" tone="success" />
    <StatCard label="空间成员" :value="String(totals.memberCount)" detail="已授权用户" icon="Bot" tone="neutral" />
    <StatCard label="纳管资源" :value="String(totals.resourceCount)" detail="Agent/知识库/工具/工作流" icon="Gauge" tone="warning" />
  </section>

  <section class="governance-card-tabs">
    <button class="governance-tab-card" :class="{ active: activePanel === 'organizations' }" type="button" @click="activePanel = 'organizations'">
      <span>组织列表</span>
      <b>{{ organizations.length }}</b>
      <small>团队、租户和组织边界</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'workspaces' }" type="button" @click="activePanel = 'workspaces'">
      <span>工作空间</span>
      <b>{{ workspaces.length }}</b>
      <small>Agent、知识库、工具、工作流资源边界</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'members' }" type="button" @click="activePanel = 'members'">
      <span>成员与角色</span>
      <b>{{ members.length }}</b>
      <small>{{ selectedWorkspace?.workspaceName || '请选择工作空间' }}</small>
    </button>
  </section>

  <section class="section-block workspace-governance-panel">
    <template v-if="activePanel === 'organizations'">
      <div class="section-title">
        <h2>组织列表</h2>
        <div class="title-actions">
          <span>承载团队和租户边界</span>
          <button class="primary-button slim" type="button" @click="openCreateOrgModal">
            <Plus :size="14" /> 新增组织
          </button>
        </div>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>组织</th><th>成员</th><th>空间</th><th>状态</th></tr>
        </thead>
        <tbody>
          <tr v-for="org in pagedOrganizations" :key="org.id">
            <td><b>{{ org.orgName }}</b><span class="muted block">{{ org.orgCode }}</span></td>
            <td>{{ org.memberCount }}</td>
            <td>{{ org.workspaceCount }}</td>
            <td><StatusBadge :label="org.status === 'enabled' ? '启用' : org.status" /></td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="orgPage" :total="organizations.length" />
    </template>

    <template v-else-if="activePanel === 'workspaces'">
      <div class="section-title">
        <h2>工作空间</h2>
        <div class="title-actions">
          <span>Agent、知识库、工具、工作流的资源边界</span>
          <button class="primary-button slim" type="button" @click="openCreateWorkspaceModal">
            <Plus :size="14" /> 新增工作空间
          </button>
        </div>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>空间</th><th>资源</th><th>成员</th><th>权限</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr
            v-for="workspace in pagedWorkspaces"
            :key="workspace.id"
            :class="{ selected: selectedWorkspace?.id === workspace.id }"
            @click="selectWorkspace(workspace)"
          >
            <td>
              <b>{{ workspace.workspaceName }}</b>
              <span class="muted block">{{ workspace.organizationName }} · {{ workspace.workspaceCode }}</span>
            </td>
            <td>
              A{{ workspace.agentCount }} / K{{ workspace.knowledgeBaseCount }} /
              T{{ workspace.toolCount }} / W{{ workspace.workflowCount }}
            </td>
            <td>{{ workspace.memberCount }}</td>
            <td><StatusBadge :label="roleLabel(workspace.currentUserRole)" /></td>
            <td>
              <button class="secondary-button slim" type="button" @click.stop="openEditWorkspaceModal(workspace)">
                <Pencil :size="14" /> 编辑
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="workspacePage" :total="workspaces.length" />
    </template>

    <template v-else>
      <div class="section-title">
        <h2>成员与角色</h2>
        <div class="title-actions">
          <span>{{ selectedWorkspace?.workspaceName || '请选择工作空间' }}</span>
          <button class="primary-button slim" type="button" :disabled="!selectedWorkspace" @click="openCreateMemberModal">
            <UserPlus :size="14" /> 新增成员与角色
          </button>
        </div>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>用户</th><th>角色</th><th>状态</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="member in pagedMembers" :key="member.id">
            <td>
              <b>{{ member.displayName || member.username || member.userId }}</b>
              <span class="muted block">{{ member.userId }}</span>
            </td>
            <td><StatusBadge :label="roleLabel(member.memberRole)" /></td>
            <td><StatusBadge :label="member.status === 'enabled' ? '启用' : member.status" /></td>
            <td>
              <button class="secondary-button slim" type="button" :disabled="member.memberRole === 'owner'" @click="removeMember(member.userId)">
                <Trash2 :size="14" /> 移除
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="memberPage" :total="members.length" />

      <div v-if="selectedWorkspace" class="trace-meta">
        <span><ShieldCheck :size="14" /> 空间资源</span>
        <b>Agent {{ selectedWorkspace.agentCount }}</b>
        <b>知识库 {{ selectedWorkspace.knowledgeBaseCount }}</b>
        <b>工具 {{ selectedWorkspace.toolCount }}</b>
        <b>工作流 {{ selectedWorkspace.workflowCount }}</b>
      </div>
    </template>
  </section>

  <div v-if="orgModalOpen" class="overlay-backdrop" @click.self="closeOrgModal">
    <section class="modal-panel organization-modal">
      <header class="overlay-header">
        <div>
          <h2>新增组织</h2>
          <p class="muted">创建团队或租户边界，用于承载工作空间和成员治理。</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closeOrgModal"><X :size="18" /></button>
      </header>
      <div class="form-grid">
        <label>
          组织名称
          <input v-model="orgForm.orgName" placeholder="如：研发中心" />
        </label>
        <label>
          组织编码
          <input v-model="orgForm.orgCode" placeholder="不填则后端自动生成" />
        </label>
        <label class="wide">
          组织描述
          <textarea v-model="orgForm.description" rows="3" placeholder="描述组织职责或业务边界"></textarea>
        </label>
      </div>
      <div class="toolbar compact">
        <button class="secondary-button" type="button" @click="closeOrgModal">取消</button>
        <button class="primary-button" type="button" :disabled="loading || !orgForm.orgName" @click="saveOrganization">
          <Save :size="16" /> 保存组织
        </button>
      </div>
    </section>
  </div>

  <div v-if="workspaceModalOpen" class="overlay-backdrop" @click.self="closeWorkspaceModal">
    <section class="modal-panel workspace-modal">
      <header class="overlay-header">
        <div>
          <h2>{{ workspaceForm.id ? '编辑工作空间' : '新增工作空间' }}</h2>
          <p class="muted">设置资源归属、空间类型和默认空间标记。</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closeWorkspaceModal"><X :size="18" /></button>
      </header>
      <div class="form-grid">
        <label>
          所属组织
          <select v-model="workspaceForm.organizationId">
            <option v-for="org in organizations" :key="org.id" :value="org.id">{{ org.orgName }}</option>
          </select>
        </label>
        <label>
          空间名称
          <input v-model="workspaceForm.workspaceName" placeholder="如：研发空间" />
        </label>
        <label>
          空间编码
          <input v-model="workspaceForm.workspaceCode" placeholder="如：dev-workspace" />
        </label>
        <label>
          空间类型
          <select v-model="workspaceForm.workspaceType">
            <option value="team">团队空间</option>
            <option value="project">项目空间</option>
            <option value="personal">个人空间</option>
          </select>
        </label>
        <label class="wide">
          描述
          <textarea v-model="workspaceForm.description" rows="3" placeholder="描述这个空间的用途和资源范围"></textarea>
        </label>
        <label class="checkbox-line">
          <input v-model="workspaceForm.defaultFlag" type="checkbox" />
          默认工作空间
        </label>
      </div>
      <div class="toolbar compact">
        <button class="secondary-button" type="button" @click="closeWorkspaceModal">取消</button>
        <button class="primary-button" type="button" :disabled="loading || !workspaceForm.workspaceName" @click="saveWorkspace">
          <Save :size="16" /> 保存空间
        </button>
      </div>
    </section>
  </div>

  <div v-if="memberModalOpen" class="overlay-backdrop" @click.self="closeMemberModal">
    <section class="modal-panel member-role-modal">
      <header class="overlay-header">
        <div>
          <h2>新增成员与角色</h2>
          <p class="muted">{{ selectedWorkspace?.workspaceName || '请选择工作空间' }}</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closeMemberModal"><X :size="18" /></button>
      </header>
      <div class="form-grid">
        <label>
          用户 ID
          <input v-model="memberForm.userId" placeholder="请输入用户 ID" />
        </label>
        <label>
          成员角色
          <select v-model="memberForm.memberRole">
            <option value="admin">管理员</option>
            <option value="member">成员</option>
            <option value="viewer">只读</option>
          </select>
        </label>
      </div>
      <div class="toolbar compact">
        <button class="secondary-button" type="button" @click="closeMemberModal">取消</button>
        <button class="primary-button" type="button" :disabled="loading || !selectedWorkspace || !memberForm.userId" @click="addMember">
          <UserPlus :size="16" /> 保存成员与角色
        </button>
      </div>
    </section>
  </div>
</template>
