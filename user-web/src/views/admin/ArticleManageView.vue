<template>
  <div class="page-card">
    <h3 class="section-title">题目管理</h3>

    <div class="filters">
      <el-select v-model="query.columnType" placeholder="全部专栏" clearable style="width: 160px" @change="reload">
        <el-option label="技术问题专栏" value="tech" />
        <el-option label="文章专栏" value="topic" />
        <el-option label="学习专题" value="learn" />
      </el-select>
      <el-select v-if="query.columnType !== 'topic'" v-model="query.categoryId" placeholder="全部分类" clearable style="width: 200px">
        <template v-for="cat in categories" :key="cat.id">
          <el-option :label="cat.name" :value="cat.id">
            <span :class="{ 'cat-parent-label': cat.children.length }">{{ cat.name }}</span>
          </el-option>
          <el-option v-for="sub in cat.children" :key="sub.id" :label="sub.name" :value="sub.id">
            <span class="cat-child-label">{{ sub.name }}</span>
          </el-option>
        </template>
      </el-select>
      <el-select v-model="query.difficulty" placeholder="全部难度" clearable style="width: 140px">
        <el-option label="简单" value="EASY" />
        <el-option label="中等" value="MEDIUM" />
        <el-option label="困难" value="HARD" />
      </el-select>
      <el-button type="primary" plain @click="reload">查询</el-button>
    </div>

    <!-- 文章专栏：支持拖拽排序 + 置顶（仅管理员可见此操作） -->
    <div v-if="query.columnType === 'topic'" class="topic-sort-list">
      <div
        v-for="(row, i) in list"
        :key="row.id"
        class="topic-sort-item"
        :class="{ dragging: dragIndex === i, 'drag-over': dragOverIndex === i }"
        draggable="true"
        @dragstart="onDragStart(i)"
        @dragover.prevent="onDragOver(i)"
        @dragleave="onDragLeave(i)"
        @drop.prevent="onDrop"
        @dragend="onDragEnd"
      >
        <span class="drag-handle" title="拖拽调整顺序">⠿</span>
        <span class="sort-idx">{{ i + 1 }}</span>
        <div class="sort-main">
          <div class="sort-title">
            {{ row.title }}
            <span v-if="row.isPinned === 1" class="pinned-badge">置顶</span>
          </div>
          <div class="sort-meta">{{ formatDateTime(row.updatedAt) }}</div>
        </div>
        <el-switch
          :model-value="row.isPinned === 1"
          inline-prompt
          active-text="置顶"
          inactive-text="普通"
          @change="(v: boolean | string | number) => togglePinned(row, v === true || v === 1 || v === '1')"
        />
        <div class="sort-ops">
          <el-button size="small" text type="primary" @click="$router.push(`/admin/articles/${row.slug}/edit`)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </div>
      </div>
    </div>

    <el-table v-else :data="list" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
      <el-table-column label="专栏" width="130">
        <template #default="{ row }">
          <el-tag :type="columnTagType(row.columnType)" size="small">
            {{ columnTagText(row.columnType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分类" width="140">
        <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
      </el-table-column>
      <el-table-column label="难度" width="90">
        <template #default="{ row }">
          <template v-if="row.columnType !== 'tech'">
            <span class="no-difficulty">-</span>
          </template>
          <el-tag v-else :type="difficultyType(row.difficulty)" size="small">{{ difficultyText(row.difficulty) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标签" min-width="140">
        <template #default="{ row }">
          <el-tag v-for="t in row.tags" :key="t" size="small" effect="plain" type="warning" class="tag">{{ t }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="90" />
      <el-table-column label="更新时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="$router.push(`/admin/articles/${row.slug}/edit`)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="onPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCategoryStore } from '@/stores/category'
import { getAdminArticlesApi, deleteArticleApi } from '@/api/admin'
import { reorderTopicArticlesApi } from '@/api/article'
import type { ArticleListItem } from '@/types'
import { formatDateTime } from '@/utils/format'

const categoryStore = useCategoryStore()
const categories = computed(() => categoryStore.tree)
const list = ref<ArticleListItem[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({
  columnType: '',
  categoryId: undefined as number | undefined,
  difficulty: '',
  page: 1,
  size: 10
})
const dragIndex = ref<number | null>(null)
const dragOverIndex = ref<number | null>(null)

function flatten(nodes: typeof categoryStore.tree): Map<number, string> {
  const map = new Map<number, string>()
  for (const n of nodes) {
    map.set(n.id, n.name)
    flatten(n.children).forEach((v, k) => map.set(k, v))
  }
  return map
}

const nameMap = computed(() => flatten(categories.value))

function categoryName(id: number): string {
  return nameMap.value.get(id) || `#${id}`
}

function difficultyText(d: string): string {
  return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[d] || d
}

function difficultyType(d: string): 'success' | 'warning' | 'danger' {
  return { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }[d] || 'info'
}

function columnTagText(columnType?: string): string {
  if (columnType === 'topic') return '文章'
  if (columnType === 'learn') return '学习'
  return '技术问题'
}

function columnTagType(columnType?: string): 'success' | 'warning' | 'info' {
  if (columnType === 'topic') return 'success'
  if (columnType === 'learn') return 'warning'
  return 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await getAdminArticlesApi({
      ...query,
      // 文章专栏模式下一次加载全部，保证拖拽排序在整栏范围内生效
      size: query.columnType === 'topic' ? 100 : query.size
    })
    list.value = res.list
    total.value = res.total
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function onDragStart(i: number) {
  dragIndex.value = i
}

function onDragOver(i: number) {
  dragOverIndex.value = i
}

function onDragLeave(i: number) {
  if (dragOverIndex.value === i) {
    dragOverIndex.value = null
  }
}

function onDragEnd() {
  dragIndex.value = null
  dragOverIndex.value = null
}

function onDrop() {
  const from = dragIndex.value
  const to = dragOverIndex.value
  onDragEnd()
  if (from === null || to === null || from === to) return
  const arr = [...list.value]
  const [moved] = arr.splice(from, 1)
  arr.splice(to, 0, moved)
  list.value = arr
  persistOrder()
}

async function persistOrder() {
  const items = list.value.map((a, i) => ({
    id: a.id,
    sortOrder: i + 1,
    isPinned: a.isPinned === 1 ? 1 : 0
  }))
  try {
    await reorderTopicArticlesApi(items)
    ElMessage.success('排序已保存')
  } catch {
    load()
  }
}

async function togglePinned(row: ArticleListItem, pinned: boolean) {
  row.isPinned = pinned ? 1 : 0
  await persistOrder()
}

function reload() {
  query.page = 1
  load()
}

function onPage(p: number) {
  query.page = p
  load()
}

async function remove(row: ArticleListItem) {
  try {
    await ElMessageBox.confirm(`确定删除题目“${row.title}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteArticleApi(row.id)
    ElMessage.success('删除成功')
    load()
  } catch {
    // 拦截器已提示
  }
}

onMounted(async () => {
  await categoryStore.fetchTree()
  load()
})
</script>

<style scoped>
.page-card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 18px 20px;
}

.section-title {
  margin: 0 0 16px;
  color: #f0c674;
}

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.topic-sort-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.topic-sort-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid var(--app-border);
  transition: border-color 0.15s, background 0.15s, opacity 0.15s;
}

.topic-sort-item.dragging {
  opacity: 0.45;
}

.topic-sort-item.drag-over {
  border-color: var(--app-accent);
  background: rgba(232, 154, 31, 0.08);
}

.drag-handle {
  cursor: grab;
  font-size: 18px;
  color: var(--app-text-secondary);
  user-select: none;
}

.drag-handle:active {
  cursor: grabbing;
}

.sort-idx {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: var(--app-accent-soft);
  color: var(--app-accent);
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.sort-main {
  flex: 1;
  min-width: 0;
}

.sort-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pinned-badge {
  margin-left: 8px;
  padding: 1px 7px;
  border-radius: 5px;
  border: 1px solid rgba(255, 92, 92, 0.7);
  color: #ff7d7d;
  font-size: 11px;
  font-weight: 400;
}

.no-difficulty {
  color: var(--app-text-secondary);
}

.sort-meta {
  margin-top: 3px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.sort-ops {
  flex-shrink: 0;
  display: flex;
  gap: 4px;
}

.tag {
  margin-right: 4px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.cat-parent-label {
  font-weight: 600;
}

.cat-child-label {
  padding-left: 18px;
}
</style>
