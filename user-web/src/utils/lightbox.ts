/**
 * 文章正文图片双击放大（lightbox）。
 *
 * 全局监听 .article-body 内 <img> 的双击事件，用固定定位遮罩以大图展示；
 * 点击遮罩 / 右上角关闭按钮 / 按 Esc 关闭。
 * 打开后可滚轮放大/缩小（仅作用于遮罩内图片，正文其它内容不受影响），
 * 放大后按住拖动可平移，双击图片恢复适配尺寸。
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

  // 禁止浏览器原生“拖图片到新标签页/桌面”，灯箱里的拖拽只用于缩放后的平移；
  // 想要保存图片仍可用右键“图片另存为”。顺带把正文里的 <img> 也一并禁掉拖拽。
  img.setAttribute('draggable', 'false')
  document.addEventListener('dragstart', (e) => {
    const t = e.target as HTMLElement
    if (t instanceof HTMLImageElement && (t.closest('.article-body') || t.closest('.image-lightbox'))) {
      e.preventDefault()
    }
  })

  // 缩放/平移状态
  let scale = 1
  let tx = 0
  let ty = 0
  let baseW = 0 // 适配尺寸（scale=1）时的图片显示宽度
  let baseH = 0
  const MAX_SCALE = 12

  const clamp = (v: number, min: number, max: number) => Math.min(max, Math.max(min, v))
  const applyTransform = () => {
    img.style.transform = `translate(${tx}px, ${ty}px) scale(${scale})`
    img.classList.toggle('is-zoomed', scale > 1)
  }
  const resetTransform = () => {
    scale = 1
    tx = 0
    ty = 0
    applyTransform()
  }
  // 平移时保持图片始终有一部分贴近视口中心，避免拖出屏幕找不回来
  const clampPan = (x: number, y: number): [number, number] => {
    if (scale <= 1) return [0, 0]
    const halfW = (baseW * scale) / 2
    const halfH = (baseH * scale) / 2
    const keepW = Math.min(baseW / 2, 120)
    const keepH = Math.min(baseH / 2, 120)
    return [clamp(x, -(halfW - keepW), halfW - keepW), clamp(y, -(halfH - keepH), halfH - keepH)]
  }
  const measureBase = () => {
    const r = img.getBoundingClientRect()
    if (!r.width || !r.height) return
    baseW = r.width / Math.max(scale, 1)
    baseH = r.height / Math.max(scale, 1)
  }

  const open = (src: string) => {
    img.src = src
    resetTransform()
    baseW = baseH = 0
    overlay.style.display = 'flex'
    document.body.style.overflow = 'hidden'
    // 等图片就绪后测出适配尺寸，供后续以光标为中心缩放
    const onLoad = () => {
      measureBase()
      img.removeEventListener('load', onLoad)
    }
    img.addEventListener('load', onLoad)
    if (img.complete && img.naturalWidth) onLoad()
  }
  const closeLightbox = () => {
    overlay.style.display = 'none'
    img.src = ''
    document.body.style.overflow = ''
    resetTransform()
  }

  // 滚轮放大/缩小（以光标为中心）
  img.addEventListener('wheel', (e: WheelEvent) => {
    e.preventDefault()
    e.stopPropagation()
    if (!baseW) measureBase()
    if (!baseW) return
    const factor = Math.exp(-e.deltaY * 0.0015)
    const next = clamp(scale * factor, 1, MAX_SCALE)
    if (next === scale) return
    const cx = window.innerWidth / 2
    const cy = window.innerHeight / 2
    // 保持光标下的像素点位置不变：
    const bx = (e.clientX - cx - tx) / scale
    const by = (e.clientY - cy - ty) / scale
    scale = next
    tx = e.clientX - cx - bx * scale
    ty = e.clientY - cy - by * scale
    if (scale <= 1.001) {
      scale = 1
      tx = 0
      ty = 0
    } else {
      ;[tx, ty] = clampPan(tx, ty)
    }
    applyTransform()
  }, { passive: false })

  // 放大后按住拖动平移；拖拽时不算作“点击遮罩关闭”
  let dragging = false
  let moved = false
  let startX = 0
  let startY = 0
  let startTx = 0
  let startTy = 0
  overlay.addEventListener('pointerdown', (e: PointerEvent) => {
    if (e.button !== 0) return
    if (close.contains(e.target as Node)) return
    dragging = true
    moved = false
    startX = e.clientX
    startY = e.clientY
    startTx = tx
    startTy = ty
    try {
      overlay.setPointerCapture(e.pointerId)
    } catch {
      // 忽略 capture 失败
    }
  })
  overlay.addEventListener('pointermove', (e: PointerEvent) => {
    if (!dragging) return
    const dx = e.clientX - startX
    const dy = e.clientY - startY
    if (scale > 1) {
      if (Math.abs(dx) + Math.abs(dy) > 3) moved = true
      ;[tx, ty] = clampPan(startTx + dx, startTy + dy)
      applyTransform()
    }
  })
  const endDrag = (e: PointerEvent) => {
    if (!dragging) return
    dragging = false
    try {
      if (overlay.hasPointerCapture(e.pointerId)) overlay.releasePointerCapture(e.pointerId)
    } catch {
      // 忽略
    }
  }
  overlay.addEventListener('pointerup', endDrag)
  overlay.addEventListener('pointercancel', endDrag)

  // 双击图片：恢复适配尺寸
  img.addEventListener('dblclick', (e: MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()
    resetTransform()
  })

  mask.addEventListener('click', () => {
    // 拖拽平移后松开不算点击，避免误关
    if (moved) return
    closeLightbox()
  })
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
