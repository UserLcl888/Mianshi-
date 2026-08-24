<template>
  <AuthLayout title="一起加油，一起进步" subtitle="Create your secure learning account">
    <div class="auth-tabs">
      <button
        type="button"
        class="auth-tab"
        :class="{ active: registerType === 'email' }"
        @click="switchType('email')"
      >
        邮箱注册
      </button>
      <button
        type="button"
        class="auth-tab"
        :class="{ active: registerType === 'phone' }"
        @click="switchType('phone')"
      >
        手机号注册
      </button>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
      <el-form-item prop="account" class="auth-field">
        <template #label>
          <span class="field-label">{{ registerType === 'email' ? '账户邮箱' : '手机号' }} <i class="req">*</i></span>
        </template>
        <el-input
          v-model="form.account"
          :placeholder="registerType === 'email' ? '请输入您的注册邮箱' : '请输入您的手机号'"
          :prefix-icon="registerType === 'email' ? Message : Iphone"
          class="auth-input"
        />
      </el-form-item>

      <el-form-item prop="nickname" class="auth-field">
        <template #label>
          <span class="field-label">昵称（可选，默认登录账号）</span>
        </template>
        <el-input
          v-model="form.nickname"
          placeholder="不填则使用登录账号"
          :prefix-icon="User"
          class="auth-input"
        />
      </el-form-item>

      <el-form-item prop="password" class="auth-field">
        <template #label>
          <span class="field-label">通行密码 <i class="req">*</i></span>
        </template>
        <el-input
          v-model="form.password"
          type="password"
          show-password
          placeholder="请输入通行密码"
          :prefix-icon="Lock"
          class="auth-input"
        />
      </el-form-item>

      <el-form-item prop="confirmPassword" class="auth-field">
        <template #label>
          <span class="field-label">确认密码 <i class="req">*</i></span>
        </template>
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          placeholder="请再次输入密码"
          :prefix-icon="Lock"
          class="auth-input"
        />
      </el-form-item>

      <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
        立即注册
        <span class="btn-arrow">→</span>
      </el-button>
    </el-form>

    <div class="auth-switch">
      <span>已有账号？</span>
      <router-link to="/login">立即登录 →</router-link>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Iphone, Lock, Message, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from '@/components/auth/AuthLayout.vue'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const registerType = ref<'email' | 'phone'>('email')
const form = reactive({ account: '', nickname: '', password: '', confirmPassword: '' })

function switchType(type: 'email' | 'phone') {
  if (registerType.value === type) return
  registerType.value = type
  form.account = ''
  formRef.value?.clearValidate()
}

const rules = computed<FormRules>(() => ({
  account: [
    {
      required: true,
      message: registerType.value === 'email' ? '请输入邮箱' : '请输入手机号',
      trigger: 'blur'
    },
    registerType.value === 'email'
      ? {
          pattern: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/,
          message: '邮箱格式不正确',
          trigger: 'blur'
        }
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
    const payload =
      registerType.value === 'email'
        ? { ...base, email: form.account.trim() }
        : { ...base, phone: form.account.trim() }
    await auth.register(payload)
    ElMessage.success('注册成功，请登录')
    router.push({ path: '/login', query: { type: registerType.value, account: form.account.trim() } })
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-tabs {
  display: flex;
  border-bottom: 1px solid rgba(255, 255, 255, 0.09);
  margin-bottom: 10px;
}

.auth-tab {
  appearance: none;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0 4px 12px;
  margin-right: 28px;
  font-size: 15px;
  color: #5a6b82;
  position: relative;
  transition: color 0.2s ease;
}

.auth-tab::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  border-radius: 2px;
  background: #f5a623;
  transform: scaleX(0);
  transition: transform 0.2s ease;
}

.auth-tab.active {
  color: #f5a623;
  font-weight: 600;
}

.auth-tab.active::after {
  transform: scaleX(1);
}

.auth-field {
  margin-bottom: 16px;
}

.auth-field :deep(.el-form-item__label) {
  display: block;
  width: 100%;
  text-align: left;
  padding: 0;
  line-height: 1.5;
}

.auth-field :deep(.el-form-item__label)::before {
  content: none !important;
}

.field-label {
  font-size: 13px;
  color: #b8c2d0;
}

.field-label .req {
  color: #ff6b6b;
  font-style: normal;
  margin-left: 1px;
}

.submit-btn {
  width: 100%;
  height: 50px;
  margin-top: 8px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #ffb338 0%, #f7931e 100%);
  box-shadow: 0 6px 20px rgba(245, 166, 35, 0.32);
  transition: box-shadow 0.2s ease, transform 0.15s ease, filter 0.2s ease;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #ffc14f 0%, #ffa32e 100%);
  box-shadow: 0 8px 26px rgba(245, 166, 35, 0.42);
  transform: translateY(-1px);
}

.btn-arrow {
  margin-left: 6px;
  font-weight: 400;
}

.auth-switch {
  margin-top: 8px;
  text-align: center;
  font-size: 13px;
  color: #8a9bb5;
}

.auth-switch a {
  color: #f5a623;
  font-weight: 600;
  transition: color 0.2s;
}

.auth-switch a:hover {
  color: #ffb338;
}

.auth-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 5px 12px;
}

.auth-input :deep(.el-input__prefix) {
  font-size: 16px;
  margin-right: 4px;
}
</style>
