import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = auth.token
  }
  return config
})

request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && body.code === 200) {
      return body.data
    }
    ElMessage.error(body?.message || '请求失败')
    return Promise.reject(new Error(body?.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      const auth = useAuthStore()
      auth.clearAuth()
      ElMessage.warning('登录已过期，请重新登录')
      router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      return Promise.reject(error)
    } else {
      const message = error.response?.data?.message || '网络错误，请稍后重试'
      // 40301=受限内容未授权，页面自行渲染“需申请访问”，不弹通用错误
      if (error.response?.data?.code !== 40301) {
        ElMessage.error(message)
      }
      const err = new Error(message) as Error & { code?: number }
      err.code = error.response?.data?.code
      return Promise.reject(err)
    }
  }
)

export default request
