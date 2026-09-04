import hljs from 'highlight.js'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import type { Router } from 'vue-router'

/** 渲染 Markdown 为消毒后的 HTML（题目、用户上传等多处复用）。 */
export function renderMarkdown(md: string): string {
  // 允许 data:image URI：编辑器里粘贴/复制的 base64 图片，在保存前也能预览和复制
  return DOMPurify.sanitize(marked.parse(md || '') as string, {
    ADD_DATA_URI_TAGS: ['img'],
    ADD_DATA_URI_ATTRS: ['src']
  })
}

/** 为已渲染的 HTML 容器内的代码块做高亮。 */
export function highlightCodeBlocks(container: HTMLElement | null): void {
  if (!container) return
  container.querySelectorAll('pre code').forEach((block) => {
    const el = block as HTMLElement
    // Mermaid 图交由 renderDiagrams 渲染成图，不参与代码高亮
    if (/language-(mermaid|flowchart)/i.test(el.className || '')) return
    hljs.highlightElement(el)
  })
}

let mermaidInstance: unknown = null

async function loadMermaid(): Promise<any> {
  if (mermaidInstance) return mermaidInstance
  const mod = await import('mermaid')
  const mermaid = (mod as any).default
  // 站点主题：white 用浅色，其余（amber/black）用深色
  const theme = document.documentElement.getAttribute('data-theme') === 'white' ? 'default' : 'dark'
  mermaid.initialize({
    startOnLoad: false,
    securityLevel: 'strict',
    theme,
    // 用与站点一致的中文字体栈。mermaid 默认用 trebuchet/verdana/arial 测节点标签宽度，
    // 中文会回退到系统字体，可能与实际渲染宽度不一致，导致节点按“偏小尺寸”定框、
    // 真实中文标签溢出 <foreignObject> 被裁切（表现为字体显示不完整）。
    // 统一度量字体后，测量与渲染一致，节点尺寸更准确。
    themeVariables: {
      fontFamily: '"PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif'
    }
  })
  mermaidInstance = mermaid
  return mermaid
}

/**
 * 把正文里的 Mermaid / flowchart 代码块替换为渲染后的 SVG 图。
 * - 仅在存在 mermaid 代码块时动态加载 mermaid，避免打进主包。
 * - 渲染失败（语法/内容问题）时保留原代码块，不阻断其它内容。
 */
export async function renderDiagrams(container: HTMLElement | null): Promise<void> {
  if (!container) return
  // 常见 mermaid 指令首行，用于在服务端未输出 language 类时仍能识别
  const DIRECTIVE = /^(flowchart|graph|sequenceDiagram|classDiagram|stateDiagram|erDiagram|gantt|pie|journey|mindmap|gitGraph|quadrantChart|timeline|xychart|block|sankey|requirement|packet|c4)\b/i
  const blocks = Array.from(container.querySelectorAll<HTMLElement>('pre code')).filter((codeEl) => {
    if (/language-(mermaid|flowchart)/i.test(codeEl.className || '')) return true
    return DIRECTIVE.test((codeEl.textContent || '').trimStart())
  })
  if (!blocks.length) return
  let mermaid: any
  try {
    mermaid = await loadMermaid()
  } catch {
    // 加载失败：保持原样展示为代码块
    return
  }
  let seq = 0
  for (const codeEl of blocks) {
    const pre = codeEl.closest('pre')
    if (!pre) continue
    const source = (codeEl.textContent || '').trim()
    if (!source) continue
    const id = `mmd-${Date.now()}-${++seq}`
    try {
      const { svg } = await mermaid.render(id, source)
      const div = document.createElement('div')
      div.className = 'mermaid-render'
      div.innerHTML = svg
      // 强制 svg 随容器宽度自适应（svg 带 viewBox，会按比例缩放），避免被 mermaid 写死的固定宽度裁切
      const svgEl = div.querySelector('svg')
      if (svgEl) {
        svgEl.setAttribute('width', '100%')
        svgEl.style.maxWidth = '100%'
        svgEl.style.height = 'auto'
      }
      pre.replaceWith(div)
    } catch {
      // 单个图失败不阻断其它图，保留原代码块
    }
  }
}

/**
 * 处理 Markdown 正文里的链接点击。
 * - 站内文章/分类链接（/article/、/category/）走 SPA 跳转；
 * - 外链用新标签页打开，被拦截时退回当前页打开。
 * 用于文章详情、学习专栏等 v-html 正文，避免 <a target="_blank"> 的默认跳转。
 */
export function handleBodyLinkClick(e: MouseEvent, router: Router): void {
  const target = (e.target as HTMLElement).closest('a') as HTMLAnchorElement | null
  if (!target) return
  const href = target.getAttribute('href') || ''
  if (href.startsWith('/article/') || href.startsWith('/category/')) {
    e.preventDefault()
    router.push(href)
  } else if (/^https?:\/\//i.test(href)) {
    e.preventDefault()
    const w = window.open(href, '_blank', 'noopener,noreferrer')
    if (!w) window.location.href = href
  }
}
