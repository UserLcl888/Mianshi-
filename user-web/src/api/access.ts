import request from './request'
import type { AccessApplyItem, AccessStatusItem, LockedCategoryItem, PageResult } from '@/types'

// 用户端
export async function getLockedCategoriesApi(): Promise<LockedCategoryItem[]> {
  return request.get('/access/locked-categories')
}

export async function applyAccessApi(payload: {
  categoryId?: number
  scope: 'CATEGORY' | 'ALL'
  reason?: string
}): Promise<void> {
  return request.post('/access/apply', payload)
}

export async function getMyAccessApi(): Promise<AccessApplyItem[]> {
  return request.get('/access/my')
}

export async function getAccessStatusApi(slug: string): Promise<AccessStatusItem> {
  return request.get('/access/status', { params: { slug } })
}

// 管理端
export async function getAdminAccessApi(params: {
  keyword?: string
  status?: number
  scope?: string
  page?: number
  size?: number
}): Promise<PageResult<AccessApplyItem>> {
  return request.get('/admin/access', { params })
}

export async function getAdminAccessDetailApi(id: number): Promise<AccessApplyItem> {
  return request.get(`/admin/access/${id}`)
}

export async function approveAccessApi(id: number, remark?: string): Promise<void> {
  return request.put(`/admin/access/${id}/approve`, { remark })
}

export async function rejectAccessApi(id: number, remark?: string): Promise<void> {
  return request.put(`/admin/access/${id}/reject`, { remark })
}

export async function replyAccessApi(id: number, content: string): Promise<void> {
  return request.put(`/admin/access/${id}/reply`, { content })
}

export async function deleteAccessApi(id: number): Promise<void> {
  return request.delete(`/admin/access/${id}`)
}
