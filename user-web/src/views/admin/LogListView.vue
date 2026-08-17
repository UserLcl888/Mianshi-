<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="section-title">操作日志</h3>
    </div>

    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="adminId" label="操作人ID" width="100" />
      <el-table-column prop="action" label="动作" width="180" />
      <el-table-column prop="targetType" label="对象类型" width="110" />
      <el-table-column prop="targetId" label="对象ID" width="90" />
      <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip />
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        layout="total, prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="onPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getAdminLogsApi, type AdminLogItem } from '@/api/admin'
import { formatDateTime } from '@/utils/format'

const list = ref<AdminLogItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

async function load() {
  try {
    const res = await getAdminLogsApi(page.value, size.value)
    list.value = res.list
    total.value = res.total
  } catch {
    // 拦截器已提示
  }
}

function onPage(p: number) {
  page.value = p
  load()
}

onMounted(load)
</script>

<style scoped>
.page-card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 18px 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  color: #6b5208;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
