import { onBeforeUnmount, onMounted } from 'vue'

/**
 * 简单轮询：组件挂载后每隔 intervalMs 执行一次 fn（失败静默），卸载时自动停止。
 * 用于页面停留时自动刷新新内容（文章/上传状态等）。
 */
export function usePolling(fn: () => void | Promise<void>, intervalMs: number) {
  let timer: number | null = null

  onMounted(() => {
    timer = window.setInterval(async () => {
      try {
        await fn()
      } catch {
        // 轮询失败保持现状，下一轮继续
      }
    }, intervalMs)
  })

  onBeforeUnmount(() => {
    if (timer !== null) {
      window.clearInterval(timer)
      timer = null
    }
  })
}
