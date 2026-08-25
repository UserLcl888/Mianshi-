<template>
  <el-button class="code-btn" :disabled="!sendable || countdown > 0 || loading" :loading="loading" @click="send">
    {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
  </el-button>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { sendEmailCodeApi } from '@/api/auth'

const props = defineProps<{
  email: string
  scene: 'login' | 'register' | 'reset'
  sendable: boolean
}>()

const loading = ref(false)
const countdown = ref(0)
let timer: number | null = null

async function send() {
  loading.value = true
  try {
    await sendEmailCodeApi(props.email.trim(), props.scene)
    ElMessage.success('验证码已发送，请查收邮箱')
    countdown.value = 60
    timer = window.setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && timer) {
        window.clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.code-btn {
  width: 120px;
  flex-shrink: 0;
}
</style>
