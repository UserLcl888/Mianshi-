import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'
import { unsavedState } from '@/utils/unsaved'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },
    { path: '/category/:slug', name: 'category', component: () => import('@/views/CategoryView.vue') },
    { path: '/article/:slug', name: 'article', component: () => import('@/views/ArticleDetailView.vue') },
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { guestOnly: true } },
    { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue'), meta: { guestOnly: true } },
    { path: '/profile', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { requiresAuth: true } },
    { path: '/profile/password', name: 'change-password', component: () => import('@/views/ChangePasswordView.vue'), meta: { requiresAuth: true } },
    { path: '/admin', name: 'admin', component: () => import('@/views/AdminView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/edit/:slug', name: 'admin-edit', component: () => import('@/views/AdminView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const current = router.currentRoute.value
  const leavingAdmin = current.path.startsWith('/admin')
  const enteringAdmin = to.path.startsWith('/admin')
  if (leavingAdmin && !enteringAdmin && unsavedState.dirty) {
    try {
      await ElMessageBox.confirm('当前有未保存的修改，确定放弃并离开吗？', '提示', {
        confirmButtonText: '放弃修改',
        cancelButtonText: '继续编辑',
        type: 'warning'
      })
    } catch {
      return false
    }
  }
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.isLoggedIn) {
    return { path: '/' }
  }
  if (to.meta.requiresAdmin && auth.userInfo?.role !== 'ADMIN') {
    return { path: '/' }
  }
  return true
})

export default router
