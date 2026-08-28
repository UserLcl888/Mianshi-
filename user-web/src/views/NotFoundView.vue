<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在或已下架</p>
        <router-link to="/home">
      <el-button type="primary">返回首页</el-button>
    </router-link>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--app-text-secondary);
}

.not-found h1 {
  font-size: 60px;
  margin: 0;
  color: var(--app-accent);
}
</style>
