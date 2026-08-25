<template>
  <div class="page">
    <AppHeader />
    <div class="page-body">
      <main class="content">
        <el-breadcrumb class="breadcrumb-bar" separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/profile' }">个人中心</el-breadcrumb-item>
          <el-breadcrumb-item>我的申请</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="app-card">
          <h3 class="section-title">访问申请记录</h3>
          <el-table :data="list" stripe>
            <el-table-column label="范围" width="110">
              <template #default="{ row }">
                <el-tag :type="row.scope === 'ALL' ? 'warning' : 'info'" size="small">
                  {{ row.scope === 'ALL' ? '全部分类' : '单分类' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="申请分类" min-width="200">
              <template #default="{ row }">
                <div>{{ row.categoryName }}</div>
              </template>
            </el-table-column>
            <el-table-column label="理由" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">{{ row.reason || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审批备注" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ row.reviewRemark || '-' }}</template>
            </el-table-column>
            <el-table-column label="管理员回复" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.adminReply || '-' }}</template>
            </el-table-column>
            <el-table-column label="申请时间" min-width="150">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <router-link v-if="row.categorySlug && row.status === 1" :to="`/category/${row.categorySlug}`">
                  <el-button size="small" text type="primary">查看分类</el-button>
                </router-link>
                <router-link v-else-if="row.categorySlug && row.status === 2" :to="`/category/${row.categorySlug}`">
                  <el-button size="small" text type="warning">重新申请</el-button>
                </router-link>
                <router-link v-else-if="row.scope === 'ALL' && row.status === 1" to="/">
                  <el-button size="small" text type="primary">去首页</el-button>
                </router-link>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!loading && !list.length" class="empty-tip">暂无申请记录</div>
        </div>
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { getMyAccessApi } from '@/api/access'
import { formatDateTime } from '@/utils/format'
import type { AccessApplyItem } from '@/types'

const list = ref<AccessApplyItem[]>([])
const loading = ref(false)

function statusType(status: number): 'warning' | 'success' | 'danger' {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info'
}

function statusText(status: number): string {
  return { 0: '待审批', 1: '已通过', 2: '已拒绝' }[status] || '未知'
}

onMounted(async () => {
  loading.value = true
  try {
    list.value = await getMyAccessApi()
  } finally {
    loading.value = false
  }
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
  padding: 16px 24px;
  display: flex;
  justify-content: center;
}

.content {
  width: 100%;
  max-width: 1080px;
}

.section-title {
  margin: 0 0 16px;
  color: #e9b862;
}

.sub-text {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.empty-tip {
  padding: 24px 0;
  text-align: center;
  color: var(--app-text-secondary);
  font-size: 13px;
}
</style>
