<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Building2, RefreshCw, Save, ShieldCheck, Trash2, UserPlus } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
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

const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const organizations = ref<OrganizationSummary[]>([]);
const workspaces = ref<WorkspaceSummary[]>([]);
const selectedWorkspace = ref<WorkspaceDetail | null>(null);

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
    orgForm.orgCode = '';
    orgForm.orgName = '';
    orgForm.description = '';
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
    memberForm.userId = '';
    memberForm.memberRole = 'member';
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
      <button class="primary-button" type="button" @click="resetWorkspaceForm">
        <Building2 :size="16" /> 新建空间
      </button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
  <p v-if="successMessage" class="form-success">{{ successMessage }}</p>

  <section class="metrics-grid">
    <StatCard label="组织数" :value="String(totals.orgCount)" detail="当前可见组织" icon="ShieldCheck" tone="info" />
    <StatCard label="工作空间" :value="String(totals.workspaceCount)" detail="资源隔离边界" icon="Workflow" tone="success" />
    <StatCard label="空间成员" :value="String(totals.memberCount)" detail="已授权用户" icon="Bot" tone="neutral" />
    <StatCard label="纳管资源" :value="String(totals.resourceCount)" detail="Agent/知识库/工具/工作流" icon="Gauge" tone="warning" />
  </section>

  <section class="settings-layout">
    <div class="section-block">
      <div class="section-title">
        <h2>组织列表</h2>
        <span>承载团队和租户边界</span>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>组织</th><th>成员</th><th>空间</th><th>状态</th></tr>
        </thead>
        <tbody>
          <tr v-for="org in organizations" :key="org.id">
            <td><b>{{ org.orgName }}</b><span class="muted block">{{ org.orgCode }}</span></td>
            <td>{{ org.memberCount }}</td>
            <td>{{ org.workspaceCount }}</td>
            <td><StatusBadge :label="org.status === 'enabled' ? '启用' : org.status" /></td>
          </tr>
        </tbody>
      </table>

      <div class="inline-form">
        <input v-model="orgForm.orgName" placeholder="组织名称" />
        <input v-model="orgForm.orgCode" placeholder="组织编码" />
        <button class="secondary-button" type="button" :disabled="loading || !orgForm.orgName" @click="saveOrganization">
          <Save :size="16" /> 保存组织
        </button>
      </div>
    </div>

    <div class="section-block">
      <div class="section-title">
        <h2>工作空间</h2>
        <span>Agent、知识库、工具、工作流的资源边界</span>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>空间</th><th>资源</th><th>成员</th><th>权限</th></tr>
        </thead>
        <tbody>
          <tr
            v-for="workspace in workspaces"
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
          </tr>
        </tbody>
      </table>
    </div>
  </section>

  <section class="settings-layout">
    <div class="section-block">
      <div class="section-title">
        <h2>{{ workspaceForm.id ? '编辑工作空间' : '创建工作空间' }}</h2>
        <span>设置资源归属和默认空间</span>
      </div>
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
      <button class="primary-button" type="button" :disabled="loading || !workspaceForm.workspaceName" @click="saveWorkspace">
        <Save :size="16" /> 保存空间
      </button>
    </div>

    <div class="section-block">
      <div class="section-title">
        <h2>成员与角色</h2>
        <span>{{ selectedWorkspace?.workspaceName || '请选择工作空间' }}</span>
      </div>
      <div class="inline-form">
        <input v-model="memberForm.userId" placeholder="用户 ID" />
        <select v-model="memberForm.memberRole">
          <option value="admin">管理员</option>
          <option value="member">成员</option>
          <option value="viewer">只读</option>
        </select>
        <button class="secondary-button" type="button" :disabled="loading || !selectedWorkspace || !memberForm.userId" @click="addMember">
          <UserPlus :size="16" /> 保存成员
        </button>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>用户</th><th>角色</th><th>状态</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="member in selectedWorkspace?.members || []" :key="member.id">
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

      <div v-if="selectedWorkspace" class="trace-meta">
        <span><ShieldCheck :size="14" /> 空间资源</span>
        <b>Agent {{ selectedWorkspace.agentCount }}</b>
        <b>知识库 {{ selectedWorkspace.knowledgeBaseCount }}</b>
        <b>工具 {{ selectedWorkspace.toolCount }}</b>
        <b>工作流 {{ selectedWorkspace.workflowCount }}</b>
      </div>
    </div>
  </section>
</template>
