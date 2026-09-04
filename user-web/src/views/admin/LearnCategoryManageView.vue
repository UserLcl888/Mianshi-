<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="section-title">学习专题分类</h3>
      <el-button type="primary" size="small" @click="openCreate">新增分类</el-button>
    </div>

    <div class="sort-list">
      <div
        v-for="(row, i) in list"
        :key="row.id"
        class="sort-item"
        :class="{ dragging: dragIndex === i, 'drag-over': dragOverIndex === i }"
        draggable="true"
        @dragstart="onDragStart(i)"
        @dragover.prevent="onDragOver(i)"
        @dragleave="onDragLeave(i)"
        @drop.prevent="onDrop"
        @dragend="onDragEnd"
      >
        <span class="drag-handle" title="拖拽调整顺序">⠿</span>
        <span class="sort-idx">{{ i + 1 }}</span>
        <div class="sort-main">
          <div class="sort-title">{{ row.name }}</div>
          <div class="sort-meta">{{ row.slug }} · {{ row.articleCount }} 篇</div>
        </div>
        <div class="sort-ops">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </div>
      </div>
      <div v-if="!loading && !list.length" class="empty-tip">暂无学习专题，可直接新增</div>
    </div>

    <el-dialog v-model="visible" :title="isEdit ? '编辑分类' : '新增分类'" width="420px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="分类名" required>
          <el-input v-model="form.name" placeholder="如：Java、AI、MySQL、前端" />
        </el-form-item>
        <el-form-item label="标识 slug">
          <el-input v-model="form.slug" placeholder="选填，留空自动生成" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="封面图">
          <div class="cover-field">
            <el-upload
              accept=".png,.jpg,.jpeg,.webp"
              :show-file-list="false"
              :http-request="onCoverUpload"
              :before-upload="beforeCoverUpload"
            >
              <img v-if="form.coverUrl" :src="form.coverUrl" class="cover-preview" alt="封面预览" />
              <div v-else class="cover-placeholder">上传封面</div>
            </el-upload>
            <el-button v-if="form.coverUrl" size="small" type="danger" plain @click="form.coverUrl = ''">移除</el-button>
          </div>
          <div class="cover-size-tip">最大 10MB，建议 16:9</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdminLearnCategoryApi,
  deleteAdminLearnCategoryApi,
  getAdminLearnCategoriesApi,
  reorderLearnCategoriesApi,
  updateAdminLearnCategoryApi,
  uploadCoverApi
} from '@/api/admin'
import { dataUrlToFile, readFileAsDataUrl } from '@/utils/file'
import type { LearnCategory } from '@/types'

const list = ref<LearnCategory[]>([])
const loading = ref(false)
const dragIndex = ref<number | null>(null)
const dragOverIndex = ref<number | null>(null)
const visible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const editId = ref(0)
const form = reactive({ name: '', slug: '', sortOrder: 0, coverUrl: '' })

async function load() {
  loading.value = true
  try {
    list.value = await getAdminLearnCategoriesApi()
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function onDragStart(i: number) {
  dragIndex.value = i
}
function onDragOver(i: number) {
  dragOverIndex.value = i
}
function onDragLeave(i: number) {
  if (dragOverIndex.value === i) {
    dragOverIndex.value = null
  }
}
function onDragEnd() {
  dragIndex.value = null
  dragOverIndex.value = null
}
function onDrop() {
  const from = dragIndex.value
  const to = dragOverIndex.value
  onDragEnd()
  if (from === null || to === null || from === to) return
  const arr = [...list.value]
  const [moved] = arr.splice(from, 1)
  arr.splice(to, 0, moved)
  list.value = arr
  persistOrder()
}

async function persistOrder() {
  const items = list.value.map((c, i) => ({ id: c.id, sortOrder: i + 1 }))
  try {
    await reorderLearnCategoriesApi(items)
    ElMessage.success('排序已保存')
  } catch {
    load()
  }
}

function openCreate() {
  isEdit.value = false
  editId.value = 0
  form.name = ''
  form.slug = ''
  form.sortOrder = 0
  form.coverUrl = ''
  visible.value = true
}

function openEdit(row: LearnCategory) {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.slug = row.slug
  form.sortOrder = row.sortOrder || 0
  form.coverUrl = row.coverUrl || ''
  visible.value = true
}

async function onCoverUpload(options: { file: File; onSuccess: (res: unknown) => void; onError: (err: Error) => void }) {
  try {
    form.coverUrl = await readFileAsDataUrl(options.file)
    options.onSuccess({ url: form.coverUrl })
    ElMessage.success('封面已选择，保存时上传')
  } catch (e) {
    options.onError(e as Error)
  }
}

function beforeCoverUpload(file: File): boolean {
  const ok =
    ['image/png', 'image/jpeg', 'image/jpg', 'image/webp'].includes(file.type) ||
    /\.(png|jpe?g|webp)$/i.test(file.name || '')
  if (!ok) {
    ElMessage.warning('仅支持 png/jpg/jpeg/webp 格式')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('封面图片不能超过 10MB')
    return false
  }
  return true
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入分类名')
    return
  }
  saving.value = true
  try {
    let coverUrl = form.coverUrl.trim() || undefined
    if (coverUrl && coverUrl.startsWith('data:')) {
      coverUrl = (await uploadCoverApi(dataUrlToFile(coverUrl, 'cover.png'))).url
    }
    if (isEdit.value) {
      await updateAdminLearnCategoryApi(editId.value, {
        name: form.name.trim(),
        slug: form.slug.trim() || undefined,
        coverUrl
      })
      ElMessage.success('保存成功')
    } else {
      await createAdminLearnCategoryApi({
        name: form.name.trim(),
        slug: form.slug.trim() || undefined,
        sortOrder: form.sortOrder,
        coverUrl
      })
      ElMessage.success('创建成功')
    }
    visible.value = false
    load()
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function remove(row: LearnCategory) {
  try {
    await ElMessageBox.confirm(`确定删除分类“${row.name}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteAdminLearnCategoryApi(row.id)
    ElMessage.success('删除成功')
    load()
  } catch {
    // 拦截器已提示
  }
}

onMounted(load)
</script>

<style scoped>
.page-card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 18px 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  color: #f0c674;
}

.cover-field {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cover-preview {
  width: 140px;
  height: 84px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid var(--app-border);
  cursor: pointer;
}

.cover-placeholder {
  width: 140px;
  height: 84px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-text-secondary);
  font-size: 13px;
  border: 1px dashed var(--app-border);
  border-radius: 10px;
  cursor: pointer;
}

.cover-size-tip {
  margin-top: 6px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.sort-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sort-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.07);
  transition: background 0.15s, border-color 0.15s;
}

.sort-item:hover {
  border-color: rgba(232, 154, 31, 0.35);
  background: rgba(255, 255, 255, 0.05);
}

.sort-item.dragging {
  opacity: 0.6;
}

.sort-item.drag-over {
  border-color: var(--app-accent);
  background: var(--app-accent-soft);
}

.drag-handle {
  cursor: grab;
  font-size: 18px;
  color: var(--app-text-secondary);
  user-select: none;
}

.drag-handle:active {
  cursor: grabbing;
}

.sort-idx {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: var(--app-accent-soft);
  color: var(--app-accent);
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.sort-main {
  flex: 1;
  min-width: 0;
}

.sort-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text);
}

.sort-meta {
  margin-top: 2px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.sort-ops {
  flex-shrink: 0;
  display: flex;
  gap: 4px;
}

.empty-tip {
  padding: 18px 0;
  text-align: center;
  color: var(--app-text-secondary);
  font-size: 13px;
}
</style>
