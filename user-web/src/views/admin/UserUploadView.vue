<template>
  <div class="page-card">
    <h3 class="section-title">用户上传</h3>

    <div class="filters">
      <el-input
        v-model="query.keyword"
        placeholder="标题 / 主题 / 分组 / 用户昵称或账号"
        clearable
        style="width: 240px"
        @keyup.enter="reload"
      />
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
        <el-option label="待处理" :value="0" />
        <el-option label="已回复" :value="1" />
      </el-select>
      <el-button type="primary" plain @click="reload">查询</el-button>
    </div>

    <el-table
      :data="list"
      stripe
      row-key="id"
      :row-class-name="({ row }) => (highlightId === row.id ? 'highlight-row' : '')"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-row">
            <div class="expand-label">管理员回复（{{ row.adminReply ? formatDateTime(row.repliedAt || '') : '暂无回复' }}）</div>
            <div v-if="row.adminReply" class="expand-text">{{ row.adminReply }}</div>
            <div v-else class="expand-empty">暂未回复</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
      <el-table-column label="主题 / 分组" min-width="110">
        <template #default="{ row }">
          {{ row.categoryName }}<template v-if="row.groupName"> / {{ row.groupName }}</template>
        </template>
      </el-table-column>
      <el-table-column label="上传用户" min-width="170">
        <template #default="{ row }">
          <div>{{ row.nickname || '-' }}</div>
          <div class="sub-text">
            <template v-if="row.email">{{ row.email }}</template>
            <template v-else-if="row.phone">手机：{{ row.phone }}</template>
            <template v-else>未绑定账号</template>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.adminReply ? 'success' : 'warning'" size="small">
            {{ row.adminReply ? '已回复' : '待处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" min-width="150">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="回复时间" min-width="150">
        <template #default="{ row }">{{ formatDateTime(row.repliedAt || '') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="viewContent(row.id)">查看</el-button>
          <el-button size="small" text type="warning" @click="openReply(row.id)">快速回复</el-button>
          <el-button size="small" text type="danger" @click="removeUpload(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="onPage"
      />
    </div>

    <!-- 查看：仅上传信息 + 预览/原文 -->
    <el-dialog v-model="contentVisible" title="上传内容" width="980px" top="4vh" append-to-body>
      <template v-if="contentDetail">
        <el-descriptions :column="2" border size="small" class="detail-desc">
          <el-descriptions-item label="标题" :span="2">{{ contentDetail.title }}</el-descriptions-item>
          <el-descriptions-item label="主题">{{ contentDetail.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="分组">{{ contentDetail.groupName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="contentDetail.adminReply ? 'success' : 'warning'" size="small">
              {{ contentDetail.adminReply ? '已回复' : '待处理' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="文件名">{{ contentDetail.fileName }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(contentDetail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="回复时间">{{ formatDateTime(contentDetail.repliedAt || '') }}</el-descriptions-item>
        </el-descriptions>

        <div class="user-box">
          <div class="box-title">用户信息</div>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="昵称">{{ contentDetail.nickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ contentDetail.email || '未绑定' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ contentDetail.phone || '未绑定' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <el-tabs v-model="contentTab">
          <el-tab-pane label="预览效果" name="preview">
            <div ref="detailPreview" class="article-body detail-md" v-html="contentDetail.contentHtml"></div>
          </el-tab-pane>
          <el-tab-pane label="作者编辑（原文）" name="raw">
            <pre class="raw-md">{{ contentDetail.contentMd }}</pre>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-dialog>

    <!-- 快速回复：仅历史对话 + 回复框（内容预览/原文在“查看”中展示） -->
    <el-dialog v-model="replyVisible" title="快速回复" width="480px" top="8vh" append-to-body>
      <div v-if="replyDetail" class="reply-panel">
        <div class="side-user">
          <div class="side-user-name">{{ replyDetail.nickname || '-' }}</div>
          <div class="side-user-account">
            <template v-if="replyDetail.email">{{ replyDetail.email }}</template>
            <template v-else-if="replyDetail.phone">手机：{{ replyDetail.phone }}</template>
            <template v-else>未绑定账号</template>
          </div>
        </div>

        <div class="side-title">历史对话</div>
        <div class="chat-list">
          <div v-if="replyDetail.adminReply" class="chat-item admin">
            <div class="chat-role">管理员</div>
            <div class="chat-text">{{ replyDetail.adminReply }}</div>
            <div class="chat-time">{{ formatDateTime(replyDetail.repliedAt || '') }}</div>
          </div>
          <div v-if="!replyDetail.adminReply" class="chat-empty">暂无回复，回复后用户可在“我的上传”中查看</div>
        </div>

        <el-alert
          v-if="repliedOnce"
          type="warning"
          :closable="false"
          class="replied-hint"
          title="该内容已回复，不能重复回复"
        />
        <el-input
          v-model="replyContent"
          type="textarea"
          :rows="5"
          maxlength="2000"
          show-word-limit
          placeholder="输入要回复的内容"
          :disabled="repliedOnce"
        />
        <el-button
          class="side-reply-btn"
          type="primary"
          :loading="replying"
          :disabled="repliedOnce"
          @click="submitReply"
        >
          发送回复
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteAdminUploadApi,
  getAdminUploadDetailApi,
  getAdminUploadsApi,
  replyAdminUploadApi
} from '@/api/upload'
import { highlightCodeBlocks, renderDiagrams } from '@/utils/markdown'
import { formatDateTime } from '@/utils/format'
import type { UserUploadDetail, UserUploadItem } from '@/types'

const route = useRoute()
const router = useRouter()

const list = ref<UserUploadItem[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', status: undefined as number | undefined, page: 1, size: 10 })
const highlightId = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    const res = await getAdminUploadsApi({
      keyword: query.keyword || undefined,
      status: query.status,
      page: query.page,
      size: query.size
    })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

/** 静默轮询：同步最新回复与状态（多管理员协作时无需刷新） */
let pollTimer: number | null = null

async function refreshSilently() {
  try {
    const res = await getAdminUploadsApi({
      keyword: query.keyword || undefined,
      status: query.status,
      page: query.page,
      size: query.size
    })
    total.value = res.total
    const byId = new Map(list.value.map((x) => [x.id, x]))
    for (const item of res.list) {
      const exist = byId.get(item.id)
      if (exist) {
        Object.assign(exist, item)
      } else {
        list.value.push(item)
      }
    }
    // 已打开的弹窗同步最新详情
    const openId = replyDetail.value?.id || contentDetail.value?.id
    if (openId) {
      const fresh = await getAdminUploadDetailApi(openId)
      if (replyDetail.value?.id === openId) replyDetail.value = fresh
      if (contentDetail.value?.id === openId) contentDetail.value = fresh
    }
  } catch {
    // 轮询失败静默忽略
  }
}

function reload() {
  query.page = 1
  load()
}

function onPage(p: number) {
  query.page = p
  load()
}

async function removeUpload(row: UserUploadItem) {
  try {
    await ElMessageBox.confirm(`确定删除用户“${row.nickname || '-'}”上传的内容“${row.title}”吗？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteAdminUploadApi(row.id)
    if (replyDetail.value?.id === row.id) replyVisible.value = false
    if (contentDetail.value?.id === row.id) contentVisible.value = false
    ElMessage.success('删除成功')
    load()
  } catch {
    // 拦截器已提示
  }
}

/** 从通知点击跳转过来时，仅定位并高亮对应行，不自动打开回复弹窗 */
async function handleJump() {
  const id = Number(route.query.upload || 0)
  if (!id) return
  await load()
  let row = list.value.find((x) => x.id === id)
  if (!row) {
    try {
      const res = await getAdminUploadsApi({ page: 1, size: 100 })
      row = res.list.find((x) => x.id === id) || null
      if (row) {
        list.value = res.list
        total.value = res.total
      }
    } catch {
      row = null
    }
  }
  if (row) {
    highlightId.value = id
    await nextTick()
    document.querySelector(`.el-table__body tr[data-row-key="${id}"]`)?.scrollIntoView({
      block: 'center',
      behavior: 'smooth'
    })
    window.setTimeout(() => {
      if (highlightId.value === id) highlightId.value = null
    }, 2500)
  }
  router.replace({ query: {} })
}

// 查看：仅上传信息 + 预览/原文
const contentVisible = ref(false)
const contentDetail = ref<UserUploadDetail | null>(null)
const contentTab = ref('preview')
const detailPreview = ref<HTMLElement | null>(null)

async function viewContent(id: number) {
  contentDetail.value = await getAdminUploadDetailApi(id)
  contentTab.value = 'preview'
  contentVisible.value = true
  await nextTick()
  highlightCodeBlocks(detailPreview.value)
  await renderDiagrams(detailPreview.value)
}

// 快速回复：内容 + 历史对话 + 回复框
const replyVisible = ref(false)
const replyDetail = ref<UserUploadDetail | null>(null)
const replyContent = ref('')
const replying = ref(false)
const repliedOnce = computed(() => !!replyDetail.value?.adminReply)

async function openReply(id: number) {
  replyDetail.value = await getAdminUploadDetailApi(id)
  replyContent.value = ''
  replyVisible.value = true
}

async function submitReply() {
  if (!replyDetail.value) return
  if (repliedOnce.value) {
    ElMessage.warning('该内容已回复，不能重复回复')
    return
  }
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    const updated = await replyAdminUploadApi(replyDetail.value.id, replyContent.value.trim())
    replyDetail.value = updated
    const row = list.value.find((x) => x.id === updated.id)
    if (row) {
      row.status = updated.status
      row.adminReply = updated.adminReply
      row.repliedAt = updated.repliedAt
    }
    if (contentDetail.value?.id === updated.id) {
      contentDetail.value = updated
    }
    replyContent.value = ''
    ElMessage.success('回复成功，用户可在“我的上传”中查看')
  } finally {
    replying.value = false
  }
}

onMounted(() => {
  load()
  handleJump()
  pollTimer = window.setInterval(refreshSilently, 5000)
})

onBeforeUnmount(() => {
  if (pollTimer !== null) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<style scoped>
.page-card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 20px 24px;
}

.section-title {
  margin: 0 0 16px;
  color: #f0c674;
}

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

/* 增加表格列之间的缩进 */
:deep(.el-table .cell) {
  padding: 0 12px;
}

:deep(.el-table .highlight-row) {
  background: var(--app-accent-soft) !important;
}

.sub-text {
  color: var(--app-text-secondary);
  font-size: 12px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.expand-row {
  padding: 6px 12px;
}

.expand-label {
  font-size: 12px;
  color: var(--app-text-secondary);
  margin-bottom: 4px;
}

.expand-text {
  font-size: 13px;
  color: var(--app-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.expand-row-sub {
  margin-top: 8px;
  border-top: 1px dashed var(--app-border);
  padding-top: 8px;
}

.expand-empty {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.detail-desc {
  margin-bottom: 14px;
}

.user-box {
  margin-bottom: 14px;
}

.box-title {
  font-size: 13px;
  font-weight: 600;
  color: #f0c674;
  margin-bottom: 8px;
}

.detail-md {
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 16px 20px;
}

.raw-md {
  max-height: 55vh;
  overflow: auto;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 10px;
  padding: 16px 18px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.side-title {
  font-size: 14px;
  font-weight: 600;
  color: #f0c674;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--app-border);
  margin-bottom: 12px;
}

.side-user {
  margin-bottom: 12px;
}

.side-user-name {
  font-weight: 600;
  color: var(--app-text);
}

.side-user-account {
  font-size: 12px;
  color: var(--app-text-secondary);
  margin-top: 2px;
}

.chat-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
  max-height: 260px;
  overflow-y: auto;
}

.chat-item {
  border: 1px solid var(--app-border);
  border-radius: 8px;
  padding: 10px;
  background: var(--app-card);
}

.chat-item.user {
  border-color: var(--app-accent);
  background: var(--app-accent-soft);
}

.chat-role {
  font-size: 12px;
  font-weight: 600;
  color: #f0c674;
  margin-bottom: 4px;
}

.chat-text {
  font-size: 13px;
  color: var(--app-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-time {
  margin-top: 4px;
  font-size: 11px;
  color: var(--app-text-secondary);
}

.chat-empty {
  font-size: 13px;
  color: var(--app-text-secondary);
  text-align: center;
  padding: 20px 0;
}

.replied-hint {
  margin-bottom: 10px;
}

.side-reply-btn {
  width: 100%;
  margin-top: 10px;
}
</style>
