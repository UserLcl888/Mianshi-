/**
 * 站点级配置。
 * MINIO_PUBLIC_BASE / MINIO_BUCKET：图片公开访问前缀与桶名，部署后按实际修改。
 */
export const MINIO_PUBLIC_BASE = 'http://103.236.54.34:9000'
export const MINIO_BUCKET = 'interview-images'

/** 拼 MinIO 对象访问地址，如 minioUrl('banner/java.png') */
export const minioUrl = (objectPath: string): string =>
  `${MINIO_PUBLIC_BASE}/${MINIO_BUCKET}/${objectPath}`

/**
 * 首页轮播图（banner）列表：对象路径 = MinIO `interview-images` 桶下相对路径。
 * 以后新增轮播图：把图片传到 MinIO 的 banner/ 目录 → 在这里加一行即可，无需改业务页面。
 */
export const CAROUSEL_BANNERS: { src: string; title: string }[] = [
  { src: minioUrl('banner/konwledage.png'), title: '知识' },
  { src: minioUrl('banner/java.png'), title: 'Java' },
  { src: minioUrl('banner/ai.png'), title: 'AI' }
]
