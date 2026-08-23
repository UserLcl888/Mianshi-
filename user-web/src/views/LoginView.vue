<template>
  <AuthLayout>
    <el-tabs v-model="loginType" class="auth-tabs">
      <el-tab-pane label="邮箱登录" name="email" />
      <el-tab-pane label="手机号登录" name="phone" />
    </el-tabs>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
      <div v-if="loginType === 'email'" class="mode-switch">
        <el-radio-group v-model="emailMode" size="small">
          <el-radio-button value="password">密码登录</el-radio-button>
          <el-radio-button value="code">验证码登录</el-radio-button>
        </el-radio-group>
      </div>

      <el-form-item :label="loginType === 'email' ? '邮箱' : '手机号'" prop="account">
        <el-input v-model="form.account" :placeholder="loginType === 'email' ? '请输入邮箱' : '请输入手机号'" />
      </el-form-item>

      <template v-if="loginType === 'email' && emailMode === 'code'">
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="6 位验证码" class="code-input" maxlength="6" />
            <VerifyCodeButton :email="form.account" scene="login" :sendable="emailValid" />
          </div>
        </el-form-item>
      </template>
      <template v-else>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
      </template>

      <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">登录</el-button>
    </el-form>
    <div class="auth-links">
      <router-link to="/forgot-password" class="forgot-link">忘记密码？</router-link>
      <span class="auth-switch">
        还没有账号？<router-link to="/register">去注册</router-link>
      </span>
    </div>
    <div class="admin-hint">
      测试账号：2090323327@qq.com / 123456（管理员）· 2090323328@qq.com / 123456（普通用户）
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
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

watch(loginType, () => {
  form.account = ''
  form.password = ''
  form.code = ''
  emailMode.value = 'password'
  formRef.value?.clearValidate()
})

watch(emailMode, () => {
  form.password = ''
  form.code = ''
  formRef.value?.clearValidate()
})

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
  margin-bottom: 4px;
}

.mode-switch {
  text-align: center;
  margin-bottom: 14px;
}

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

.auth-links {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.forgot-link {
  color: var(--app-text-secondary);
}

.forgot-link:hover {
  color: var(--app-accent);
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
