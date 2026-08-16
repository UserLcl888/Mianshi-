import request from './request'

export interface HotArticle {
  id: number
  slug: string
  title: string
  categoryName: string
  viewCount: number
}

export interface HotTag {
  name: string
  count: number
}

export interface HomeOverview {
  articleCount: number
  viewCount: number
  hotArticles: HotArticle[]
  hotTags: HotTag[]
}

export interface DailyQuote {
  content: string
  author: string
}

export async function getHomeOverviewApi(): Promise<HomeOverview> {
  return request.get('/home/overview')
}

export async function getHomeQuoteApi(): Promise<DailyQuote> {
  return request.get('/home/quote')
}
