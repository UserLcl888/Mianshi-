<template>
  <div class="page">
    <AppHeader />
    <div class="page-body">
      <main class="content">
        <el-breadcrumb class="breadcrumb-bar" separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>个人中心</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="app-card profile-card">
          <h3 class="section-title">个人信息</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="昵称">{{ auth.userInfo?.nickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ auth.userInfo?.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ auth.userInfo?.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ auth.userInfo?.createdAt }}</el-descriptions-item>
          </el-descriptions>
          <div class="actions">
            <el-button type="primary" plain @click="openNickDialog">修改昵称</el-button>
            <router-link to="/profile/applies">
              <el-button type="primary" plain>我的申请</el-button>
            </router-link>
            <router-link to="/profile/password">
              <el-button type="primary" plain>修改密码</el-button>
            </router-link>
          </div>
        </div>

        <el-dialog v-model="nickDialogVisible" title="修改昵称" width="420px" append-to-body>
          <el-form ref="nickFormRef" :model="nickForm" :rules="nickRules" label-position="top">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="nickForm.nickname" placeholder="请输入昵称" maxlength="50" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="nickDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="nickSaving" @click="saveNickname">保存</el-button>
          </template>
        </el-dialog>
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { useAuthStore } from '@/stores/auth'
import { updateNicknameApi } from '@/api/auth'

const auth = useAuthStore()
const nickDialogVisible = ref(false)
const nickSaving = ref(false)
const nickFormRef = ref<FormInstance>()
const nickForm = reactive({ nickname: '' })
const nickRules: FormRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

onMounted(() => {
  if (!auth.userInfo) auth.fetchProfile()
})

function openNickDialog() {
  nickForm.nickname = auth.userInfo?.nickname || ''
  nickDialogVisible.value = true
}

async function saveNickname() {
  const valid = await nickFormRef.value?.validate().catch(() => false)
  if (!valid) return
  nickSaving.value = true
  try {
    await updateNicknameApi(nickForm.nickname.trim())
    await auth.fetchProfile()
    ElMessage.success('昵称修改成功')
    nickDialogVisible.value = false
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    nickSaving.value = false
  }
}
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
  padding: 16px 24px;
  display: flex;
  justify-content: center;
}

.content {
  width: 100%;
  max-width: 720px;
}

.section-title {
  margin: 0 0 16px;
  color: #e9b862;
}

.profile-card {
  padding: 28px 32px;
  box-shadow: 0 8px 30px rgba(217, 167, 22, 0.1);
}

.actions {
  margin-top: 18px;
  display: flex;
  gap: 10px;
}
</style>
