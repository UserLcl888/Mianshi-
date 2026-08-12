<template>
  <div class="page">
    <AppHeader v-if="!embedded" />
    <div class="page-body" :class="{ embedded }">
      <main class="content">
        <el-breadcrumb v-if="!embedded" class="breadcrumb-bar" separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>内容管理</el-breadcrumb-item>
          <template v-if="isEdit">
            <el-breadcrumb-item v-for="c in categoryPath" :key="c.id" :to="`/category/${c.slug}`">{{ c.name }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ form.title || '题目' }}</el-breadcrumb-item>
          </template>
        </el-breadcrumb>

        <div class="app-card">
          <h3 class="section-title">{{ isEdit ? '编辑面试题' : '添加面试题' }}</h3>
          <el-form label-width="90px" class="edit-form">
            <el-form-item label="标题" required>
              <el-input v-model="form.title" placeholder="如：TCP 为什么需要三次握手？" />
            </el-form-item>
            <el-form-item label="固定链接 slug">
              <el-input v-model="form.slug" placeholder="可选，留空自动生成（如 redis-distributed-lock）" />
            </el-form-item>
            <el-form-item label="摘要">
              <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="一句话概括本题要点" />
            </el-form-item>
            <el-form-item label="所属分类" required>
              <div class="category-field">
                <el-select v-model="form.categoryId" placeholder="选择分类（支持子分类）" class="category-select">
                  <template v-for="cat in categories" :key="cat.id">
                    <el-option-group v-if="cat.children.length" :label="cat.name">
                      <el-option :label="cat.name" :value="cat.id" />
                      <el-option v-for="sub in cat.children" :key="sub.id" :label="`　${sub.name}`" :value="sub.id" />
                    </el-option-group>
                    <el-option v-else :label="cat.name" :value="cat.id" />
                  </template>
                </el-select>
                <el-button size="small" @click="categoryManageVisible = true">管理分类</el-button>
              </div>
            </el-form-item>
            <el-form-item label="难度">
              <el-select v-model="form.difficulty" style="width: 160px">
                <el-option label="简单" value="EASY" />
                <el-option label="中等" value="MEDIUM" />
                <el-option label="困难" value="HARD" />
              </el-select>
            </el-form-item>
            <el-form-item label="标签">
              <el-input v-model="form.tagsText" placeholder="多个标签用逗号分隔，如：TCP, 传输层" />
            </el-form-item>
            <el-form-item label="正文">
              <div class="content-editor">
                <div class="editor-toolbar">
                  <span class="editor-hint">支持 Markdown：## 标题、- 列表、``` 代码块、**加粗**</span>
                  <div class="editor-actions">
                    <el-button size="small" @click="mdImportVisible = true">导入 Markdown</el-button>
                    <el-button size="small" type="primary" plain :disabled="!form.content.trim()" @click="previewVisible = true">
                      预览
                    </el-button>
                  </div>
                </div>
                <el-input v-model="form.content" type="textarea" :rows="12" placeholder="支持 Markdown，留空则使用默认模板" />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="submit">{{ isEdit ? '保存修改' : '保存并查看' }}</el-button>
            </el-form-item>
          </el-form>
        </div>

        <el-dialog v-model="previewVisible" title="正文预览" width="960px" top="6vh" class="preview-dialog" append-to-body @open="onPreviewOpen">
          <div ref="previewBody" class="article-body preview-body" v-html="previewHtml"></div>
        </el-dialog>

        <CategoryManageDialog v-model="categoryManageVisible" />

        <el-dialog v-model="mdImportVisible" title="导入 Markdown 正文" width="520px" top="12vh" append-to-body @open="onMdImportOpen">
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
              <div class="upload-tip">只导入正文内容（自动去掉文件头 frontmatter），会替换当前正文；分类、难度、标签请在表单中填写</div>
            </div>
          </el-upload>
          <div v-if="mdFileName" class="md-file-name">已选择：{{ mdFileName }}</div>
          <template #footer>
            <el-button @click="mdImportVisible = false">取消</el-button>
            <el-button type="primary" :disabled="!mdContent" @click="applyMdImport">导入到正文</el-button>
          </template>
        </el-dialog>
      </main>
    </div>
    <AppFooter v-if="!embedded" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type UploadFile, type UploadUserFile } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import hljs from 'highlight.js'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { unsavedState } from '@/utils/unsaved'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import CategoryManageDialog from '@/components/admin/CategoryManageDialog.vue'
import { getArticleDetail, createArticleApi, updateArticleApi } from '@/api/article'
import { useCategoryStore } from '@/stores/category'
import { getCategoryPath } from '@/utils/category'

const router = useRouter()
const route = useRoute()
const categoryStore = useCategoryStore()
const embedded = computed(() => route.meta.embedded === true)
const saving = ref(false)
const detailId = ref(0)
const previewVisible = ref(false)
const categoryManageVisible = ref(false)
const mdImportVisible = ref(false)
const mdFileList = ref<UploadUserFile[]>([])
const mdContent = ref('')
const mdFileName = ref('')
const editCategorySlug = ref('')
const previewBody = ref<HTMLElement | null>(null)

const categories = computed(() => categoryStore.tree)

const form = reactive({
  title: '',
  slug: '',
  summary: '',
  categoryId: undefined as number | undefined,
  difficulty: 'MEDIUM',
  tagsText: '',
  content: '',
})
const initialSnapshot = ref(snapshotForm())

const editingSlug = computed(() => String(route.params.slug || ''))
const isEdit = computed(() => !!editingSlug.value)
const isDirty = computed(() => snapshotForm() !== initialSnapshot.value)
const categoryPath = computed(() =>
  isEdit.value && editCategorySlug.value
    ? getCategoryPath(editCategorySlug.value, categoryStore.tree) || []
    : []
)

watch(categories, () => {
  if (!form.categoryId) return
  const exists = categories.value.some(
    (c) => c.id === form.categoryId || c.children.some((s) => s.id === form.categoryId)
  )
  if (!exists) {
    form.categoryId = undefined
  }
})

function snapshotForm(): string {
  return JSON.stringify({
    title: form.title,
    slug: form.slug,
    summary: form.summary,
    categoryId: form.categoryId,
    difficulty: form.difficulty,
    tagsText: form.tagsText,
    content: form.content
  })
}

watch(isDirty, (v) => {
  unsavedState.dirty = v
}, { immediate: true })

const previewHtml = computed(() => DOMPurify.sanitize(marked.parse(form.content || '') as string))

function onPreviewOpen() {
  nextTick(() => {
    previewBody.value?.querySelectorAll('pre code').forEach((block) => {
      hljs.highlightElement(block as HTMLElement)
    })
  })
}

function onMdImportOpen() {
  mdFileList.value = []
  mdContent.value = ''
  mdFileName.value = ''
}

function onMdExceed() {
  ElMessage.warning('只能选择一个 .md 文件')
}

function onMdFileChange(file: UploadFile, files: UploadUserFile[]) {
  mdFileList.value = files.slice(-1)
  const raw = file.raw
  if (!raw) return
  const reader = new FileReader()
  reader.onload = () => {
    mdContent.value = stripFrontmatter(String(reader.result || ''))
    mdFileName.value = raw.name
  }
  reader.readAsText(raw, 'utf-8')
}

function stripFrontmatter(text: string): string {
  const trimmed = text.trim()
  if (trimmed.startsWith('---')) {
    const end = trimmed.indexOf('\n---')
    if (end > 0) {
      return trimmed.slice(end + 4).trim()
    }
  }
  return trimmed
}

function applyMdImport() {
  if (!mdContent.value) {
    ElMessage.warning('请先选择 .md 文件')
    return
  }
  form.content = mdContent.value
  ElMessage.success(`已导入 ${mdFileName.value}，可点击“预览”查看效果`)
  mdImportVisible.value = false
}

function htmlToText(html: string): string {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  return (doc.body.textContent || '').trim()
}


onMounted(() => {
  initialSnapshot.value = snapshotForm()
  categoryStore.fetchTree()
  if (isEdit.value) {
    loadForEdit()
  }
})

async function loadForEdit() {
  try {
    const resp = await getArticleDetail(editingSlug.value)
    const a = resp.article
    detailId.value = a.id
    editCategorySlug.value = a.categorySlug
    form.title = a.title
    form.slug = a.slug
    form.summary = a.summary
    form.categoryId = a.categoryId
    form.difficulty = a.difficulty
    form.tagsText = a.tags.join(', ')
    form.content = a.contentMd || htmlToText(a.contentHtml)
    initialSnapshot.value = snapshotForm()
  } catch {
    router.replace('/admin')
  }
}

async function submit() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  if (!form.categoryId) {
    ElMessage.warning('请选择所属分类')
    return
  }
  saving.value = true
  try {
    const tags = form.tagsText
      .split(/[,，]/)
      .map((t) => t.trim())
      .filter(Boolean)
    const payload = {
      title: form.title.trim(),
      slug: form.slug.trim() || undefined,
      summary: form.summary.trim(),
      categoryId: form.categoryId,
      difficulty: form.difficulty,
      tags,
      contentMd: form.content
    }
    const article = isEdit.value
      ? await updateArticleApi(detailId.value, payload)
      : await createArticleApi(payload)
    ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
    initialSnapshot.value = snapshotForm()
    unsavedState.dirty = false
    router.push(`/article/${article.slug}`)
  } finally {
    saving.value = false
  }
}
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

.page-body.embedded {
  padding: 0;
}

.content {
  width: 100%;
  max-width: 960px;
}

.edit-form {
  width: 100%;
}

.category-field {
  display: flex;
  gap: 8px;
  width: 100%;
}

.category-select {
  flex: 1;
}

.section-title {
  margin: 0 0 18px;
  color: #6b5208;
}

.content-editor {
  width: 100%;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.editor-hint {
  color: var(--app-text-secondary);
  font-size: 12px;
}

.editor-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.preview-dialog {
  background: var(--app-bg);
  border: 1px solid var(--app-border);
  border-radius: 12px;
}

.preview-dialog .el-dialog__body {
  padding: 4px 16px 16px;
}

.preview-body {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 28px 36px;
  width: 100%;
  max-width: 920px;
  margin: 0 auto;
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

.md-file-name {
  margin-top: 10px;
  font-size: 13px;
  color: var(--app-text);
}
</style>
