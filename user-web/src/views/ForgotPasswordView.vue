<template>
  <AuthLayout>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
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
        <el-input v-model="form.newPassword" type="password" show-password placeholder="6~32 位" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
      </el-form-item>
      <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">重置密码</el-button>
    </el-form>
    <div class="auth-switch">
      想起密码了？<router-link to="/login">去登录</router-link>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
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
.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.code-input {
  flex: 1;
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
