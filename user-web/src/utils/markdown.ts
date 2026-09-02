import hljs from 'highlight.js'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

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
    hljs.highlightElement(block as HTMLElement)
  })
}
