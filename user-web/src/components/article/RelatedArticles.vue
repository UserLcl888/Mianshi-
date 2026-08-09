<template>
  <div v-if="articles.length" class="related-panel">
    <div class="related-title">相关题目</div>
    <router-link v-for="a in articles" :key="a.id" :to="`/article/${a.slug}`" class="related-item">
      {{ a.title }}
    </router-link>
  </div>
</template>

<script setup lang="ts">
import type { ArticleListItem } from '@/types'

defineProps<{ articles: ArticleListItem[] }>()
</script>

<style scoped>
.related-panel {
  width: 250px;
  flex-shrink: 0;
  position: sticky;
  top: 72px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 12px;
}

.related-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-secondary);
  padding-bottom: 8px;
  border-bottom: 1px solid var(--app-border);
  margin-bottom: 8px;
}

.related-item {
  display: block;
  padding: 7px 8px;
  font-size: 13px;
  color: #6b5b2f;
  border-radius: 5px;
  line-height: 1.5;
  transition: all 0.15s;
}

.related-item:hover {
  background: var(--app-card-hover);
  color: #a87f18;
}
</style>
