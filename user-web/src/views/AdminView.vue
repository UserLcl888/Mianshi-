<template>
  <div class="page">
    <AppHeader />
    <div class="page-body">
      <main class="content">
        <el-breadcrumb class="breadcrumb-bar" separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>内容管理</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="app-card">
          <h3 class="section-title">{{ isEdit ? '编辑面试题' : '添加面试题' }}</h3>
          <el-form label-width="90px" style="max-width: 720px">
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
              <el-select v-model="form.categoryId" placeholder="选择分类（支持子分类）" style="width: 100%">
                <el-option-group v-for="cat in categories" :key="cat.id" :label="cat.name">
                  <el-option :label="cat.name" :value="cat.id" />
                  <el-option v-for="sub in cat.children" :key="sub.id" :label="`　${sub.name}`" :value="sub.id" />
                </el-option-group>
              </el-select>
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
            <el-form-item label="关联题目">
              <el-select
                v-model="form.relatedIds"
                multiple
                filterable
                collapse-tags
                placeholder="选择相关联的题目（如：分布式锁）"
                style="width: 100%"
              >
                <el-option v-for="a in relatedOptions" :key="a.id" :label="a.title" :value="a.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="正文">
              <el-input v-model="form.content" type="textarea" :rows="10" placeholder="可选，支持 Markdown（## 标题、- 列表、代码块等）；留空则使用默认模板" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="submit">{{ isEdit ? '保存修改' : '保存并查看' }}</el-button>
            </el-form-item>
          </el-form>
        </div>
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { getArticleDetail, createArticleApi, updateArticleApi, getArticles } from '@/api/article'
import { useCategoryStore } from '@/stores/category'

const router = useRouter()
const route = useRoute()
const categoryStore = useCategoryStore()
const saving = ref(false)
const detailId = ref(0)
const relatedOptions = ref<Array<{ id: number; title: string }>>([])

const categories = computed(() => categoryStore.tree)

const form = reactive({
  title: '',
  slug: '',
  summary: '',
  categoryId: undefined as number | undefined,
  difficulty: 'MEDIUM',
  tagsText: '',
  content: '',
  relatedIds: [] as number[]
})

const editingSlug = computed(() => String(route.params.slug || ''))
const isEdit = computed(() => !!editingSlug.value)

function htmlToText(html: string): string {
  return html
    .replace(/<pre><code[^>]*>/g, '')
    .replace(/<\/code><\/pre>/g, '')
    .replace(/<[^>]+>/g, '\n')
    .replace(/&nbsp;/g, ' ')
    .replace(/\n{2,}/g, '\n')
    .trim()
}

onMounted(() => {
  categoryStore.fetchTree()
  loadRelatedOptions()
  if (isEdit.value) {
    loadForEdit()
  }
})

async function loadRelatedOptions() {
  try {
    const res = await getArticles({ page: 1, size: 200 })
    relatedOptions.value = res.list.map((a) => ({ id: a.id, title: a.title }))
  } catch {
    // 关联题目加载失败不阻塞编辑
  }
}

async function loadForEdit() {
  try {
    const resp = await getArticleDetail(editingSlug.value)
    const a = resp.article
    detailId.value = a.id
    form.title = a.title
    form.slug = a.slug
    form.summary = a.summary
    form.categoryId = a.categoryId
    form.difficulty = a.difficulty
    form.tagsText = a.tags.join(', ')
    form.content = htmlToText(a.contentHtml)
    form.relatedIds = resp.relatedIds || []
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
      contentMd: form.content,
      relatedIds: form.relatedIds
    }
    const article = isEdit.value
      ? await updateArticleApi(detailId.value, payload)
      : await createArticleApi(payload)
    ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
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
}

.content {
  max-width: 860px;
}

.section-title {
  margin: 0 0 18px;
  color: #6b5208;
}
</style>
