<template>
  <div class="author-page">
    <AppHeader />
    <div class="author-body">
      <!-- 左侧目录 -->
      <aside class="author-nav">
        <div class="nav-title">目录</div>
        <a
          v-for="item in nav"
          :key="item.id"
          class="nav-item"
          :class="{ active: activeNav === item.id }"
          href="#"
          @click.prevent="go(item.id)"
        >
          <span class="nav-dot">●</span>
          {{ item.label }}
        </a>
      </aside>

      <!-- 主内容 -->
      <main class="author-main">
        <section class="author-hero">
          <img src="/logo.png" alt="avatar" class="avatar" />
          <div class="tagline">JAVA &amp; AI</div>
          <h1 class="name">笨笨的派大星</h1>
          <p class="intro">全栈开发者，AI 爱好玩家，Vibcoding 初级用户</p>
        </section>

        <section id="about" class="author-card">
          <h2 class="card-title">关于我</h2>
          <p class="card-text">
            你好，我是<strong>笨笨的派大星</strong>，一名全栈开发和 Agent 开发学习者。平时喜欢把学到的知识整理成技术点分享出来，也会分享一些文章内容，帮自己加深理解，也希望能帮到正在学习的人，大家一同进步。
          </p>
        </section>

        <section id="doing" class="author-card">
          <h2 class="card-title now">我正在做什么</h2>
          <ul class="doing-list">
            <li>搭建并维护这个知识分享平台，沉淀面试题与学习笔记</li>
            <li>整理 Java 后端的内容，贯通知识点和自我沉淀</li>
            <li>整理 AI 相关最佳实践，总结相关问题</li>
            <li>寻找实习中！！！</li>
          </ul>
        </section>

        <section id="skills" class="author-card">
          <h2 class="card-title">技术栈</h2>
          <div class="skill-list">
            <div v-for="s in skills" :key="s.name" class="skill-item">
              <b class="skill-name">{{ s.name }}</b>
              <div class="skill-chips">
                <span v-for="t in s.items" :key="t" class="chip">{{ t }}</span>
              </div>
            </div>
          </div>
        </section>

        <section id="projects" class="author-card">
          <h2 class="card-title">我的项目</h2>
          <div class="project-list">
            <div v-for="p in projects" :key="p.name" class="project-item">
              <div class="project-name">{{ p.name }}</div>
              <div class="project-desc">{{ p.desc }}</div>
            </div>
          </div>
        </section>

        <section id="hobbies" class="author-card">
          <h2 class="card-title">我的爱好</h2>
          <div class="hobby-list">
            <span v-for="h in hobbies" :key="h" class="hobby-item">{{ h }}</span>
          </div>
        </section>

        <section id="contact" class="author-card">
          <h2 class="card-title">联系我</h2>
          <div class="contact-list">
            <div class="contact-item">GitHub：https://github.com/UserLcl888</div>
            <div class="contact-item">邮箱：2090323327@qq.com</div>
            <div class="contact-item">微信：L19203681853（添加时备注：知识分享）</div>
          </div>
        </section>
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'

const nav = [
  { id: 'about', label: '关于我' },
  { id: 'doing', label: '正在做' },
  { id: 'skills', label: '技术栈' },
  { id: 'projects', label: '我的项目' },
  { id: 'hobbies', label: '我的爱好' },
  { id: 'contact', label: '联系我' }
]
const skills = [
  { name: '学习语言', items: ['Java', 'Python', 'SQL', 'C'] },
  { name: '语言框架', items: ['SSM', 'SpringCloud', 'FastAPI'] },
  { name: '存储和中间件', items: ['MySQL', 'Redis', 'MongoDB', 'RabbitMQ'] },
  { name: '辅助工具', items: ['Maven', 'Git', 'Docker', 'Linux'] },
  { name: '前端基础', items: ['HTML', 'CSS', 'JS/TS', 'Vue3'] },
  { name: 'AI应用', items: ['Prompt', 'RAG', 'Skill', 'MCP'] }
]
const projects = [
  { name: '知识分享', desc: '当前站点：面试、文章、学习专题一站式沉淀' },
  { name: '代驾', desc: '代驾平台：Java 后端实战项目，覆盖订单调度、派单、计价与结算' },
  { name: 'RAG', desc: '基于大模型检索增强生成的知识问答应用，梳理 RAG、Prompt 与 Agent 实践' }
]
const hobbies = ['看动漫', '学习技术', '折腾AI', '写网站内容', '睡觉摆烂']
const activeNav = ref('about')
let scrollTimer: number | null = null

function go(id: string) {
  activeNav.value = id
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function onScroll() {
  if (scrollTimer !== null) return
  scrollTimer = window.setTimeout(() => {
    scrollTimer = null
    let current = 'about'
    for (const item of nav) {
      const el = document.getElementById(item.id)
      if (el && el.getBoundingClientRect().top <= 140) {
        current = item.id
      }
    }
    activeNav.value = current
  }, 80)
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  if (scrollTimer !== null) {
    window.clearTimeout(scrollTimer)
  }
})
</script>

<style scoped>
.author-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.author-body {
  flex: 1;
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
  padding: 28px 24px 44px;
  display: flex;
  align-items: flex-start;
  gap: 28px;
}

.author-nav {
  width: 190px;
  flex-shrink: 0;
  position: sticky;
  top: 86px;
}

.nav-title {
  font-size: 13px;
  color: var(--app-text-secondary);
  padding: 8px 10px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--app-text);
  transition: background 0.15s, color 0.15s;
}

.nav-item:hover {
  color: var(--app-accent);
}

.nav-item.active {
  background: var(--app-accent-soft);
  color: var(--app-accent);
  font-weight: 600;
}

.nav-dot {
  font-size: 9px;
  color: var(--app-accent);
}

.author-main {
  flex: 1;
  min-width: 0;
}

.author-hero {
  text-align: center;
  padding: 22px 0 30px;
}

.avatar {
  width: 104px;
  height: 104px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--app-accent);
  box-shadow: 0 0 26px rgba(232, 154, 31, 0.35);
}

.tagline {
  margin-top: 14px;
  font-size: 12px;
  letter-spacing: 4px;
  color: var(--app-accent);
}

.name {
  margin: 8px 0 6px;
  font-size: 30px;
  font-weight: 700;
  color: var(--app-text);
  letter-spacing: 2px;
}

.intro {
  margin: 0;
  font-size: 14px;
  color: var(--app-text-secondary);
}

.tags {
  margin-top: 14px;
  display: flex;
  justify-content: center;
  gap: 10px;
}

.pill {
  padding: 5px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: #d8dee8;
  font-size: 12.5px;
}

.author-card {
  margin-bottom: 18px;
  padding: 22px 26px;
  border-radius: 14px;
  background: var(--app-card-translucent);
  border: 1px solid rgba(255, 255, 255, 0.08);
  scroll-margin-top: 90px;
}

.card-title {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 700;
  color: var(--app-accent);
}

.card-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.9;
  color: var(--app-text);
}

.card-text strong {
  color: var(--app-text);
}

.doing-list {
  margin: 0;
  padding-left: 18px;
  color: var(--app-text);
  font-size: 14px;
  line-height: 2;
}

.skill-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skill-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  font-size: 14px;
}

.skill-name {
  min-width: 150px;
  color: var(--app-text);
  font-size: 14px;
  line-height: 26px;
  flex-shrink: 0;
}

.skill-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  padding: 3px 12px;
  border-radius: 14px;
  background: var(--app-accent-soft);
  border: 1px solid rgba(232, 154, 31, 0.35);
  color: var(--app-accent);
  font-size: 12.5px;
  line-height: 20px;
  white-space: nowrap;
}

.project-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.project-item {
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.07);
}

.project-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text);
}

.project-desc {
  margin-top: 4px;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.hobby-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hobby-item {
  padding: 5px 14px;
  border-radius: 20px;
  background: var(--app-accent-soft);
  color: var(--app-accent);
  font-size: 12.5px;
}

.contact-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 14px;
  color: var(--app-text);
}

@media (max-width: 860px) {
  .author-body {
    flex-direction: column;
  }

  .author-nav {
    width: 100%;
    position: static;
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }

  .nav-title {
    width: 100%;
  }
}
</style>
