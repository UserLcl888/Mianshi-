<template>
  <el-tag :type="info.type" size="small" effect="light" disable-transitions>
    {{ info.label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ difficulty: string }>()

const DIFF_MAP: Record<string, { label: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
  EASY: { label: '简单', type: 'success' },
  MEDIUM: { label: '中等', type: 'warning' },
  HARD: { label: '困难', type: 'danger' }
}

const info = computed(() => DIFF_MAP[props.difficulty] || { label: props.difficulty, type: 'info' })
</script>
