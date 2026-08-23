<template>
  <div class="page-card">
    <h3 class="section-title">访问申请</h3>

    <div class="filters">
      <el-input
        v-model="query.keyword"
        placeholder="申请人 / 账号 / 文档标题"
        clearable
        style="width: 240px"
        @keyup.enter="reload"
      />
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
        <el-option label="待审批" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
      <el-select v-model="query.scope" placeholder="全部范围" clearable style="width: 130px">
        <el-option label="单篇文档" value="ARTICLE" />
        <el-option label="全部文档" value="ALL" />
      </el-select>
      <el-button type="primary" plain @click="reload">查询</el-button>
    </div>

    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="申请人" min-width="170">
        <template #default="{ row }">
          <div>{{ row.nickname || '-' }}</div>
          <div class="sub-text">
            <template v-if="row.email">{{ row.email }}</template>
            <template v-else-if="row.phone">手机：{{ row.phone }}</template>
            <template v-else>未绑定账号</template>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="申请内容" min-width="190">
        <template #default="{ row }">
          <el-tag :type="row.scope === 'ALL' ? 'warning' : 'info'" size="small" class="scope-tag">
            {{ row.scope === 'ALL' ? '全部分类' : '单分类' }}
          </el-tag>
          <span>{{ row.categoryName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="理由" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.reason || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" min-width="150">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="330" fixed="right">
        <template #default="{ row }">
          <div class="row-ops">
            <el-button size="small" text type="primary" @click="viewDetail(row.id)">查看</el-button>
            <el-button size="small" text type="warning" @click="openReply(row.id)">回复</el-button>
            <el-button size="small" text type="success" @click="approve(row)">通过</el-button>
            <el-button size="small" text type="danger" @click="reject(row)">拒绝</el-button>
            <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
          </div>
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

    <el-dialog v-model="detailVisible" title="申请详情" width="640px" append-to-body>
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="申请人" :span="2">{{ detail.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email || '未绑定' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.phone || '未绑定' }}</el-descriptions-item>
          <el-descriptions-item label="范围">{{ detail.scope === 'ALL' ? '全部分类' : '单分类' }}</el-descriptions-item>
          <el-descriptions-item label="申请分类">{{ detail.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detail.status)" size="small">{{ statusText(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请理由" :span="2">{{ detail.reason || '（未填写）' }}</el-descriptions-item>
          <el-descriptions-item label="审批备注" :span="2">{{ detail.reviewRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="管理员回复" :span="2" class="pre-wrap">{{ detail.adminReply || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="审批时间">{{ formatDateTime(detail.reviewedAt || '') }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <el-dialog v-model="replyVisible" title="快速回复" width="480px" append-to-body>
      <div v-if="replyDetail" class="reply-panel">
        <div class="reply-user">{{ replyDetail.nickname || '-' }}</div>
        <div class="reply-account">
          <template v-if="replyDetail.email">{{ replyDetail.email }}</template>
          <template v-else-if="replyDetail.phone">手机：{{ replyDetail.phone }}</template>
          <template v-else>未绑定账号</template>
        </div>
        <div class="reply-target">申请分类：{{ replyDetail.categoryName }}</div>
        <el-input
          v-model="replyContent"
          type="textarea"
          :rows="5"
          maxlength="2000"
          show-word-limit
          placeholder="输入要回复的内容，将作为站内消息通知申请人"
        />
        <el-button class="reply-btn" type="primary" :loading="replying" @click="submitReply">发送回复</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveAccessApi,
  deleteAccessApi,
  getAdminAccessApi,
  getAdminAccessDetailApi,
  rejectAccessApi,
  replyAccessApi
} from '@/api/access'
import { formatDateTime } from '@/utils/format'
import type { AccessApplyItem } from '@/types'

const list = ref<AccessApplyItem[]>([])
const total = ref(0)
const query = reactive({ keyword: '', status: undefined as number | undefined, scope: undefined as string | undefined, page: 1, size: 10 })

async function load() {
  const res = await getAdminAccessApi({
    keyword: query.keyword || undefined,
    status: query.status,
    scope: query.scope,
    page: query.page,
    size: query.size
  })
  list.value = res.list
  total.value = res.total
}

function reload() {
  query.page = 1
  load()
}

function onPage(p: number) {
  query.page = p
  load()
}

function statusType(status: number): 'warning' | 'success' | 'danger' {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info'
}

function statusText(status: number): string {
  return { 0: '待审批', 1: '已通过', 2: '已拒绝' }[status] || '未知'
}

const detailVisible = ref(false)
const detail = ref<AccessApplyItem | null>(null)

async function viewDetail(id: number) {
  detail.value = await getAdminAccessDetailApi(id)
  detailVisible.value = true
}

const replyVisible = ref(false)
const replyDetail = ref<AccessApplyItem | null>(null)
const replyContent = ref('')
const replying = ref(false)

async function openReply(id: number) {
  replyDetail.value = await getAdminAccessDetailApi(id)
  replyContent.value = ''
  replyVisible.value = true
}

async function submitReply() {
  if (!replyDetail.value) return
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    await replyAccessApi(replyDetail.value.id, replyContent.value.trim())
    ElMessage.success('回复已发送，申请人将在通知中看到')
    replyContent.value = ''
    replyVisible.value = false
    load()
  } finally {
    replying.value = false
  }
}

async function approve(row: AccessApplyItem) {
  try {
    await ElMessageBox.confirm(`确认通过 ${row.nickname || '该用户'} 对“${row.categoryName}”的申请吗？`, '通过申请', {
      type: 'warning',
      confirmButtonText: '通过',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await approveAccessApi(row.id)
  ElMessage.success('已通过，申请人将收到通知')
  if (detail.value?.id === row.id) detail.value = await getAdminAccessDetailApi(row.id)
  load()
}

async function reject(row: AccessApplyItem) {
  let remark = ''
  try {
    const result = await ElMessageBox.prompt(`拒绝 ${row.nickname || '该用户'} 对“${row.categoryName}”的申请`, '拒绝申请', {
      type: 'warning',
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputPlaceholder: '拒绝原因（选填）',
      inputValue: '',
      inputValidator: () => true
    })
    remark = (result.value || '').trim()
  } catch {
    return
  }
  await rejectAccessApi(row.id, remark || undefined)
  ElMessage.success('已拒绝，申请人将收到通知')
  if (detail.value?.id === row.id) detail.value = await getAdminAccessDetailApi(row.id)
  load()
}

async function remove(row: AccessApplyItem) {
  try {
    await ElMessageBox.confirm('确定删除这条申请记录吗？删除后不可恢复。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteAccessApi(row.id)
  ElMessage.success('删除成功')
  if (detail.value?.id === row.id) detailVisible.value = false
  load()
}

onMounted(load)
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
  color: #6b5208;
}

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

:deep(.el-table .cell) {
  padding: 0 10px;
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

.row-ops {
  display: flex;
  align-items: center;
  white-space: nowrap;
}

.scope-tag {
  margin-right: 8px;
}

.pre-wrap {
  white-space: pre-wrap;
}

.reply-panel {
  display: flex;
  flex-direction: column;
}

.reply-user {
  font-weight: 600;
  color: var(--app-text);
}

.reply-account {
  font-size: 12px;
  color: var(--app-text-secondary);
  margin-top: 2px;
}

.reply-target {
  font-size: 13px;
  color: var(--app-text);
  margin: 10px 0;
}

.reply-btn {
  width: 100%;
  margin-top: 10px;
}
</style>
