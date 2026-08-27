<template>
  <div class="page">
    <AppHeader />
    <div class="page-body">
      <aside class="left-col">
        <CategorySidebar :active-category-slug="''" :active-article-slug="''" />
        <div class="side-card daily-quote">
          <div class="side-title">每日一句</div>
          <p class="quote-text">“{{ quote.text }}”</p>
          <p class="quote-author">—— {{ quote.author }}</p>
        </div>
      </aside>

      <main class="content">
        <el-carousel
          class="banner-carousel"
          height="240px"
          :interval="4000"
          arrow="hover"
          indicator-position="outside"
        >
          <el-carousel-item v-for="b in banners" :key="b.src">
            <div class="banner-item">
              <img :src="b.src" class="banner-img" :alt="b.title" />
              <div class="banner-mask">
                <div class="banner-title">{{ b.title }}</div>
                <div class="banner-desc">{{ b.desc }}</div>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>

        <div v-if="loading" class="category-loading">加载中…</div>
        <div v-else class="category-grid">
          <router-link
            v-for="cat in visibleCategories"
            :key="cat.id"
            :to="`/category/${cat.slug}`"
            class="category-card"
          >
            <div class="card-name">{{ cat.name }}</div>
            <div class="card-desc">{{ cat.description || '暂无描述' }}</div>
            <span class="card-link">进入分类 →</span>
          </router-link>
        </div>

    <div class="home-footer">知识分享平台 · 仅供学习交流使用</div>
      </main>

      <aside class="right-col">
        <div class="side-card hot-doc-card">
          <div class="side-title">热门文档</div>
          <div v-if="!overview" class="side-empty">加载中…</div>
          <div v-else class="hot-list">
            <router-link
              v-for="(a, i) in overview?.hotArticles || []"
              :key="a.id"
              :to="`/article/${a.slug}`"
              class="hot-item"
            >
              <span class="hot-rank" :class="`rank-${i + 1}`">{{ i + 1 }}</span>
              <span class="hot-title">{{ a.title }}</span>
              <span class="hot-views">{{ formatNum(a.viewCount) }}</span>
            </router-link>
          </div>
        </div>

        <div class="side-card side-fixed-card">
          <div class="side-title">热门标签</div>
          <div class="tag-cloud">
            <el-tag
              v-for="(t, i) in overview?.hotTags || []"
              :key="t.name"
              size="small"
              effect="plain"
              class="hot-tag"
              :style="tagStyle(i)"
            >
              {{ t.name }}
            </el-tag>
          </div>
        </div>

        <div class="side-card side-fixed-card">
          <div class="side-title">站点统计</div>
          <div class="stat-grid">
            <div class="stat-cell">
              <div class="stat-num">{{ formatNum(overview?.articleCount || 0) }}</div>
              <div class="stat-label">文章数</div>
            </div>
            <div class="stat-cell">
              <div class="stat-num">{{ formatNum(overview?.viewCount || 0) }}</div>
              <div class="stat-label">访问量</div>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import CategorySidebar from '@/components/layout/CategorySidebar.vue'
import { useCategoryStore } from '@/stores/category'
import { useAuthStore } from '@/stores/auth'
import { getHomeOverviewApi, getHomeQuoteApi, type HomeOverview } from '@/api/home'

const categoryStore = useCategoryStore()
const auth = useAuthStore()
const loading = ref(true)
const overview = ref<HomeOverview | null>(null)
const categories = computed(() => categoryStore.tree)
const visibleCategories = computed(() => categories.value.slice(0, 8))

const quote = ref({ text: '每一天都是新的开始，加油！', author: '每日一句' })

const banners = [
  { src: '/images/java-banner.png', title: 'Java 面试精选', desc: '集合、并发、JVM 高频考点' },
  { src: '/images/ai-banner.png', title: 'AI 知识库', desc: 'RAG / Prompt / MCP 持续更新' },
  { src: '/images/interview-banner.png', title: '面试真题', desc: '真实面试经验与解题思路' },
  { src: '/images/doc-banner.png', title: '文档链接合集', desc: '好用的官方文档与技术资源' }
]

const tagColors = [
  '#d9a716', '#4d9f6e', '#c05b8a', '#3f7fc1', '#b0771f',
  '#dfaa40', '#1f8f8f', '#8a5fc0', '#c2572f', '#2f8f5b',
  '#a06a1f', '#4d7fc1', '#8f6a2f', '#b8456e', '#5f7f2f'
]

function tagStyle(i: number) {
  const c = tagColors[i % tagColors.length]
  return { color: c, backgroundColor: `${c}18`, borderColor: `${c}55` }
}

function formatNum(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  return String(n)
}

onMounted(async () => {
  await categoryStore.fetchTree()
  loading.value = false
  try {
    overview.value = await getHomeOverviewApi()
  } catch {
    overview.value = { articleCount: 0, viewCount: 0, hotArticles: [], hotTags: [] }
  }
  try {
    const q = await getHomeQuoteApi()
    if (q.content) {
      quote.value = { text: q.content, author: q.author || '每日一句' }
    }
  } catch {
    // 保持默认语录
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
  padding: 16px var(--layout-pad-x) 16px var(--sidebar-offset);
  display: flex;
  align-items: flex-start;
  gap: 18px;
}

.left-col,
.right-col {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.left-col {
  width: 250px;
  position: sticky;
  top: 72px;
  max-height: calc(100vh - 92px);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.left-col :deep(.sidebar) {
  position: static;
  max-height: none;
  flex: 1;
  min-height: 0;
}

.left-col .daily-quote {
  flex-shrink: 0;
}

.right-col {
  width: 280px;
  position: sticky;
  top: 72px;
  height: calc(100vh - 92px);
  max-height: calc(100vh - 92px);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 92px);
}

/* 侧边卡片通用 */
.side-card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 14px 16px;
  box-shadow: 0 8px 30px rgba(217, 167, 22, 0.08);
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.hot-doc-card {
  flex: 1 1 auto;
  min-height: 0;
}

.side-fixed-card {
  flex: 0 0 auto;
}

.side-title {
  font-size: 14px;
  font-weight: 600;
  color: #e9b862;
  padding-left: 8px;
  border-left: 3px solid var(--app-accent);
  margin-bottom: 10px;
}

.side-empty {
  color: var(--app-text-secondary);
  font-size: 13px;
  padding: 6px 0;
}

.side-card .side-title {
  flex-shrink: 0;
}

/* 每日一句 */
.quote-text {
  margin: 0 0 6px;
  font-size: 13px;
  line-height: 1.7;
  color: #b8c0cf;
}

.quote-author {
  margin: 0;
  text-align: right;
  font-size: 12px;
  color: var(--app-text-secondary);
}

/* 轮播图 */
.banner-carousel {
  border-radius: 14px;
  overflow: hidden;
  margin-bottom: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35);
}

.banner-item {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 14px;
  overflow: hidden;
  background: linear-gradient(120deg, #0c111c 0%, #090d16 55%, #05080f 100%);
}

.banner-item::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.82) 0%, rgba(0, 0, 0, 0.38) 55%, rgba(0, 0, 0, 0.5) 100%);
  pointer-events: none;
}

.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: brightness(0.52) saturate(0.82) contrast(1.02);
}

.banner-mask {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 1;
  width: 64%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 36px;
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.86) 0%, rgba(0, 0, 0, 0.42) 72%, rgba(0, 0, 0, 0) 100%);
}

.banner-title {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 1px;
  text-shadow: 0 2px 14px rgba(0, 0, 0, 0.6);
}

.banner-title::before {
  content: '';
  display: block;
  width: 34px;
  height: 3px;
  border-radius: 2px;
  background: linear-gradient(90deg, #f2a82e, #e89a1f);
  margin-bottom: 14px;
  box-shadow: 0 0 10px rgba(232, 154, 31, 0.5);
}

.banner-desc {
  margin-top: 10px;
  font-size: 14px;
  color: #c6cfdc;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.55);
}

.banner-carousel :deep(.el-carousel__button) {
  width: 18px;
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.45);
  transition: width 0.2s ease, background 0.2s ease;
}

.banner-carousel :deep(.is-active .el-carousel__button) {
  width: 30px;
  background: var(--app-accent);
}

/* 分类卡片 */
.category-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.category-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 12px;
  padding: 18px 20px;
  transition: all 0.15s;
}

.category-card:hover {
  border-color: var(--app-accent);
  background: var(--app-card-hover);
  transform: translateY(-2px);
  box-shadow: 0 10px 26px rgba(217, 167, 22, 0.14);
}

.card-name {
  font-size: 17px;
  font-weight: 600;
  color: #e8ecf3;
}

.card-desc {
  color: var(--app-text-secondary);
  font-size: 13px;
  line-height: 1.7;
  flex: 1;
}

.card-link {
  color: var(--app-accent);
  font-size: 13px;
  font-weight: 600;
}

.category-loading {
  padding: 30px;
  text-align: center;
  color: var(--app-text-secondary);
}

.home-footer {
  margin-top: auto;
  text-align: center;
  padding: 18px 0 4px;
  color: var(--app-text-secondary);
  font-size: 13px;
}

/* 热门文档 */
.hot-list {
  overflow-y: auto;
  min-height: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 4px;
  border-radius: 6px;
  font-size: 13px;
  color: #b8c0cf;
  transition: all 0.15s;
  /* 10 条自动均分撑满卡片高度，避免窗口高时底部留白；空间不足时不压缩，改为列表滚动 */
  flex: 1 0 auto;
}

.hot-item:hover {
  background: var(--app-card-hover);
  color: var(--app-accent);
}

.hot-rank {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: var(--app-accent-soft);
  color: var(--app-accent);
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hot-rank.rank-1 {
  background: #d9a716;
  color: #141a26;
}

.hot-rank.rank-2 {
  background: #e3b54e;
  color: #141a26;
}

.hot-rank.rank-3 {
  background: #e8c96a;
  color: #141a26;
}

.hot-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-views {
  flex-shrink: 0;
  color: var(--app-text-secondary);
  font-size: 12px;
}

/* 热门标签 */
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-content: flex-start;
  overflow-y: auto;
  min-height: 0;
}

.hot-tag {
  cursor: default;
}

/* 站点统计 */
.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.stat-cell {
  background: var(--app-accent-soft);
  border-radius: 10px;
  padding: 14px 10px;
  text-align: center;
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: #e9b862;
}

.stat-label {
  margin-top: 4px;
  font-size: 12px;
  color: var(--app-text-secondary);
}
</style>
