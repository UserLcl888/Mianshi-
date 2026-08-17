<template>
  <div class="page-card">
    <h3 class="section-title">题目管理</h3>

    <div class="filters">
      <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 200px">
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

    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
      <el-table-column label="分类" width="140">
        <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
      </el-table-column>
      <el-table-column label="难度" width="90">
        <template #default="{ row }">
          <el-tag :type="difficultyType(row.difficulty)" size="small">{{ difficultyText(row.difficulty) }}</el-tag>
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
import type { ArticleListItem } from '@/types'
import { formatDateTime } from '@/utils/format'

const categoryStore = useCategoryStore()
const categories = computed(() => categoryStore.tree)
const list = ref<ArticleListItem[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ categoryId: undefined as number | undefined, difficulty: '', page: 1, size: 10 })

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

async function load() {
  loading.value = true
  try {
    const res = await getAdminArticlesApi(query)
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
  color: #6b5208;
}

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
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
