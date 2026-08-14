<template>
  <div class="page">
    <AppHeader />
    <div class="page-body">
      <main class="content">
        <el-breadcrumb class="breadcrumb-bar" separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/profile' }">个人中心</el-breadcrumb-item>
          <el-breadcrumb-item>修改密码</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="app-card pwd-card">
          <h3 class="section-title">修改密码</h3>
          <el-tabs v-model="mode" class="pwd-tabs">
            <el-tab-pane label="原密码修改" name="password" />
            <el-tab-pane label="邮箱验证码找回" name="code" />
          </el-tabs>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="pwd-form">
            <template v-if="mode === 'password'">
              <el-form-item label="旧密码" prop="oldPassword">
                <el-input v-model="form.oldPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="form.newPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="form.confirmPassword" type="password" show-password />
              </el-form-item>
            </template>

            <template v-else>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" placeholder="请输入注册邮箱" />
              </el-form-item>
              <el-form-item label="验证码" prop="code">
                <div class="code-row">
                  <el-input v-model="form.code" placeholder="6 位验证码" class="code-input" maxlength="6" />
                  <VerifyCodeButton :email="form.email" scene="reset" :sendable="emailValid" />
                </div>
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="form.newPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="form.confirmPassword" type="password" show-password />
              </el-form-item>
            </template>

            <el-form-item>
              <el-button type="primary" :loading="loading" @click="submit">
                {{ mode === 'password' ? '确认修改' : '重置密码' }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import VerifyCodeButton from '@/components/common/VerifyCodeButton.vue'
import { changePasswordApi, resetPasswordByCodeApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const mode = ref<'password' | 'code'>('password')
const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const form = reactive({
  oldPassword: '',
  email: auth.userInfo?.email || '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})
const emailValid = computed(() => emailPattern.test(form.email.trim()))

watch(mode, () => {
  form.oldPassword = ''
  form.code = ''
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
})

const basePasswordRules = [
  { required: true, message: '请输入新密码', trigger: 'blur' },
  { min: 6, max: 32, message: '密码长度为 6~32 位', trigger: 'blur' }
]

const rules = computed<FormRules>(() => {
  if (mode.value === 'code') {
    return {
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { pattern: emailPattern, message: '邮箱格式不正确', trigger: 'blur' }
      ],
      code: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { pattern: /^\d{6}$/, message: '请输入 6 位数字验证码', trigger: 'blur' }
      ],
      newPassword: basePasswordRules,
      confirmPassword: [
        { required: true, message: '请再次输入新密码', trigger: 'blur' },
        {
          validator: (_rule, value, callback) => {
            if (value !== form.newPassword) callback(new Error('两次输入的密码不一致'))
            else callback()
          },
          trigger: 'blur'
        }
      ]
    }
  }
  return {
    oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
    newPassword: basePasswordRules,
    confirmPassword: [
      { required: true, message: '请再次输入新密码', trigger: 'blur' },
      {
        validator: (_rule, value, callback) => {
          if (value !== form.newPassword) callback(new Error('两次输入的密码不一致'))
          else callback()
        },
        trigger: 'blur'
      }
    ]
  }
})

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    if (mode.value === 'password') {
      await changePasswordApi(form.oldPassword, form.newPassword)
      ElMessage.success('密码修改成功，请重新登录')
    } else {
      await resetPasswordByCodeApi(form.email.trim(), form.code.trim(), form.newPassword)
      ElMessage.success('密码已重置，请重新登录')
    }
    auth.clearAuth()
    router.push('/login')
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
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
  max-width: 520px;
}

.section-title {
  margin: 0 0 10px;
  color: #6b5208;
}

.pwd-card {
  padding: 28px 32px;
  box-shadow: 0 8px 30px rgba(217, 167, 22, 0.1);
}

.pwd-tabs {
  margin-bottom: 12px;
}

.pwd-form {
  width: 100%;
}

.pwd-form :deep(.el-form-item__label) {
  white-space: nowrap;
}

.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.code-input {
  flex: 1;
}

.mode-tip {
  margin: -4px 0 12px;
  font-size: 12px;
  color: var(--app-text-secondary);
}
</style>
