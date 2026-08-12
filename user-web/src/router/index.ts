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
    { path: '/forgot-password', name: 'forgot-password', component: () => import('@/views/ForgotPasswordView.vue'), meta: { guestOnly: true } },
    { path: '/profile', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { requiresAuth: true } },
    { path: '/profile/password', name: 'change-password', component: () => import('@/views/ChangePasswordView.vue'), meta: { requiresAuth: true } },
    { path: '/admin', name: 'admin', component: () => import('@/views/AdminView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/edit/:slug', name: 'admin-edit', component: () => import('@/views/AdminView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    {
      path: '/admin/articles/create',
      component: () => import('@/components/layout/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: '', name: 'admin-article-create', component: () => import('@/views/AdminView.vue'), meta: { embedded: true } }
      ]
    },
    {
      path: '/admin/articles/:slug/edit',
      component: () => import('@/components/layout/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: '', name: 'admin-article-edit', component: () => import('@/views/AdminView.vue'), meta: { embedded: true } }
      ]
    },
    {
      path: '/admin/dashboard',
      component: () => import('@/components/layout/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [{ path: '', name: 'admin-dashboard', component: () => import('@/views/admin/DashboardView.vue') }]
    },
    {
      path: '/admin/articles',
      component: () => import('@/components/layout/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [{ path: '', name: 'admin-articles', component: () => import('@/views/admin/ArticleManageView.vue') }]
    },
    {
      path: '/admin/categories',
      component: () => import('@/components/layout/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [{ path: '', name: 'admin-categories', component: () => import('@/views/admin/CategoryManageView.vue') }]
    },
    {
      path: '/admin/users',
      component: () => import('@/components/layout/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [{ path: '', name: 'admin-users', component: () => import('@/views/admin/UserManageView.vue') }]
    },
    {
      path: '/admin/tags',
      component: () => import('@/components/layout/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [{ path: '', name: 'admin-tags', component: () => import('@/views/admin/TagManageView.vue') }]
    },
    {
      path: '/admin/logs',
      component: () => import('@/components/layout/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [{ path: '', name: 'admin-logs', component: () => import('@/views/admin/LogListView.vue') }]
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const current = router.currentRoute.value
  const editPages = ['admin', 'admin-edit', 'admin-article-create', 'admin-article-edit']
  const editPage = editPages.includes(String(current.name))
  const targetEditPage = editPages.includes(String(to.name))
  if (editPage && !targetEditPage && unsavedState.dirty) {
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
