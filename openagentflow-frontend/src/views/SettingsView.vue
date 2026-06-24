<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Bell, Building2, FileClock, KeyRound, Pencil, Plus, Save, TestTube2, Trash2, UserCog, UserPlus, X } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import PaginationBar from '../components/PaginationBar.vue';
import StatusBadge from '../components/StatusBadge.vue';
import {
  createIamDepartment,
  createIamRole,
  createIamUser,
  deleteIamDepartment,
  deleteIamRole,
  deleteIamUser,
  fetchIamDepartments,
  fetchIamOverview,
  fetchIamPermissions,
  fetchIamRoles,
  fetchIamUsers,
  updateIamDepartment,
  updateIamRole,
  updateIamUser,
  type DepartmentNode,
  type DepartmentRequest,
  type IamOverview,
  type PermissionNode,
  type RoleRequest,
  type RoleSummary,
  type UserRequest,
  type UserSummary,
} from '../api/iam';
import {
  createModelProvider,
  deleteModelProvider,
  fetchModelProviders,
  testModelProvider,
  updateModelProvider,
  type ModelProviderRequest,
  type ModelProviderSummary,
} from '../api/models';
import { useOverlay } from '../composables/useOverlay';
import { usePagination } from '../composables/usePagination';

interface FlatDepartment {
  node: DepartmentNode;
  depth: number;
}

interface FlatPermission {
  node: PermissionNode;
  depth: number;
}

const { showDrawer, showModal } = useOverlay();
const overview = ref<IamOverview>({ userCount: 0, departmentCount: 0, roleCount: 0, permissionCount: 0 });
const users = ref<UserSummary[]>([]);
const departments = ref<DepartmentNode[]>([]);
const roles = ref<RoleSummary[]>([]);
const permissions = ref<PermissionNode[]>([]);
const providers = ref<ModelProviderSummary[]>([]);
const modelRows = computed(() => providers.value.flatMap((provider) => provider.models.map((model) => ({ provider, model }))));
const flatDepartments = computed(() => flattenDepartments(departments.value));
const flatPermissions = computed(() => flattenPermissions(permissions.value));
const { currentPage: userPage, pagedItems: pagedUsers } = usePagination(users);
const { currentPage: deptPage, pagedItems: pagedDepartments } = usePagination(flatDepartments);
const { currentPage: rolePage, pagedItems: pagedRoles } = usePagination(roles);
const { currentPage: providerPage, pagedItems: pagedProviders } = usePagination(providers);
const { currentPage: modelPage, pagedItems: pagedModelRows } = usePagination(modelRows);
const editingProviderId = ref('');
const editingUserId = ref('');
const editingDepartmentId = ref('');
const editingRoleId = ref('');
const loading = ref(false);
const errorMessage = ref('');
const testMessage = ref('');
const activePanel = ref<'users' | 'departments' | 'roles' | 'providers' | 'models'>('users');
const providerModalOpen = ref(false);
const userModalOpen = ref(false);
const departmentModalOpen = ref(false);
const roleModalOpen = ref(false);

const userForm = reactive<UserRequest>({
  departmentId: '',
  username: '',
  email: '',
  phone: '',
  password: '',
  displayName: '',
  status: 'enabled',
  roleIds: [],
});

const departmentForm = reactive<DepartmentRequest>({
  parentId: '',
  deptCode: '',
  deptName: '',
  sortOrder: 0,
  status: 'enabled',
});

const roleForm = reactive<RoleRequest>({
  roleCode: '',
  roleName: '',
  description: '',
  status: 'enabled',
  permissionIds: [],
});

const providerForm = reactive({
  providerCode: 'doubao',
  providerName: '豆包 Doubao',
  providerType: 'openai_compatible',
  baseUrl: 'https://ark.cn-beijing.volces.com/api/v3',
  authType: 'api_key',
  apiKey: '',
  status: 'enabled',
  modelId: '',
  modelCode: 'ep-20260605102340-bwv2d',
  modelName: '豆包接入点 ep-20260605102340-bwv2d',
  modelType: 'chat',
  contextWindow: 32768,
  maxOutputTokens: 4096,
  inputPricePer1k: 0,
  outputPricePer1k: 0,
  supportStream: true,
  supportFunctionCalling: false,
  supportVision: false,
  isDefault: true,
});

onMounted(() => {
  void loadData();
});

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [overviewData, userList, departmentTree, roleList, permissionTree, providerList] = await Promise.all([
      fetchIamOverview(),
      fetchIamUsers(),
      fetchIamDepartments(),
      fetchIamRoles(),
      fetchIamPermissions(),
      fetchModelProviders(),
    ]);
    overview.value = overviewData;
    users.value = userList;
    departments.value = departmentTree;
    roles.value = roleList;
    permissions.value = permissionTree;
    providers.value = providerList;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '系统设置数据加载失败';
  } finally {
    loading.value = false;
  }
}

function flattenDepartments(items: DepartmentNode[], depth = 0): FlatDepartment[] {
  return items.flatMap((node) => [{ node, depth }, ...flattenDepartments(node.children || [], depth + 1)]);
}

function flattenPermissions(items: PermissionNode[], depth = 0): FlatPermission[] {
  return items.flatMap((node) => [{ node, depth }, ...flattenPermissions(node.children || [], depth + 1)]);
}

function resetUserForm() {
  editingUserId.value = '';
  userForm.departmentId = '';
  userForm.username = '';
  userForm.email = '';
  userForm.phone = '';
  userForm.password = '';
  userForm.displayName = '';
  userForm.status = 'enabled';
  userForm.roleIds = [];
}

function openCreateUserModal() {
  resetUserForm();
  userModalOpen.value = true;
}

function editUser(user: UserSummary) {
  editingUserId.value = user.id;
  userForm.departmentId = user.departmentId || '';
  userForm.username = user.username;
  userForm.email = user.email || '';
  userForm.phone = user.phone || '';
  userForm.password = '';
  userForm.displayName = user.displayName;
  userForm.status = user.status;
  userForm.roleIds = [...(user.roleIds || [])];
  userModalOpen.value = true;
}

function closeUserModal() {
  userModalOpen.value = false;
  resetUserForm();
}

async function saveUser() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const payload = { ...userForm, departmentId: userForm.departmentId || undefined, password: userForm.password || undefined };
    if (editingUserId.value) {
      await updateIamUser(editingUserId.value, payload);
    } else {
      await createIamUser(payload);
    }
    closeUserModal();
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '用户保存失败';
  } finally {
    loading.value = false;
  }
}

async function removeUser(user: UserSummary) {
  loading.value = true;
  errorMessage.value = '';
  try {
    await deleteIamUser(user.id);
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '用户删除失败';
  } finally {
    loading.value = false;
  }
}

function resetDepartmentForm() {
  editingDepartmentId.value = '';
  departmentForm.parentId = '';
  departmentForm.deptCode = '';
  departmentForm.deptName = '';
  departmentForm.sortOrder = 0;
  departmentForm.status = 'enabled';
}

function openCreateDepartmentModal() {
  resetDepartmentForm();
  departmentModalOpen.value = true;
}

function editDepartment(row: FlatDepartment) {
  editingDepartmentId.value = row.node.id;
  departmentForm.parentId = row.node.parentId || '';
  departmentForm.deptCode = row.node.deptCode;
  departmentForm.deptName = row.node.deptName;
  departmentForm.sortOrder = row.node.sortOrder || 0;
  departmentForm.status = row.node.status;
  departmentModalOpen.value = true;
}

function closeDepartmentModal() {
  departmentModalOpen.value = false;
  resetDepartmentForm();
}

async function saveDepartment() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const payload = { ...departmentForm, parentId: departmentForm.parentId || undefined };
    if (editingDepartmentId.value) {
      await updateIamDepartment(editingDepartmentId.value, payload);
    } else {
      await createIamDepartment(payload);
    }
    closeDepartmentModal();
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '部门保存失败';
  } finally {
    loading.value = false;
  }
}

async function removeDepartment(row: FlatDepartment) {
  loading.value = true;
  errorMessage.value = '';
  try {
    await deleteIamDepartment(row.node.id);
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '部门删除失败';
  } finally {
    loading.value = false;
  }
}

function resetRoleForm() {
  editingRoleId.value = '';
  roleForm.roleCode = '';
  roleForm.roleName = '';
  roleForm.description = '';
  roleForm.status = 'enabled';
  roleForm.permissionIds = [];
}

function openCreateRoleModal() {
  resetRoleForm();
  roleModalOpen.value = true;
}

function editRole(role: RoleSummary) {
  editingRoleId.value = role.id;
  roleForm.roleCode = role.roleCode;
  roleForm.roleName = role.roleName;
  roleForm.description = role.description || '';
  roleForm.status = role.status;
  roleForm.permissionIds = [...(role.permissionIds || [])];
  roleModalOpen.value = true;
}

function closeRoleModal() {
  roleModalOpen.value = false;
  resetRoleForm();
}

async function saveRole() {
  loading.value = true;
  errorMessage.value = '';
  try {
    if (editingRoleId.value) {
      await updateIamRole(editingRoleId.value, roleForm);
    } else {
      await createIamRole(roleForm);
    }
    closeRoleModal();
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '角色保存失败';
  } finally {
    loading.value = false;
  }
}

async function removeRole(role: RoleSummary) {
  loading.value = true;
  errorMessage.value = '';
  try {
    await deleteIamRole(role.id);
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '角色删除失败';
  } finally {
    loading.value = false;
  }
}

function resetProviderForm() {
  editingProviderId.value = '';
  providerForm.providerCode = 'doubao';
  providerForm.providerName = '豆包 Doubao';
  providerForm.providerType = 'openai_compatible';
  providerForm.baseUrl = 'https://ark.cn-beijing.volces.com/api/v3';
  providerForm.authType = 'api_key';
  providerForm.apiKey = '';
  providerForm.status = 'enabled';
  providerForm.modelId = '';
  providerForm.modelCode = 'ep-20260605102340-bwv2d';
  providerForm.modelName = '豆包接入点 ep-20260605102340-bwv2d';
  providerForm.modelType = 'chat';
  providerForm.contextWindow = 32768;
  providerForm.maxOutputTokens = 4096;
  providerForm.inputPricePer1k = 0;
  providerForm.outputPricePer1k = 0;
  providerForm.supportStream = true;
  providerForm.supportFunctionCalling = false;
  providerForm.supportVision = false;
  providerForm.isDefault = true;
}

function openCreateProviderModal() {
  resetProviderForm();
  providerModalOpen.value = true;
}

function closeProviderModal() {
  providerModalOpen.value = false;
  resetProviderForm();
}

function editProvider(provider: ModelProviderSummary) {
  const firstChatModel = provider.models.find((model) => model.modelType === 'chat') ?? provider.models[0];
  editingProviderId.value = provider.id;
  providerForm.providerCode = provider.providerCode;
  providerForm.providerName = provider.providerName;
  providerForm.providerType = provider.providerType;
  providerForm.baseUrl = provider.baseUrl;
  providerForm.authType = provider.authType || 'api_key';
  providerForm.apiKey = '';
  providerForm.status = provider.status;
  providerForm.modelId = firstChatModel?.id ?? '';
  providerForm.modelCode = firstChatModel?.modelCode ?? '';
  providerForm.modelName = firstChatModel?.modelName ?? '';
  providerForm.modelType = firstChatModel?.modelType ?? 'chat';
  providerForm.contextWindow = firstChatModel?.contextWindow ?? 32768;
  providerForm.maxOutputTokens = firstChatModel?.maxOutputTokens ?? 4096;
  providerForm.inputPricePer1k = firstChatModel?.inputPricePer1k ?? 0;
  providerForm.outputPricePer1k = firstChatModel?.outputPricePer1k ?? 0;
  providerForm.supportStream = firstChatModel?.supportStream ?? true;
  providerForm.supportFunctionCalling = firstChatModel?.supportFunctionCalling ?? false;
  providerForm.supportVision = firstChatModel?.supportVision ?? false;
  providerForm.isDefault = firstChatModel?.isDefault ?? false;
  providerModalOpen.value = true;
}

function buildProviderPayload(): ModelProviderRequest {
  return {
    providerCode: providerForm.providerCode,
    providerName: providerForm.providerName,
    providerType: providerForm.providerType,
    baseUrl: providerForm.baseUrl,
    authType: providerForm.authType,
    apiKey: providerForm.apiKey || undefined,
    status: providerForm.status,
    sortOrder: 0,
    models: [
      {
        id: providerForm.modelId || undefined,
        modelCode: providerForm.modelCode,
        modelName: providerForm.modelName,
        modelType: providerForm.modelType,
        contextWindow: providerForm.contextWindow,
        maxOutputTokens: providerForm.maxOutputTokens,
        inputPricePer1k: providerForm.inputPricePer1k,
        outputPricePer1k: providerForm.outputPricePer1k,
        supportStream: providerForm.supportStream,
        supportFunctionCalling: providerForm.supportFunctionCalling,
        supportVision: providerForm.supportVision,
        defaultParams: '{}',
        status: 'enabled',
        isDefault: providerForm.isDefault,
      },
    ],
  };
}

async function saveProvider() {
  loading.value = true;
  errorMessage.value = '';
  testMessage.value = '';
  try {
    if (editingProviderId.value) {
      await updateModelProvider(editingProviderId.value, buildProviderPayload());
    } else {
      await createModelProvider(buildProviderPayload());
    }
    closeProviderModal();
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '模型服务商保存失败';
  } finally {
    loading.value = false;
  }
}

async function removeProvider(provider: ModelProviderSummary) {
  loading.value = true;
  errorMessage.value = '';
  try {
    await deleteModelProvider(provider.id);
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '模型服务商删除失败';
  } finally {
    loading.value = false;
  }
}

async function runProviderTest(provider: ModelProviderSummary) {
  loading.value = true;
  errorMessage.value = '';
  testMessage.value = '';
  try {
    const result = await testModelProvider(provider.id);
    testMessage.value = result.success
      ? `连通成功，耗时 ${result.latencyMs}ms，${result.responseText ?? ''}`
      : `连通失败：${result.errorMessage ?? '未知错误'}`;
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '模型连通性测试失败';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <PageHeader title="系统设置" description="用户权限、部门组织、模型供应商、通知中心与操作审计">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadData">刷新</button>
      <button class="secondary-button" type="button" @click="showDrawer('notices')"><Bell :size="16" /> 通知中心</button>
      <button class="secondary-button" type="button" @click="showModal('audit')"><FileClock :size="16" /> 操作日志详情</button>
    </template>
  </PageHeader>

  <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
  <p v-if="testMessage" class="form-success">{{ testMessage }}</p>

  <section class="governance-card-tabs">
    <button class="governance-tab-card" :class="{ active: activePanel === 'users' }" type="button" @click="activePanel = 'users'">
      <span>用户管理</span>
      <b>{{ overview.userCount || users.length }}</b>
      <small>用户 CRUD、所属部门和系统角色</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'departments' }" type="button" @click="activePanel = 'departments'">
      <span>部门树</span>
      <b>{{ overview.departmentCount || flatDepartments.length }}</b>
      <small>部门层级、状态和用户归属</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'roles' }" type="button" @click="activePanel = 'roles'">
      <span>角色权限</span>
      <b>{{ overview.roleCount || roles.length }}</b>
      <small>角色 CRUD 和权限勾选</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'providers' }" type="button" @click="activePanel = 'providers'">
      <span>模型供应商配置</span>
      <b>{{ providers.length }}</b>
      <small>OpenAI-compatible / Ollama / Qwen 等</small>
    </button>
    <button class="governance-tab-card" :class="{ active: activePanel === 'models' }" type="button" @click="activePanel = 'models'">
      <span>模型列表</span>
      <b>{{ modelRows.length }}</b>
      <small>单价、流式和启用状态</small>
    </button>
  </section>

  <section class="section-block settings-panel">
    <template v-if="activePanel === 'users'">
      <div class="section-title">
        <h2>用户管理</h2>
        <div class="title-actions">
          <span>用户所属部门和系统级角色在这里统一设置</span>
          <button class="primary-button slim" type="button" @click="openCreateUserModal"><UserPlus :size="14" /> 新增用户</button>
        </div>
      </div>
      <table class="data-table rich">
        <thead><tr><th>用户</th><th>角色</th><th>所属部门</th><th>来源</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="user in pagedUsers" :key="user.id">
            <td><b>{{ user.displayName }}</b><span class="muted block">{{ user.username }} / {{ user.email || '未设置邮箱' }}</span></td>
            <td>{{ user.roleNames.length ? user.roleNames.join('、') : '未分配' }}</td>
            <td>{{ user.departmentName || '未设置' }}</td>
            <td>{{ user.sourceType }}</td>
            <td><StatusBadge :label="user.status === 'enabled' ? '启用' : '禁用'" :tone="user.status === 'enabled' ? 'success' : 'warning'" /></td>
            <td>
              <div class="table-actions">
                <button class="icon-button" type="button" title="编辑用户" @click="editUser(user)"><Pencil :size="16" /></button>
                <button class="icon-button danger-text" type="button" title="删除用户" :disabled="user.username === 'admin'" @click="removeUser(user)"><Trash2 :size="16" /></button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && users.length === 0"><td colspan="6"><div class="empty-state">暂无用户</div></td></tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="userPage" :total="users.length" />
    </template>

    <template v-else-if="activePanel === 'departments'">
      <div class="section-title">
        <h2>部门树</h2>
        <div class="title-actions">
          <span>按层级维护部门，用户弹框可选择所属部门</span>
          <button class="primary-button slim" type="button" @click="openCreateDepartmentModal"><Building2 :size="14" /> 新增部门</button>
        </div>
      </div>
      <table class="data-table">
        <thead><tr><th>部门</th><th>编码</th><th>直属用户</th><th>排序</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="row in pagedDepartments" :key="row.node.id">
            <td>
              <b :style="{ paddingLeft: `${row.depth * 18}px` }">{{ row.depth ? '└ ' : '' }}{{ row.node.deptName }}</b>
              <span class="muted block" :style="{ paddingLeft: `${row.depth * 18}px` }">{{ row.node.parentId ? '子部门' : '根部门' }}</span>
            </td>
            <td class="mono">{{ row.node.deptCode }}</td>
            <td>{{ row.node.userCount }}</td>
            <td>{{ row.node.sortOrder }}</td>
            <td><StatusBadge :label="row.node.status === 'enabled' ? '启用' : '禁用'" /></td>
            <td>
              <div class="table-actions">
                <button class="icon-button" type="button" title="编辑部门" @click="editDepartment(row)"><Pencil :size="16" /></button>
                <button class="icon-button danger-text" type="button" title="删除部门" @click="removeDepartment(row)"><Trash2 :size="16" /></button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && flatDepartments.length === 0"><td colspan="6"><div class="empty-state">暂无部门</div></td></tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="deptPage" :total="flatDepartments.length" />
    </template>

    <template v-else-if="activePanel === 'roles'">
      <div class="section-title">
        <h2>角色权限</h2>
        <div class="title-actions">
          <span>{{ overview.permissionCount || flatPermissions.length }} 个权限点可配置</span>
          <button class="primary-button slim" type="button" @click="openCreateRoleModal"><KeyRound :size="14" /> 新增角色</button>
        </div>
      </div>
      <table class="data-table rich">
        <thead><tr><th>角色</th><th>绑定用户</th><th>权限数量</th><th>状态</th><th>类型</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="role in pagedRoles" :key="role.id">
            <td><b>{{ role.roleName }}</b><span class="muted block mono">{{ role.roleCode }}</span></td>
            <td>{{ role.userCount }}</td>
            <td>{{ role.permissionIds.length }}</td>
            <td><StatusBadge :label="role.status === 'enabled' ? '启用' : '禁用'" /></td>
            <td>{{ role.builtIn ? '内置' : '自定义' }}</td>
            <td>
              <div class="table-actions">
                <button class="icon-button" type="button" title="编辑角色权限" @click="editRole(role)"><UserCog :size="16" /></button>
                <button class="icon-button danger-text" type="button" title="删除角色" :disabled="role.builtIn" @click="removeRole(role)"><Trash2 :size="16" /></button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && roles.length === 0"><td colspan="6"><div class="empty-state">暂无角色</div></td></tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="rolePage" :total="roles.length" />
    </template>

    <template v-else-if="activePanel === 'providers'">
      <div class="section-title">
        <h2>模型供应商配置</h2>
        <div class="title-actions">
          <span>OpenAI-compatible / Ollama / Qwen / DeepSeek / Doubao</span>
          <button class="primary-button slim" type="button" @click="openCreateProviderModal"><Plus :size="14" /> 新增服务商</button>
        </div>
      </div>

      <div class="provider-grid">
        <article v-for="provider in pagedProviders" :key="provider.id" class="provider-card">
          <div><h3>{{ provider.providerName }}</h3><span>{{ provider.providerType }}</span></div>
          <StatusBadge :label="provider.healthStatus" :tone="provider.healthStatus === 'healthy' ? 'success' : provider.healthStatus === 'unhealthy' ? 'danger' : 'warning'" />
          <p>{{ provider.models.length }} 个模型 · {{ provider.baseUrl }}</p>
          <p class="mono">{{ provider.keyMask || '未配置 API Key' }}</p>
          <div class="row-actions">
            <button class="secondary-button slim" type="button" @click="editProvider(provider)"><Pencil :size="14" /> 编辑</button>
            <button class="secondary-button slim" type="button" :disabled="loading" @click="runProviderTest(provider)"><TestTube2 :size="14" /> 测试</button>
            <button class="secondary-button slim danger-text" type="button" :disabled="loading" @click="removeProvider(provider)"><Trash2 :size="14" /> 删除</button>
          </div>
        </article>
      </div>
      <PaginationBar v-model:page="providerPage" :total="providers.length" />
    </template>

    <template v-else>
      <div class="section-title"><h2>模型列表</h2><span>{{ modelRows.length }} 个模型</span></div>
      <table class="data-table">
        <thead><tr><th>供应商</th><th>模型</th><th>类型</th><th>单价 / 1K Token</th><th>流式</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="row in pagedModelRows" :key="row.model.id">
            <td><b>{{ row.provider.providerName }}</b></td>
            <td>{{ row.model.modelName }}<span class="muted block mono">{{ row.model.modelCode }}</span></td>
            <td>{{ row.model.modelType }}</td>
            <td>输入 ¥{{ Number(row.model.inputPricePer1k || 0).toFixed(6) }}<span class="muted block">输出 ¥{{ Number(row.model.outputPricePer1k || 0).toFixed(6) }}</span></td>
            <td>{{ row.model.supportStream ? '支持' : '不支持' }}</td>
            <td><StatusBadge :label="row.model.status" /></td>
          </tr>
        </tbody>
      </table>
      <PaginationBar v-model:page="modelPage" :total="modelRows.length" />
    </template>
  </section>

  <div v-if="userModalOpen" class="overlay-backdrop" @click.self="closeUserModal">
    <section class="modal-panel iam-user-modal">
      <header class="overlay-header">
        <div>
          <h2>{{ editingUserId ? '编辑用户' : '新增用户' }}</h2>
          <p class="muted">设置登录账号、所属部门和系统级角色。</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closeUserModal"><X :size="18" /></button>
      </header>
      <div class="settings-form">
        <label>用户名<input v-model="userForm.username" :disabled="!!editingUserId" /></label>
        <label>显示名称<input v-model="userForm.displayName" /></label>
        <label>邮箱<input v-model="userForm.email" /></label>
        <label>手机号<input v-model="userForm.phone" /></label>
        <label>所属部门
          <select v-model="userForm.departmentId">
            <option value="">未设置</option>
            <option v-for="row in flatDepartments" :key="row.node.id" :value="row.node.id">{{ '　'.repeat(row.depth) }}{{ row.node.deptName }}</option>
          </select>
        </label>
        <label>状态
          <select v-model="userForm.status">
            <option value="enabled">启用</option>
            <option value="disabled">禁用</option>
          </select>
        </label>
        <label class="wide">密码<input v-model="userForm.password" type="password" :placeholder="editingUserId ? '留空表示不修改密码' : '创建用户必须填写密码'" /></label>
        <div class="wide">
          <b>系统角色</b>
          <div class="permission-check-grid">
            <label v-for="role in roles" :key="role.id" class="check-line">
              <input v-model="userForm.roleIds" type="checkbox" :value="role.id" />
              {{ role.roleName }} <span class="muted mono">{{ role.roleCode }}</span>
            </label>
          </div>
        </div>
      </div>
      <div class="toolbar compact">
        <button class="secondary-button" type="button" @click="closeUserModal">取消</button>
        <button class="primary-button" type="button" :disabled="loading || !userForm.username || !userForm.displayName" @click="saveUser"><Save :size="16" /> 保存用户</button>
      </div>
    </section>
  </div>

  <div v-if="departmentModalOpen" class="overlay-backdrop" @click.self="closeDepartmentModal">
    <section class="modal-panel iam-department-modal">
      <header class="overlay-header">
        <div>
          <h2>{{ editingDepartmentId ? '编辑部门' : '新增部门' }}</h2>
          <p class="muted">维护部门编码、父级和启停状态。</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closeDepartmentModal"><X :size="18" /></button>
      </header>
      <div class="settings-form">
        <label>父部门
          <select v-model="departmentForm.parentId">
            <option value="">根部门</option>
            <option v-for="row in flatDepartments" :key="row.node.id" :value="row.node.id" :disabled="row.node.id === editingDepartmentId">{{ '　'.repeat(row.depth) }}{{ row.node.deptName }}</option>
          </select>
        </label>
        <label>部门编码<input v-model="departmentForm.deptCode" /></label>
        <label>部门名称<input v-model="departmentForm.deptName" /></label>
        <label>排序<input v-model.number="departmentForm.sortOrder" type="number" /></label>
        <label>状态
          <select v-model="departmentForm.status">
            <option value="enabled">启用</option>
            <option value="disabled">禁用</option>
          </select>
        </label>
      </div>
      <div class="toolbar compact">
        <button class="secondary-button" type="button" @click="closeDepartmentModal">取消</button>
        <button class="primary-button" type="button" :disabled="loading || !departmentForm.deptCode || !departmentForm.deptName" @click="saveDepartment"><Save :size="16" /> 保存部门</button>
      </div>
    </section>
  </div>

  <div v-if="roleModalOpen" class="overlay-backdrop" @click.self="closeRoleModal">
    <section class="modal-panel iam-role-modal">
      <header class="overlay-header">
        <div>
          <h2>{{ editingRoleId ? '编辑角色权限' : '新增角色' }}</h2>
          <p class="muted">配置角色基础信息，并勾选该角色拥有的菜单和 API 权限。</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closeRoleModal"><X :size="18" /></button>
      </header>
      <div class="settings-form">
        <label>角色编码<input v-model="roleForm.roleCode" /></label>
        <label>角色名称<input v-model="roleForm.roleName" /></label>
        <label>状态
          <select v-model="roleForm.status">
            <option value="enabled">启用</option>
            <option value="disabled">禁用</option>
          </select>
        </label>
        <label class="wide">描述<textarea v-model="roleForm.description" rows="3" /></label>
        <div class="wide">
          <b>权限配置</b>
          <div class="permission-check-grid tall">
            <label v-for="row in flatPermissions" :key="row.node.id" class="check-line" :style="{ paddingLeft: `${row.depth * 18}px` }">
              <input v-model="roleForm.permissionIds" type="checkbox" :value="row.node.id" />
              {{ row.node.permissionName }}
              <span class="muted mono">{{ row.node.permissionCode }}</span>
            </label>
          </div>
        </div>
      </div>
      <div class="toolbar compact">
        <button class="secondary-button" type="button" @click="closeRoleModal">取消</button>
        <button class="primary-button" type="button" :disabled="loading || !roleForm.roleCode || !roleForm.roleName" @click="saveRole"><Save :size="16" /> 保存角色</button>
      </div>
    </section>
  </div>

  <div v-if="providerModalOpen" class="overlay-backdrop" @click.self="closeProviderModal">
    <section class="modal-panel provider-modal">
      <header class="overlay-header">
        <div>
          <h2>{{ editingProviderId ? '编辑模型供应商' : '新增模型供应商' }}</h2>
          <p class="muted">配置服务商、接入地址、API Key、模型和计费单价。</p>
        </div>
        <button class="icon-button" type="button" title="关闭" @click="closeProviderModal"><X :size="18" /></button>
      </header>
      <div class="settings-form">
        <label>服务商编码<input v-model="providerForm.providerCode" /></label>
        <label>服务商名称<input v-model="providerForm.providerName" /></label>
        <label>服务商类型
          <select v-model="providerForm.providerType">
            <option value="openai_compatible">OpenAI-compatible</option>
            <option value="ollama">Ollama</option>
          </select>
        </label>
        <label>Base URL<input v-model="providerForm.baseUrl" /></label>
        <label>认证类型
          <select v-model="providerForm.authType">
            <option value="api_key">API Key</option>
            <option value="none">None</option>
          </select>
        </label>
        <label>API Key<input v-model="providerForm.apiKey" type="password" placeholder="编辑时留空表示不替换" /></label>
        <label>模型编码<input v-model="providerForm.modelCode" /></label>
        <label>模型名称<input v-model="providerForm.modelName" /></label>
        <label>上下文窗口<input v-model.number="providerForm.contextWindow" type="number" /></label>
        <label>最大输出 Token<input v-model.number="providerForm.maxOutputTokens" type="number" /></label>
        <label>输入每千 Token 单价<input v-model.number="providerForm.inputPricePer1k" type="number" min="0" step="0.000001" /></label>
        <label>输出每千 Token 单价<input v-model.number="providerForm.outputPricePer1k" type="number" min="0" step="0.000001" /></label>
        <label class="check-line"><input v-model="providerForm.supportStream" type="checkbox" /> 支持流式输出</label>
        <label class="check-line"><input v-model="providerForm.isDefault" type="checkbox" /> 设为默认聊天模型</label>
      </div>
      <div class="toolbar compact">
        <button class="secondary-button" type="button" @click="closeProviderModal">取消</button>
        <button class="primary-button" type="button" :disabled="loading" @click="saveProvider"><Save :size="16" /> 保存服务商</button>
      </div>
    </section>
  </div>
</template>
