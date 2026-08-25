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

export function findCategoryById(id: number, nodes: CategoryNode[]): CategoryNode | null {
  for (const n of nodes) {
    if (n.id === id) return n
    const found = findCategoryById(id, n.children)
    if (found) return found
  }
  return null
}

/** 分类是否受限：自身或任一祖先 accessLevel = APPLY */
export function isRestrictedCategory(node: CategoryNode | null, tree: CategoryNode[]): boolean {
  if (!node) return false
  if (node.accessLevel === 'APPLY') return true
  return isRestrictedCategory(findParent(node.parentId, tree), tree)
}

function findParent(id: number, nodes: CategoryNode[]): CategoryNode | null {
  for (const n of nodes) {
    if (n.id === id) return n
    const found = findParent(id, n.children)
    if (found) return found
  }
  return null
}
