<template>
  <div class="admin-layout">
    <aside class="admin-side">
      <div class="admin-brand">
        <div class="brand-main">面试题知识库</div>
        <div class="brand-sub">管理后台</div>
      </div>
      <nav class="admin-nav">
        <router-link
          v-for="item in menu"
          :key="item.path"
          :to="item.path"
          class="admin-nav-item"
          active-class="active"
        >
          {{ item.label }}
        </router-link>
      </nav>
    </aside>
    <div class="admin-main">
      <header class="admin-top">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>管理后台</el-breadcrumb-item>
          <el-breadcrumb-item>{{ currentLabel }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="admin-top-right">
          <router-link to="/">
            <el-button size="small" type="primary">返回前台</el-button>
          </router-link>
          <el-button size="small" type="danger" plain @click="logout">退出登录</el-button>
        </div>
      </header>
      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menu = [
  { path: '/admin/dashboard', label: '数据统计' },
  { path: '/admin/articles', label: '题目管理' },
  { path: '/admin/articles/create', label: '添加题目' },
  { path: '/admin/categories', label: '分类管理' },
  { path: '/admin/users', label: '用户管理' },
  { path: '/admin/tags', label: '标签管理' },
  { path: '/admin/logs', label: '操作日志' }
]

const labelMap: Record<string, string> = {
  'admin-dashboard': '数据统计',
  'admin-articles': '题目管理',
  'admin-article-create': '添加题目',
  'admin-article-edit': '编辑题目',
  'admin-categories': '分类管理',
  'admin-users': '用户管理',
  'admin-tags': '标签管理',
  'admin-logs': '操作日志',
  admin: '添加题目',
  'admin-edit': '编辑题目'
}

const currentLabel = computed(() => labelMap[String(route.name)] || '管理后台')

async function logout() {
  await auth.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
  background: var(--app-bg);
}

.admin-side {
  width: 220px;
  flex-shrink: 0;
  background: var(--app-header-bg);
  border-right: 1px solid var(--app-border);
  padding: 20px 12px;
}

.admin-brand {
  padding: 0 10px 18px;
  border-bottom: 1px solid var(--app-border);
  margin-bottom: 14px;
}

.brand-main {
  font-size: 16px;
  font-weight: 700;
  color: #6b5208;
  white-space: nowrap;
}

.brand-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.admin-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.admin-nav-item {
  padding: 10px 14px;
  border-radius: 8px;
  color: #6b5b2f;
  transition: all 0.15s;
}

.admin-nav-item:hover {
  background: var(--app-accent-soft);
  color: #6b5208;
}

.admin-nav-item.active {
  background: var(--app-accent-soft);
  color: #a87f18;
  font-weight: 600;
}

.admin-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-top {
  height: 58px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--app-card);
  border-bottom: 1px solid var(--app-border);
}

.admin-top-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.top-link {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.top-link:hover {
  color: var(--app-accent);
}

.admin-content {
  flex: 1;
  padding: 20px 24px;
}
</style>
