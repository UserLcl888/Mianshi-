<template>
  <div class="dashboard">
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-label">用户总数</div>
        <div class="stat-value">{{ overview?.userCount ?? '-' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">题目总数</div>
        <div class="stat-value">{{ overview?.articleCount ?? '-' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">分类数</div>
        <div class="stat-value">{{ overview?.categoryCount ?? '-' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已发布</div>
        <div class="stat-value">{{ overview?.publishedCount ?? '-' }}</div>
      </div>
    </div>

    <div class="dashboard-main">
      <div class="app-card top-card">
        <h3 class="section-title">浏览量 Top 10</h3>
        <el-table
          ref="tableRef"
          :data="topList"
          size="small"
          stripe
          :row-style="rowStyle"
        >
          <el-table-column prop="title" label="标题" width="230" show-overflow-tooltip />
          <el-table-column label="分类" width="92">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" type="warning">{{ row.categoryName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览" width="66" align="right" />
        </el-table>
        <div v-if="!topList.length" class="empty-tip">暂无数据</div>
      </div>

      <div class="charts">
        <div class="chart-card">
          <h3 class="section-title">分类浏览量</h3>
          <div ref="viewsChartRef" class="chart-box"></div>
        </div>
        <div class="chart-card">
          <h3 class="section-title">分类题目分布</h3>
          <div ref="countsChartRef" class="chart-box"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ElTable } from 'element-plus'
import {
  getStatsOverviewApi,
  getTopArticlesApi,
  getCategoryStatsApi,
  type StatsOverview,
  type TopArticle,
  type CategoryStatsItem
} from '@/api/admin'

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const overview = ref<StatsOverview | null>(null)
const topList = ref<TopArticle[]>([])
const categoryStats = ref<CategoryStatsItem[]>([])
const viewsChartRef = ref<HTMLElement>()
const countsChartRef = ref<HTMLElement>()
const tableRef = ref<InstanceType<typeof ElTable>>()
const rowHeightPx = ref(44)
let viewsChart: echarts.ECharts | null = null
let countsChart: echarts.ECharts | null = null
let tableObserver: ResizeObserver | null = null

const rowStyle = () => ({ height: rowHeightPx.value + 'px' })

function computeRowHeight() {
  const root = tableRef.value?.$el as HTMLElement | undefined
  if (!root) return
  const header = root.querySelector('.el-table__header-wrapper') as HTMLElement | null
  const available = root.clientHeight - (header ? header.clientHeight : 0)
  const n = topList.value.length
  rowHeightPx.value = n > 0 ? Math.max(36, Math.floor(available / n)) : 44
}

function startTableObserver() {
  const root = tableRef.value?.$el as HTMLElement | undefined
  if (!root || typeof ResizeObserver === 'undefined') return
  tableObserver = new ResizeObserver(() => computeRowHeight())
  tableObserver.observe(root)
}

function barOption(data: { name: string; value: number }[]): echarts.EChartsCoreOption {
  return {
    grid: { left: 8, right: 12, top: 20, bottom: 46, containLabel: false },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}：{c}' },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.name),
      axisLine: { lineStyle: { color: '#d9a716' } },
      axisTick: { show: false },
      axisLabel: { color: '#6b5b2f', fontSize: 10, interval: 0, rotate: 30 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f0e1b9' } },
      axisLabel: { color: '#8a7a4f', fontSize: 10 }
    },
    series: [
      {
        type: 'bar',
        data: data.map((d) => d.value),
        barMaxWidth: 22,
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#f0c966' },
            { offset: 1, color: '#c88f1f' }
          ])
        },
        animationDuration: 1000
      }
    ]
  }
}

function renderCharts() {
  if (!viewsChartRef.value || !countsChartRef.value) return
  viewsChart = echarts.init(viewsChartRef.value)
  countsChart = echarts.init(countsChartRef.value)
  viewsChart.setOption(barOption(categoryStats.value.map((c) => ({ name: c.name, value: c.viewCount }))))
  countsChart.setOption(barOption(categoryStats.value.map((c) => ({ name: c.name, value: c.articleCount }))))
  requestAnimationFrame(() => {
    viewsChart?.resize()
    countsChart?.resize()
  })
}

function onResize() {
  viewsChart?.resize()
  countsChart?.resize()
}

onMounted(async () => {
  try {
    const [o, t, cs] = await Promise.all([
      getStatsOverviewApi(),
      getTopArticlesApi(),
      getCategoryStatsApi()
    ])
    overview.value = o
    topList.value = t
    categoryStats.value = cs
    await nextTick()
    renderCharts()
    await nextTick()
    computeRowHeight()
    startTableObserver()
    window.addEventListener('resize', onResize)
  } catch {
    // 错误提示由请求拦截器统一处理
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  tableObserver?.disconnect()
  viewsChart?.dispose()
  countsChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 58px - 40px);
  min-height: 520px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 14px;
  flex-shrink: 0;
}

.stat-card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 14px 18px;
  box-shadow: 0 8px 30px rgba(217, 167, 22, 0.08);
}

.stat-label {
  color: var(--app-text-secondary);
  font-size: 12px;
}

.stat-value {
  margin-top: 4px;
  font-size: 24px;
  font-weight: 700;
  color: #6b5208;
}

.dashboard-main {
  display: flex;
  gap: 14px;
  align-items: stretch;
  width: 100%;
  flex: 1;
  min-height: 0;
}

.top-card {
  flex: 0 0 auto;
  min-width: 0;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
}

.top-card .el-table {
  width: 390px;
  flex: 1;
  min-height: 0;
}

.charts {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chart-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 12px 14px;
  box-shadow: 0 8px 30px rgba(217, 167, 22, 0.08);
}

.section-title {
  margin: 0 0 8px;
  color: #6b5208;
  font-size: 15px;
}

.chart-box {
  flex: 1;
  min-height: 0;
  width: 100%;
}

.empty-tip {
  text-align: center;
  color: var(--app-text-secondary);
  padding: 14px 0;
}
</style>
