<template>
  <div class="page">
    <AppHeader />
    <div class="page-body">
      <main class="content">
        <el-breadcrumb class="breadcrumb-bar" separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>个人中心</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="app-card">
          <h3 class="section-title">个人信息</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{ auth.userInfo?.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ auth.userInfo?.nickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ auth.userInfo?.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ auth.userInfo?.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ auth.userInfo?.createdAt }}</el-descriptions-item>
          </el-descriptions>
          <div class="actions">
            <router-link to="/profile/password">
              <el-button type="primary" plain>修改密码</el-button>
            </router-link>
          </div>
        </div>
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

onMounted(() => {
  if (!auth.userInfo) auth.fetchProfile()
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
}

.content {
  max-width: 720px;
}

.section-title {
  margin: 0 0 16px;
  color: #6b5208;
}

.actions {
  margin-top: 18px;
}
</style>
