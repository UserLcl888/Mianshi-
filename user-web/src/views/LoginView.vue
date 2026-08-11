<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2 class="auth-title">登录</h2>
      <el-tabs v-model="loginType" class="auth-tabs">
        <el-tab-pane label="邮箱登录" name="email" />
        <el-tab-pane label="手机号登录" name="phone" />
      </el-tabs>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item :label="loginType === 'email' ? '邮箱' : '手机号'" prop="account">
          <el-input v-model="form.account" :placeholder="loginType === 'email' ? '请输入邮箱' : '请输入手机号'" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">登录</el-button>
      </el-form>
      <div class="auth-switch">
        还没有账号？<router-link to="/register">去注册</router-link>
      </div>
      <div class="admin-hint">
        测试账号：2090323327@qq.com / 123456（管理员）· 2090323328@qq.com / 123456（普通用户）
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const loginType = ref<'email' | 'phone'>('email')
const form = reactive({ account: '', password: '' })

watch(loginType, () => {
  form.account = ''
  formRef.value?.clearValidate()
})

const rules = computed<FormRules>(() => ({
  account: [
    { required: true, message: loginType.value === 'email' ? '请输入邮箱' : '请输入手机号', trigger: 'blur' },
    loginType.value === 'email'
      ? { pattern: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/, message: '邮箱格式不正确', trigger: 'blur' }
      : { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}))

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.login(form.account.trim(), form.password)
    ElMessage.success('登录成功')
    router.push(String(route.query.redirect || '/'))
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

.admin-hint {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--app-border);
  text-align: center;
  color: var(--app-text-secondary);
  font-size: 12px;
}
</style>
