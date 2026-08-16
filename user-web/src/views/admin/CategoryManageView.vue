<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="section-title">分类管理</h3>
      <el-button type="primary" size="small" @click="startAddTop">＋ 新主题</el-button>
    </div>

    <div v-if="!tree.length" class="empty-tip">暂无分类，点击“＋ 新主题”创建</div>

    <template v-for="cat in tree" :key="cat.id">
      <!-- 一级主题行 -->
      <div class="cat-row">
        <template v-if="isEditing(cat)">
          <el-input v-model="editing.name" size="small" placeholder="分类名称" class="row-input" />
          <el-input v-model="editing.slug" size="small" placeholder="slug" class="row-input" />
          <span class="pri-label">优先级</span>
          <el-input-number v-model="editing.priority" size="small" :min="0" controls-position="right" :value-on-clear="0" class="pri-input" />
          <el-button size="small" type="primary" :loading="saving" @click="save">保存</el-button>
          <el-button size="small" @click="cancel">取消</el-button>
          <el-input v-model="editing.description" size="small" placeholder="描述（选填，会显示在首页分类卡片）" class="desc-input" />
        </template>
        <template v-else>
          <span class="cat-name">{{ cat.name }}</span>
          <span class="cat-slug">
            <span class="slug-text">
              {{ cat.slug }}<span v-if="cat.priority" class="pri-badge">P{{ cat.priority }}</span>
            </span>
            <span v-if="cat.description" class="cat-desc">{{ cat.description }}</span>
          </span>
          <span class="cat-actions">
            <el-button size="small" text type="primary" @click="startAddChild(cat)">＋ 子集</el-button>
            <el-button size="small" text @click="startEdit(cat)">编辑</el-button>
            <el-button size="small" text type="danger" @click="remove(cat)">删除</el-button>
          </span>
        </template>
      </div>

      <!-- 新增子集输入行 -->
      <div v-if="isAddingChild(cat.id)" class="cat-row cat-form">
        <el-input v-model="editing.name" size="small" placeholder="子集名称" class="row-input" />
        <el-input v-model="editing.slug" size="small" placeholder="slug" class="row-input" />
        <span class="pri-label">优先级</span>
        <el-input-number v-model="editing.priority" size="small" :min="0" controls-position="right" :value-on-clear="0" class="pri-input" />
        <el-button size="small" type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button size="small" @click="cancel">取消</el-button>
        <el-input v-model="editing.description" size="small" placeholder="描述（选填）" class="desc-input" />
      </div>

      <!-- 子集行 -->
      <div v-for="sub in cat.children" :key="sub.id" class="cat-row cat-row-child">
        <template v-if="isEditing(sub)">
            <el-input v-model="editing.name" size="small" placeholder="分类名称" class="row-input" />
            <el-input v-model="editing.slug" size="small" placeholder="slug" class="row-input" />
            <span class="pri-label">优先级</span>
            <el-input-number v-model="editing.priority" size="small" :min="0" controls-position="right" :value-on-clear="0" class="pri-input" />
            <el-button size="small" type="primary" :loading="saving" @click="save">保存</el-button>
            <el-button size="small" @click="cancel">取消</el-button>
            <el-input v-model="editing.description" size="small" placeholder="描述（选填）" class="desc-input" />
          </template>
          <template v-else>
            <span class="cat-name">{{ sub.name }}</span>
            <span class="cat-slug">
              <span class="slug-text">
                {{ sub.slug }}<span v-if="sub.priority" class="pri-badge">P{{ sub.priority }}</span>
              </span>
              <span v-if="sub.description" class="cat-desc">{{ sub.description }}</span>
            </span>
          <span class="cat-actions">
            <el-button size="small" text @click="startEdit(sub)">编辑</el-button>
            <el-button size="small" text type="danger" @click="remove(sub)">删除</el-button>
          </span>
        </template>
      </div>
    </template>

    <!-- 底部新增主题输入行 -->
    <div v-if="isAddingChild(0)" class="cat-row cat-form">
      <el-input v-model="editing.name" size="small" placeholder="主题名称" class="row-input" />
      <el-input v-model="editing.slug" size="small" placeholder="slug" class="row-input" />
      <span class="pri-label">优先级</span>
      <el-input-number v-model="editing.priority" size="small" :min="0" controls-position="right" :value-on-clear="0" class="pri-input" />
      <el-button size="small" type="primary" :loading="saving" @click="save">保存</el-button>
      <el-button size="small" @click="cancel">取消</el-button>
      <el-input v-model="editing.description" size="small" placeholder="描述（选填，会显示在首页分类卡片）" class="desc-input" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createCategoryApi, updateCategoryApi, deleteCategoryApi } from '@/api/category'
import { useCategoryStore } from '@/stores/category'
import type { CategoryNode } from '@/types'

const categoryStore = useCategoryStore()
const tree = computed(() => categoryStore.tree)
const saving = ref(false)

interface EditingState {
  mode: 'add' | 'edit'
  parentId: number
  target: CategoryNode | null
  name: string
  slug: string
  description: string
  priority: number
}

const editing = ref<EditingState | null>(null)

function isEditing(cat: CategoryNode): boolean {
  return editing.value?.mode === 'edit' && editing.value.target?.id === cat.id
}

function isAddingChild(parentId: number): boolean {
  return editing.value?.mode === 'add' && editing.value.parentId === parentId
}

function autoSlug(): string {
  return `cat-${Date.now()}`
}

function startAddChild(parent: CategoryNode) {
  editing.value = { mode: 'add', parentId: parent.id, target: null, name: '', slug: autoSlug(), description: '', priority: 0 }
}

function startAddTop() {
  editing.value = { mode: 'add', parentId: 0, target: null, name: '', slug: autoSlug(), description: '', priority: 0 }
}

function startEdit(cat: CategoryNode) {
  editing.value = {
    mode: 'edit',
    parentId: cat.parentId,
    target: cat,
    name: cat.name,
    slug: cat.slug,
    description: cat.description || '',
    priority: cat.priority ?? 0
  }
}

function cancel() {
  editing.value = null
}

async function save() {
  if (!editing.value) return
  const name = editing.value.name.trim()
  const slug = editing.value.slug.trim()
  if (!name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  if (!slug) {
    ElMessage.warning('请输入 slug')
    return
  }
  saving.value = true
  try {
    if (editing.value.mode === 'edit' && editing.value.target) {
      const t = editing.value.target
      await updateCategoryApi(t.id, {
        name,
        slug,
        parentId: t.parentId,
        sortOrder: t.sortOrder,
        priority: Number(editing.value.priority) || 0,
        description: editing.value.description.trim()
      })
    } else {
      await createCategoryApi({
        name,
        slug,
        parentId: editing.value.parentId,
        sortOrder: 0,
        priority: Number(editing.value.priority) || 0,
        description: editing.value.description.trim()
      })
    }
    ElMessage.success('保存成功')
    editing.value = null
    await categoryStore.fetchTree(true)
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function remove(cat: CategoryNode) {
  try {
    await ElMessageBox.confirm(`确定删除分类“${cat.name}”吗？其下有子分类或题目时无法删除。`, '删除分类', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteCategoryApi(cat.id)
    ElMessage.success('删除成功')
    await categoryStore.fetchTree(true)
  } catch {
    // 拦截器已提示
  }
}

onMounted(() => {
  categoryStore.fetchTree(true)
})
</script>

<style scoped>
.page-card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 18px 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-title {
  margin: 0;
  color: #6b5208;
}

.empty-tip {
  text-align: center;
  color: var(--app-text-secondary);
  padding: 20px 0;
}

.cat-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-bottom: 1px dashed var(--app-border);
}

.cat-row-child {
  padding-left: 40px;
}

.cat-name {
  font-weight: 600;
  color: var(--app-text);
  min-width: 120px;
}

.cat-slug {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.slug-text {
  color: var(--app-text-secondary);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat-desc {
  font-size: 12px;
  color: var(--app-text-secondary);
  opacity: 0.75;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat-actions {
  display: flex;
  gap: 2px;
  white-space: nowrap;
}

.cat-form {
  background: var(--app-accent-soft);
  border-radius: 8px;
  flex-wrap: wrap;
}

.row-input {
  width: 200px;
}

.pri-label {
  font-size: 12px;
  color: var(--app-text-secondary);
  white-space: nowrap;
}

.pri-input {
  width: 130px;
}

.pri-badge {
  margin-left: 6px;
  font-size: 11px;
  color: #a87f18;
  background: var(--app-accent-soft);
  border-radius: 4px;
  padding: 1px 5px;
}

.desc-input {
  width: 100%;
  flex-basis: 100%;
}
</style>
