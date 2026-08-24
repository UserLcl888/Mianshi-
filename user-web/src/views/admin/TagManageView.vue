<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="section-title">标签管理</h3>
      <el-button type="primary" size="small" @click="openDialog()">新增标签</el-button>
    </div>

    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="标签名" min-width="200" />
      <el-table-column label="创建时间" width="190">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openDialog(row)">重命名</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="!list.length" class="empty-tip">暂无标签</div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '重命名标签' : '新增标签'" width="420px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="70px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getAdminTagsApi,
  createAdminTagApi,
  updateAdminTagApi,
  deleteAdminTagApi,
  type AdminTag
} from '@/api/admin'
import { formatDateTime } from '@/utils/format'

const list = ref<AdminTag[]>([])
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ id: 0, name: '' })
const rules: FormRules = {
  name: [{ required: true, message: '请输入标签名', trigger: 'blur' }]
}

async function load() {
  try {
    list.value = await getAdminTagsApi()
  } catch {
    // 拦截器已提示
  }
}

function openDialog(row?: AdminTag) {
  form.id = row?.id || 0
  form.name = row?.name || ''
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await updateAdminTagApi(form.id, form.name.trim())
    } else {
      await createAdminTagApi(form.name.trim())
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } catch {
  } finally {
    saving.value = false
  }
}

async function remove(row: AdminTag) {
  try {
    await ElMessageBox.confirm(`确定删除标签“${row.name}”吗？关联的题目会解除该标签。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteAdminTagApi(row.id)
    ElMessage.success('删除成功')
    load()
  } catch {
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

.empty-tip {
  text-align: center;
  color: var(--app-text-secondary);
  padding: 20px 0;
}
</style>
