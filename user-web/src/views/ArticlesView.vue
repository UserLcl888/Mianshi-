<template>
  <div class="topic-page">
    <AppHeader />
    <div class="topic-body">
      <main class="topic-main">
        <header class="topic-head">
          <h1 class="topic-title">文章分享</h1>
          <div class="topic-search">
            <el-input
              v-model="query.keyword"
              placeholder="搜索文章标题"
              clearable
              class="search-input"
              :prefix-icon="Search"
              @keyup.enter="reload"
              @clear="reload"
            />
            <el-button type="primary" class="search-btn" @click="reload">搜索</el-button>
          </div>
        </header>

        <p class="topic-tip">专题，知识合集，智慧之光</p>

        <div v-if="loading" class="topic-empty">加载中…</div>
        <div v-else-if="!list.length" class="topic-empty">暂无专题文章，敬请期待</div>

        <div v-else class="block-list">
          <article
            v-for="a in list"
            :key="a.id"
            class="block-card"
            @click="$router.push(`/articles/${a.slug}`)"
          >
            <div class="block-cover">
              <img v-if="a.coverUrl" :src="a.coverUrl" :alt="a.title" class="cover-img" />
              <div v-else class="cover-placeholder">
                <span class="placeholder-main">知识分享</span>
                <span class="placeholder-sub">文章分享</span>
              </div>
            </div>
            <div class="block-body">
              <div class="block-title-row">
                <h2 class="block-title">{{ a.title }}</h2>
                <span v-if="a.isPinned === 1" class="pinned-tag">置顶</span>
              </div>
              <p class="block-summary">{{ a.summary || '暂无简介' }}</p>
              <div class="block-meta">
                <span class="meta-item">
                  <el-icon :size="13"><CollectionTag /></el-icon>
                  文章分享
                </span>
                <span class="meta-item">
                  <el-icon :size="13"><Clock /></el-icon>
                  {{ formatDateTime(a.updatedAt) }}
                </span>
                <span class="meta-item">
                  <el-icon :size="13"><View /></el-icon>
                  {{ formatNum(a.viewCount) }} 次浏览
                </span>
              </div>
            </div>
          </article>
        </div>

        <div v-if="total > query.size" class="topic-pager">
          <el-pagination
            layout="total, prev, pager, next"
            :total="total"
            :page-size="query.size"
            :current-page="query.page"
            @current-change="onPage"
          />
        </div>
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Clock, CollectionTag, Search, View } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { getTopicArticlesApi } from '@/api/article'
import type { ArticleListItem } from '@/types'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const list = ref<ArticleListItem[]>([])
const total = ref(0)
const query = reactive({ keyword: '', page: 1, size: 8 })

async function load() {
  loading.value = true
  try {
    const res = await getTopicArticlesApi({
      keyword: query.keyword.trim() || undefined,
      page: query.page,
      size: query.size
    })
    list.value = res.list
    total.value = res.total
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
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

function formatNum(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  return String(n)
}

onMounted(load)
</script>

<style scoped>
.topic-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.topic-body {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 26px 20px 36px;
}

.topic-main {
  width: 100%;
  max-width: 900px;
}

.topic-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.topic-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 1px;
}

.topic-search {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 260px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12) inset;
}

.search-btn {
  border-radius: 10px;
}

.topic-tip {
  margin: 0 0 18px;
  font-size: 13px;
  color: #8a9bb5;
}

.topic-empty {
  padding: 70px 20px;
  text-align: center;
  color: #8a9bb5;
  font-size: 14px;
  background: rgba(18, 24, 38, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
}

.block-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.block-card {
  display: flex;
  gap: 14px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(18, 24, 38, 0.62);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.block-card:hover {
  transform: translateY(-2px);
  border-color: rgba(232, 154, 31, 0.5);
  background: rgba(24, 32, 50, 0.78);
}

.block-cover {
  width: 196px;
  flex-shrink: 0;
  aspect-ratio: 16 / 9;
  border-radius: 10px;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  background: linear-gradient(135deg, rgba(232, 154, 31, 0.22) 0%, rgba(24, 32, 50, 0.9) 70%);
  color: #e9b862;
}

.placeholder-main {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
}

.placeholder-sub {
  font-size: 12px;
  letter-spacing: 4px;
  color: #aab2c0;
}

.block-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.block-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.block-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pinned-tag {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 6px;
  border: 1px solid rgba(255, 92, 92, 0.7);
  color: #ff7d7d;
  font-size: 11px;
}

.block-summary {
  margin: 6px 0 10px;
  font-size: 12px;
  line-height: 1.7;
  color: #aab2c0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.block-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 11.5px;
  color: #8a9bb5;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.topic-pager {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

@media (max-width: 760px) {
  .topic-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-input {
    width: 100%;
  }

  .block-card {
    flex-direction: column;
  }

  .block-cover {
    width: 100%;
  }
}
</style>
