<template>
  <div class="page">
    <AppHeader />
    <div class="page-body">
      <CategorySidebar :active-category-slug="activeSlug" :active-article-slug="''" />
      <main class="content">
        <el-breadcrumb class="breadcrumb-bar" separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <template v-for="(c, idx) in categoryPath" :key="c.id">
            <el-breadcrumb-item v-if="idx < categoryPath.length - 1" :to="`/category/${c.slug}`">{{ c.name }}</el-breadcrumb-item>
            <el-breadcrumb-item v-else>{{ c.name }}</el-breadcrumb-item>
          </template>
        </el-breadcrumb>

        <div v-if="accessState" class="app-card access-card">
          <div class="access-icon"><el-icon :size="42"><Lock /></el-icon></div>
          <h2 class="access-title">{{ accessTitle || '该分类需申请访问' }}</h2>
          <p class="access-desc">{{ accessState.message }}</p>
          <div class="access-actions">
            <template v-if="!auth.isLoggedIn">
              <el-button type="primary" @click="goLogin">去登录申请</el-button>
            </template>
            <template v-else-if="accessStatus === 'PENDING'">
              <el-tag type="warning" size="large">申请审核中，请耐心等待</el-tag>
            </template>
            <template v-else-if="accessStatus === 'REJECTED'">
              <el-tag type="danger">申请未通过</el-tag>
              <el-button type="primary" @click="openApply">重新申请</el-button>
            </template>
            <template v-else>
              <el-button type="primary" @click="openApply">申请访问</el-button>
            </template>
          </div>
        </div>

        <template v-else>
          <div class="toolbar">
            <span class="category-desc">{{ currentCategory?.description }}</span>
            <el-select v-model="difficulty" placeholder="全部难度" clearable style="width: 140px">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
          </div>

          <div ref="listContainer" class="list-container">
            <template v-if="list.length">
              <ArticleCard v-for="a in list" :key="a.id" :article="a" />
            </template>
            <EmptyState v-else-if="!loading" text="该分类下暂无题目" />

            <div ref="sentinel" class="load-more">
              <span v-if="loading">加载中…</span>
              <span v-else-if="!hasMore && list.length">已经到底啦</span>
            </div>
          </div>
        </template>
      </main>

      <el-dialog v-model="applyVisible" title="申请访问" width="500px" append-to-body>
        <div class="apply-scope">
          <el-radio-group v-model="applyScope" @change="onScopeChange">
            <el-radio-button value="CATEGORY">仅申请该分类</el-radio-button>
            <el-radio-button value="ALL">申请全部分类</el-radio-button>
          </el-radio-group>
        </div>
        <div v-if="applyScope === 'ALL'" class="apply-all-tip">
          将申请 <b>{{ lockedList.length }}</b> 个暂无权限的受限分类
          <el-button link type="primary" @click="lockedVisible = true">查看分类</el-button>
        </div>
        <el-form label-position="top">
          <el-form-item label="申请理由（选填）">
            <el-input v-model="applyReason" maxlength="200" show-word-limit placeholder="简单说明申请原因" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="applyVisible = false">取消</el-button>
          <el-button type="primary" :loading="applying" @click="submitApply">提交申请</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="lockedVisible" title="暂无权限的受限分类" width="480px" append-to-body>
        <div v-if="!lockedList.length" class="locked-empty">暂无需要申请的分类</div>
        <el-table v-else :data="lockedList" stripe size="small" max-height="400">
          <el-table-column prop="name" label="分类名称" min-width="180" />
          <el-table-column prop="slug" label="标识" min-width="160" />
        </el-table>
        <template #footer>
          <el-button @click="lockedVisible = false">关闭</el-button>
        </template>
      </el-dialog>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import CategorySidebar from '@/components/layout/CategorySidebar.vue'
import ArticleCard from '@/components/article/ArticleCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getArticles } from '@/api/article'
import { applyAccessApi, getAccessStatusApi, getLockedCategoriesApi } from '@/api/access'
import { useCategoryStore } from '@/stores/category'
import { useAuthStore } from '@/stores/auth'
import { getCategoryPath, isRestrictedCategory } from '@/utils/category'
import type { AccessStatusItem, ArticleListItem, LockedCategoryItem } from '@/types'

const route = useRoute()
const router = useRouter()
const categoryStore = useCategoryStore()
const auth = useAuthStore()

const activeSlug = computed(() => String(route.params.slug || ''))
const categoryPath = computed(() => getCategoryPath(activeSlug.value, categoryStore.tree) || [])
const currentCategory = computed(() => categoryPath.value[categoryPath.value.length - 1] || null)

const list = ref<ArticleListItem[]>([])
const page = ref(1)
const size = 10
const hasMore = ref(true)
const loading = ref(false)
const difficulty = ref('')
const accessState = ref<{ message: string } | null>(null)
const accessStatus = ref<AccessStatusItem['status'] | null>(null)
const accessTitle = ref('')
const categoryIdForApply = ref<number | null>(null)
const applyVisible = ref(false)
const applyScope = ref<'CATEGORY' | 'ALL'>('CATEGORY')
const applyReason = ref('')
const applying = ref(false)
const lockedVisible = ref(false)
const lockedList = ref<LockedCategoryItem[]>([])

const listContainer = ref<HTMLElement | null>(null)
const sentinel = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null
let accessPollTimer: number | null = null

async function loadMore() {
  if (loading.value || !hasMore.value || !currentCategory.value) return
  loading.value = true
  try {
    const res = await getArticles({
      categoryId: currentCategory.value.id,
      difficulty: difficulty.value || undefined,
      page: page.value,
      size
    })
    list.value.push(...res.list)
    hasMore.value = res.hasMore
    page.value += 1
  } catch (e) {
    const err = e as Error & { code?: number }
    if (err.code === 40301) {
      // 后端校验受限分类未授权：重新拉取状态并展示申请页，防止前端分类树缓存过期导致绕过
      await checkGate()
    }
  } finally {
    loading.value = false
  }
}

async function reload() {
  list.value = []
  page.value = 1
  hasMore.value = true
  await loadMore()
}

function goLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

async function checkGate() {
  stopAccessPoll()
  accessState.value = null
  accessStatus.value = null
  accessTitle.value = ''
  categoryIdForApply.value = null
  if (!currentCategory.value) {
    return
  }
  if (!auth.isLoggedIn) {
    // 游客：依据分类树判断是否受限，受限则提示登录申请
    if (isRestrictedCategory(currentCategory.value, categoryStore.tree)) {
      accessState.value = { message: '该分类需登录后申请访问' }
    }
    return
  }
  // 已登录：始终以服务端权限状态为准，避免本地分类树缓存过期导致漏拦截
  try {
    const st = await getAccessStatusApi(activeSlug.value)
    accessTitle.value = st.title
    accessStatus.value = st.status
    categoryIdForApply.value = st.categoryId
    if (st.status !== 'GRANTED') {
      startAccessPoll()
      accessState.value = {
        message:
          st.status === 'PENDING'
            ? '该分类正在审核中，请耐心等待'
            : st.status === 'REJECTED'
              ? `申请未通过（${st.reviewRemark || '未说明原因'}）`
              : '该分类需申请后访问'
      }
    }
  } catch {
    accessState.value = { message: '该分类需申请后访问' }
  }
}

function stopAccessPoll() {
  if (accessPollTimer !== null) {
    window.clearInterval(accessPollTimer)
    accessPollTimer = null
  }
}

/** 受限分类页面：后台审批通过后自动解锁，无需用户手动刷新 */
function startAccessPoll() {
  stopAccessPoll()
  accessPollTimer = window.setInterval(async () => {
    if (!auth.isLoggedIn || !accessState.value) {
      stopAccessPoll()
      return
    }
    try {
      const st = await getAccessStatusApi(activeSlug.value)
      if (st.status === 'GRANTED') {
        stopAccessPoll()
        await checkGate()
        if (!accessState.value) await reload()
      }
    } catch {
      // 轮询失败保持现状，下一轮继续
    }
  }, 10000)
}

async function onScopeChange() {
  if (applyScope.value === 'ALL' && !lockedList.value.length) {
    try {
      lockedList.value = await getLockedCategoriesApi()
    } catch {
      lockedList.value = []
    }
  }
}

function openApply() {
  applyVisible.value = true
  applyScope.value = 'CATEGORY'
  applyReason.value = ''
}

async function submitApply() {
  if (applyScope.value === 'CATEGORY' && !categoryIdForApply.value) {
    ElMessage.warning('无法获取分类信息，请刷新后重试')
    return
  }
  applying.value = true
  try {
    await applyAccessApi({
      categoryId: applyScope.value === 'CATEGORY' ? categoryIdForApply.value || undefined : undefined,
      scope: applyScope.value,
      reason: applyReason.value.trim() || undefined
    })
    ElMessage.success(applyScope.value === 'ALL' ? '已申请全部分类，等待管理员审批' : '申请已提交，等待管理员审批')
    accessStatus.value = 'PENDING'
    applyVisible.value = false
  } finally {
    applying.value = false
  }
}

watch(
  () => [activeSlug.value, difficulty.value],
  async () => {
    await checkGate()
    if (!accessState.value) reload()
  }
)

onMounted(async () => {
  await categoryStore.fetchTree()
  await checkGate()
  if (accessState.value) return
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting) loadMore()
    },
    { root: listContainer.value, rootMargin: '120px' }
  )
  if (sentinel.value) observer.observe(sentinel.value)
  await reload()
})

onBeforeUnmount(() => {
  observer?.disconnect()
  stopAccessPoll()
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

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.category-desc {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  max-height: calc(100vh - 210px);
  padding-right: 4px;
}

.load-more {
  text-align: center;
  color: var(--app-text-secondary);
  font-size: 13px;
  padding: 14px 0 4px;
}

.access-card {
  text-align: center;
  padding: 60px 32px;
}

.access-icon {
  color: var(--app-accent);
  margin-bottom: 14px;
}

.access-title {
  font-size: 20px;
  color: #e8ecf3;
  margin: 0 0 10px;
}

.access-desc {
  color: var(--app-text-secondary);
  font-size: 14px;
  margin: 0 0 20px;
}

.access-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.apply-scope {
  margin-bottom: 14px;
}

.apply-all-tip {
  margin-bottom: 14px;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.locked-empty {
  padding: 40px 0;
  text-align: center;
  color: var(--app-text-secondary);
  font-size: 13px;
}
</style>
