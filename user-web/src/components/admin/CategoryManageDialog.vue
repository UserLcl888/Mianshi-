<template>
  <el-dialog
    :model-value="modelValue"
    title="分类管理"
    width="680px"
    top="8vh"
    append-to-body
    @update:model-value="$emit('update:modelValue', $event)"
    @open="onOpen"
  >
    <div class="category-manage">
      <div v-if="!tree.length" class="empty-tip">暂无分类，点击下方“＋ 新主题”创建</div>

      <template v-for="cat in tree" :key="cat.id">
        <!-- 一级主题行 -->
        <div class="cat-row">
          <template v-if="isEditing(cat)">
            <el-input v-model="editing.name" size="small" placeholder="分类名称" class="row-input" />
            <el-input v-model="editing.slug" size="small" placeholder="slug" class="row-input" />
            <el-button size="small" type="primary" :loading="saving" @click="save">保存</el-button>
            <el-button size="small" @click="cancel">取消</el-button>
          </template>
          <template v-else>
            <span class="cat-name">{{ cat.name }}</span>
            <span class="cat-slug">{{ cat.slug }}</span>
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
          <el-button size="small" type="primary" :loading="saving" @click="save">保存</el-button>
          <el-button size="small" @click="cancel">取消</el-button>
        </div>

        <!-- 子集行 -->
        <div v-for="sub in cat.children" :key="sub.id" class="cat-row cat-row-child">
          <template v-if="isEditing(sub)">
            <el-input v-model="editing.name" size="small" placeholder="分类名称" class="row-input" />
            <el-input v-model="editing.slug" size="small" placeholder="slug" class="row-input" />
            <el-button size="small" type="primary" :loading="saving" @click="save">保存</el-button>
            <el-button size="small" @click="cancel">取消</el-button>
          </template>
          <template v-else>
            <span class="cat-name">{{ sub.name }}</span>
            <span class="cat-slug">{{ sub.slug }}</span>
            <span class="cat-actions">
              <el-button size="small" text @click="startEdit(sub)">编辑</el-button>
              <el-button size="small" text type="danger" @click="remove(sub)">删除</el-button>
            </span>
          </template>
        </div>
      </template>

      <!-- 底部：新主题 -->
      <div class="cat-row cat-row-add">
        <el-button v-if="!isAddingChild(0)" text type="primary" @click="startAddTop">＋ 新主题</el-button>
      </div>
      <div v-if="isAddingChild(0)" class="cat-row cat-form">
        <el-input v-model="editing.name" size="small" placeholder="主题名称" class="row-input" />
        <el-input v-model="editing.slug" size="small" placeholder="slug" class="row-input" />
        <el-button size="small" type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button size="small" @click="cancel">取消</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createCategoryApi, updateCategoryApi, deleteCategoryApi } from '@/api/category'
import { useCategoryStore } from '@/stores/category'
import type { CategoryNode } from '@/types'

defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const categoryStore = useCategoryStore()
const tree = computed(() => categoryStore.tree)
const saving = ref(false)

interface EditingState {
  mode: 'add' | 'edit'
  parentId: number
  target: CategoryNode | null
  name: string
  slug: string
}

const editing = ref<EditingState | null>(null)

function onOpen() {
  categoryStore.fetchTree(true)
}

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
  editing.value = { mode: 'add', parentId: parent.id, target: null, name: '', slug: autoSlug() }
}

function startAddTop() {
  editing.value = { mode: 'add', parentId: 0, target: null, name: '', slug: autoSlug() }
}

function startEdit(cat: CategoryNode) {
  editing.value = { mode: 'edit', parentId: cat.parentId, target: cat, name: cat.name, slug: cat.slug }
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
        description: t.description || ''
      })
    } else {
      await createCategoryApi({
        name,
        slug,
        parentId: editing.value.parentId,
        sortOrder: 0,
        description: ''
      })
    }
    ElMessage.success('保存成功')
    editing.value = null
    await categoryStore.fetchTree(true)
  } catch {
    // 错误提示由请求拦截器统一处理
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
</script>

<style scoped>
.category-manage {
  max-height: 60vh;
  overflow-y: auto;
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
  padding: 8px 10px;
  border-bottom: 1px dashed var(--app-border);
}

.cat-row-child {
  padding-left: 36px;
}

.cat-name {
  font-weight: 600;
  color: var(--app-text);
  min-width: 110px;
}

.cat-slug {
  color: var(--app-text-secondary);
  font-size: 12px;
  flex: 1;
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
}

.row-input {
  width: 180px;
}

.cat-row-add {
  border-bottom: none;
  justify-content: center;
  padding-top: 12px;
}
</style>
