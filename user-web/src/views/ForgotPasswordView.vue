<template>
  <AuthLayout title="找回密码" subtitle="Reset your secure learning password">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
      <el-form-item prop="email" class="auth-field">
        <template #label>
          <span class="field-label">账户邮箱 <i class="req">*</i></span>
        </template>
        <el-input
          v-model="form.email"
          placeholder="请输入您的注册邮箱"
          :prefix-icon="Message"
          class="auth-input"
        />
      </el-form-item>

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
          <VerifyCodeButton :email="form.email" scene="reset" :sendable="emailValid" />
        </div>
      </el-form-item>

      <el-form-item prop="newPassword" class="auth-field">
        <template #label>
          <span class="field-label">新密码 <i class="req">*</i></span>
        </template>
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          placeholder="请输入新密码"
          :prefix-icon="Lock"
          class="auth-input"
        />
      </el-form-item>

      <el-form-item prop="confirmPassword" class="auth-field">
        <template #label>
          <span class="field-label">确认新密码 <i class="req">*</i></span>
        </template>
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          placeholder="请再次输入新密码"
          :prefix-icon="Lock"
          class="auth-input"
        />
      </el-form-item>

      <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
        重置密码
        <span class="btn-arrow">→</span>
      </el-button>
    </el-form>

    <div class="auth-switch">
      <span>想起密码了？</span>
      <router-link to="/login">去登录 →</router-link>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Key, Lock, Message } from '@element-plus/icons-vue'
import { resetPasswordByCodeApi } from '@/api/auth'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import VerifyCodeButton from '@/components/common/VerifyCodeButton.vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const form = reactive({ email: '', code: '', newPassword: '', confirmPassword: '' })
const emailValid = computed(() => emailPattern.test(form.email.trim()))

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: emailPattern, message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入 6 位数字验证码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6~32 位', trigger: 'blur' }
  ],
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

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await resetPasswordByCodeApi(form.email.trim(), form.code.trim(), form.newPassword)
    ElMessage.success('密码已重置，请重新登录')
    router.push('/login')
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
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
