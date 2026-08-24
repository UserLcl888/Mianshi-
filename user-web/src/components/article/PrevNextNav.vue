<template>
  <div class="prev-next">
    <router-link v-if="prev" :to="`/article/${prev.slug}`" class="pn-item">
      <span class="pn-label">上一篇</span>
      <span class="pn-title">{{ prev.title }}</span>
    </router-link>
    <span v-else class="pn-item disabled">已经是第一篇</span>

    <router-link v-if="next" :to="`/article/${next.slug}`" class="pn-item right">
      <span class="pn-label">下一篇</span>
      <span class="pn-title">{{ next.title }}</span>
    </router-link>
    <span v-else class="pn-item disabled right">已经是最后一篇</span>
  </div>
</template>

<script setup lang="ts">
import type { ArticleBrief } from '@/types'

defineProps<{ prev: ArticleBrief | null; next: ArticleBrief | null }>()
</script>

<style scoped>
.prev-next {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 24px;
}

.pn-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 16px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 8px;
  transition: all 0.15s;
}

.pn-item:hover {
  border-color: var(--app-accent);
}

.pn-item.right {
  align-items: flex-end;
  text-align: right;
}

.pn-item.disabled {
  opacity: 0.45;
  cursor: default;
}

.pn-label {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.pn-title {
  font-size: 14px;
  color: #e8ecf3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
