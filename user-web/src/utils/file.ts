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
