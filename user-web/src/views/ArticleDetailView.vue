<template>
  <div class="page">
    <AppHeader />
    <div class="page-body">
      <CategorySidebar :active-category-slug="categorySlug" :active-article-slug="activeSlug" />

      <main class="content">
        <el-breadcrumb class="breadcrumb-bar" separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-for="c in categoryPath" :key="c.id" :to="`/category/${c.slug}`">{{ c.name }}</el-breadcrumb-item>
          <el-breadcrumb-item>{{ detail?.article.title }}</el-breadcrumb-item>
        </el-breadcrumb>

        <article v-if="detail" class="app-card">
          <header class="article-header">
            <h1 class="article-title">{{ detail.article.title }}</h1>
            <div class="article-meta">
              <DifficultyTag :difficulty="detail.article.difficulty" />
              <el-tag v-for="t in detail.article.tags" :key="t" size="small" effect="plain" type="warning">{{ t }}</el-tag>
              <span class="meta-text">{{ detail.article.viewCount }} 次浏览</span>
              <span class="meta-text">更新于 {{ detail.article.updatedAt }}</span>
              <router-link v-if="isAdmin" :to="`/admin/edit/${detail.article.slug}`" class="edit-link">
                <el-button size="small" type="warning" plain>编辑</el-button>
              </router-link>
            </div>
          </header>

          <div class="article-layout">
            <div ref="contentEl" class="article-body" v-html="detail.article.contentHtml" @click="onBodyClick"></div>
          </div>

          <PrevNextNav :prev="detail.prev" :next="detail.next" />
        </article>

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
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import hljs from 'highlight.js'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import CategorySidebar from '@/components/layout/CategorySidebar.vue'
import DifficultyTag from '@/components/article/DifficultyTag.vue'
import TocPanel from '@/components/article/TocPanel.vue'
import PrevNextNav from '@/components/article/PrevNextNav.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getArticleDetail, recordViewApi } from '@/api/article'
import { useCategoryStore } from '@/stores/category'
import { useAuthStore } from '@/stores/auth'
import { getCategoryPath } from '@/utils/category'
import type { ArticleDetailResp } from '@/types'

const route = useRoute()
const router = useRouter()
const categoryStore = useCategoryStore()
const auth = useAuthStore()

const activeSlug = computed(() => String(route.params.slug || ''))
const detail = ref<ArticleDetailResp | null>(null)
const loading = ref(true)
const contentEl = ref<HTMLElement | null>(null)
const activeTocId = ref('')
const isAdmin = computed(() => auth.userInfo?.role === 'ADMIN')
let viewTimer: number | null = null
let viewReported = false

const categorySlug = computed(() => detail.value?.article.categorySlug || '')
const categoryPath = computed(() =>
  categorySlug.value ? getCategoryPath(categorySlug.value, categoryStore.tree) || [] : []
)

async function load() {
  clearViewTimer()
  viewReported = false
  loading.value = true
  activeTocId.value = ''
  try {
    detail.value = await getArticleDetail(activeSlug.value)
    await nextTick()
    if (contentEl.value) {
      contentEl.value.querySelectorAll('pre code').forEach((block) => {
        hljs.highlightElement(block as HTMLElement)
      })
    }
    startViewTimer()
  } catch {
    detail.value = null
  } finally {
    loading.value = false
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

function onBodyClick(e: MouseEvent) {
  const target = (e.target as HTMLElement).closest('a') as HTMLAnchorElement | null
  if (!target) return
  const href = target.getAttribute('href') || ''
  if (href.startsWith('/article/') || href.startsWith('/category/')) {
    e.preventDefault()
    router.push(href)
  }
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

.article-header {
  margin-bottom: 16px;
}

.article-title {
  font-size: 24px;
  color: #4d4020;
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
</style>
