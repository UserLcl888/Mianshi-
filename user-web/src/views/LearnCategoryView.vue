<template>
  <div class="learn-cat-page">
    <AppHeader />
    <div class="learn-cat-body">
      <!-- 左：该板块学习文章列表（无滚动条，随页面滑动） -->
      <aside class="cat-list">
        <div class="cat-list-title">{{ categoryName }}</div>
        <div v-if="loadingList" class="cat-tip">加载中…</div>
        <template v-else>
          <div
            v-for="a in articles"
            :key="a.id"
            class="cat-item"
            :class="{ active: a.slug === currentSlug }"
            @click="select(a.slug)"
          >
            {{ a.title }}
          </div>
          <div v-if="!articles.length" class="cat-tip">暂无内容</div>
        </template>
      </aside>

      <!-- 中：当前文章正文 -->
      <main class="cat-content">
        <div v-if="loadingDetail" class="cat-empty">加载中…</div>
        <article v-else-if="detail" class="cat-article">
          <h1 class="cat-article-title">{{ detail.article.title }}</h1>
          <p v-if="detail.article.summary" class="cat-article-summary">{{ detail.article.summary }}</p>
          <div ref="contentEl" class="article-body" v-html="detail.article.contentHtml"></div>
        </article>
        <div v-else class="cat-empty">文章不存在或已下架</div>
      </main>

      <!-- 右：当前文章目录 -->
      <aside class="cat-toc">
        <div class="toc-title">目录</div>
        <div v-if="!detail || !detail.article.toc.length" class="toc-empty">暂无目录</div>
        <a
          v-for="t in detail?.article.toc || []"
          :key="t.id"
          class="toc-item"
          :class="`lv-${t.level}`"
          href="#"
          @click.prevent="scrollTo(t.id)"
        >
          {{ t.text }}
        </a>
      </aside>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import hljs from 'highlight.js'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { getArticleDetail, getLearnArticlesApi, getLearnCategoriesApi } from '@/api/article'
import type { ArticleDetailResp, ArticleListItem } from '@/types'

const route = useRoute()
const categorySlug = computed(() => String(route.params.categorySlug || ''))
const categoryName = ref('学习专题')
const articles = ref<ArticleListItem[]>([])
const currentSlug = ref('')
const detail = ref<ArticleDetailResp | null>(null)
const loadingList = ref(false)
const loadingDetail = ref(false)
const contentEl = ref<HTMLElement | null>(null)

async function loadList() {
  loadingList.value = true
  try {
    const [cats, res] = await Promise.all([
      getLearnCategoriesApi(),
      getLearnArticlesApi({ categorySlug: categorySlug.value, page: 1, size: 100 })
    ])
    const hit = cats.find((c) => c.slug === categorySlug.value)
    if (hit) categoryName.value = hit.name
    articles.value = res.list
    if (res.list.length && !currentSlug.value) {
      select(res.list[0].slug)
    }
  } catch {
    // 拦截器已提示
  } finally {
    loadingList.value = false
  }
}

async function select(slug: string) {
  if (slug === currentSlug.value && detail.value) return
  currentSlug.value = slug
  loadingDetail.value = true
  try {
    detail.value = await getArticleDetail(slug)
    await nextTick()
    if (contentEl.value) {
      contentEl.value.querySelectorAll('pre code').forEach((block) => {
        hljs.highlightElement(block as HTMLElement)
      })
    }
  } catch {
    detail.value = null
  } finally {
    loadingDetail.value = false
  }
}

function scrollTo(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

watch(categorySlug, () => {
  currentSlug.value = ''
  detail.value = null
  loadList()
}, { immediate: true })
</script>

<style scoped>
.learn-cat-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.learn-cat-body {
  flex: 1;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 24px 40px;
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

/* 左：文章列表，无滚动条，随页面滑动 */
.cat-list {
  width: 240px;
  flex-shrink: 0;
}

.cat-list-title {
  font-size: 15px;
  font-weight: 700;
  color: #e9b862;
  padding: 10px 12px;
  border-left: 3px solid var(--app-accent);
  margin-bottom: 10px;
}

.cat-item {
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 13px;
  color: #b8c0cf;
  cursor: pointer;
  line-height: 1.5;
  transition: background 0.15s, color 0.15s;
}

.cat-item:hover {
  background: var(--app-accent-soft);
  color: #e9b862;
}

.cat-item.active {
  background: var(--app-accent-soft);
  color: var(--app-accent);
  font-weight: 600;
}

.cat-tip {
  padding: 12px;
  font-size: 13px;
  color: #8a9bb5;
}

/* 中：正文 */
.cat-content {
  flex: 1;
  min-width: 0;
}

.cat-article {
  background: rgba(18, 24, 38, 0.66);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 30px 36px;
}

.cat-article-title {
  margin: 0 0 12px;
  font-size: 26px;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.4;
}

.cat-article-summary {
  margin: 0 0 22px;
  padding: 12px 16px;
  border-radius: 10px;
  background: rgba(232, 154, 31, 0.08);
  border: 1px solid rgba(232, 154, 31, 0.22);
  font-size: 13px;
  color: #b8c0cf;
  line-height: 1.7;
}

.cat-empty {
  padding: 70px 20px;
  text-align: center;
  color: #8a9bb5;
  font-size: 14px;
}

/* 右：目录 */
.cat-toc {
  width: 220px;
  flex-shrink: 0;
}

.toc-title {
  font-size: 14px;
  font-weight: 700;
  color: #e9b862;
  padding: 8px 10px;
  border-bottom: 1px solid var(--app-border);
  margin-bottom: 8px;
}

.toc-empty {
  padding: 10px;
  font-size: 13px;
  color: #8a9bb5;
}

.toc-item {
  display: block;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 13px;
  color: #b8c0cf;
  line-height: 1.5;
  transition: background 0.15s, color 0.15s;
}

.toc-item:hover {
  background: var(--app-accent-soft);
  color: var(--app-accent);
}

.toc-item.lv-2 {
  padding-left: 18px;
}

.toc-item.lv-3 {
  padding-left: 28px;
  font-size: 12.5px;
}

@media (max-width: 980px) {
  .learn-cat-body {
    flex-direction: column;
  }

  .cat-list,
  .cat-toc {
    width: 100%;
  }
}
</style>
