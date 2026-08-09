import type { CategoryNode } from '@/types'

export function getCategoryPath(slug: string, nodes: CategoryNode[], path: CategoryNode[] = []): CategoryNode[] | null {
  for (const n of nodes) {
    const next = [...path, n]
    if (n.slug === slug) return next
    const found = getCategoryPath(slug, n.children, next)
    if (found) return found
  }
  return null
}

export function findCategory(slug: string, nodes: CategoryNode[]): CategoryNode | null {
  for (const n of nodes) {
    if (n.slug === slug) return n
    const found = findCategory(slug, n.children)
    if (found) return found
  }
  return null
}
