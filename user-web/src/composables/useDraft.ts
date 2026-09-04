import { onBeforeUnmount, watch } from 'vue'

/**
 * 基于 sessionStorage 的轻量草稿持久化。
 *
 * 目的：在页面刷新、或是同一标签页内路由切换时，用户尚未提交的表单内容不丢失。
 * - 存储按「key」隔离，key 用 getKey() 动态计算，可在路由切换（同组件复用）时跟随变化。
 * - restoreStored()：由调用方决定恢复时机。例如编辑页在异步拉取服务端数据后再恢复，让草稿优先。
 * - clear()：提交成功或用户确认放弃后调用，清除草稿。
 * - 内容超出 sessionStorage 配额（例如大 base64 图片）时静默放弃保存，不影响正常提交。
 *
 * 注意：请勿把密码、验证码等敏感字段放进草稿。
 */
export function useDraftStorage(opts: {
  /** 动态计算存储 key，允许跟随路由/编辑对象变化。 */
  getKey: () => string
  /** 返回要持久化的快照（建议为 JSON 字符串）。 */
  getSnapshot: () => string
  /** 从快照恢复到页面状态。 */
  restore: (raw: string) => void
  /** 变化后延迟保存的时间（毫秒），避免每敲一个字符都写存储。 */
  delay?: number
}) {
  const { getKey, getSnapshot, restore, delay = 400 } = opts
  let timer: number | null = null

  const cancel = () => {
    if (timer !== null) {
      window.clearTimeout(timer)
      timer = null
    }
  }

  const save = () => {
    try {
      sessionStorage.setItem(getKey(), getSnapshot())
    } catch {
      // 超出配额或存储不可用：放弃持久化，不影响正常功能
    }
  }

  const stopWatch = watch(
    getSnapshot,
    (snap, prev) => {
      if (snap === prev) return
      cancel()
      timer = window.setTimeout(save, delay)
    },
    { flush: 'post' }
  )

  async function restoreStored(): Promise<boolean> {
    try {
      const raw = sessionStorage.getItem(getKey())
      if (!raw) return false
      restore(raw)
      return true
    } catch {
      return false
    }
  }

  function clear() {
    cancel()
    try {
      sessionStorage.removeItem(getKey())
    } catch {
      // 忽略
    }
  }

  onBeforeUnmount(() => {
    stopWatch()
    cancel()
  })

  return { restoreStored, clear }
}
