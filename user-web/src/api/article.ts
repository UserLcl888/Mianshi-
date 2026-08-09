import request from './request'
import type { ArticleDetailResp, ArticleListItem, ArticleQuery, PageResult } from '@/types'

export interface ArticleSavePayload {
  title: string
  slug?: string
  summary?: string
  categoryId: number
  difficulty: string
  tags: string[]
  contentMd?: string
}

export async function getArticles(params: ArticleQuery = {}): Promise<PageResult<ArticleListItem>> {
  return request.get('/articles', { params })
}

export async function getArticleDetail(slug: string): Promise<ArticleDetailResp> {
  return request.get(`/articles/${slug}`)
}

export async function recordViewApi(id: number): Promise<number> {
  return request.post(`/articles/${id}/view`)
}

export async function createArticleApi(payload: ArticleSavePayload): Promise<{ id: number; slug: string; title: string }> {
  return request.post('/admin/articles', payload)
}

export async function updateArticleApi(
  id: number,
  payload: ArticleSavePayload
): Promise<{ id: number; slug: string; title: string }> {
  return request.put(`/admin/articles/${id}`, payload)
}
