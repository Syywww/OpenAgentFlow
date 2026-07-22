<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { AlertTriangle, ArrowRight, Check, Copy, FileUp, Play, Plus, X } from 'lucide-vue-next';
import StatusBadge from './StatusBadge.vue';
import { useOverlay } from '../composables/useOverlay';
import {
  fetchNotifications,
  markNotificationRead,
  notifyNotificationChanged,
  type NotificationItem,
} from '../api/notifications';

type SourceDrawerPanel = 'retrieval' | 'tools' | 'stats';

const router = useRouter();
const { overlay, closeModal, closeDrawer, toast } = useOverlay();
const activeSourceDrawerPanel = ref<SourceDrawerPanel>('retrieval');
const notices = ref<NotificationItem[]>([]);
const noticesLoading = ref(false);

const sourceDrawerItems = [
  {
    name: '企业知识库建设白皮书.pdf',
    score: '0.92',
    page: 'P12-14',
    summary: '企业知识库应采用分层架构，包括数据采集、知识处理、知识存储、服务应用与终端交互。',
  },
  {
    name: '企业数字化转型技术架构指南.docx',
    score: '0.89',
    page: 'P13-15',
    summary: '知识处理链路需要覆盖解析、清洗、切片、向量化、质量巡检和引用追溯。',
  },
  {
    name: '知识图谱在企业知识管理中的应用.pdf',
    score: '0.86',
    page: 'P14-16',
    summary: '图谱关系、向量召回与关键词召回可以互补提升复杂问题的可解释性。',
  },
];

const sourceDrawerTools = [
  {
    name: '架构模板生成器',
    status: '成功',
    latency: '1.23s',
    detail: '{"template":"enterprise-rag","sections":["模型接入","知识库","Trace"]}',
  },
  {
    name: '知识库质量检查',
    status: '成功',
    latency: '0.84s',
    detail: '{"lowConfidence":false,"citationCoverage":0.87}',
  },
];

const sourceDrawerStats = [
  { label: '检索结果', value: '3', help: '本次命中的知识片段' },
  { label: '最佳置信', value: '0.92', help: '最高相似度得分' },
  { label: '工具调用', value: '2', help: '参与回答的工具动作' },
  { label: '引用覆盖', value: '87%', help: '回答内容可追溯比例' },
];

watch(
  () => overlay.drawer,
  async (drawer) => {
    if (drawer === 'sources') {
      activeSourceDrawerPanel.value = 'retrieval';
    }
    if (drawer === 'notices') {
      noticesLoading.value = true;
      try {
        notices.value = (await fetchNotifications({ status: 'all', pageNo: 1, pageSize: 8 })).records;
      } catch (error) {
        toast(error instanceof Error ? error.message : '通知加载失败');
      } finally {
        noticesLoading.value = false;
      }
    }
  },
);

function go(path: string) {
  closeModal();
  router.push(path);
}

function switchSourceDrawerPanel(panel: SourceDrawerPanel) {
  activeSourceDrawerPanel.value = panel;
}

async function openNotice(notice: NotificationItem) {
  if (!notice.read) {
    await markNotificationRead(notice.id);
    notice.read = true;
    notifyNotificationChanged();
  }
  closeDrawer();
  if (notice.actionUrl) await router.push(notice.actionUrl);
}

async function openNotificationCenter() {
  closeDrawer();
  await router.push('/notifications');
}
</script>

<template>
  <Teleport to="body">
    <div v-if="overlay.modal" class="overlay-backdrop" @click.self="closeModal">
      <section class="modal-panel" :class="{ compact: ['risk', 'publish', 'toast'].includes(overlay.modal) }">
        <header class="overlay-header">
          <h2>
            <template v-if="overlay.modal === 'new-agent'">新建智能体</template>
            <template v-else-if="overlay.modal === 'prompt'">Prompt 预览</template>
            <template v-else-if="overlay.modal === 'upload'">上传文档解析</template>
            <template v-else-if="overlay.modal === 'schema'">JSON Schema 编辑器</template>
            <template v-else-if="overlay.modal === 'risk'">高风险工具确认</template>
            <template v-else-if="overlay.modal === 'mcp-test'">MCP 连接测试</template>
            <template v-else-if="overlay.modal === 'node-debug'">节点调试</template>
            <template v-else-if="overlay.modal === 'publish'">发布工作流</template>
            <template v-else-if="overlay.modal === 'eval-task'">创建评测任务</template>
            <template v-else-if="overlay.modal === 'audit'">操作日志详情</template>
            <template v-else>提示</template>
          </h2>
          <button class="icon-button" type="button" title="关闭" @click="closeModal"><X :size="18" /></button>
        </header>

        <div v-if="overlay.modal === 'new-agent'" class="overlay-grid">
          <div>
            <div class="tabs">
              <button class="tab active" type="button">推荐模板</button>
              <button class="tab" type="button">我的模板</button>
            </div>
            <div class="template-grid">
              <article v-for="name in ['通用问答助手', '客服助手', '内容创作助手', '数据分析师', '编程助手', '法律顾问助手']" :key="name" class="template-tile" :class="{ selected: name === '客服助手' }">
                <h3>{{ name }}</h3>
                <p>内置推荐 Prompt、知识库绑定策略和工具调用边界。</p>
                <StatusBadge :label="name === '客服助手' ? '已选择' : '模板'" />
              </article>
            </div>
          </div>
          <div class="form-stack">
            <label>智能体名称<input value="客服助手" /></label>
            <label>类型<select><option>客服</option><option>知识问答</option><option>数据分析</option></select></label>
            <label>绑定模型<select><option>GPT-4o</option><option>Qwen2.5-72B</option></select></label>
            <div class="switch-list">
              <label><input type="checkbox" checked /> 启用知识库</label>
              <label><input type="checkbox" checked /> 启用工具</label>
              <label><input type="checkbox" /> 开启记忆</label>
              <label><input type="checkbox" /> 联网搜索</label>
            </div>
            <div class="action-row end">
              <button class="secondary-button" type="button" @click="closeModal">取消</button>
              <button class="primary-button" type="button" @click="go('/agents/new')">
                下一步 <ArrowRight :size="16" />
              </button>
            </div>
          </div>
        </div>

        <div v-else-if="overlay.modal === 'prompt'">
          <div class="tabs">
            <button class="tab" type="button">System</button>
            <button class="tab" type="button">用户输入</button>
            <button class="tab" type="button">检索结果</button>
            <button class="tab" type="button">工具结果</button>
            <button class="tab active" type="button">最终 Prompt</button>
          </div>
          <pre class="code-block light"># 最终 Prompt
你是一个企业知识架构专家，请基于以下检索到的资料与工具结果，输出结构化的架构设计方案。

## 检索结果
&#123;&#123;retrieval_context&#125;&#125;

## 工具结果
&#123;&#123;tool_result&#125;&#125;</pre>
          <div class="action-row end">
            <button class="secondary-button" type="button"><Copy :size="16" /> 复制</button>
            <button class="primary-button" type="button" @click="closeModal">关闭</button>
          </div>
        </div>

        <div v-else-if="overlay.modal === 'upload'" class="overlay-grid">
          <div>
            <div class="upload-zone">
              <FileUp :size="42" />
              <b>拖拽文件到此处，或点击上传</b>
              <span>PDF、DOCX、PPTX、XLSX、TXT、MD、图片</span>
            </div>
            <div class="queue-row">
              <b>产品架构图.pdf</b>
              <span>解析中 60%</span>
              <div class="progress"><i style="width: 60%" /></div>
            </div>
            <div class="queue-row">
              <b>功能更新说明.docx</b>
              <span>等待中</span>
              <div class="progress"><i style="width: 8%" /></div>
            </div>
          </div>
          <div class="form-stack">
            <label>解析模式<select><option>快速模式</option><option>精细模式</option></select></label>
            <label>切片策略<select><option>按固定长度切片</option><option>按标题层级切片</option></select></label>
            <div class="two-cols">
              <label>切片大小<input value="512 tokens" /></label>
              <label>切片重叠<input value="64 tokens" /></label>
            </div>
            <div class="switch-list">
              <label><input type="checkbox" checked /> OCR 识别</label>
              <label><input type="checkbox" checked /> 去重处理</label>
              <label><input type="checkbox" checked /> 元数据提取</label>
            </div>
            <div class="action-row end">
              <button class="secondary-button" type="button" @click="closeModal">取消</button>
              <button class="primary-button" type="button" @click="toast('开始解析，稍后可在知识库详情查看进度')">
                <Play :size="16" /> 开始解析
              </button>
            </div>
          </div>
        </div>

        <div v-else-if="overlay.modal === 'schema'" class="overlay-grid">
          <div class="schema-tree">
            <b>根节点：object</b>
            <button class="secondary-button" type="button"><Plus :size="16" /> 添加字段</button>
            <span>userId string</span>
            <span>page number</span>
            <span>filter object</span>
          </div>
          <div class="form-stack">
            <label>字段名<input value="userId" /></label>
            <label>类型<select><option>string</option><option>number</option><option>object</option></select></label>
            <label><input type="checkbox" checked /> 必填</label>
            <label>字段描述<textarea>用户 ID</textarea></label>
            <div class="action-row end">
              <button class="secondary-button" type="button" @click="closeModal">取消</button>
              <button class="primary-button" type="button" @click="closeModal">确定</button>
            </div>
          </div>
        </div>

        <div v-else-if="overlay.modal === 'risk'" class="confirm-block">
          <AlertTriangle :size="48" />
          <h3>您正在启用高风险工具</h3>
          <p>工具「订单数据库（order_db）」被标记为高风险工具，启用后可能对数据造成变更或删除等影响。</p>
          <StatusBadge label="高风险" />
          <label>确认码<input placeholder="输入 CONFIRM 以确认" /></label>
          <div class="action-row end">
            <button class="secondary-button" type="button" @click="closeModal">取消</button>
            <button class="danger-button" type="button" @click="closeModal">确认启用</button>
          </div>
        </div>

        <div v-else-if="overlay.modal === 'mcp-test'" class="three-cols">
          <div class="form-stack">
            <label>传输类型<select><option>stdio</option><option>http</option><option>sse</option></select></label>
            <label>命令<input value="npx @modelcontextprotocol/server-filesystem" /></label>
            <label>参数<input value="/allowed/path" /></label>
            <label><input type="checkbox" checked /> 仅允许访问白名单路径</label>
          </div>
          <div class="mini-timeline">
            <div v-for="item in ['启动进程', '建立连接', '协议握手', '能力协商', '连接保持', '工具测试']" :key="item">
              <Check :size="16" />
              <span>{{ item }}</span>
            </div>
          </div>
          <div class="result-panel">
            <StatusBadge label="连接测试成功" />
            <p>Server 响应延迟：45ms</p>
            <p>发现工具：8 个；资源：0 个；提示：0 个</p>
            <StatusBadge label="安全警告" />
            <p>请确保路径权限符合最小权限原则。</p>
          </div>
        </div>

        <div v-else-if="overlay.modal === 'node-debug'">
          <div class="inline-meta">
            <StatusBadge label="调试成功" />
            <span>耗时 2.42s · 总 Token 1,236</span>
          </div>
          <div class="tabs">
            <button class="tab" type="button">输入</button>
            <button class="tab active" type="button">输出</button>
            <button class="tab" type="button">日志</button>
            <button class="tab" type="button">追踪</button>
          </div>
          <pre class="code-block light">{
  "answer": "根据检索到的知识，以下是问题的解答...",
  "usage": {
    "prompt_tokens": 568,
    "completion_tokens": 668,
    "total_tokens": 1236
  }
}</pre>
        </div>

        <div v-else-if="overlay.modal === 'publish'" class="form-stack">
          <label>版本号<input value="v1.0.0" /></label>
          <label>发布说明<textarea>发布客服智能问答流程 v1.0.0，优化 LLM 提示词，新增人工确认分支。</textarea></label>
          <label>发布环境<select><option>测试环境</option><option>开发环境</option><option>生产环境</option></select></label>
          <label><input type="checkbox" checked /> 发布后立即生效</label>
          <div class="action-row end">
            <button class="secondary-button" type="button" @click="closeModal">取消</button>
            <button class="primary-button" type="button" @click="toast('工作流已发布')">确认发布</button>
          </div>
        </div>

        <div v-else-if="overlay.modal === 'eval-task'" class="overlay-grid">
          <div class="form-stack">
            <label>任务名称<input value="金融智能体模型对比评测" /></label>
            <label>评测集<select><option>金融问答基准集 v1.2</option></select></label>
            <label>智能体<select><option>金融客服 Agent</option></select></label>
            <label>模型<select><option>GPT-4o</option><option>Qwen2.5-72B</option></select></label>
          </div>
          <div class="form-stack">
            <label>评分方法<select><option>综合评分</option></select></label>
            <div class="switch-list">
              <label><input type="checkbox" checked /> 准确率</label>
              <label><input type="checkbox" checked /> 相关性</label>
              <label><input type="checkbox" checked /> 幻觉率</label>
              <label><input type="checkbox" checked /> 平均延迟</label>
            </div>
            <p class="muted">预计消耗：约 25,684 Tokens</p>
            <div class="action-row end">
              <button class="secondary-button" type="button" @click="closeModal">取消</button>
              <button class="primary-button" type="button" @click="go('/eval/result')">创建并运行</button>
            </div>
          </div>
        </div>

        <div v-else-if="overlay.modal === 'audit'" class="overlay-grid">
          <div>
            <p>操作时间：2024-05-26 14:32:21</p>
            <p>操作用户：admin</p>
            <p>操作类型：创建</p>
            <p>资源类型：Agent</p>
            <p>操作结果：<StatusBadge label="成功" /></p>
            <p>IP 地址：192.168.1.100</p>
          </div>
          <pre class="code-block">{
  "name": "知识库问答 Agent",
  "category": "办公助手",
  "model_provider": "qwen",
  "knowledge_ids": ["kb_001"]
}</pre>
        </div>

        <div v-else class="confirm-block">
          <Check :size="42" />
          <p>{{ overlay.message }}</p>
          <button class="primary-button" type="button" @click="closeModal">知道了</button>
        </div>
      </section>
    </div>

    <div v-if="overlay.drawer" class="drawer-backdrop" @click.self="closeDrawer">
      <aside class="drawer-panel">
        <header class="overlay-header">
          <h2>
            <template v-if="overlay.drawer === 'sources'">引用来源</template>
            <template v-else-if="overlay.drawer === 'step'">步骤详情</template>
            <template v-else>通知中心</template>
          </h2>
          <button class="icon-button" type="button" title="关闭" @click="closeDrawer"><X :size="18" /></button>
        </header>

        <div v-if="overlay.drawer === 'sources'" class="drawer-stack">
          <div class="tabs source-drawer-tabs" role="tablist">
            <button
              class="tab"
              :class="{ active: activeSourceDrawerPanel === 'retrieval' }"
              type="button"
              role="tab"
              :aria-selected="activeSourceDrawerPanel === 'retrieval'"
              @click="switchSourceDrawerPanel('retrieval')"
            >
              检索结果
            </button>
            <button
              class="tab"
              :class="{ active: activeSourceDrawerPanel === 'tools' }"
              type="button"
              role="tab"
              :aria-selected="activeSourceDrawerPanel === 'tools'"
              @click="switchSourceDrawerPanel('tools')"
            >
              工具调用
            </button>
            <button
              class="tab"
              :class="{ active: activeSourceDrawerPanel === 'stats' }"
              type="button"
              role="tab"
              :aria-selected="activeSourceDrawerPanel === 'stats'"
              @click="switchSourceDrawerPanel('stats')"
            >
              引用统计
            </button>
          </div>
          <div v-if="activeSourceDrawerPanel === 'retrieval'" class="drawer-tab-panel">
            <article v-for="(source, index) in sourceDrawerItems" :key="source.name" class="source-item">
              <b>{{ index + 1 }}. {{ source.name }}</b>
              <span>相似度 {{ source.score }} · 页码 {{ source.page }}</span>
              <p>{{ source.summary }}</p>
            </article>
          </div>
          <div v-else-if="activeSourceDrawerPanel === 'tools'" class="drawer-tab-panel">
            <article v-for="tool in sourceDrawerTools" :key="tool.name" class="source-item">
              <b>{{ tool.name }}</b>
              <StatusBadge :label="tool.status" />
              <span>耗时 {{ tool.latency }}</span>
              <pre class="code-block light">{{ tool.detail }}</pre>
            </article>
          </div>
          <div v-else class="trace-stat-grid drawer-stat-grid">
            <article v-for="item in sourceDrawerStats" :key="item.label" class="trace-stat-card">
              <span>{{ item.label }}</span>
              <b>{{ item.value }}</b>
              <small>{{ item.help }}</small>
            </article>
          </div>
        </div>

        <div v-else-if="overlay.drawer === 'step'" class="drawer-stack">
          <div class="inline-meta">
            <h3>步骤 3：知识检索</h3>
            <StatusBadge label="成功" />
          </div>
          <p>耗时：2.45s · 开始时间：2024-05-26 14:32:22</p>
          <pre class="code-block">{
  "query": "我的订单什么时候发货？订单号是：A202405260001",
  "top_k": 10,
  "score_threshold": 0.3
}</pre>
          <pre class="code-block">{
  "results": [{"id": "doc_001", "score": 0.92, "source": "order_system"}],
  "total": 10,
  "returned": 6,
  "latency": 2.45
}</pre>
          <div v-for="score in [0.92, 0.88, 0.76, 0.68, 0.55, 0.41]" :key="score" class="score-line">
            <span>订单状态说明：已发货订单通常在 24 小时内更新物流信息</span>
            <b>{{ score }}</b>
          </div>
        </div>

        <div v-else class="drawer-stack notification-drawer-stack">
          <div v-if="noticesLoading" class="empty-state">正在加载通知...</div>
          <div v-else-if="notices.length === 0" class="empty-state">暂无通知</div>
          <article
            v-for="notice in notices"
            v-else
            :key="notice.id"
            class="source-item notification-drawer-item"
            :class="{ unread: !notice.read }"
            @click="openNotice(notice)"
          >
            <div class="inline-meta">
              <b>{{ notice.title }}</b>
              <StatusBadge :label="notice.severity" :tone="notice.severity === 'critical' ? 'danger' : notice.severity === 'warning' ? 'warning' : 'info'" />
            </div>
            <p>{{ notice.content }}</p>
            <span>{{ new Date(notice.createdAt).toLocaleString() }}</span>
          </article>
          <button class="primary-button full" type="button" @click="openNotificationCenter">查看全部通知</button>
        </div>
      </aside>
    </div>
  </Teleport>
</template>
