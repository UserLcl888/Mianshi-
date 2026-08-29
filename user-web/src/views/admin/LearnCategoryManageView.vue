<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="section-title">学习专题分类</h3>
      <el-button type="primary" size="small" @click="openCreate">新增分类</el-button>
    </div>

    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="分类名" min-width="160" />
      <el-table-column prop="slug" label="标识" min-width="200" />
      <el-table-column prop="articleCount" label="文章数" width="100" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="isEdit ? '编辑分类' : '新增分类'" width="420px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="分类名" required>
          <el-input v-model="form.name" placeholder="如：Java、AI、MySQL、前端" />
        </el-form-item>
        <el-form-item label="标识 slug">
          <el-input v-model="form.slug" placeholder="选填，留空自动生成" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdminLearnCategoryApi,
  deleteAdminLearnCategoryApi,
  getAdminLearnCategoriesApi,
  updateAdminLearnCategoryApi
} from '@/api/admin'
import type { LearnCategory } from '@/types'

const list = ref<LearnCategory[]>([])
const visible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const editId = ref(0)
const form = reactive({ name: '', slug: '', sortOrder: 0 })

async function load() {
  try {
    list.value = await getAdminLearnCategoriesApi()
  } catch {
    // 拦截器已提示
  }
}

function openCreate() {
  isEdit.value = false
  editId.value = 0
  form.name = ''
  form.slug = ''
  form.sortOrder = 0
  visible.value = true
}

function openEdit(row: LearnCategory) {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.slug = row.slug
  form.sortOrder = 0
  visible.value = true
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入分类名')
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateAdminLearnCategoryApi(editId.value, {
        name: form.name.trim(),
        slug: form.slug.trim() || undefined
      })
      ElMessage.success('保存成功')
    } else {
      await createAdminLearnCategoryApi({
        name: form.name.trim(),
        slug: form.slug.trim() || undefined,
        sortOrder: form.sortOrder
      })
      ElMessage.success('创建成功')
    }
    visible.value = false
    load()
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function remove(row: LearnCategory) {
  try {
    await ElMessageBox.confirm(`确定删除分类“${row.name}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteAdminLearnCategoryApi(row.id)
    ElMessage.success('删除成功')
    load()
  } catch {
    // 拦截器已提示
  }
}

onMounted(load)
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
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  color: #f0c674;
}
</style>
