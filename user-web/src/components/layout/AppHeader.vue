<template>
  <header class="app-header">
    <div class="ambient" aria-hidden="true">
      <span class="aurora a"></span>
      <span class="aurora b"></span>
      <span class="grid"></span>
      <span class="beam"></span>
    </div>

    <div class="header-inner">
      <router-link to="/home" class="brand" data-text="知识分享">知识分享</router-link>

      <nav class="nav">
        <router-link to="/home" class="nav-item" :class="{ active: isHome }">首页</router-link>
        <router-link to="/learn" class="nav-item" :class="{ active: isLearnArea }">专题</router-link>
        <router-link to="/articles" class="nav-item" :class="{ active: isArticlesArea }">文章</router-link>
        <router-link to="/author" class="nav-item" :class="{ active: isAuthor }">作者</router-link>
      </nav>

      <div class="header-right">
        <template v-if="auth.isLoggedIn">
          <el-popover v-model:visible="notifVisible" placement="bottom-end" :width="360" trigger="click" @show="loadNotifs">
            <template #reference>
              <span class="notif-bell" :class="{ 'has-unread': unreadCount > 0 }">
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
              <img v-if="auth.userInfo?.avatar" :src="auth.userInfo.avatar" class="user-avatar" alt="头像" />
              <el-icon v-else><User /></el-icon>
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
        <button class="theme-btn" type="button" title="切换主题" @click="cycleTheme">
          <el-icon :size="18"><Sunny /></el-icon>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Bell, Sunny, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useCategoryStore } from '@/stores/category'
import { useTheme } from '@/composables/useTheme'
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
const { cycleTheme } = useTheme()
const isAdmin = computed(() => auth.userInfo?.role === 'ADMIN')
const isHome = computed(() => {
  const p = route.path
  return p === '/home' || p.startsWith('/category') || p === '/article' || p.startsWith('/article/')
})
const isLearnArea = computed(() => route.path.startsWith('/learn'))
const isArticlesArea = computed(() => route.path.startsWith('/articles'))
const isAuthor = computed(() => route.path === '/author')

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
  overflow: hidden;
}

/* ==== 中间空区动态背景：极光 + 网格 + 流动光带（新增） ==== */
.ambient {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.aurora {
  position: absolute;
  width: 46%;
  height: 320%;
  top: -130%;
  border-radius: 50%;
  filter: blur(38px);
  opacity: .62;
}
.aurora.a {
  left: -8%;
  background: radial-gradient(circle, rgba(232, 154, 31, .75), transparent 60%);
  animation: drift-a 9s ease-in-out infinite alternate;
}
.aurora.b {
  right: -8%;
  background: radial-gradient(circle, rgba(34, 211, 238, .6), transparent 60%);
  animation: drift-b 7s ease-in-out infinite alternate;
}
@keyframes drift-a {
  0%   { transform: translate(-6%, -3%) scale(1); }
  100% { transform: translate(10%, 4%) scale(1.16); }
}
@keyframes drift-b {
  0%   { transform: translate(6%, 4%) scale(1.14); }
  100% { transform: translate(-10%, -3%) scale(1); }
}
.grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, .07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, .07) 1px, transparent 1px);
  background-size: 42px 42px;
  -webkit-mask-image: radial-gradient(140% 100% at 50% 0%, #000 50%, transparent 100%);
          mask-image: radial-gradient(140% 100% at 50% 0%, #000 50%, transparent 100%);
}
.beam {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 170px;
  background: linear-gradient(90deg, transparent, rgba(232, 154, 31, .5), rgba(34, 211, 238, .5), transparent);
  filter: blur(8px);
  opacity: .18;
  animation: beam 6s linear infinite;
}
@keyframes beam {
  0%   { transform: translateX(-200px); }
  100% { transform: translateX(calc(100vw + 200px)); }
}

.header-inner {
  position: relative;
  width: 100%;
  /* 900px 内容 + 左右 20px 内边距 = 940px，与文章页内容区左右边缘精确对齐 */
  max-width: 940px;
  margin: 0 auto;
  height: 53px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  flex-shrink: 0;
  font-size: clamp(18px, 5vw, 23px);
  font-weight: 700;
  color: #e9b862;
  white-space: nowrap;
  letter-spacing: 2px;
  text-shadow: 0 0 18px rgba(232, 154, 31, 0.35);
  position: relative;
  text-decoration: none;
  transition: filter 0.25s;
}
.brand::after {
  content: attr(data-text);
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent 0%, #fff 45%, #ffe9c4 50%, #fff 55%, transparent 100%);
  background-size: 250% 100%;
  -webkit-background-clip: text;
          background-clip: text;
  color: transparent;
  animation: shimmer 2.6s linear infinite;
  pointer-events: none;
}
.brand:hover {
  filter: brightness(1.18) drop-shadow(0 0 12px rgba(232, 154, 31, .5));
}
@keyframes shimmer {
  0%   { background-position: 250% 0; }
  100% { background-position: -250% 0; }
}

.nav {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
}
.nav-item {
  padding: 7px clamp(10px, 1.4vw, 16px);
  border-radius: 9px;
  font-size: 14px;
  color: var(--app-text);
  white-space: nowrap;
  text-decoration: none;
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
  gap: 6px;
  cursor: pointer;
  color: var(--app-text);
  outline: none;
  font-size: 15px;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--app-border);
}

.notif-bell {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  color: var(--app-text);
  outline: none;
  padding: 4px;
  border-radius: 6px;
  transform-origin: top center;
  transition: background 0.15s, color 0.15s;
}
.notif-bell:hover {
  background: var(--app-accent-soft);
  color: var(--app-accent);
}
.notif-bell.has-unread {
  animation: swing 1.7s ease-in-out infinite;
}

.theme-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--app-text);
  cursor: pointer;
  padding: 0;
  transition: background 0.15s, color 0.15s;
}

.theme-btn:hover {
  background: var(--app-accent-soft);
  color: var(--app-accent);
}

@keyframes swing {
  0%, 100% { transform: rotate(0); }
  12% { transform: rotate(26deg); }
  28% { transform: rotate(-22deg); }
  46% { transform: rotate(14deg); }
  66% { transform: rotate(-8deg); }
  82% { transform: rotate(4deg); }
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
  color: var(--app-text);
  text-decoration: none;
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

@media (max-width: 1100px) {
  .header-inner {
    gap: 0;
  }
  .header-right {
    gap: 6px;
  }
}

@media (max-width: 860px) {
  .nav {
    gap: 2px;
  }
  .nav-item {
    padding: 5px 9px;
    border-radius: 8px;
  }
}

@media (max-width: 640px) {
  .header-inner {
    height: 53px;
    padding: 0 12px;
  }
  .brand {
    letter-spacing: 1px;
  }
  .nav-item {
    padding: 4px 7px;
    font-size: 12.5px;
  }
  .user-entry .el-icon {
    display: none;
  }
}

/* 系统开启「减少动态」时自动停用动画 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.001ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.001ms !important;
  }
}
</style>
