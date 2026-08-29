<template>
  <div class="learn-page">
    <AppHeader />
    <div class="learn-body">
      <main class="learn-main">
        <header class="learn-head">
          <h1 class="learn-title">学习专题</h1>
          <p class="learn-sub">我的学习笔记与代码实践，点开一个板块开始学习</p>
        </header>

        <div v-if="loading" class="learn-empty">加载中…</div>
        <div v-else-if="!list.length" class="learn-empty">暂无学习专题，敬请期待</div>

        <div v-else class="learn-grid">
          <div
            v-for="(c, i) in list"
            :key="c.slug"
            class="learn-card"
            @click="$router.push(`/learn/${c.slug}`)"
          >
            <div class="learn-cover">
              <span class="cover-index">{{ String(i + 1).padStart(2, '0') }}</span>
              <span class="cover-name">{{ c.name }}</span>
              <span class="cover-sub">LEARNING</span>
            </div>
            <div class="learn-info">
              <h2 class="learn-card-name">{{ c.name }}</h2>
              <div class="learn-meta">
                <span class="meta-item">
                  <el-icon :size="13"><CollectionTag /></el-icon>
                  {{ c.articleCount }} 篇文章
                </span>
                <span class="meta-item">
                  <el-icon :size="13"><Clock /></el-icon>
                  {{ formatDate(c.updatedAt) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Clock, CollectionTag } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { getLearnCategoriesApi } from '@/api/article'
import type { LearnCategory } from '@/types'

const loading = ref(false)
const list = ref<LearnCategory[]>([])

function formatDate(d: string | null): string {
  if (!d) return '刚刚'
  return d.slice(0, 10)
}

onMounted(async () => {
  loading.value = true
  try {
    list.value = await getLearnCategoriesApi()
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.learn-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.learn-body {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 30px 20px 44px;
}

.learn-main {
  width: 100%;
  max-width: 900px;
}

.learn-head {
  margin-bottom: 22px;
}

.learn-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 1px;
}

.learn-sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: #8a9bb5;
}

.learn-empty {
  padding: 70px 20px;
  text-align: center;
  color: #8a9bb5;
  font-size: 14px;
  background: rgba(18, 24, 38, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
}

.learn-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.learn-card {
  border-radius: 14px;
  overflow: hidden;
  background: rgba(18, 24, 38, 0.68);
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.learn-card:hover {
  transform: translateY(-3px);
  border-color: rgba(232, 154, 31, 0.5);
  background: rgba(24, 32, 50, 0.82);
}

.learn-cover {
  position: relative;
  aspect-ratio: 16 / 9;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background:
    radial-gradient(circle at 80% 15%, rgba(232, 154, 31, 0.35) 0%, transparent 45%),
    linear-gradient(135deg, rgba(38, 52, 80, 0.9) 0%, rgba(16, 22, 36, 0.95) 70%);
}

.cover-index {
  position: absolute;
  left: 12px;
  top: 10px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
}

.cover-name {
  font-size: 18px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 2px;
  text-shadow: 0 2px 14px rgba(0, 0, 0, 0.5);
}

.cover-sub {
  font-size: 11px;
  letter-spacing: 5px;
  color: #e9b862;
}

.learn-info {
  padding: 10px 12px;
}

.learn-card-name {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
}

.learn-meta {
  margin-top: 7px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 11px;
  color: #8a9bb5;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

@media (max-width: 860px) {
  .learn-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .learn-grid {
    grid-template-columns: 1fr;
  }
}
</style>
