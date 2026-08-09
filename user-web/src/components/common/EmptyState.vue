<template>
  <div class="empty-state">
    <el-empty :description="text" />
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ text?: string }>(), { text: '暂无数据' })
</script>

<style scoped>
.empty-state {
  padding: 30px 0;
}
</style>
