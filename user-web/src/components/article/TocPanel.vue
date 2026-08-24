<template>
  <nav class="toc-panel">
    <div class="toc-title">目录</div>
    <a
      v-for="item in toc"
      :key="item.id"
      :href="`#${item.id}`"
      class="toc-item"
      :class="{ active: activeId === item.id }"
      :style="{ paddingLeft: indent(item.level) + 'px', fontSize: fontSize(item.level) + 'px' }"
      @click.prevent="scrollTo(item.id)"
    >
      {{ item.text }}
    </a>
    <div v-if="!toc.length" class="toc-empty">暂无目录</div>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TocItem } from '@/types'

const props = defineProps<{ toc: TocItem[]; activeId: string }>()
const emit = defineEmits<{ (e: 'select', id: string): void }>()

const minLevel = computed(() => {
  if (!props.toc.length) return 2
  return Math.min(...props.toc.map((t) => t.level))
})

function indent(level: number): number {
  return 8 + (level - minLevel.value) * 14
}

function fontSize(level: number): number {
  return Math.max(12, 14 - (level - minLevel.value))
}

function scrollTo(id: string) {
  const el = document.getElementById(id)
  if (!el) return
  emit('select', id)
  const HEADER_OFFSET = 84
  const targetTop = window.scrollY + el.getBoundingClientRect().top - HEADER_OFFSET
  const maxScroll = document.documentElement.scrollHeight - window.innerHeight
  const finalTop = Math.max(0, Math.min(targetTop, maxScroll))
  window.scrollTo({ top: finalTop, behavior: 'smooth' })
}
</script>

<style scoped>
.toc-panel {
  width: 240px;
  flex-shrink: 0;
  position: sticky;
  top: 72px;
  max-height: calc(100vh - 92px);
  overflow-y: auto;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 12px;
}

.toc-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-secondary);
  padding-bottom: 8px;
  border-bottom: 1px solid var(--app-border);
  margin-bottom: 8px;
}

.toc-item {
  display: block;
  padding: 6px 8px;
  font-size: 13px;
  color: #b8c0cf;
  border-radius: 5px;
  line-height: 1.5;
  transition: all 0.15s;
}

.toc-item:hover {
  background: var(--app-card-hover);
}

.toc-item.active {
  color: #f5a623;
  font-weight: 600;
  background: var(--app-accent-soft);
}

.toc-empty {
  color: var(--app-text-secondary);
  font-size: 13px;
  padding: 8px;
}
</style>
