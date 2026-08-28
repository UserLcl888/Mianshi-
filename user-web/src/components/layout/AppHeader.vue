<template>
  <header class="app-header">
    <div class="header-inner">
      <router-link to="/home" class="brand">
        <img src="/logo.png" alt="logo" class="brand-logo" />
        <span class="brand-name">知识分享平台</span>
      </router-link>

      <nav class="nav">
        <router-link to="/topic" class="nav-item topic-entry" :class="{ active: isTopicArea }">专题</router-link>
        <router-link to="/home" class="nav-item" exact-active-class="active">首页</router-link>
        <template v-if="isTopicArea">
          <span class="topic-welcome">欢迎访问专栏模块，大家一起进步</span>
        </template>
        <template v-else>
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
        </template>
      </nav>

      <div class="header-right">
        <template v-if="auth.isLoggedIn">
          <el-popover v-model:visible="notifVisible" placement="bottom-end" :width="360" trigger="click" @show="loadNotifs">
            <template #reference>
              <span class="notif-bell">
                <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
                  <el-icon :size="20"><Bell /></el-icon>
                </el-badge>
              </span>
            </template>
            <div class="notif-panel">
              <div class="notif-header">
                <span class="notif-title">通知</span>
                <el-button v-if="unreadCount" size="small" text type="primary" @click="markAllRead">全部已读</el-button>
              </div>
              <div v-if="notifLoading" class="notif-empty">加载中…</div>
              <div v-else-if="!notifList.length" class="notif-empty">暂无通知</div>
              <div v-else class="notif-list">
                <div
                  v-for="n in notifList"
                  :key="n.id"
                  class="notif-item"
                  :class="{ unread: !n.isRead }"
                  @click="onNotifClick(n)"
                >
                  <span v-if="!n.isRead" class="notif-dot"></span>
                  <div class="notif-content">
                    <div class="notif-text">{{ n.content }}</div>
                    <div class="notif-time">{{ formatDateTime(n.createdAt) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </el-popover>
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
                <el-dropdown-item v-if="!isAdmin" command="applies">我的申请</el-dropdown-item>
                <el-dropdown-item v-if="!isAdmin" command="upload">内容上传</el-dropdown-item>
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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Bell, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useCategoryStore } from '@/stores/category'
import { findCategoryById } from '@/utils/category'
import {
  getAdminNotificationUnreadCountApi,
  getAdminNotificationsApi,
  getMyNotificationUnreadCountApi,
  getMyNotificationsApi,
  markAdminNotificationReadApi,
  markAdminNotificationsAllReadApi,
  markMyNotificationReadApi,
  markMyNotificationsAllReadApi
} from '@/api/notify'
import { formatDateTime } from '@/utils/format'
import type { NotificationItem } from '@/types'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const categoryStore = useCategoryStore()
const categories = computed(() => categoryStore.tree)
const visibleCategories = computed(() => categories.value.slice(0, 13))
const moreCategories = computed(() => categories.value.slice(13))
const isAdmin = computed(() => auth.userInfo?.role === 'ADMIN')
const isTopicArea = computed(() => route.path.startsWith('/topic'))

const notifVisible = ref(false)
const notifList = ref<NotificationItem[]>([])
const unreadCount = ref(0)
const notifLoading = ref(false)
let notifTimer: number | null = null

async function loadNotifs() {
  if (!auth.isLoggedIn) return
  notifLoading.value = true
  try {
    if (isAdmin.value) {
      const res = await getAdminNotificationsApi({ page: 1, size: 20 })
      notifList.value = res.list
      unreadCount.value = await getAdminNotificationUnreadCountApi()
    } else {
      const res = await getMyNotificationsApi({ page: 1, size: 20 })
      notifList.value = res.list
      unreadCount.value = await getMyNotificationUnreadCountApi()
    }
  } catch {
    // 静默失败
  } finally {
    notifLoading.value = false
  }
}

async function refreshUnread() {
  if (!auth.isLoggedIn) return
  try {
    unreadCount.value = isAdmin.value
      ? await getAdminNotificationUnreadCountApi()
      : await getMyNotificationUnreadCountApi()
  } catch {
    // 静默失败
  }
}

async function onNotifClick(n: NotificationItem) {
  if (!n.isRead) {
    try {
      if (isAdmin.value) {
        await markAdminNotificationReadApi(n.id)
      } else {
        await markMyNotificationReadApi(n.id)
      }
      n.isRead = 1
      if (unreadCount.value > 0) unreadCount.value -= 1
    } catch {
      // 忽略
    }
  }
  notifVisible.value = false
  if (n.type.startsWith('ACCESS_')) {
    if (n.type === 'ACCESS_APPROVED') {
      if (n.uploadId) {
        await categoryStore.fetchTree(true)
        const cat = findCategoryById(n.uploadId, categoryStore.tree)
        if (cat) {
          router.push(`/category/${cat.slug}`)
          return
        }
      } else {
        // 全部受限分类开通：直接回首页，所有内容已可访问
        router.push('/home')
        return
      }
    }
    router.push(n.type === 'ACCESS_APPLY' && isAdmin.value ? '/admin/access' : '/profile/applies')
    return
  }
  if (n.uploadId) {
    router.push(isAdmin.value ? `/admin/uploads?upload=${n.uploadId}&reply=1` : `/profile/uploads?upload=${n.uploadId}`)
  } else if (isAdmin.value) {
    router.push('/admin/uploads')
  } else {
    router.push('/profile/uploads')
  }
}

async function markAllRead() {
  try {
    if (isAdmin.value) {
      await markAdminNotificationsAllReadApi()
    } else {
      await markMyNotificationsAllReadApi()
    }
    notifList.value.forEach((n) => (n.isRead = 1))
    unreadCount.value = 0
  } catch {
    // 忽略
  }
}

function isCategoryActive(slug: string): boolean {
  const current = String(route.params.slug || '')
  return current === slug || current.startsWith(`${slug}-`)
}

function onCategoryCommand(slug: string) {
  router.push(`/category/${slug}`)
}

onMounted(() => {
  categoryStore.fetchTree()
  refreshUnread()
  notifTimer = window.setInterval(refreshUnread, 10000)
})

onBeforeUnmount(() => {
  if (notifTimer !== null) {
    window.clearInterval(notifTimer)
    notifTimer = null
  }
})

async function onCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'password') {
    router.push('/profile/password')
  } else if (command === 'applies') {
    router.push('/profile/applies')
  } else if (command === 'upload') {
    router.push('/profile/uploads')
  } else if (command === 'admin-console') {
    router.push('/admin/dashboard')
  } else if (command === 'admin') {
    router.push('/admin')
  } else if (command === 'logout') {
    await auth.logout()
    ElMessage.success('已退出登录')
    router.push('/home')
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
  color: #e9b862;
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
  color: #b8c0cf;
  white-space: nowrap;
  transition: all 0.15s;
}

.nav-item:hover {
  background: var(--app-accent-soft);
  color: #e9b862;
}

.nav-item.active {
  background: var(--app-accent-soft);
  color: var(--app-accent);
  font-weight: 600;
  box-shadow: inset 0 -2px 0 var(--app-accent);
}

/* 专题入口：橙金渐变胶囊 + 呼吸光晕，突出且保持夜色氛围 */
.topic-entry {
  margin-left: 6px;
  padding: 6px 15px;
  border-radius: 20px;
  font-weight: 600;
  color: #141a26;
  background: linear-gradient(135deg, #f2a82e 0%, #e18b18 100%);
  box-shadow: 0 0 10px rgba(232, 154, 31, 0.4);
  animation: topic-breathe 2.6s ease-in-out infinite;
  transition: transform 0.15s ease, box-shadow 0.2s ease, filter 0.2s ease;
  white-space: nowrap;
}

.topic-entry:hover {
  transform: translateY(-1px);
  filter: brightness(1.08);
  box-shadow: 0 0 22px rgba(232, 154, 31, 0.7);
  animation-play-state: paused;
}

.topic-entry.active {
  color: #141a26;
  background: linear-gradient(135deg, #f7b845 0%, #ea9624 100%);
  box-shadow: 0 0 24px rgba(232, 154, 31, 0.8);
}

@keyframes topic-breathe {
  0%,
  100% {
    box-shadow: 0 0 8px rgba(232, 154, 31, 0.28);
  }
  50% {
    box-shadow: 0 0 18px rgba(232, 154, 31, 0.62);
  }
}

.category-trigger {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  outline: none;
  user-select: none;
}

.topic-welcome {
  margin-left: 12px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  color: #e9b862;
  background: rgba(232, 154, 31, 0.1);
  border: 1px solid rgba(232, 154, 31, 0.3);
  white-space: nowrap;
}

.category-menu {
  max-height: 60vh;
  overflow-y: auto;
}

.category-menu :deep(.el-dropdown-menu__item.menu-active) {
  color: var(--app-accent);
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
  color: #b8c0cf;
  outline: none;
}

.notif-bell {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  color: #b8c0cf;
  outline: none;
  padding: 4px;
  border-radius: 6px;
  transition: background 0.15s;
}

.notif-bell:hover {
  background: var(--app-accent-soft);
  color: var(--app-accent);
}

.notif-panel {
  display: flex;
  flex-direction: column;
}

.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--app-border);
  margin-bottom: 8px;
}

.notif-title {
  font-size: 14px;
  font-weight: 600;
  color: #e9b862;
}

.notif-list {
  max-height: 320px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.notif-item:hover {
  background: var(--app-accent-soft);
}

.notif-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e6a23c;
  margin-top: 6px;
  flex-shrink: 0;
}

.notif-content {
  min-width: 0;
  flex: 1;
}

.notif-text {
  font-size: 13px;
  color: var(--app-text);
  line-height: 1.5;
  word-break: break-word;
}

.notif-time {
  margin-top: 4px;
  font-size: 11px;
  color: var(--app-text-secondary);
}

.notif-empty {
  padding: 26px 0;
  text-align: center;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.header-btn {
  padding: 6px 16px;
  border-radius: 6px;
  border: 1px solid var(--app-border);
  color: #b8c0cf;
  transition: all 0.15s;
}

.header-btn:hover {
  border-color: var(--app-accent);
  color: var(--app-accent);
}

.header-btn.primary {
  background: var(--app-accent);
  border-color: var(--app-accent);
  color: #141a26;
}

.header-btn.primary:hover {
  background: var(--el-color-primary-dark-2);
  border-color: var(--el-color-primary-dark-2);
}
</style>
