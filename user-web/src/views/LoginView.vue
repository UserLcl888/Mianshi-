<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2 class="auth-title">登录</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
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
        测试账号：admin / 123456（管理员）· demo / 123456（普通用户）
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.login(form.username, form.password)
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
  margin: 0 0 24px;
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
