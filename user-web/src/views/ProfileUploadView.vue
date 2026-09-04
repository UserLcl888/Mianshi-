<template>
  <div class="page">
    <AppHeader />
    <div class="page-body">
      <main class="content">
        <el-breadcrumb class="breadcrumb-bar" separator="/">
          <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/profile' }">个人中心</el-breadcrumb-item>
          <el-breadcrumb-item>内容上传</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="app-card">
          <h3 class="section-title">上传内容（Markdown）</h3>
          <el-form label-width="100px" class="upload-form" @submit.prevent>
            <el-form-item label="标题" required>
              <el-input v-model="form.title" placeholder="请输入内容标题，如：我的 Redis 面试总结" maxlength="200" />
            </el-form-item>

            <el-form-item label="主题分类" required>
              <div class="category-field">
                <el-select v-model="form.categoryMode" placeholder="选择已有主题，或自定义" style="flex: 1">
                  <el-option v-for="cat in categoryOptions" :key="cat.id" :label="cat.name" :value="String(cat.id)" />
                  <el-option label="自定义主题" value="custom" />
                </el-select>
                <el-input
                  v-if="form.categoryMode === 'custom'"
                  v-model="form.customCategory"
                  placeholder="输入你的自定义主题，如：架构设计心得"
                  style="flex: 1"
                />
              </div>
            </el-form-item>

            <el-form-item label="分组" v-if="form.categoryMode && form.categoryMode !== 'custom'">
              <div class="category-field">
                <el-select v-model="form.groupMode" placeholder="选择分组（可不选）" clearable style="flex: 1">
                  <el-option v-for="sub in groupOptions" :key="sub.id" :label="sub.name" :value="String(sub.id)" />
                  <el-option label="自定义分组" value="custom" />
                </el-select>
                <el-input
                  v-if="form.groupMode === 'custom'"
                  v-model="form.customGroup"
                  placeholder="输入你的自定义分组"
                  style="flex: 1"
                />
              </div>
            </el-form-item>

            <el-form-item label="分组" v-else-if="form.categoryMode === 'custom'">
              <el-input v-model="form.customGroup" placeholder="自定义分组（选填），如：一面记录" />
            </el-form-item>

            <el-form-item label="MD 文件" required>
              <el-upload
                drag
                accept=".md,.markdown"
                :auto-upload="false"
                :limit="1"
                :file-list="mdFileList"
                :on-change="onMdFileChange"
                :on-exceed="onMdExceed"
              >
                <div class="upload-hint">
                  <el-icon class="upload-icon"><UploadFilled /></el-icon>
                  <div>拖拽 .md 文件到此处，或 <em>点击选择</em></div>
                  <div class="upload-tip">仅支持 .md / .markdown，最大 20MB，内容将以 UTF-8 读取</div>
                </div>
              </el-upload>
            </el-form-item>

            <el-form-item>
              <el-button native-type="button" :disabled="!mdContent" size="small" @click="previewVisible = true">预览效果</el-button>
              <el-button native-type="button" type="primary" :loading="saving" @click="submit">提交上传</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="app-card my-list-card">
          <h3 class="section-title">我的上传</h3>
          <el-table :data="list" stripe row-key="id" :expand-row-keys="expandedKeys">
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-row">
                  <div class="expand-label">管理员回复（{{ row.adminReply ? formatDateTime(row.repliedAt || '') : '暂无回复' }}）</div>
                  <div v-if="row.adminReply" class="expand-text">{{ row.adminReply }}</div>
                  <div v-else class="expand-empty">暂未回复，请耐心等待</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
            <el-table-column label="主题 / 分组" min-width="160">
              <template #default="{ row }">
                {{ row.categoryName }}<template v-if="row.groupName"> / {{ row.groupName }}</template>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.adminReply ? 'success' : 'warning'" size="small">
                  {{ row.adminReply ? '已回复' : '待处理' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" min-width="160">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="openDetail(row.id)">查看</el-button>
                <el-button size="small" text type="danger" @click="removeUpload(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!loading && !list.length" class="empty-tip">暂无上传记录</div>
          <div class="pager">
            <el-pagination
              layout="total, prev, pager, next"
              :total="total"
              :page-size="query.size"
              :current-page="query.page"
              @current-change="onPage"
            />
          </div>
        </div>

        <el-dialog v-model="previewVisible" title="Markdown 预览" width="860px" top="6vh" append-to-body @open="onPreviewOpen">
          <div ref="previewBody" class="article-body preview-body" v-html="previewHtml"></div>
        </el-dialog>

        <el-dialog v-model="detailVisible" title="上传详情" width="860px" top="6vh" append-to-body>
          <template v-if="detail">
            <el-descriptions :column="2" border size="small" class="detail-desc">
              <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
              <el-descriptions-item label="主题">{{ detail.categoryName }}</el-descriptions-item>
              <el-descriptions-item label="分组">{{ detail.groupName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="detail.adminReply ? 'success' : 'warning'" size="small">
                  {{ detail.adminReply ? '已回复' : '待处理' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="文件名">{{ detail.fileName }}</el-descriptions-item>
              <el-descriptions-item label="提交时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
            </el-descriptions>

            <el-tabs v-model="detailTab">
              <el-tab-pane label="预览效果" name="preview">
                <div ref="detailPreview" class="article-body detail-md" v-html="detail.contentHtml"></div>
              </el-tab-pane>
              <el-tab-pane label="原文" name="raw">
                <pre class="raw-md">{{ detail.contentMd }}</pre>
              </el-tab-pane>
            </el-tabs>
          </template>
        </el-dialog>
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type UploadFile, type UploadUserFile } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { useCategoryStore } from '@/stores/category'
import { createUserUploadApi, deleteMyUploadApi, getMyUploadDetailApi, getMyUploadsApi } from '@/api/upload'
import { highlightCodeBlocks, renderDiagrams, renderMarkdown } from '@/utils/markdown'
import { readFileAsText } from '@/utils/file'
import { formatDateTime } from '@/utils/format'
import type { UserUploadDetail, UserUploadItem } from '@/types'

const categoryStore = useCategoryStore()
const route = useRoute()
const router = useRouter()
const categoryOptions = computed(() => categoryStore.tree)

/** 表单状态（含 md 内容）保存到 sessionStorage，刷新页面后自动恢复 */
const STORAGE_KEY = 'profile-upload-form'

const form = reactive({
  title: '',
  categoryMode: '',
  customCategory: '',
  groupMode: '',
  customGroup: ''
})

const selectedCategory = computed(
  () => categoryOptions.value.find((c) => String(c.id) === form.categoryMode) || null
)
const groupOptions = computed(() => selectedCategory.value?.children || [])

const categoryName = computed(() => {
  if (form.categoryMode === 'custom') return form.customCategory.trim()
  return selectedCategory.value?.name || ''
})

const groupName = computed(() => {
  if (form.groupMode === 'custom') return form.customGroup.trim()
  const sub = groupOptions.value.find((s) => String(s.id) === form.groupMode)
  return sub?.name || ''
})

watch(
  () => form.categoryMode,
  () => {
    form.groupMode = ''
    form.customGroup = ''
  }
)

const mdFileList = ref<UploadUserFile[]>([])
const selectedRawFile = ref<File | null>(null)
const mdContent = ref('')
const saving = ref(false)
const previewVisible = ref(false)
const previewBody = ref<HTMLElement | null>(null)
const previewHtml = computed(() => renderMarkdown(mdContent.value))

function snapshotForm(): string {
  return JSON.stringify({
    title: form.title,
    categoryMode: form.categoryMode,
    customCategory: form.customCategory,
    groupMode: form.groupMode,
    customGroup: form.customGroup,
    mdContent: mdContent.value,
    mdFileName: mdFileList.value[0]?.name || ''
  })
}

watch(snapshotForm, (snapshot) => {
  try {
    sessionStorage.setItem(STORAGE_KEY, snapshot)
  } catch {
    // 内容过大超出存储配额时放弃持久化，不影响正常上传
  }
})

function restoreForm() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return
    const saved = JSON.parse(raw) as Record<string, string>
    if (saved.title) form.title = saved.title
    if (saved.categoryMode) form.categoryMode = saved.categoryMode
    if (saved.customCategory) form.customCategory = saved.customCategory
    if (saved.groupMode) form.groupMode = saved.groupMode
    if (saved.customGroup) form.customGroup = saved.customGroup
    if (saved.mdContent) {
      mdContent.value = saved.mdContent
      const fileName = saved.mdFileName || 'restored.md'
      mdFileList.value = [{ name: fileName, status: 'ready' }] as UploadUserFile[]
      selectedRawFile.value = new File([saved.mdContent], fileName, { type: 'text/markdown' })
    }
  } catch {
    // 恢复失败时保持空表单
  }
}

function onMdExceed() {
  ElMessage.warning('只能选择一个 .md 文件')
}

async function onMdFileChange(file: UploadFile, files: UploadUserFile[]) {
  mdFileList.value = files.slice(-1)
  const raw = file.raw
  if (!raw) return
  if (raw.size > 20 * 1024 * 1024) {
    ElMessage.warning('文档不能超过 20MB')
    return
  }
  try {
    mdContent.value = await readFileAsText(raw)
    selectedRawFile.value = raw
  } catch {
    // 读取失败忽略
  }
}

function onPreviewOpen() {
  nextTick(async () => {
    highlightCodeBlocks(previewBody.value)
    await renderDiagrams(previewBody.value)
  })
}

async function submit() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  if (!categoryName.value) {
    ElMessage.warning('请选择或填写主题分类')
    return
  }
  if (!selectedRawFile.value || !mdContent.value.trim()) {
    ElMessage.warning('请选择并读取 Markdown 文件')
    return
  }
  saving.value = true
  try {
    const fd = new FormData()
    fd.append('title', form.title.trim())
    fd.append('categoryName', categoryName.value)
    if (groupName.value) fd.append('groupName', groupName.value)
    fd.append('file', selectedRawFile.value)
    await createUserUploadApi(fd)
    ElMessage.success('上传成功，等待管理员处理')
    sessionStorage.removeItem(STORAGE_KEY)
    resetForm()
    reload()
  } finally {
    saving.value = false
  }
}

function resetForm() {
  form.title = ''
  form.categoryMode = ''
  form.customCategory = ''
  form.groupMode = ''
  form.customGroup = ''
  mdFileList.value = []
  selectedRawFile.value = null
  mdContent.value = ''
}

const list = ref<UserUploadItem[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })
const expandedKeys = ref<number[]>([])

async function load() {
  loading.value = true
  try {
    const res = await getMyUploadsApi({ page: query.page, size: query.size })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

/** 静默轮询：管理员回复后，用户端无需刷新即可看到最新状态 */
let pollTimer: number | null = null

async function refreshSilently() {
  try {
    const res = await getMyUploadsApi({ page: query.page, size: query.size })
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
    if (detail.value?.id) {
      const fresh = await getMyUploadDetailApi(detail.value.id)
      detail.value = fresh
    }
  } catch {
    // 轮询失败静默忽略，不打扰用户
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
    await ElMessageBox.confirm(`确定删除上传内容“${row.title}”吗？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteMyUploadApi(row.id)
    if (detail.value?.id === row.id) detailVisible.value = false
    ElMessage.success('删除成功')
    reload()
  } catch {
    // 拦截器已提示
  }
}

/** 从通知点击跳转过来时，自动展开对应上传；不在当前页则打开详情 */
async function handleJump() {
  const id = Number(route.query.upload || 0)
  if (!id) return
  if (!list.value.length) await load()
  const row = list.value.find((x) => x.id === id)
  if (row) {
    expandedKeys.value = [id]
  } else {
    try {
      detail.value = await getMyUploadDetailApi(id)
      detailVisible.value = true
    } catch {
      // 已删除或无权限，忽略
    }
  }
  router.replace({ query: {} })
}

const detailVisible = ref(false)
const detail = ref<UserUploadDetail | null>(null)
const detailTab = ref('preview')
const detailPreview = ref<HTMLElement | null>(null)

async function openDetail(id: number) {
  detailTab.value = 'preview'
  detail.value = await getMyUploadDetailApi(id)
  detailVisible.value = true
  await nextTick()
  highlightCodeBlocks(detailPreview.value)
  await renderDiagrams(detailPreview.value)
}

onMounted(async () => {
  restoreForm()
  await categoryStore.fetchTree()
  await load()
  await handleJump()
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
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.page-body {
  flex: 1;
  width: 100%;
  padding: 16px 24px;
  display: flex;
  justify-content: center;
}

.content {
  width: 100%;
  max-width: 960px;
}

.section-title {
  margin: 0 0 16px;
  color: #e9b862;
}

.upload-form {
  width: 100%;
}

.category-field {
  display: flex;
  gap: 8px;
  width: 100%;
}

.upload-hint {
  text-align: center;
  padding: 8px 0;
}

.upload-icon {
  font-size: 36px;
  color: var(--app-text-secondary);
  margin-bottom: 6px;
}

.upload-tip {
  margin-top: 6px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.my-list-card {
  margin-top: 18px;
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

.expand-empty {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.empty-tip {
  padding: 18px 0;
  text-align: center;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.preview-body {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 20px 24px;
}

.detail-desc {
  margin-bottom: 14px;
}

.reply-box {
  margin-bottom: 14px;
}

.reply-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.reply-content {
  white-space: pre-wrap;
}

.detail-md {
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 16px 20px;
}

.raw-md {
  max-height: 60vh;
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
</style>
