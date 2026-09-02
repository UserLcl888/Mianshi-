import request from './request'
import type { NoticeItem } from '@/types'

// 前台公开：启用中的公告
export async function getNoticesApi(): Promise<NoticeItem[]> {
  return request.get('/notices')
}

// 后台：公告管理
export async function getAdminNoticesApi(): Promise<NoticeItem[]> {
  return request.get('/admin/notices')
}

export async function createNoticeApi(payload: {
  content: string
  sortOrder?: number
  status?: number
}): Promise<NoticeItem> {
  return request.post('/admin/notices', payload)
}

export async function updateNoticeApi(
  id: number,
  payload: { content: string; sortOrder?: number; status?: number }
): Promise<NoticeItem> {
  return request.put(`/admin/notices/${id}`, payload)
}

export async function deleteNoticeApi(id: number): Promise<void> {
  return request.delete(`/admin/notices/${id}`)
}
