<template>
  <router-link :to="`/article/${article.slug}`" class="article-card">
    <div class="card-main">
      <div class="card-title">{{ article.title }}</div>
      <div class="card-summary">{{ article.summary }}</div>
      <div class="card-meta">
        <DifficultyTag :difficulty="article.difficulty" />
        <el-tag v-for="t in article.tags" :key="t" size="small" effect="plain" type="warning">{{ t }}</el-tag>
        <span class="views">{{ article.viewCount }} 次浏览</span>
      </div>
    </div>
    <el-icon class="arrow"><ArrowRight /></el-icon>
  </router-link>
</template>

<script setup lang="ts">
import { ArrowRight } from '@element-plus/icons-vue'
import DifficultyTag from './DifficultyTag.vue'
import type { ArticleListItem } from '@/types'

defineProps<{ article: ArticleListItem }>()
</script>

<style scoped>
.article-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 16px 18px;
  transition: all 0.15s;
}

.article-card:hover {
  border-color: var(--app-accent);
  background: var(--app-card-hover);
  transform: translateY(-1px);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #e8ecf3;
  margin-bottom: 6px;
}

.card-summary {
  color: var(--app-text-secondary);
  font-size: 13px;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.views {
  margin-left: auto;
  color: var(--app-text-secondary);
  font-size: 12px;
}

.arrow {
  color: var(--app-accent);
  flex-shrink: 0;
}
</style>
