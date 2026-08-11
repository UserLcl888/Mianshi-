<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2 class="auth-title">注册</h2>
      <el-tabs v-model="registerType" class="auth-tabs">
        <el-tab-pane label="邮箱注册" name="email" />
        <el-tab-pane label="手机号注册" name="phone" />
      </el-tabs>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item :label="registerType === 'email' ? '邮箱' : '手机号'" prop="account">
          <el-input
            v-model="form.account"
            :placeholder="registerType === 'email' ? '请输入邮箱' : '请输入 11 位手机号'"
          />
        </el-form-item>
        <el-form-item label="昵称（可选）" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6~32 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入密码" />
        </el-form-item>
        <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">注册</el-button>
      </el-form>
      <div class="auth-switch">
        已有账号？<router-link to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const registerType = ref<'email' | 'phone'>('email')
const form = reactive({ account: '', nickname: '', password: '', confirmPassword: '' })

watch(registerType, () => {
  form.account = ''
  formRef.value?.clearValidate()
})

const rules = computed<FormRules>(() => ({
  account: [
    { required: true, message: registerType.value === 'email' ? '请输入邮箱' : '请输入手机号', trigger: 'blur' },
    registerType.value === 'email'
      ? { pattern: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/, message: '邮箱格式不正确', trigger: 'blur' }
      : { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6~32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}))

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const base = { password: form.password, nickname: form.nickname.trim() || undefined }
    const payload = registerType.value === 'email'
      ? { ...base, email: form.account.trim() }
      : { ...base, phone: form.account.trim() }
    await auth.register(payload)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-bg);
}

.auth-card {
  width: 380px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 8px 30px rgba(217, 167, 22, 0.12);
}

.auth-title {
  text-align: center;
  color: #6b5208;
  margin: 0 0 16px;
}

.auth-tabs {
  margin-bottom: 14px;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
}

.auth-switch {
  margin-top: 16px;
  text-align: center;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.auth-switch a {
  color: var(--app-accent);
  font-weight: 600;
}
</style>
