import request from './request'
import type { ArticleListItem, LearnCategory, PageResult, UserInfo } from '@/types'

export interface StatsOverview {
  userCount: number
  articleCount: number
  categoryCount: number
  publishedCount: number
}

export interface TopArticle {
  id: number
  title: string
  categoryName: string
  viewCount: number
}

export interface CategoryStatsItem {
  id: number
  name: string
  viewCount: number
  articleCount: number
}

export interface AdminTag {
  id: number
  name: string
  createdAt: string
}

export interface AdminLogItem {
  id: number
  adminId: number
  action: string
  targetType: string
  targetId: number
  detail: string
  createdAt: string
}

// 数据统计
export async function getStatsOverviewApi(): Promise<StatsOverview> {
  return request.get('/admin/stats/overview')
}

export async function getTopArticlesApi(): Promise<TopArticle[]> {
  return request.get('/admin/stats/top-articles')
}

export async function getCategoryStatsApi(): Promise<CategoryStatsItem[]> {
  return request.get('/admin/stats/category-stats')
}

// 题目管理
export async function getAdminArticlesApi(params: {
  columnType?: string
  categoryId?: number
  difficulty?: string
  page?: number
  size?: number
}): Promise<PageResult<ArticleListItem>> {
  return request.get('/admin/articles', { params })
}

export async function deleteArticleApi(id: number): Promise<void> {
  return request.delete(`/admin/articles/${id}`)
}

// 学习专题分类管理
export async function getAdminLearnCategoriesApi(): Promise<LearnCategory[]> {
  return request.get('/admin/learn-categories')
}

export async function createAdminLearnCategoryApi(payload: {
  name: string
  slug?: string
  sortOrder?: number
  coverUrl?: string
}): Promise<LearnCategory> {
  return request.post('/admin/learn-categories', payload)
}

export async function updateAdminLearnCategoryApi(
  id: number,
  payload: { name: string; slug?: string; sortOrder?: number; coverUrl?: string }
): Promise<LearnCategory> {
  return request.put(`/admin/learn-categories/${id}`, payload)
}

export async function deleteAdminLearnCategoryApi(id: number): Promise<void> {
  return request.delete(`/admin/learn-categories/${id}`)
}

/** 管理员上传专题封面图，返回可直接存入 coverUrl 的图片地址 */
export async function uploadCoverApi(file: File): Promise<{ url: string }> {
  const form = new FormData()
  form.append('file', file)
  return request.post('/admin/upload/cover', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 用户管理
export async function getAdminUsersApi(params: {
  keyword?: string
  status?: number
  page?: number
  size?: number
}): Promise<PageResult<UserInfo>> {
  return request.get('/admin/users', { params })
}

export async function createAdminUserApi(payload: {
  password: string
  nickname?: string
  role?: string
  email?: string
  phone?: string
}): Promise<UserInfo> {
  return request.post('/admin/users', payload)
}

export async function updateAdminUserApi(
  id: number,
  payload: { nickname?: string; email?: string; role?: string }
): Promise<UserInfo> {
  return request.put(`/admin/users/${id}`, payload)
}

export async function updateUserStatusApi(id: number, status: number): Promise<void> {
  return request.put(`/admin/users/${id}/status`, { status })
}

export async function resetUserPasswordApi(id: number, newPassword: string): Promise<void> {
  return request.put(`/admin/users/${id}/password`, { newPassword })
}

export async function deleteAdminUserApi(id: number): Promise<void> {
  return request.delete(`/admin/users/${id}`)
}

// 标签管理
export async function getAdminTagsApi(): Promise<AdminTag[]> {
  return request.get('/admin/tags')
}

export async function createAdminTagApi(name: string): Promise<AdminTag> {
  return request.post('/admin/tags', { name })
}

export async function updateAdminTagApi(id: number, name: string): Promise<AdminTag> {
  return request.put(`/admin/tags/${id}`, { name })
}

export async function deleteAdminTagApi(id: number): Promise<void> {
  return request.delete(`/admin/tags/${id}`)
}

// 操作日志
export async function getAdminLogsApi(page = 1, size = 10): Promise<PageResult<AdminLogItem>> {
  return request.get('/admin/logs', { params: { page, size } })
}
