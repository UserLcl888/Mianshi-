import request from './request'
import type { CategoryNode } from '@/types'

export interface CategorySavePayload {
  name: string
  slug: string
  parentId: number
  sortOrder?: number
  description?: string
}

export interface CategoryReorderItem {
  id: number
  parentId?: number
  sortOrder: number
}

export async function getCategoryTree(): Promise<CategoryNode[]> {
  return request.get('/categories/tree')
}

export async function createCategoryApi(payload: CategorySavePayload): Promise<CategoryNode> {
  return request.post('/admin/categories', payload)
}

export async function updateCategoryApi(id: number, payload: CategorySavePayload): Promise<CategoryNode> {
  return request.put(`/admin/categories/${id}`, payload)
}

export async function deleteCategoryApi(id: number): Promise<void> {
  return request.delete(`/admin/categories/${id}`)
}

export async function reorderCategoriesApi(items: CategoryReorderItem[]): Promise<void> {
  return request.put('/admin/categories/reorder', items)
}
