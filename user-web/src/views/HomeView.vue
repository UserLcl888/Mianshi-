<template>
  <div class="page">
    <AppHeader />
    <div class="page-body">
      <CategorySidebar :active-category-slug="''" :active-article-slug="''" />
      <main class="content">
        <div class="welcome">
          <h1 class="welcome-title">欢迎来到面试题知识库</h1>
          <p class="welcome-desc">
            {{ auth.isLoggedIn ? '精选后端高频面试题，按分类整理，助你高效备战面试。' : '登录后即可查看全部面试题内容。' }}
          </p>
        </div>

        <div v-if="loading" class="category-loading">加载中…</div>
        <div v-else class="category-grid">
          <router-link
            v-for="cat in categories"
            :key="cat.id"
            :to="`/category/${cat.slug}`"
            class="category-card"
          >
            <div class="card-name">{{ cat.name }}</div>
            <div class="card-desc">{{ cat.description || '暂无描述' }}</div>
            <span class="card-link">进入分类 →</span>
          </router-link>
        </div>
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import CategorySidebar from '@/components/layout/CategorySidebar.vue'
import { useCategoryStore } from '@/stores/category'
import { useAuthStore } from '@/stores/auth'

const categoryStore = useCategoryStore()
const auth = useAuthStore()
const loading = ref(true)
const categories = computed(() => categoryStore.tree)

onMounted(async () => {
  await categoryStore.fetchTree()
  loading.value = false
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.page-body {
  flex: 1;
  width: 100%;
  padding: 16px var(--layout-pad-x) 16px var(--sidebar-offset);
  display: flex;
  align-items: flex-start;
  gap: 18px;
}

.content {
  flex: 1;
  min-width: 0;
  max-width: 1160px;
}

.welcome {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 28px 32px;
  margin-bottom: 18px;
}

.welcome-title {
  margin: 0 0 10px;
  font-size: 24px;
  color: #6b5208;
}

.welcome-desc {
  margin: 0;
  color: var(--app-text-secondary);
  font-size: 14px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

.category-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 18px 20px;
  transition: all 0.15s;
}

.category-card:hover {
  border-color: var(--app-accent);
  background: var(--app-card-hover);
  transform: translateY(-2px);
}

.card-name {
  font-size: 17px;
  font-weight: 600;
  color: #4d4020;
}

.card-desc {
  color: var(--app-text-secondary);
  font-size: 13px;
  line-height: 1.6;
  flex: 1;
}

.card-link {
  color: var(--app-accent);
  font-size: 13px;
  font-weight: 600;
}

.category-loading {
  padding: 30px;
  text-align: center;
  color: var(--app-text-secondary);
}
</style>
