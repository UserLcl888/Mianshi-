import request from './request'
import type { NotificationItem, PageResult } from '@/types'

// 普通用户通知
export async function getMyNotificationsApi(params: { page?: number; size?: number } = {}): Promise<PageResult<NotificationItem>> {
  return request.get('/user/notifications', { params })
}

export async function getMyNotificationUnreadCountApi(): Promise<number> {
  return request.get('/user/notifications/unread-count')
}

export async function markMyNotificationReadApi(id: number): Promise<void> {
  return request.put(`/user/notifications/${id}/read`)
}

export async function markMyNotificationsAllReadApi(): Promise<void> {
  return request.put('/user/notifications/read-all')
}

// 管理员通知
export async function getAdminNotificationsApi(params: { page?: number; size?: number } = {}): Promise<PageResult<NotificationItem>> {
  return request.get('/admin/notifications', { params })
}

export async function getAdminNotificationUnreadCountApi(): Promise<number> {
  return request.get('/admin/notifications/unread-count')
}

export async function markAdminNotificationReadApi(id: number): Promise<void> {
  return request.put(`/admin/notifications/${id}/read`)
}

export async function markAdminNotificationsAllReadApi(): Promise<void> {
  return request.put('/admin/notifications/read-all')
}
