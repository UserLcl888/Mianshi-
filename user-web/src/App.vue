<template>
  <div class="app-shell">
    <!-- 全局背景：登录页背景图 + 暗色渐变，铺满几乎所有页面 -->
    <div class="app-bg" aria-hidden="true"></div>
    <div class="app-view">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useTheme } from '@/composables/useTheme'

const { initTheme } = useTheme()
onMounted(initTheme)
</script>

<style>
.app-shell {
  position: relative;
  min-height: 100%;
}

.app-bg {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 0;
  pointer-events: none;
  background:
    radial-gradient(1100px 720px at 18% 8%, rgba(31, 47, 74, 0.5) 0%, rgba(31, 47, 74, 0) 62%),
    radial-gradient(760px 560px at 88% 88%, rgba(20, 30, 48, 0.55) 0%, rgba(20, 30, 48, 0) 60%),
    linear-gradient(135deg, #0a0e17 0%, #070b12 45%, #05080f 100%);
}

.app-bg::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background: url('/auth-bg.png') center / cover no-repeat;
  opacity: 0.16;
  filter: brightness(1.25) contrast(1.05) saturate(1.05);
}

.app-view {
  position: relative;
  z-index: 1;
}

.page-enter-active,
.page-leave-active {
  transition: opacity 0.28s ease, transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: opacity, transform;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(14px) scale(0.995);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.998);
}
</style>
