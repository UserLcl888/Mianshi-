import request from './request'
import type { CategoryNode } from '@/types'

export async function getCategoryTree(): Promise<CategoryNode[]> {
  return request.get('/categories/tree')
}
