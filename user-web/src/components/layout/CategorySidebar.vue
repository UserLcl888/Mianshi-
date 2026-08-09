<template>
  <aside class="sidebar">
    <!-- 已进入某个分类：左侧显示分类名 + 子分类分组 / 题目列表 -->
    <template v-if="activeCategoryName">
      <div class="sidebar-title category-active">
        <span>{{ activeCategoryName }}</span>
        <el-tag v-if="totalCount" size="small" type="warning" effect="plain">{{ totalCount }}</el-tag>
      </div>
      <div v-if="articleLoading" class="question-loading">加载中…</div>

      <!-- 有子分类：按子分类分组显示 -->
      <template v-else-if="subCategories.length">
        <div v-for="g in grouped" :key="g.sub.id" class="sub-group">
          <div class="sub-header">
            <router-link
              :to="`/category/${g.sub.slug}`"
              class="sub-title"
              :class="{ active: activeCategorySlug === g.sub.slug }"
            >
              <span>{{ g.sub.name }}</span>
              <el-tag v-if="g.items.length" size="small" type="warning" effect="plain">{{ g.items.length }}</el-tag>
            </router-link>
            <button
              class="collapse-btn"
              :class="{ expanded: isExpanded(g.sub.id) }"
              :title="isExpanded(g.sub.id) ? '收起' : '展开'"
              @click="toggleGroup(g.sub.id)"
            >
              <el-icon><ArrowDown /></el-icon>
            </button>
          </div>
          <nav v-if="isExpanded(g.sub.id)" class="question-list sub-list">
            <router-link
              v-for="a in g.items"
              :key="a.id"
              :to="`/article/${a.slug}`"
              class="question-item"
              :class="{ active: a.slug === activeArticleSlug }"
            >
              {{ a.title }}
            </router-link>
          </nav>
        </div>
      </template>

      <!-- 无子分类：直接平铺题目 -->
      <nav v-else class="question-list">
        <router-link
          v-for="a in articles"
          :key="a.id"
          :to="`/article/${a.slug}`"
          class="question-item"
          :class="{ active: a.slug === activeArticleSlug }"
        >
          {{ a.title }}
        </router-link>
        <div v-if="!articles.length" class="question-empty">暂无题目</div>
      </nav>
    </template>

    <!-- 未进入具体分类（兜底）：显示分类导航 -->
    <template v-else>
      <div class="sidebar-title">分类导航</div>
      <nav class="category-list">
        <router-link
          v-for="cat in categories"
          :key="cat.id"
          :to="`/category/${cat.slug}`"
          class="category-item"
          :class="{ active: cat.slug === activeCategorySlug }"
        >
          <span class="category-name">{{ cat.name }}</span>
          <el-tag v-if="countByCategory(cat)" size="small" type="warning" effect="plain">
            {{ countByCategory(cat) }}
          </el-tag>
        </router-link>
      </nav>
    </template>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { getArticles } from '@/api/article'
import { useCategoryStore } from '@/stores/category'
import type { ArticleListItem, CategoryNode } from '@/types'

const props = defineProps<{
  activeCategorySlug?: string
  activeArticleSlug?: string
}>()

const categoryStore = useCategoryStore()
const categories = computed(() => categoryStore.tree)
const articles = ref<ArticleListItem[]>([])
const articleLoading = ref(false)
const categoryCounts = ref<Record<number, number>>({})
const expandedGroups = ref<Set<number>>(new Set())

const activeCategory = computed(() => findCategory(props.activeCategorySlug || '', categories.value))
const activeCategoryName = computed(() => activeCategory.value?.name || '')
const subCategories = computed(() => activeCategory.value?.children || [])

const totalCount = computed(() => {
  if (!activeCategory.value) return 0
  return categoryCounts.value[activeCategory.value.id] ?? 0
})

const grouped = computed(() => {
  const map = new Map<number, ArticleListItem[]>()
  for (const a of articles.value) {
    const arr = map.get(a.categoryId) || []
    arr.push(a)
    map.set(a.categoryId, arr)
  }
  return subCategories.value.map((sub) => ({
    sub,
    items: map.get(sub.id) || []
  }))
})

function isExpanded(id: number): boolean {
  return expandedGroups.value.has(id)
}

function toggleGroup(id: number) {
  const next = new Set(expandedGroups.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expandedGroups.value = next
}

function findCategory(slug: string, nodes: CategoryNode[]): CategoryNode | null {
  for (const n of nodes) {
    if (n.slug === slug) return n
    const found = findCategory(slug, n.children)
    if (found) return found
  }
  return null
}

function countByCategory(cat: CategoryNode): number {
  return categoryCounts.value[cat.id] ?? 0
}

async function loadArticles(slug: string) {
  articleLoading.value = true
  try {
    const cat = findCategory(slug, categories.value)
    if (!cat) return
    const res = await getArticles({ categoryId: cat.id, page: 1, size: 100 })
    articles.value = res.list
    categoryCounts.value = { ...categoryCounts.value, [cat.id]: res.total }
  } finally {
    articleLoading.value = false
  }
}

watch(
  () => props.activeCategorySlug,
  (slug) => {
    if (slug) loadArticles(slug)
  },
  { immediate: true }
)

// 自动展开当前题目所在的子分类
watch(
  [articles, () => props.activeArticleSlug],
  ([list, slug]) => {
    if (!slug) return
    const a = (list as ArticleListItem[]).find((x) => x.slug === slug)
    if (a && !expandedGroups.value.has(a.categoryId)) {
      expandedGroups.value = new Set([...expandedGroups.value, a.categoryId])
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.sidebar {
  width: 250px;
  flex-shrink: 0;
  background: var(--app-sidebar-bg);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 12px;
  max-height: calc(100vh - 92px);
  overflow-y: auto;
  position: sticky;
  top: 72px;
}

.sidebar-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-secondary);
  padding: 4px 8px 10px;
  border-bottom: 1px solid var(--app-border);
  margin-bottom: 8px;
}

.category-active {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  color: #6b5208;
}

.category-list,
.question-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.category-item,
.question-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  color: #6b5b2f;
  font-size: 14px;
  transition: all 0.15s;
}

.category-item:hover,
.question-item:hover {
  background: var(--app-card-hover);
}

.category-item.active {
  background: var(--app-accent-soft);
  color: #a87f18;
  font-weight: 600;
}

.sub-group {
  margin-bottom: 10px;
}

.sub-header {
  display: flex;
  align-items: center;
  gap: 2px;
  background: var(--app-accent-soft);
  border-radius: 6px;
  margin-bottom: 4px;
  padding-right: 4px;
}

.sub-title {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #7a5c10;
  transition: all 0.15s;
}

.sub-title:hover {
  background: var(--app-card-hover);
}

.sub-title.active {
  background: var(--app-accent);
  color: #fffdf6;
}

.collapse-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #7a5c10;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border-radius: 4px;
}

.collapse-btn:hover {
  background: var(--app-card-hover);
}

.collapse-btn .el-icon {
  transition: transform 0.2s;
}

.collapse-btn.expanded .el-icon {
  transform: rotate(180deg);
}

.sub-list {
  padding-left: 8px;
}

.question-item {
  font-size: 13px;
  line-height: 1.5;
  padding: 7px 10px;
}

.question-item.active {
  background: var(--app-accent);
  color: #fffdf6;
  font-weight: 600;
}

.question-loading,
.question-empty {
  padding: 12px 10px;
  color: var(--app-text-secondary);
  font-size: 13px;
}
</style>
