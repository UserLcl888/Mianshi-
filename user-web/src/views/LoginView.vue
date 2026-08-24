<template>
  <AuthLayout>
    <div class="auth-tabs">
      <button
        type="button"
        class="auth-tab"
        :class="{ active: loginType === 'email' }"
        @click="switchType('email')"
      >
        邮箱登录
      </button>
      <button
        type="button"
        class="auth-tab"
        :class="{ active: loginType === 'phone' }"
        @click="switchType('phone')"
      >
        手机号登录
      </button>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
      <el-form-item prop="account" class="auth-field">
        <template #label>
          <span class="field-label">{{ loginType === 'email' ? '账户邮箱' : '手机号' }} <i class="req">*</i></span>
        </template>
        <el-input
          v-model="form.account"
          :placeholder="loginType === 'email' ? '请输入您的注册邮箱' : '请输入您的手机号'"
          :prefix-icon="loginType === 'email' ? Message : Iphone"
          class="auth-input"
        />
      </el-form-item>

      <template v-if="loginType === 'email' && emailMode === 'code'">
        <el-form-item prop="code" class="auth-field">
          <template #label>
            <span class="field-label">验证码 <i class="req">*</i></span>
          </template>
          <div class="code-row">
            <el-input
              v-model="form.code"
              placeholder="6 位验证码"
              class="auth-input code-input"
              maxlength="6"
              :prefix-icon="Key"
            />
            <VerifyCodeButton :email="form.account" scene="login" :sendable="emailValid" />
          </div>
        </el-form-item>
      </template>
      <template v-else>
        <el-form-item prop="password" class="auth-field">
          <template #label>
            <div class="field-label-row">
              <span class="field-label">通行密码 <i class="req">*</i></span>
              <router-link to="/forgot-password" class="forgot-link">忘记密码？</router-link>
            </div>
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
      </template>

      <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
        立即登录平台
        <span class="btn-arrow">→</span>
      </el-button>
    </el-form>

    <template v-if="loginType === 'email'">
      <div class="divider">
        <span class="divider-text">
          其他登录验证方式
          <em class="divider-en">Other login verification methods</em>
        </span>
      </div>

      <button
        type="button"
        class="code-login-btn"
        :class="{ active: emailMode === 'code' }"
        @click="toggleEmailMode"
      >
        <el-icon :size="15">
          <Lock v-if="emailMode === 'code'" />
          <svg v-else viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="7" y="2.5" width="10" height="19" rx="2.5" />
            <path d="M10.5 18.5h3" />
            <path d="M12 6v2.5" />
          </svg>
        </el-icon>
        <span class="code-login-cn">{{ emailMode === 'code' ? '密码登录' : '验证码登录' }}</span>
      </button>
    </template>

    <div class="auth-switch">
      <span>还没有账号？</span>
      <router-link to="/register">立即申请入驻 →</router-link>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Iphone, Key, Lock, Message } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import VerifyCodeButton from '@/components/common/VerifyCodeButton.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const loginType = ref<'email' | 'phone'>('email')
const emailMode = ref<'password' | 'code'>('password')
const form = reactive({ account: '', password: '', code: '' })

// 注册成功跳转回来时，按注册类型选中对应 Tab 并回填账号
const queryType = String(route.query.type || '')
if (queryType === 'phone') {
  loginType.value = 'phone'
}
const queryAccount = String(route.query.account || '')
if (queryAccount) {
  form.account = queryAccount
}

const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const phonePattern = /^1[3-9]\d{9}$/
const emailValid = computed(() => emailPattern.test(form.account.trim()))

function switchType(type: 'email' | 'phone') {
  if (loginType.value === type) return
  loginType.value = type
  form.account = ''
  form.password = ''
  form.code = ''
  emailMode.value = 'password'
  formRef.value?.clearValidate()
}

function toggleEmailMode() {
  emailMode.value = emailMode.value === 'code' ? 'password' : 'code'
  form.password = ''
  form.code = ''
  formRef.value?.clearValidate()
}

const rules = computed<FormRules>(() => {
  const accountRule =
    loginType.value === 'phone'
      ? [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: phonePattern, message: '手机号格式不正确', trigger: 'blur' }
        ]
      : [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { pattern: emailPattern, message: '邮箱格式不正确', trigger: 'blur' }
        ]
  if (loginType.value === 'email' && emailMode.value === 'code') {
    return {
      account: accountRule,
      code: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { pattern: /^\d{6}$/, message: '请输入 6 位数字验证码', trigger: 'blur' }
      ]
    }
  }
  return {
    account: accountRule,
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
  }
})

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    if (loginType.value === 'email' && emailMode.value === 'code') {
      await auth.loginByCode(form.account.trim(), form.code.trim())
    } else {
      await auth.login(form.account.trim(), form.password)
    }
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
  background: var(--app-accent);
  transform: scaleX(0);
  transition: transform 0.2s ease;
}

.auth-tab.active {
  color: var(--app-accent);
  font-weight: 600;
}

.auth-tab.active::after {
  transform: scaleX(1);
}

.auth-field {
  margin-bottom: 18px;
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

.field-label-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.forgot-link {
  font-size: 12.5px;
  color: #8a9bb5;
  transition: color 0.2s;
}

.forgot-link:hover {
  color: var(--app-accent);
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-input {
  flex: 1;
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
  background: linear-gradient(135deg, #f2a82e 0%, #e18b18 100%);
  box-shadow: 0 6px 20px rgba(232, 154, 31, 0.32);
  transition: box-shadow 0.2s ease, transform 0.15s ease, filter 0.2s ease;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #f7b845 0%, #ea9624 100%);
  box-shadow: 0 8px 26px rgba(232, 154, 31, 0.42);
  transform: translateY(-1px);
}

.submit-btn.is-loading {
  background: linear-gradient(135deg, #e39a26 0%, #d17f13 100%);
}

.btn-arrow {
  margin-left: 6px;
  font-weight: 400;
}

.divider {
  display: flex;
  align-items: center;
  margin: 10px 0 8px;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(255, 255, 255, 0.13);
}

.divider-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 14px;
  font-size: 13px;
  color: #9aa6b8;
  white-space: nowrap;
}

.divider-en {
  margin-top: 2px;
  font-size: 11px;
  font-style: normal;
  color: #68788f;
}

.code-login-btn {
  appearance: none;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 11px;
  cursor: pointer;
  font-size: 13.5px;
  font-weight: 500;
  color: #c8d2e0;
  background: rgba(255, 255, 255, 0.055);
  border: 1px solid rgba(255, 255, 255, 0.12);
  transition: all 0.2s ease;
}

.code-login-btn:hover {
  background: rgba(255, 255, 255, 0.09);
  border-color: rgba(232, 154, 31, 0.5);
  color: var(--app-accent);
}

.code-login-btn.active {
  background: rgba(232, 154, 31, 0.14);
  border-color: rgba(232, 154, 31, 0.55);
  color: var(--app-accent);
}

.code-login-cn {
  font-size: 13.5px;
  font-weight: 500;
}

.auth-switch {
  margin-top: 8px;
  text-align: center;
  font-size: 13px;
  color: #8a9bb5;
}

.auth-switch a {
  color: var(--app-accent);
  font-weight: 600;
  transition: color 0.2s;
}

.auth-switch a:hover {
  color: #f2a82e;
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
