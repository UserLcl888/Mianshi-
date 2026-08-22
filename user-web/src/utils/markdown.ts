import hljs from 'highlight.js'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

/** 渲染 Markdown 为消毒后的 HTML（题目、用户上传等多处复用）。 */
export function renderMarkdown(md: string): string {
  return DOMPurify.sanitize(marked.parse(md || '') as string)
}

/** 为已渲染的 HTML 容器内的代码块做高亮。 */
export function highlightCodeBlocks(container: HTMLElement | null): void {
  if (!container) return
  container.querySelectorAll('pre code').forEach((block) => {
    hljs.highlightElement(block as HTMLElement)
  })
}
