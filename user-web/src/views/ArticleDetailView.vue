<template>
  <div class="page">
    <AppHeader />
    <div class="page-body">
      <template v-if="!isTopicArticle">
        <CategorySidebar :active-category-slug="categorySlug" :active-article-slug="activeSlug" />
      </template>
      <aside v-else class="topic-side">
        <router-link to="/articles" class="topic-back-card">
          <span class="back-arrow">←</span>
          <div class="back-body">
            <div class="back-title">知识分享</div>
            <div class="back-sub">文章分享</div>
          </div>
        </router-link>
        <router-link
          v-if="isAdmin"
          :to="`/admin/edit/${detail?.article.slug}`"
          class="topic-edit-card"
        >
          <el-icon :size="16"><EditPen /></el-icon>
          <span>编辑这篇文章</span>
        </router-link>
      </aside>

      <main class="content">
        <el-breadcrumb class="breadcrumb-bar" separator="/">
          <el-breadcrumb-item :to="isTopicArticle ? { path: '/articles' } : { path: '/home' }">
            {{ isTopicArticle ? '文章' : '首页' }}
          </el-breadcrumb-item>
          <el-breadcrumb-item v-for="c in categoryPath" :key="c.id" :to="`/category/${c.slug}`">{{ c.name }}</el-breadcrumb-item>
          <el-breadcrumb-item>{{ detail?.article.title }}</el-breadcrumb-item>
        </el-breadcrumb>

        <article v-if="detail" class="app-card">
          <img
            v-if="detail.article.coverUrl"
            :src="detail.article.coverUrl"
            :alt="detail.article.title"
            class="article-cover"
          />
          <header class="article-header">
            <h1 class="article-title">{{ detail.article.title }}</h1>
            <div class="article-meta">
              <el-tag v-if="detail.article.columnType === 'topic'" size="small" effect="plain" type="success">文章分享</el-tag>
              <DifficultyTag v-if="!isTopicArticle" :difficulty="detail.article.difficulty" />
              <el-tag v-for="t in detail.article.tags" :key="t" size="small" effect="plain" type="warning">{{ t }}</el-tag>
              <span class="meta-text">{{ detail.article.viewCount }} 次浏览</span>
              <span class="meta-text">更新于 {{ formatDateTime(detail.article.updatedAt) }}</span>
              <a
                v-if="detail.article.docUrl"
                :href="detail.article.docUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="doc-link"
              >
                <el-button size="small" type="primary" plain>文档链接</el-button>
              </a>
              <router-link v-if="isAdmin" :to="`/admin/edit/${detail.article.slug}`" class="edit-link">
                <el-button size="small" type="warning" plain>编辑</el-button>
              </router-link>
            </div>
          </header>

          <div class="article-layout">
            <div ref="contentEl" class="article-body" v-html="detail.article.contentHtml || renderMarkdown(detail.article.contentMd)"></div>
          </div>

          <PrevNextNav :prev="detail.prev" :next="detail.next" :base="isTopicArticle ? '/articles' : '/article'" />
        </article>

        <div v-else-if="accessState" class="app-card access-card">
          <div class="access-icon"><el-icon :size="42"><Lock /></el-icon></div>
          <h2 class="access-title">{{ accessTitle || '该内容需申请访问' }}</h2>
          <p class="access-desc">{{ accessState.message }}</p>
          <div class="access-actions">
            <template v-if="!auth.isLoggedIn">
              <el-button type="primary" @click="goLogin">去登录申请</el-button>
            </template>
            <template v-else-if="accessStatus === 'PENDING'">
              <el-tag type="warning" size="large">申请审核中，请耐心等待</el-tag>
            </template>
            <template v-else-if="accessStatus === 'REJECTED'">
              <el-tag type="danger">申请未通过</el-tag>
              <el-button type="primary" @click="openApply">重新申请</el-button>
            </template>
            <template v-else>
              <el-button type="primary" @click="openApply">申请访问</el-button>
            </template>
          </div>
        </div>

        <div v-else-if="!loading" class="app-card">
          <EmptyState text="题目不存在或已下架" />
        </div>
      </main>
      <TocPanel
        v-if="detail"
        :toc="detail.article.toc"
        :active-id="activeTocId"
        @select="activeTocId = $event"
      />

      <el-dialog v-model="applyVisible" title="申请访问" width="500px" append-to-body>
        <div class="apply-scope">
          <el-radio-group v-model="applyScope" @change="onScopeChange">
            <el-radio-button value="CATEGORY">仅申请该分类</el-radio-button>
            <el-radio-button value="ALL">申请全部分类</el-radio-button>
          </el-radio-group>
        </div>
        <div v-if="applyScope === 'ALL'" class="apply-all-tip">
          将申请 <b>{{ lockedList.length }}</b> 个暂无权限的受限分类
          <el-button link type="primary" @click="lockedVisible = true">查看分类</el-button>
        </div>
        <el-form label-position="top">
          <el-form-item label="申请理由（选填）">
            <el-input v-model="applyReason" maxlength="200" show-word-limit placeholder="简单说明申请原因" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="applyVisible = false">取消</el-button>
          <el-button type="primary" :loading="applying" @click="submitApply">提交申请</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="lockedVisible" title="暂无权限的受限分类" width="480px" append-to-body>
        <div v-if="!lockedList.length" class="locked-empty">暂无需要申请的分类</div>
        <el-table v-else :data="lockedList" stripe size="small" max-height="420">
          <el-table-column prop="name" label="分类名称" min-width="180" />
          <el-table-column prop="slug" label="标识" min-width="160" />
        </el-table>
        <template #footer>
          <el-button @click="lockedVisible = false">关闭</el-button>
        </template>
      </el-dialog>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { EditPen, Lock } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import CategorySidebar from '@/components/layout/CategorySidebar.vue'
import DifficultyTag from '@/components/article/DifficultyTag.vue'
import TocPanel from '@/components/article/TocPanel.vue'
import PrevNextNav from '@/components/article/PrevNextNav.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getArticleDetail, recordViewApi } from '@/api/article'
import { highlightCodeBlocks, renderDiagrams, renderMarkdown } from '@/utils/markdown'
import { applyAccessApi, getAccessStatusApi, getLockedCategoriesApi } from '@/api/access'
import { useCategoryStore } from '@/stores/category'
import { useAuthStore } from '@/stores/auth'
import { getCategoryPath } from '@/utils/category'
import { formatDateTime } from '@/utils/format'
import type { AccessStatusItem, ArticleDetailResp, LockedCategoryItem } from '@/types'

const route = useRoute()
const router = useRouter()
const categoryStore = useCategoryStore()
const auth = useAuthStore()

const activeSlug = computed(() => String(route.params.slug || ''))
const detail = ref<ArticleDetailResp | null>(null)
const loading = ref(true)
const accessState = ref<{ message: string } | null>(null)
const accessStatus = ref<AccessStatusItem['status'] | null>(null)
const accessTitle = ref('')
const categoryIdForApply = ref<number | null>(null)
const applyVisible = ref(false)
const applyScope = ref<'CATEGORY' | 'ALL'>('CATEGORY')
const applyReason = ref('')
const applying = ref(false)
const lockedVisible = ref(false)
const lockedList = ref<LockedCategoryItem[]>([])
const contentEl = ref<HTMLElement | null>(null)
const activeTocId = ref('')
const isAdmin = computed(() => auth.userInfo?.role === 'ADMIN')
let viewTimer: number | null = null
let viewReported = false
let accessPollTimer: number | null = null

const categorySlug = computed(() => detail.value?.article.categorySlug || '')
const categoryPath = computed(() =>
  categorySlug.value ? getCategoryPath(categorySlug.value, categoryStore.tree) || [] : []
)
const isTopicArticle = computed(() => detail.value?.article.columnType === 'topic')

async function load() {
  clearViewTimer()
  stopAccessPoll()
  viewReported = false
  loading.value = true
  activeTocId.value = ''
  accessState.value = null
  accessStatus.value = null
  accessTitle.value = ''
  categoryIdForApply.value = null
  try {
    detail.value = await getArticleDetail(activeSlug.value)
    await nextTick()
    if (contentEl.value) {
      highlightCodeBlocks(contentEl.value)
      await renderDiagrams(contentEl.value)
    }
    startViewTimer()
  } catch (e) {
    detail.value = null
    const err = e as Error & { code?: number }
    if (err.code === 40301) {
      accessState.value = { message: err.message || '该内容需申请后访问' }
      if (auth.isLoggedIn) {
        try {
          const st = await getAccessStatusApi(activeSlug.value)
          accessTitle.value = st.title
          accessStatus.value = st.status
          categoryIdForApply.value = st.categoryId
          if (st.status !== 'GRANTED') {
            startAccessPoll()
          }
        } catch {
          // 状态获取失败时保持默认提示
        }
      }
    }
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

async function onScopeChange() {
  if (applyScope.value === 'ALL' && !lockedList.value.length) {
    try {
      lockedList.value = await getLockedCategoriesApi()
    } catch {
      lockedList.value = []
    }
  }
}

function openApply() {
  applyVisible.value = true
  applyScope.value = 'CATEGORY'
  applyReason.value = ''
}

async function submitApply() {
  if (applyScope.value === 'CATEGORY' && !categoryIdForApply.value) {
    ElMessage.warning('无法获取分类信息，请刷新后重试')
    return
  }
  applying.value = true
  try {
    await applyAccessApi({
      categoryId: applyScope.value === 'CATEGORY' ? categoryIdForApply.value || undefined : undefined,
      scope: applyScope.value,
      reason: applyReason.value.trim() || undefined
    })
    ElMessage.success(applyScope.value === 'ALL' ? '已申请全部分类，等待管理员审批' : '申请已提交，等待管理员审批')
    accessStatus.value = 'PENDING'
    applyVisible.value = false
  } finally {
    applying.value = false
  }
}

function startViewTimer() {
  viewTimer = window.setTimeout(async () => {
    if (detail.value && !viewReported) {
      viewReported = true
      try {
        const count = await recordViewApi(detail.value.article.id)
        if (detail.value) {
          detail.value.article.viewCount = count
        }
      } catch {
        // 上报失败不影响浏览，静默处理
      }
    }
  }, 30000)
}

function clearViewTimer() {
  if (viewTimer !== null) {
    window.clearTimeout(viewTimer)
    viewTimer = null
  }
}

function stopAccessPoll() {
  if (accessPollTimer !== null) {
    window.clearInterval(accessPollTimer)
    accessPollTimer = null
  }
}

/** 受限内容页面：后台审批通过后自动解锁，无需用户手动刷新 */
function startAccessPoll() {
  stopAccessPoll()
  accessPollTimer = window.setInterval(async () => {
    if (!auth.isLoggedIn || !accessState.value) {
      stopAccessPoll()
      return
    }
    try {
      const st = await getAccessStatusApi(activeSlug.value)
      if (st.status === 'GRANTED') {
        stopAccessPoll()
        await load()
      }
    } catch {
      // 轮询失败保持现状，下一轮继续
    }
  }, 10000)
}

watch(activeSlug, load, { immediate: true })

function onScroll() {
  if (!detail.value) return
  let current = detail.value.article.toc[0]?.id || ''
  for (const item of detail.value.article.toc) {
    const el = document.getElementById(item.id)
    if (el && el.getBoundingClientRect().top <= 90) {
      current = item.id
    }
  }
  activeTocId.value = current
}

onMounted(async () => {
  await categoryStore.fetchTree()
  window.addEventListener('scroll', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  clearViewTimer()
  stopAccessPoll()
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
  padding: 16px var(--layout-pad-x) 16px var(--sidebar-offset);
  display: flex;
  align-items: flex-start;
  gap: 18px;
}

.content {
  flex: 1;
  min-width: 0;
}

.topic-side {
  width: 250px;
  flex-shrink: 0;
  position: sticky;
  top: 72px;
}

.topic-back-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 12px;
  background: var(--app-sidebar-bg);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  transition: border-color 0.15s, background 0.15s;
}

.topic-back-card:hover {
  border-color: var(--app-accent);
  background: var(--app-card-hover);
}

.topic-edit-card {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(232, 154, 31, 0.12);
  border: 1px solid rgba(232, 154, 31, 0.45);
  color: var(--app-accent);
  font-size: 13.5px;
  font-weight: 600;
  transition: all 0.15s;
}

.topic-edit-card:hover {
  background: rgba(232, 154, 31, 0.2);
  border-color: var(--app-accent);
}

.back-arrow {
  color: var(--app-accent);
  font-size: 16px;
  line-height: 1.4;
}

.back-body {
  min-width: 0;
}

.back-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text);
}

.back-sub {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--app-text-secondary);
}

.article-header {
  margin-bottom: 16px;
}

.article-cover {
  display: block;
  width: 100%;
  max-height: 340px;
  object-fit: cover;
  border-radius: 10px;
  margin-bottom: 18px;
}

.article-title {
  font-size: 24px;
  color: var(--app-text);
  margin: 0 0 10px;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.meta-text {
  color: var(--app-text-secondary);
  font-size: 12px;
  margin-left: 4px;
}

.edit-link {
  margin-left: 8px;
}

.doc-link {
  margin-left: 8px;
}

.article-layout {
  display: flex;
  gap: 18px;
  align-items: flex-start;
}

.article-body {
  flex: 1;
  min-width: 0;
  max-width: 920px;
}

.access-card {
  text-align: center;
  padding: 60px 32px;
}

.access-icon {
  color: var(--app-accent);
  margin-bottom: 14px;
}

.access-title {
  font-size: 20px;
  color: var(--app-text);
  margin: 0 0 10px;
}

.access-desc {
  color: var(--app-text-secondary);
  font-size: 14px;
  margin: 0 0 20px;
}

.access-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.apply-scope {
  margin-bottom: 14px;
}

.apply-all-tip {
  margin-bottom: 14px;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.locked-empty {
  padding: 40px 0;
  text-align: center;
  color: var(--app-text-secondary);
  font-size: 13px;
}
</style>
