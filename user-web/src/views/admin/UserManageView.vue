<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="section-title">用户管理</h3>
      <el-button type="primary" size="small" @click="openCreate">新增用户</el-button>
    </div>

    <div class="filters">
      <el-input v-model="query.keyword" placeholder="用户名 / 昵称" clearable style="width: 200px" @keyup.enter="reload" />
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" plain @click="reload">查询</el-button>
    </div>

    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="nickname" label="昵称" width="140" />
      <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机号" width="130">
        <template #default="{ row }">{{ row.phone || '-' }}</template>
      </el-table-column>
      <el-table-column label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">{{ row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" text type="primary" @click="openReset(row)">重置密码</el-button>
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

    <!-- 新增用户 -->
    <el-dialog v-model="createVisible" title="新增用户" width="440px" append-to-body>
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="createForm.email" placeholder="选填，如 xx@qq.com" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="createForm.phone" placeholder="选填，邮箱和手机号至少填一项" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password placeholder="6~32 位" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="createForm.nickname" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createForm.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户 -->
    <el-dialog v-model="editVisible" title="编辑用户" width="440px" append-to-body>
      <el-form ref="editRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="440px" append-to-body>
      <el-form ref="resetRef" :model="resetForm" :rules="resetRules" label-width="80px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="6~32 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getAdminUsersApi,
  createAdminUserApi,
  updateAdminUserApi,
  updateUserStatusApi,
  resetUserPasswordApi,
  deleteAdminUserApi
} from '@/api/admin'
import type { UserInfo } from '@/types'

const list = ref<UserInfo[]>([])
const total = ref(0)
const saving = ref(false)
const query = reactive({ keyword: '', status: undefined as number | undefined, page: 1, size: 10 })

const createVisible = ref(false)
const editVisible = ref(false)
const resetVisible = ref(false)
const createRef = ref<FormInstance>()
const editRef = ref<FormInstance>()
const resetRef = ref<FormInstance>()
const createForm = reactive({ email: '', phone: '', password: '', nickname: '', role: 'USER' })
const editForm = reactive({ id: 0, nickname: '', email: '', role: 'USER' })
const resetForm = reactive({ id: 0, newPassword: '' })

const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const phonePattern = /^1[3-9]\d{9}$/

const createRules: FormRules = {
  email: [
    {
      validator: (_r, v, cb) => {
        const val = String(v || '').trim()
        if (val && !emailPattern.test(val)) cb(new Error('邮箱格式不正确'))
        else cb()
      },
      trigger: 'blur'
    }
  ],
  phone: [
    {
      validator: (_r, v, cb) => {
        const val = String(v || '').trim()
        if (val && !phonePattern.test(val)) cb(new Error('手机号格式不正确'))
        else cb()
      },
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6~32 位', trigger: 'blur' }
  ]
}

const editRules: FormRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const resetRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6~32 位', trigger: 'blur' }
  ]
}

async function load() {
  try {
    const res = await getAdminUsersApi(query)
    list.value = res.list
    total.value = res.total
  } catch {
    // 拦截器已提示
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

function openCreate() {
  createForm.email = ''
  createForm.phone = ''
  createForm.password = ''
  createForm.nickname = ''
  createForm.role = 'USER'
  createVisible.value = true
}

async function submitCreate() {
  const valid = await createRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!createForm.email.trim() && !createForm.phone.trim()) {
    ElMessage.warning('请填写邮箱或手机号')
    return
  }
  saving.value = true
  try {
    await createAdminUserApi({
      email: createForm.email.trim() || undefined,
      phone: createForm.phone.trim() || undefined,
      password: createForm.password,
      nickname: createForm.nickname.trim() || undefined,
      role: createForm.role
    })
    ElMessage.success('创建成功')
    createVisible.value = false
    reload()
  } catch {
  } finally {
    saving.value = false
  }
}

function openEdit(row: UserInfo) {
  editForm.id = row.id
  editForm.nickname = row.nickname
  editForm.email = row.email
  editForm.role = row.role
  editVisible.value = true
}

async function submitEdit() {
  const valid = await editRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await updateAdminUserApi(editForm.id, {
      nickname: editForm.nickname.trim(),
      email: editForm.email.trim(),
      role: editForm.role
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } catch {
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: UserInfo) {
  const next = row.status === 1 ? 0 : 1
  const label = row.nickname || row.email
  try {
    await ElMessageBox.confirm(
      next === 0 ? `确定禁用用户“${label}”吗？该用户会被立即踢下线。` : `确定启用用户“${label}”吗？`,
      next === 0 ? '禁用确认' : '启用确认',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await updateUserStatusApi(row.id, next)
    ElMessage.success(next === 0 ? '已禁用' : '已启用')
    load()
  } catch {
  }
}

function openReset(row: UserInfo) {
  resetForm.id = row.id
  resetForm.newPassword = ''
  resetVisible.value = true
}

async function submitReset() {
  const valid = await resetRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await resetUserPasswordApi(resetForm.id, resetForm.newPassword)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } catch {
  } finally {
    saving.value = false
  }
}

async function remove(row: UserInfo) {
  try {
    await ElMessageBox.confirm(`确定删除用户“${row.nickname || row.email}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteAdminUserApi(row.id)
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
  color: #6b5208;
}

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
