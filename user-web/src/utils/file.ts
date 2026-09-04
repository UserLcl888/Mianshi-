/** 把 base64 data URI 还原成 File（用于保存时上传）。 */
export function dataUrlToFile(dataUrl: string, filename: string): File {
  const meta = dataUrl.match(/^data:([^;]+);base64,(.*)$/s)
  if (!meta) throw new Error('无效的图片数据')
  const mime = meta[1]
  const bin = atob(meta[2])
  const bytes = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i++) {
    bytes[i] = bin.charCodeAt(i)
  }
  return new File([bytes], filename, { type: mime })
}

/** 读取本地文件为 base64 data URL（封面/头像/粘贴图片等预览用）。 */
export function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('读取文件失败'))
    reader.readAsDataURL(file)
  })
}

/** 读取本地文本文件（.md/.markdown）为字符串。 */
export function readFileAsText(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('读取文件失败'))
    reader.readAsText(file, 'utf-8')
  })
}
