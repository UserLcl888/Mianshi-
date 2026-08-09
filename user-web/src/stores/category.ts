import { defineStore } from 'pinia'
import { getCategoryTree } from '@/api/category'
import type { CategoryNode } from '@/types'

export const useCategoryStore = defineStore('category', {
  state: () => ({
    tree: [] as CategoryNode[],
    loaded: false
  }),
  actions: {
    async fetchTree(force = false) {
      if (this.loaded && !force) return
      this.tree = await getCategoryTree()
      this.loaded = true
    }
  }
})
