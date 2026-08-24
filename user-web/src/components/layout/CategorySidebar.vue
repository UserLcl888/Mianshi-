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
        <!-- 不属于任何子分组、直接属于当前分类的题目，直接平铺显示 -->
        <nav v-if="directArticles.length" class="question-list direct-list">
          <router-link
            v-for="(a, ai) in directArticles"
            :key="a.id"
            :to="`/article/${a.slug}`"
            class="question-item"
            :class="{ active: a.slug === activeArticleSlug }"
            :draggable="isAdmin"
            @dragstart="onDragStart($event, 'article-direct', ai)"
            @dragover="onDragOver($event)"
            @drop="onDrop($event, 'article-direct', ai)"
          >
            {{ a.title }}
          </router-link>
        </nav>
        <div v-for="(g, gi) in grouped" :key="g.sub.id" class="sub-group">
          <div
            class="sub-header"
            :class="{ 'drag-item': isAdmin }"
            :draggable="isAdmin"
            @dragstart="onDragStart($event, 'sub', gi)"
            @dragover="onDragOver($event)"
            @drop="onDrop($event, 'sub', gi)"
          >
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
              v-for="(a, ai) in g.items"
              :key="a.id"
              :to="`/article/${a.slug}`"
              class="question-item"
              :class="{ active: a.slug === activeArticleSlug }"
              :draggable="isAdmin"
              @dragstart="onDragStart($event, `article-${g.sub.id}`, ai)"
              @dragover="onDragOver($event)"
              @drop="onDrop($event, `article-${g.sub.id}`, ai)"
            >
              {{ a.title }}
            </router-link>
          </nav>
        </div>
      </template>

      <!-- 无子分类：直接平铺题目 -->
      <nav v-else class="question-list">
        <router-link
          v-for="(a, i) in articles"
          :key="a.id"
          :to="`/article/${a.slug}`"
          class="question-item"
          :class="{ active: a.slug === activeArticleSlug }"
          :draggable="isAdmin"
          @dragstart="onDragStart($event, 'article', i)"
          @dragover="onDragOver($event)"
          @drop="onDrop($event, 'article', i)"
        >
          {{ a.title }}
        </router-link>
        <div v-if="!articles.length" class="question-empty">暂无题目</div>
      </nav>
    </template>

    <!-- 未进入具体分类（兜底）：显示分类导航 -->
    <template v-else>
      <div class="sidebar-title category-active">
        <span>分类导航</span>
      </div>
      <nav class="category-list">
        <router-link
          v-for="(cat, i) in categories"
          :key="cat.id"
          :to="`/category/${cat.slug}`"
          class="category-item"
          :class="{ active: cat.slug === activeCategorySlug }"
          :draggable="isAdmin"
          @dragstart="onDragStart($event, 'cat', i)"
          @dragover="onDragOver($event)"
          @drop="onDrop($event, 'cat', i)"
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
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { getArticles, reorderArticlesApi } from '@/api/article'
import { reorderCategoriesApi } from '@/api/category'
import { useAuthStore } from '@/stores/auth'
import { useCategoryStore } from '@/stores/category'
import type { ArticleListItem, CategoryNode } from '@/types'

const props = defineProps<{
  activeCategorySlug?: string
  activeArticleSlug?: string
}>()

const categoryStore = useCategoryStore()
const auth = useAuthStore()
const isAdmin = computed(() => auth.userInfo?.role === 'ADMIN')
const categories = computed(() => categoryStore.tree)
const articles = ref<ArticleListItem[]>([])
const articleLoading = ref(false)
const categoryCounts = ref<Record<number, number>>({})
const expandedGroups = ref<Set<number>>(new Set())

/** 自定义拖拽数据类型：避免浏览器对 <a> 链接拖拽时覆盖 text/plain 导致数据丢失。 */
const DRAG_MIME = 'text/x-reorder'

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

/** 直接属于当前分类本身、不在任何子分组下的题目 */
const directArticles = computed(() =>
  articles.value.filter((a) => a.categoryId === activeCategory.value?.id)
)

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

function moveItem<T>(arr: T[], from: number, to: number): T[] {
  const next = [...arr]
  const [moved] = next.splice(from, 1)
  const insertAt = from < to ? to - 1 : to
  next.splice(Math.max(0, insertAt), 0, moved)
  return next
}

function onDragStart(e: DragEvent, key: string, index: number) {
  if (!isAdmin.value) return
  e.dataTransfer?.setData(DRAG_MIME, JSON.stringify({ key, index }))
  if (e.dataTransfer) e.dataTransfer.effectAllowed = 'move'
}

function onDragOver(e: DragEvent) {
  if (!isAdmin.value) return
  e.preventDefault()
  if (e.dataTransfer) e.dataTransfer.dropEffect = 'move'
}

async function onDrop(e: DragEvent, key: string, index: number) {
  if (!isAdmin.value) return
  e.preventDefault()
  let src: { key: string; index: number } | null = null
  try {
    const raw = e.dataTransfer?.getData(DRAG_MIME)
    src = raw ? JSON.parse(raw) : null
  } catch {
    src = null
  }
  if (!src || src.key !== key || src.index === index) return

  try {
    if (key === 'cat') {
      const ordered = moveItem(categories.value, src.index, index)
      categoryStore.tree = ordered
      await reorderCategoriesApi(ordered.map((c, i) => ({ id: c.id, parentId: 0, sortOrder: i })))
      await categoryStore.fetchTree(true)
    } else if (key === 'sub') {
      const parentId = activeCategory.value?.id ?? 0
      const ordered = moveItem(subCategories.value, src.index, index)
      applySubCategoryOrder(ordered)
      await reorderCategoriesApi(ordered.map((c, i) => ({ id: c.id, parentId, sortOrder: i })))
      await categoryStore.fetchTree(true)
    } else if (key === 'article') {
      const ordered = moveItem(articles.value, src.index, index)
      articles.value = ordered
      await reorderArticlesApi(ordered.map((a, i) => ({ id: a.id, sortOrder: i })))
      await loadArticles(activeCategorySlug.value || '')
    } else if (key === 'article-direct') {
      const parentId = activeCategory.value?.id
      if (parentId == null) return
      const items = articles.value.filter((a) => a.categoryId === parentId)
      const ordered = moveItem(items, src.index, index)
      applyGroupArticleOrder((a) => a.categoryId === parentId, ordered)
      await reorderArticlesApi(ordered.map((a, i) => ({ id: a.id, sortOrder: i })))
      await loadArticles(activeCategorySlug.value || '')
    } else if (key.startsWith('article-')) {
      const subId = Number(key.slice('article-'.length))
      const items = articles.value.filter((a) => a.categoryId === subId)
      const ordered = moveItem(items, src.index, index)
      applyGroupArticleOrder((a) => a.categoryId === subId, ordered)
      await reorderArticlesApi(ordered.map((a, i) => ({ id: a.id, sortOrder: i })))
      await loadArticles(activeCategorySlug.value || '')
    } else {
      return
    }
    ElMessage.success('顺序已保存')
  } catch {
    // 保存失败时回滚为服务端真实顺序
    try {
      if (key === 'cat' || key === 'sub') {
        await categoryStore.fetchTree(true)
      } else {
        await loadArticles(activeCategorySlug.value || '')
      }
    } catch {
      // 忽略回滚异常
    }
  }
}

/** 本地立即调整当前分类的子分类顺序（随后 fetchTree 会以服务端为准校正）。 */
function applySubCategoryOrder(ordered: CategoryNode[]) {
  const target = findCategory(props.activeCategorySlug || '', categoryStore.tree)
  if (target) {
    target.children = ordered
  }
}

/** 本地立即调整某个分组内文章顺序，其余文章保持原位。 */
function applyGroupArticleOrder(predicate: (a: ArticleListItem) => boolean, ordered: ArticleListItem[]) {
  const idSet = new Set(ordered.map((a) => a.id))
  const rest = articles.value.filter((a) => !idSet.has(a.id))
  const firstIdx = articles.value.findIndex(predicate)
  const result = [...rest]
  result.splice(Math.max(0, firstIdx), 0, ...ordered)
  articles.value = result
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

.drag-item {
  cursor: grab;
}

.category-active {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  color: #e9b862;
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
  color: #b8c0cf;
  font-size: 14px;
  transition: all 0.15s;
}

.category-item:hover,
.question-item:hover {
  background: var(--app-card-hover);
}

.category-item.active {
  background: var(--app-accent-soft);
  color: var(--app-accent);
  font-weight: 600;
}

.sub-group {
  margin-bottom: 10px;
}

.direct-list {
  margin-bottom: 8px;
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
  color: #dfaa40;
  transition: all 0.15s;
}

.sub-title:hover {
  background: var(--app-card-hover);
}

.sub-title.active {
  background: var(--app-accent);
  color: #141a26;
}

.collapse-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #dfaa40;
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
  color: #141a26;
  font-weight: 600;
}

.question-loading,
.question-empty {
  padding: 12px 10px;
  color: var(--app-text-secondary);
  font-size: 13px;
}

</style>
