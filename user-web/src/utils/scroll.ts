import type { Router } from 'vue-router'

/** 以路由全路径为 key，在 sessionStorage 里记录/恢复滚动位置，用于“刷新后回到原位置”。 */
const KEY = 'scroll-pos'

const positionKey = (fullPath: string) => `${KEY}:${fullPath}`

/** 记录某路由的滚动位置。 */
export function rememberScroll(fullPath: string, y: number): void {
  try {
    sessionStorage.setItem(positionKey(fullPath), String(y))
  } catch {
    // sessionStorage 不可用时静默忽略
  }
}

/** 读取某路由记住的滚动位置；无记录/非法值时返回 0。 */
export function readScroll(fullPath: string): number {
  try {
    const v = sessionStorage.getItem(positionKey(fullPath))
    const n = v == null ? 0 : Number(v)
    return Number.isFinite(n) && n > 0 ? Math.round(n) : 0
  } catch {
    return 0
  }
}

/**
 * 全程收集当前页滚动位置（滚动时 + 离开页面时各记一次），并关闭浏览器原生滚动恢复，
 * 交给本模块统一恢复，避免刷新后被 Vue Router 的 scrollBehavior 强制回到顶部。
 */
export function attachScrollMemory(router: Router): void {
  const save = () => {
    rememberScroll(router.currentRoute.value.fullPath, window.scrollY)
  }
  window.addEventListener('scroll', save, { passive: true })
  window.addEventListener('pagehide', save)
  if ('scrollRestoration' in history) history.scrollRestoration = 'manual'
}

/**
 * 恢复到目标滚动位置。
 * 页面内容可能是异步渲染（文章/学习页有 loading 态）、图片也异步加载，文档高度会逐步增长；
 * 这里用 requestAnimationFrame 轮询，直到文档撑到足够高度（或超时）再一次性滚过去，
 * 避免在内容还没加载时被“按短文档高度”截到顶部。
 */
export function restoreScroll(targetY: number, maxWait = 4000): void {
  if (targetY <= 0) return
  const start = performance.now()
  const tick = () => {
    const doc = document.documentElement
    const max = doc.scrollHeight - window.innerHeight
    const canReach = max >= targetY
    const timedOut = performance.now() - start > maxWait
    if (canReach || timedOut) {
      window.scrollTo(0, Math.max(0, Math.min(targetY, max)))
      return
    }
    requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}
