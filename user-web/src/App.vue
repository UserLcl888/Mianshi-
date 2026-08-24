<template>
  <router-view v-slot="{ Component }">
    <transition name="page" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<style>
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
