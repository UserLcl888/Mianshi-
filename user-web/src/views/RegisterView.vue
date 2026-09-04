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

      <el-form-item v-if="registerType === 'email'" prop="code" class="auth-field">
        <template #label>
          <span class="field-label">邮箱验证码 <i class="req">*</i></span>
        </template>
        <div class="code-row">
          <el-input
            v-model="form.code"
            placeholder="6 位验证码"
            class="auth-input code-input"
            maxlength="6"
            :prefix-icon="Key"
          />
          <VerifyCodeButton :email="form.account.trim()" scene="register" :sendable="emailValid" />
        </div>
      </el-form-item>

      <template v-if="codeFilled">
        <el-form-item prop="password" class="auth-field">
          <template #label>
            <span class="field-label">设置登录密码 <i class="req">*</i></span>
          </template>
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="6~12 位登录密码"
            :prefix-icon="Lock"
            class="auth-input"
          />
        </el-form-item>

        <el-form-item prop="confirmPassword" class="auth-field">
          <template #label>
            <span class="field-label">确认登录密码 <i class="req">*</i></span>
          </template>
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入登录密码"
            :prefix-icon="Lock"
            class="auth-input"
          />
        </el-form-item>
      </template>

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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Iphone, Key, Lock, Message, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import VerifyCodeButton from '@/components/common/VerifyCodeButton.vue'
import { useDraftStorage } from '@/composables/useDraft'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const registerType = ref<'email' | 'phone'>('email')
const form = reactive({ account: '', nickname: '', code: '', password: '', confirmPassword: '' })
const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const phonePattern = /^1[3-9]\d{9}$/
const emailValid = computed(() => emailPattern.test(form.account.trim()))
// 输入完整 6 位验证码后，再显示自定义密码
const codeFilled = computed(() => /^\d{6}$/.test(form.code))

/** 草稿只保存账号/邮箱、昵称与注册方式，绝不保存密码与验证码 */
const draft = useDraftStorage({
  getKey: () => 'draft:register',
  getSnapshot: () =>
    JSON.stringify({
      registerType: registerType.value,
      account: form.account,
      nickname: form.nickname
    }),
  restore: (raw) => {
    const s = JSON.parse(raw) as Record<string, unknown>
    if (s.registerType === 'email' || s.registerType === 'phone') registerType.value = s.registerType
    if (typeof s.account === 'string') form.account = s.account
    if (typeof s.nickname === 'string') form.nickname = s.nickname
  }
})

onMounted(() => {
  draft.restoreStored()
})

function switchType(type: 'email' | 'phone') {
  if (registerType.value === type) return
  registerType.value = type
  form.account = ''
  form.code = ''
  form.password = ''
  form.confirmPassword = ''
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
          validator: (_rule, value, callback) => {
            const val = String(value || '').trim()
            if (!val) callback(new Error('请输入邮箱'))
            else if (!emailPattern.test(val)) callback(new Error('邮箱格式不正确'))
            else callback()
          },
          trigger: 'blur'
        }
      : {
          validator: (_rule, value, callback) => {
            const val = String(value || '').trim()
            if (!val) callback(new Error('请输入手机号'))
            else if (!phonePattern.test(val)) callback(new Error('手机号格式不正确'))
            else callback()
          },
          trigger: 'blur'
        }
  ],
  ...(registerType.value === 'email'
    ? {
        code: [
          { required: true, message: '请输入验证码', trigger: 'blur' },
          { pattern: /^\d{6}$/, message: '请输入 6 位数字验证码', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请设置登录密码', trigger: 'blur' },
          { min: 6, max: 12, message: '密码长度为 6~12 位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入登录密码', trigger: 'blur' },
          {
            validator: (_rule, value, callback) => {
              if (value !== form.password) callback(new Error('两次输入的密码不一致'))
              else callback()
            },
            trigger: 'blur'
          }
        ]
      }
    : {})
}))

async function submit() {
  if (registerType.value === 'phone') {
    ElMessage.warning('暂时未开放手机号登录，请先选择邮箱登录')
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.register({
      email: form.account.trim(),
      code: form.code.trim(),
      nickname: form.nickname.trim() || undefined,
      password: form.password
    })
    ElMessage.success('注册成功，请登录')
    router.push({ path: '/login', query: { type: 'email', account: form.account.trim() } })
    draft.clear()
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
  background: linear-gradient(135deg, #f2a82e 0%, #e18b18 100%);
  box-shadow: 0 6px 20px rgba(232, 154, 31, 0.32);
  transition: box-shadow 0.2s ease, transform 0.15s ease, filter 0.2s ease;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #f7b845 0%, #ea9624 100%);
  box-shadow: 0 8px 26px rgba(232, 154, 31, 0.42);
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
