<template>
  <div class="page">
    <AppHeader v-if="!embedded" />
    <div class="page-body" :class="{ embedded }">
      <main class="content">
        <el-breadcrumb v-if="!embedded" class="breadcrumb-bar" separator="/">
          <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>内容管理</el-breadcrumb-item>
          <template v-if="isEdit">
            <el-breadcrumb-item v-for="c in categoryPath" :key="c.id" :to="`/category/${c.slug}`">{{ c.name }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ form.title || '题目' }}</el-breadcrumb-item>
          </template>
        </el-breadcrumb>

        <div class="app-card">
          <h3 class="section-title">{{ isEdit ? '编辑内容' : '添加内容' }}</h3>
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
            <el-form-item label="文档链接">
              <el-input v-model="form.docUrl" placeholder="选填，如 https://arthas.aliyun.com/（点击将在新标签页打开）" />
            </el-form-item>
            <el-form-item label="专栏类型">
              <el-radio-group v-model="form.columnType" @change="onColumnTypeChange">
                <el-radio-button value="tech">技术问题专栏</el-radio-button>
                <el-radio-button value="topic">文章专栏</el-radio-button>
                <el-radio-button value="learn">学习专题</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="form.columnType !== 'topic'" :label="form.columnType === 'learn' ? '学习分类' : '所属分类'" required>
              <div class="category-field">
                <template v-if="form.columnType === 'learn'">
                  <el-select v-model="form.learnCategoryId" placeholder="选择学习分类" class="category-select">
                    <el-option v-for="c in learnCategories" :key="c.id" :label="c.name" :value="c.id" />
                  </el-select>
                  <el-button size="small" @click="$router.push('/admin/learn-categories')">管理分类</el-button>
                </template>
                <template v-else>
                <el-select v-model="form.categoryId" placeholder="选择分类（支持子分类）" class="category-select">
                  <template v-for="cat in categories" :key="cat.id">
                    <el-option :label="cat.name" :value="cat.id">
                      <span :class="{ 'cat-parent-label': cat.children.length }">{{ cat.name }}</span>
                    </el-option>
                    <el-option v-for="sub in cat.children" :key="sub.id" :label="sub.name" :value="sub.id">
                      <span class="cat-child-label">{{ sub.name }}</span>
                    </el-option>
                  </template>
                </el-select>
                <el-button size="small" @click="categoryManageVisible = true">管理分类</el-button>
                </template>
              </div>
            </el-form-item>
            <el-form-item v-if="form.columnType === 'topic'" label="置顶">
              <el-switch v-model="form.isPinned" :active-value="1" :inactive-value="0" />
              <span class="field-tip">置顶后排在文章分享列表最前面</span>
            </el-form-item>
            <el-form-item v-if="form.columnType !== 'tech'" label="封面图">
              <div class="cover-field">
                <div class="cover-upload">
                  <el-upload
                    accept=".png,.jpg,.jpeg,.webp"
                    :show-file-list="false"
                    :http-request="onCoverUpload"
                    :before-upload="beforeCoverUpload"
                  >
                    <div v-if="form.coverUrl" class="cover-preview">
                      <img :src="form.coverUrl" alt="封面预览" />
                      <div class="cover-mask">点击更换</div>
                    </div>
                    <div v-else class="cover-placeholder-box">
                      <el-icon :size="22"><Plus /></el-icon>
                      <span>上传封面</span>
                      <span class="cover-tip">建议 16:9，如 1280×720，最大 10MB</span>
                    </div>
                  </el-upload>
                  <el-button v-if="form.coverUrl" size="small" type="danger" plain class="cover-remove" @click="removeCover">
                    移除封面
                  </el-button>
                </div>
                <el-input v-model="form.coverUrl" placeholder="或直接粘贴图片 URL" class="cover-url" />
              </div>
            </el-form-item>
            <el-form-item v-if="form.columnType === 'tech'" label="难度">
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
                <el-input
                  ref="contentTextarea"
                  v-model="form.content"
                  type="textarea"
                  :rows="12"
                  placeholder="支持 Markdown，留空则使用默认模板"
                  @paste="onContentPaste"
                />
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
          <div class="md-size-tip">单个文件最大 20MB</div>
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
import { Plus, UploadFilled } from '@element-plus/icons-vue'
import { unsavedState } from '@/utils/unsaved'
import { highlightCodeBlocks, renderDiagrams, renderMarkdown } from '@/utils/markdown'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import CategoryManageDialog from '@/components/admin/CategoryManageDialog.vue'
import { getArticleDetail, createArticleApi, updateArticleApi } from '@/api/article'
import { uploadCoverApi } from '@/api/admin'
import { getLearnCategoriesApi } from '@/api/article'
import { useCategoryStore } from '@/stores/category'
import { getCategoryPath } from '@/utils/category'
import { dataUrlToFile, readFileAsDataUrl, readFileAsText } from '@/utils/file'
import { useDraftStorage } from '@/composables/useDraft'
import type { LearnCategory } from '@/types'

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
const contentTextarea = ref()
/** 粘贴图片：占位符 token -> base64 dataURL，避免把大段 base64 写进正文 textarea */
const pasteImages = reactive<Record<string, string>>({})
let pasteSeq = 0
const editCategorySlug = ref('')
const previewBody = ref<HTMLElement | null>(null)

const categories = computed(() => categoryStore.tree)
const learnCategories = ref<LearnCategory[]>([])

const form = reactive({
  title: '',
  slug: '',
  summary: '',
  docUrl: '',
  columnType: 'tech',
  learnCategoryId: undefined as number | undefined,
  categoryId: undefined as number | undefined,
  difficulty: 'MEDIUM',
  isPinned: 0,
  coverUrl: '',
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
    docUrl: form.docUrl,
    columnType: form.columnType,
    learnCategoryId: form.learnCategoryId,
    categoryId: form.categoryId,
    difficulty: form.difficulty,
    isPinned: form.isPinned,
    coverUrl: form.coverUrl,
    tagsText: form.tagsText,
    content: form.content
  })
}

/** 草稿存储 key：按「新建/编辑+slug」隔离，路由切换（组件复用）时也能跟随变化 */
const draftKey = computed(() =>
  isEdit.value ? `draft:article:edit:${editingSlug.value}` : 'draft:article:create'
)

/** 草稿快照：额外包含粘贴图片（pasteImages），否则刷新后 `paste://` 占位符会变成死链 */
function draftSnapshot(): string {
  return JSON.stringify({
    title: form.title,
    slug: form.slug,
    summary: form.summary,
    docUrl: form.docUrl,
    columnType: form.columnType,
    learnCategoryId: form.learnCategoryId,
    categoryId: form.categoryId,
    difficulty: form.difficulty,
    isPinned: form.isPinned,
    coverUrl: form.coverUrl,
    tagsText: form.tagsText,
    content: form.content,
    pasteImages: { ...pasteImages }
  })
}

function draftRestore(raw: string) {
  const s = JSON.parse(raw) as Record<string, unknown>
  if (typeof s.title === 'string') form.title = s.title
  if (typeof s.slug === 'string') form.slug = s.slug
  if (typeof s.summary === 'string') form.summary = s.summary
  if (typeof s.docUrl === 'string') form.docUrl = s.docUrl
  if (s.columnType === 'tech' || s.columnType === 'topic' || s.columnType === 'learn') form.columnType = s.columnType
  if (typeof s.learnCategoryId === 'number') form.learnCategoryId = s.learnCategoryId
  if (typeof s.categoryId === 'number') form.categoryId = s.categoryId
  if (s.difficulty === 'EASY' || s.difficulty === 'MEDIUM' || s.difficulty === 'HARD') form.difficulty = s.difficulty
  if (s.isPinned === 1 || s.isPinned === 0) form.isPinned = s.isPinned
  if (typeof s.coverUrl === 'string') form.coverUrl = s.coverUrl
  if (typeof s.tagsText === 'string') form.tagsText = s.tagsText
  if (typeof s.content === 'string') form.content = s.content
  const paste = s.pasteImages
  if (paste && typeof paste === 'object') {
    for (const [k, v] of Object.entries(paste as Record<string, unknown>)) {
      if (typeof v === 'string') pasteImages[k] = v
    }
  }
}

/** 草稿持久化：刷新/切页不丢未保存的修改（含粘贴图片与封面）。 */
const draft = useDraftStorage({
  getKey: () => draftKey.value,
  getSnapshot: draftSnapshot,
  restore: draftRestore
})

function onColumnTypeChange() {
  if (form.columnType === 'topic') {
    form.categoryId = undefined
  }
}

function beforeCoverUpload(file: File) {
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

async function onCoverUpload(options: { file: File; onSuccess: (res: unknown) => void; onError: (err: Error) => void }) {
  try {
    form.coverUrl = await readFileAsDataUrl(options.file)
    options.onSuccess({ url: form.coverUrl })
    ElMessage.success('封面已选择，保存时上传')
  } catch (e) {
    options.onError(e as Error)
  }
}

function removeCover() {
  form.coverUrl = ''
}

watch(isDirty, (v) => {
  unsavedState.dirty = v
}, { immediate: true })

const previewHtml = computed(() => renderMarkdown(resolvePasteImages(form.content || '')))

function onPreviewOpen() {
  nextTick(async () => {
    highlightCodeBlocks(previewBody.value)
    await renderDiagrams(previewBody.value)
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

async function onMdFileChange(file: UploadFile, files: UploadUserFile[]) {
  mdFileList.value = files.slice(-1)
  const raw = file.raw
  if (!raw) return
  if (raw.size > 20 * 1024 * 1024) {
    ElMessage.warning('文档不能超过 20MB')
    return
  }
  try {
    mdContent.value = stripFrontmatter(await readFileAsText(raw))
    mdFileName.value = raw.name
  } catch {
    // 读取失败忽略
  }
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

/** 直接在正文粘贴图片：图片暂存前台（token -> base64），正文里只写短占位符，保存时再还原交给后端 */
async function onContentPaste(e: ClipboardEvent) {
  const files = e.clipboardData?.files
  const img = files && Array.from(files).find((f) => f.type.startsWith('image/'))
  if (!img) return
  e.preventDefault()
  try {
    const dataUrl = await readFileAsDataUrl(img)
    const token = `paste-${Date.now()}-${++pasteSeq}`
    pasteImages[token] = dataUrl
    insertImageUrl(`paste://${token}`)
  } catch {
    // 读取失败忽略
  }
}

/** 把正文里的 paste://token 占位符还原成 base64 dataURL（预览用；保存时也用它作为后端入参） */
function resolvePasteImages(md: string): string {
  return md.replace(/paste:\/\/([A-Za-z0-9_-]+)/g, (whole, token: string) => pasteImages[token] || whole)
}

function insertImageUrl(url: string) {
  const snippet = `![图片](${url})`
  const ta = contentTextarea.value?.textarea as HTMLTextAreaElement | undefined
  if (ta && typeof ta.selectionStart === 'number') {
    const start = ta.selectionStart
    const end = ta.selectionEnd
    form.content = form.content.slice(0, start) + snippet + form.content.slice(end)
    nextTick(() => {
      ta.focus()
      const pos = start + snippet.length
      ta.setSelectionRange(pos, pos)
    })
  } else {
    form.content = (form.content ? form.content + '\n' : '') + snippet
  }
}

function htmlToText(html: string): string {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  return (doc.body.textContent || '').trim()
}


onMounted(async () => {
  initialSnapshot.value = snapshotForm()
  categoryStore.fetchTree()
  getLearnCategoriesApi()
    .then((list) => {
      learnCategories.value = list
    })
    .catch(() => {})
  if (isEdit.value) {
    await loadForEdit()
  }
  // 恢复未保存草稿（草稿优先于服务端数据）；随后重置“基线”，避免误判为未保存修改。
  await draft.restoreStored()
  initialSnapshot.value = snapshotForm()
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
    form.docUrl = a.docUrl || ''
    form.columnType = a.columnType === 'learn' ? 'learn' : a.columnType === 'topic' ? 'topic' : 'tech'
    form.learnCategoryId = a.learnCategoryId || undefined
    form.categoryId = a.categoryId
    form.difficulty = a.difficulty
    form.isPinned = a.isPinned === 1 ? 1 : 0
    form.coverUrl = a.coverUrl || ''
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
  if (form.columnType === 'learn' && !form.learnCategoryId) {
    ElMessage.warning('请选择学习分类')
    return
  }
  if (form.columnType !== 'topic' && form.columnType !== 'learn' && !form.categoryId) {
    ElMessage.warning('请选择所属分类')
    return
  }
  saving.value = true
  try {
    let coverUrl = form.coverUrl.trim() || undefined
    if (coverUrl && coverUrl.startsWith('data:')) {
      coverUrl = (await uploadCoverApi(dataUrlToFile(coverUrl, 'cover.png'))).url
    }
    const tags = form.tagsText
      .split(/[,，]/)
      .map((t) => t.trim())
      .filter(Boolean)
    const payload = {
      title: form.title.trim(),
      slug: form.slug.trim() || undefined,
      summary: form.summary.trim(),
      docUrl: form.docUrl.trim() || undefined,
      columnType: form.columnType,
      learnCategoryId: form.columnType === 'learn' ? form.learnCategoryId : undefined,
      categoryId: form.columnType === 'topic' ? undefined : form.categoryId,
      difficulty: form.difficulty,
      isPinned: form.isPinned,
      coverUrl,
      tags,
      contentMd: resolvePasteImages(form.content)
    }
    const article = isEdit.value
      ? await updateArticleApi(detailId.value, payload)
      : await createArticleApi(payload)
    ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
    initialSnapshot.value = snapshotForm()
    unsavedState.dirty = false
    draft.clear()
    const savedColumnType = article.columnType || form.columnType
    if (savedColumnType === 'learn') {
      const cat = learnCategories.value.find((c) => c.id === form.learnCategoryId)
      router.push({ path: cat ? `/learn/${cat.slug}` : '/learn', query: { article: article.slug } })
    } else if (savedColumnType === 'topic') {
      router.push(`/articles/${article.slug}`)
    } else {
      router.push(`/article/${article.slug}`)
    }
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

.field-tip {
  margin-left: 10px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.cover-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.cover-upload {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.cover-upload :deep(.el-upload) {
  width: 320px;
}

.cover-preview,
.cover-placeholder-box {
  position: relative;
  width: 320px;
  aspect-ratio: 16 / 9;
  border-radius: 10px;
  overflow: hidden;
  border: 1px dashed rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  color: #ffffff;
  font-size: 13px;
  opacity: 0;
  transition: opacity 0.2s;
}

.cover-preview:hover .cover-mask {
  opacity: 1;
}

.cover-placeholder-box {
  flex-direction: column;
  gap: 4px;
  color: var(--app-text-secondary);
  font-size: 13px;
  background: rgba(255, 255, 255, 0.03);
}

.cover-tip {
  font-size: 11px;
  color: #68788f;
}

.cover-remove {
  margin-top: 2px;
}

.cover-url {
  width: 100%;
}

.cat-parent-label {
  font-weight: 600;
}

.cat-child-label {
  padding-left: 18px;
}

.section-title {
  margin: 0 0 18px;
  color: #e9b862;
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

.md-size-tip {
  margin-top: 8px;
  text-align: right;
  font-size: 12px;
  color: var(--app-text-secondary);
}
</style>
