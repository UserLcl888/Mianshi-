<template>
  <div class="page">
    <AppHeader />
    <div class="page-body">
      <CategorySidebar :active-category-slug="activeSlug" :active-article-slug="''" />
      <main class="content">
        <el-breadcrumb class="breadcrumb-bar" separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <template v-for="(c, idx) in categoryPath" :key="c.id">
            <el-breadcrumb-item v-if="idx < categoryPath.length - 1" :to="`/category/${c.slug}`">{{ c.name }}</el-breadcrumb-item>
            <el-breadcrumb-item v-else>{{ c.name }}</el-breadcrumb-item>
          </template>
        </el-breadcrumb>

        <div class="toolbar">
          <span class="category-desc">{{ currentCategory?.description }}</span>
          <el-select v-model="difficulty" placeholder="全部难度" clearable style="width: 140px">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </div>

        <div ref="listContainer" class="list-container">
          <template v-if="list.length">
            <ArticleCard v-for="a in list" :key="a.id" :article="a" />
          </template>
          <EmptyState v-else-if="!loading" text="该分类下暂无题目" />

          <div ref="sentinel" class="load-more">
            <span v-if="loading">加载中…</span>
            <span v-else-if="!hasMore && list.length">已经到底啦</span>
          </div>
        </div>
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import CategorySidebar from '@/components/layout/CategorySidebar.vue'
import ArticleCard from '@/components/article/ArticleCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getArticles } from '@/api/article'
import { useCategoryStore } from '@/stores/category'
import { getCategoryPath } from '@/utils/category'
import type { ArticleListItem } from '@/types'

const route = useRoute()
const categoryStore = useCategoryStore()

const activeSlug = computed(() => String(route.params.slug || ''))
const categoryPath = computed(() => getCategoryPath(activeSlug.value, categoryStore.tree) || [])
const currentCategory = computed(() => categoryPath.value[categoryPath.value.length - 1] || null)

const list = ref<ArticleListItem[]>([])
const page = ref(1)
const size = 10
const hasMore = ref(true)
const loading = ref(false)
const difficulty = ref('')

const listContainer = ref<HTMLElement | null>(null)
const sentinel = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

async function loadMore() {
  if (loading.value || !hasMore.value || !currentCategory.value) return
  loading.value = true
  try {
    const res = await getArticles({
      categoryId: currentCategory.value.id,
      difficulty: difficulty.value || undefined,
      page: page.value,
      size
    })
    list.value.push(...res.list)
    hasMore.value = res.hasMore
    page.value += 1
  } finally {
    loading.value = false
  }
}

async function reload() {
  list.value = []
  page.value = 1
  hasMore.value = true
  await loadMore()
}

watch(
  () => [activeSlug.value, difficulty.value],
  () => reload()
)

onMounted(async () => {
  await categoryStore.fetchTree()
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting) loadMore()
    },
    { root: listContainer.value, rootMargin: '120px' }
  )
  if (sentinel.value) observer.observe(sentinel.value)
  await reload()
})

onBeforeUnmount(() => {
  observer?.disconnect()
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
  max-width: 1160px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.category-desc {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  max-height: calc(100vh - 210px);
  padding-right: 4px;
}

.load-more {
  text-align: center;
  color: var(--app-text-secondary);
  font-size: 13px;
  padding: 14px 0 4px;
}
</style>
