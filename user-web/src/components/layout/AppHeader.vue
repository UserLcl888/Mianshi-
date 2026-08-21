<template>
  <header class="app-header">
    <div class="header-inner">
      <router-link to="/" class="brand">
        <img src="/logo.png" alt="logo" class="brand-logo" />
        <span class="brand-name">知识分享平台</span>
      </router-link>

      <nav class="nav">
        <router-link to="/" class="nav-item" exact-active-class="active">首页</router-link>
        <template v-for="cat in visibleCategories" :key="cat.id">
          <router-link :to="`/category/${cat.slug}`" class="nav-item" :class="{ active: isCategoryActive(cat.slug) }">
            {{ cat.name }}
          </router-link>
        </template>
        <el-dropdown v-if="moreCategories.length" trigger="click" :teleported="false" @command="onCategoryCommand">
          <span class="nav-item category-trigger">
            更多主题
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu class="category-menu">
              <el-dropdown-item
                v-for="cat in moreCategories"
                :key="cat.id"
                :command="cat.slug"
                :class="{ 'menu-active': isCategoryActive(cat.slug) }"
              >
                {{ cat.name }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </nav>

      <div class="header-right">
        <template v-if="auth.isLoggedIn">
          <el-dropdown @command="onCommand">
            <span class="user-entry">
              <el-icon><User /></el-icon>
              {{ auth.userInfo?.nickname || auth.userInfo?.email }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="admin-console">管理后台</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="admin">添加内容</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login" class="header-btn primary">登录</router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useCategoryStore } from '@/stores/category'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const categoryStore = useCategoryStore()
const categories = computed(() => categoryStore.tree)
const visibleCategories = computed(() => categories.value.slice(0, 13))
const moreCategories = computed(() => categories.value.slice(13))
const isAdmin = computed(() => auth.userInfo?.role === 'ADMIN')

function isCategoryActive(slug: string): boolean {
  const current = String(route.params.slug || '')
  return current === slug || current.startsWith(`${slug}-`)
}

function onCategoryCommand(slug: string) {
  router.push(`/category/${slug}`)
}

onMounted(() => {
  categoryStore.fetchTree()
})

async function onCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'password') {
    router.push('/profile/password')
  } else if (command === 'admin-console') {
    router.push('/admin/dashboard')
  } else if (command === 'admin') {
    router.push('/admin')
  } else if (command === 'logout') {
    await auth.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--app-header-bg);
  border-bottom: 1px solid var(--app-border);
  box-shadow: 0 2px 10px rgba(217, 167, 22, 0.08);
}

.header-inner {
  width: 100%;
  height: 58px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: var(--header-gap);
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  width: var(--brand-width);
}

.brand-logo {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid var(--app-border);
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  color: #6b5208;
  white-space: nowrap;
}

.nav {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.nav-item {
  padding: 6px 12px;
  border-radius: 6px;
  color: #6b5b2f;
  white-space: nowrap;
  transition: all 0.15s;
}

.nav-item:hover {
  background: var(--app-accent-soft);
  color: #6b5208;
}

.nav-item.active {
  background: var(--app-accent-soft);
  color: #a87f18;
  font-weight: 600;
  box-shadow: inset 0 -2px 0 var(--app-accent);
}

.category-trigger {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  outline: none;
  user-select: none;
}

.category-menu {
  max-height: 60vh;
  overflow-y: auto;
}

.category-menu :deep(.el-dropdown-menu__item.menu-active) {
  color: #a87f18;
  font-weight: 600;
  background: var(--app-accent-soft);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #6b5b2f;
  outline: none;
}

.header-btn {
  padding: 6px 16px;
  border-radius: 6px;
  border: 1px solid var(--app-border);
  color: #6b5b2f;
  transition: all 0.15s;
}

.header-btn:hover {
  border-color: var(--app-accent);
  color: #a87f18;
}

.header-btn.primary {
  background: var(--app-accent);
  border-color: var(--app-accent);
  color: #fffdf6;
}

.header-btn.primary:hover {
  background: var(--el-color-primary-dark-2);
  border-color: var(--el-color-primary-dark-2);
}
</style>
