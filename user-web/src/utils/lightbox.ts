/**
 * 文章正文图片双击放大（lightbox）。
 *
 * 全局监听 .article-body 内 <img> 的双击事件，用固定定位遮罩以大图展示；
 * 点击遮罩 / 右上角关闭按钮 / 按 Esc 关闭。
 * 文章详情、学习专栏、编辑预览、用户上传预览/详情共用 .article-body，因此一处生效。
 */
let inited = false

export function enableImageLightbox(): void {
  if (inited) return
  inited = true

  const overlay = document.createElement('div')
  overlay.className = 'image-lightbox'
  overlay.setAttribute('role', 'dialog')
  overlay.setAttribute('aria-label', '图片预览')
  overlay.innerHTML = `
    <div class="image-lightbox-mask"></div>
    <div class="image-lightbox-body"><img alt="" /></div>
    <button class="image-lightbox-close" type="button" aria-label="关闭">&times;</button>
  `
  overlay.style.display = 'none'
  document.body.appendChild(overlay)

  const img = overlay.querySelector('img') as HTMLImageElement
  const mask = overlay.querySelector('.image-lightbox-mask') as HTMLElement
  const close = overlay.querySelector('.image-lightbox-close') as HTMLButtonElement

  const open = (src: string) => {
    img.src = src
    overlay.style.display = 'flex'
    document.body.style.overflow = 'hidden'
  }
  const closeLightbox = () => {
    overlay.style.display = 'none'
    img.src = ''
    document.body.style.overflow = ''
  }

  mask.addEventListener('click', closeLightbox)
  close.addEventListener('click', closeLightbox)
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeLightbox()
  })

  document.addEventListener('dblclick', (e) => {
    const el = (e.target as HTMLElement).closest('img') as HTMLImageElement | null
    if (!el || !el.src) return
    if (!el.closest('.article-body')) return // 仅限文章正文/预览里的图片
    e.preventDefault()
    open(el.currentSrc || el.src)
  })
}
