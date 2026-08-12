export interface CategoryNode {
  id: number
  name: string
  slug: string
  parentId: number
  sortOrder: number
  description?: string
  children: CategoryNode[]
}

export interface ArticleListItem {
  id: number
  slug: string
  title: string
  summary: string
  categoryId: number
  difficulty: string
  tags: string[]
  viewCount: number
  updatedAt: string
}

export interface TocItem {
  id: string
  text: string
  level: number
}

export interface ArticleDetail {
  id: number
  slug: string
  title: string
  summary: string
  docUrl: string
  categoryId: number
  categoryName: string
  categorySlug: string
  difficulty: string
  tags: string[]
  contentMd: string
  contentHtml: string
  toc: TocItem[]
  viewCount: number
  updatedAt: string
}

export interface ArticleBrief {
  id: number
  slug: string
  title: string
}

export interface ArticleDetailResp {
  article: ArticleDetail
  prev: ArticleBrief | null
  next: ArticleBrief | null
}

export interface PageResult<T> {
  list: T[]
  page: number
  size: number
  total: number
  hasMore: boolean
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  phone?: string
  role: string
  status?: number
  createdAt: string
}

export interface LoginResult {
  token: string
  userInfo: UserInfo
}

export interface ArticleQuery {
  categoryId?: number
  difficulty?: string
  page?: number
  size?: number
}
