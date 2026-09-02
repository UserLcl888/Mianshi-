<template>
  <div class="auth-page">
    <!-- 背景插画层 -->
    <div class="auth-bg" aria-hidden="true"></div>
    <!-- 暗色压暗层，保证前景可读 -->
    <div class="auth-scrim" aria-hidden="true"></div>

    <div class="auth-shell">
      <div class="auth-card">
        <!-- 左：品牌宣传区 -->
        <aside class="auth-brand">
        <div class="brand-head">
          <span class="brand-logo">
            <svg viewBox="0 0 26 26" width="27" height="27" fill="none" stroke="#ffffff" stroke-width="1.6" stroke-linejoin="round">
              <path d="M13 2.4 21.6 7 13 11.6 4.4 7Z" />
              <path d="M13 9.6 21.6 14.2 13 18.8 4.4 14.2Z" />
              <path d="M13 16.8 21.6 21.4 13 26 4.4 21.4Z" />
            </svg>
          </span>
          <div class="brand-text">
            <div class="brand-name">知识分享</div>
            <div class="brand-sub">JAVA · AI · 场景 · 工具 · 计算机基础 · 专题</div>
          </div>
        </div>

        <div class="brand-main">
          <div class="slogan">
            <div class="slogan-line">每一次坚持，</div>
            <div class="slogan-line">
              <span class="slogan-plain">都在为</span>
              <span class="slogan-accent">更好的自己</span>
              <span class="slogan-plain">铺路</span>
            </div>
          </div>
          <p class="brand-desc">
            这里收录了作者总结的 Java 相关技术栈与 AI 问题、业务场景题、实用文档与工具<br />
            计算机四大核心知识，以及本人面试经验和经历，同时欢迎用户分享
          </p>
        </div>

        <div class="feature-grid">
          <div v-for="f in features" :key="f.name" class="feature-card">
            <span class="feature-icon" :style="{ background: f.bg, color: f.color }" v-html="f.icon"></span>
            <div class="feature-body">
              <div class="feature-name">{{ f.name }}</div>
              <div class="feature-desc">{{ f.desc }}</div>
            </div>
            <span class="feature-arrow">→</span>
          </div>
        </div>

        <div class="column-entry">
          <router-link to="/home" class="entry-card">
            <div class="entry-name">
              技术问题专栏
              <span class="entry-arrow">→</span>
            </div>
            <div class="entry-desc">面试题、知识点与实战题汇总</div>
          </router-link>
          <router-link to="/learn" class="entry-card">
            <div class="entry-name">
              学习专题
              <span class="entry-arrow">→</span>
            </div>
            <div class="entry-desc">我的学习笔记与代码实践</div>
          </router-link>
          <router-link to="/articles" class="entry-card">
            <div class="entry-name">
              文章专栏
              <span class="entry-arrow">→</span>
            </div>
            <div class="entry-desc">专题文章与分享，未登录也可浏览</div>
          </router-link>
          <router-link to="/author" class="entry-card">
            <div class="entry-name">
              作者
              <span class="entry-arrow">→</span>
            </div>
            <div class="entry-desc">认识我：技术、项目与成长</div>
          </router-link>
        </div>

        <div class="notice-bar">
          <span class="notice-icon">i</span>
          <span>未登录可浏览技术问题、学习专题、文章专栏与作者，注册登录解锁更多</span>
        </div>
        </aside>

        <!-- 右：表单面板 -->
        <main class="auth-form-side">
          <div class="auth-panel">
            <div class="form-head">
              <span class="form-star">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
                  <path
                    d="M12 2.2c.62 5.78 2.02 7.18 7.8 7.8-5.78.62-7.18 2.02-7.8 7.8-.62-5.78-2.02-7.18-7.8-7.8 5.78-.62 7.18-2.02 7.8-7.8Z"
                  />
                </svg>
              </span>
              <h1 class="form-title">{{ title }}</h1>
              <p class="form-subtitle">{{ subtitle }}</p>
            </div>
            <slot />
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    subtitle?: string
  }>(),
  {
    title: '一起加油，一起进步',
    subtitle: 'Login to your secure learning dashboard'
  }
)

const features = [
  {
    name: 'Java 技术栈',
    desc: '深入 Java 核心技术、覆盖并发、JVM、Spring、MyBatis 等主流技术栈。',
    bg: 'rgba(140, 165, 200, 0.16)',
    color: '#a8c4e8',
    icon: '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"><path d="M4 7.6 12 3l8 4.6-8 4.6-8-4.6Z"/><path d="M4 12.2 12 16.8l8-4.6"/><path d="M4 16.8 12 21.4l8-4.6"/></svg>'
  },
  {
    name: 'AI 问题与实践',
    desc: '整理 AI 大模型、RAG、Prompt、LangChain4j 等问题与实践解答。',
    bg: 'rgba(150, 105, 245, 0.16)',
    color: '#b9a0ff',
    icon: '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M9.6 4.6c-1.9 0-3.3 1.35-3.6 3.05-.85.3-1.55 1.05-1.75 2.1-.32 1.45.4 2.8 1.65 3.5-.2.72-.1 1.55.45 2.25.5.72 1.4 1.12 2.3 1.05.2 1.35 1.3 2.45 2.75 2.45 1.55 0 2.8-1.25 2.8-2.8V7c0-1.1-.9-2-2-2h-.6Z"/><path d="M14.4 4.6c1.9 0 3.3 1.35 3.6 3.05.85.3 1.55 1.05 1.75 2.1.32 1.45-.4 2.8-1.65 3.5.2.72.1 1.55-.45 2.25-.5.72-1.4 1.12-2.3 1.05-.2 1.35-1.3 2.45-2.75 2.45-1.55 0-2.8-1.25-2.8-2.8"/></svg>'
  },
  {
    name: '业务场景题',
    desc: '收录各类业务场景题及解决方案，涵盖高并发、分布式、推荐等场景。',
    bg: 'rgba(80, 185, 135, 0.16)',
    color: '#7fdcae',
    icon: '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M3.5 8a1.5 1.5 0 0 1 1.5-1.5h4l2 2.5h8A1.5 1.5 0 0 1 20.5 10.5V17A1.5 1.5 0 0 1 19 18.5H5A1.5 1.5 0 0 1 3.5 17V8Z"/><path d="M8.3 14.2 10.5 16.4l5.2-5.2"/></svg>'
  },
  {
    name: '实用文档与工具',
    desc: '精选技术文档、开发工具与效率提升资源，助力学习与实践。',
    bg: 'rgba(232, 154, 31, 0.16)',
    color: '#efb862',
    icon: '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a4.5 4.5 0 0 0-5.6 5.5L3 17.8 6.2 21l6-6.1a4.5 4.5 0 0 0 5.5-5.6l-3 3-2.8-2.8 2.8-3.2Z"/><path d="M19 3h2v2.5l-6.5 6.5"/></svg>'
  },
  {
    name: '计算机四大核心',
    desc: '计算机组成原理、操作系统、网络、数据结构与算法全面总结。',
    bg: 'rgba(75, 140, 245, 0.16)',
    color: '#7db4ff',
    icon: '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"><rect x="7" y="7" width="10" height="10" rx="2"/><path d="M9.5 2.5V5M14.5 2.5V5M9.5 19V21.5M14.5 19V21.5M2.5 9.5H5M2.5 14.5H5M19 9.5H21.5M19 14.5H21.5"/><rect x="10" y="10" width="4" height="4"/></svg>'
  },
  {
    name: '面试经验与经历',
    desc: '分享真实面试经历、面试题总结与心路，助力面试提升与职业发展。',
    bg: 'rgba(240, 110, 165, 0.16)',
    color: '#ff9fc4',
    icon: '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="7.8" r="3.1"/><path d="M3.6 20c.65-3.4 2.75-5.1 5.4-5.1s4.75 1.7 5.4 5.1"/><path d="M15.2 8.6a4.4 4.4 0 0 1 .4 7.1l-1.5 1.5.4-2.1a4.4 4.4 0 1 1 .7-6.5Z"/></svg>'
  }
]
</script>

<style scoped>
.auth-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  overflow: hidden;
  background:
    radial-gradient(1100px 720px at 18% 8%, rgba(31, 47, 74, 0.55) 0%, rgba(31, 47, 74, 0) 62%),
    radial-gradient(760px 560px at 88% 88%, rgba(20, 30, 48, 0.5) 0%, rgba(20, 30, 48, 0) 60%),
    linear-gradient(135deg, #0a0e17 0%, #070b12 45%, #05080f 100%);
}

/* 背景插画（左侧为主，向右渐隐） */
.auth-bg {
  position: absolute;
  inset: 0;
  background: url('/auth-bg.png') center / cover no-repeat;
  opacity: 0.88;
  filter: brightness(1.34) contrast(1.07) saturate(1.08);
  -webkit-mask-image: linear-gradient(to right, #000 0%, #000 58%, rgba(0, 0, 0, 0.65) 74%, transparent 92%);
  mask-image: linear-gradient(to right, #000 0%, #000 58%, rgba(0, 0, 0, 0.65) 74%, transparent 92%);
  pointer-events: none;
}

/* 压暗渐变，保证文字可读 */
.auth-scrim {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to right,
    rgba(5, 8, 15, 0.05) 0%,
    rgba(5, 8, 15, 0.16) 40%,
    rgba(7, 11, 18, 0.55) 70%,
    rgba(7, 11, 18, 0.82) 100%
  );
  pointer-events: none;
}

.auth-shell {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.auth-card {
  display: flex;
  align-items: stretch;
  justify-content: center;
  gap: 28px;
  width: 100%;
  max-width: 1240px;
  padding: 0;
}

/* ---------------- 左：品牌区 ---------------- */
.auth-brand {
  flex: 1 1 60%;
  max-width: 820px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
}

.brand-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 12px;
}

.brand-logo {
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.055);
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.35);
}

.brand-name {
  font-size: 26px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 3px;
}

.brand-sub {
  margin-top: 3px;
  font-size: 13px;
  color: #9aa3b5;
  letter-spacing: 1px;
}

.brand-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.slogan-line {
  font-size: clamp(26px, 2.4vw, 36px);
  font-weight: 700;
  line-height: 1.28;
  letter-spacing: 1px;
  color: #ffffff;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.55);
}

.slogan-accent {
  background: linear-gradient(92deg, #f2933e 0%, #efab36 48%, #e89a1f 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 24px rgba(255, 154, 68, 0.28);
}

.brand-desc {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.85;
  color: #b0b8c4;
  max-width: 640px;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.55);
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.feature-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 14px;
  background: rgba(26, 31, 46, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.07);
  backdrop-filter: blur(8px);
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.feature-card:hover {
  transform: translateY(-2px);
  border-color: rgba(232, 154, 31, 0.45);
  background: rgba(31, 38, 56, 0.85);
}

.feature-icon {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.feature-body {
  min-width: 0;
}

.feature-name {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
}

.feature-desc {
  margin-top: 4px;
  font-size: 11.5px;
  line-height: 1.55;
  color: #aab2c0;
}

.feature-arrow {
  position: absolute;
  right: 10px;
  bottom: 7px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.07);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.68);
}

.column-entry {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.entry-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 11px 13px;
  border-radius: 14px;
  background: rgba(232, 154, 31, 0.08);
  border: 1px solid rgba(232, 154, 31, 0.28);
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.entry-card:hover {
  transform: translateY(-1px);
  border-color: rgba(232, 154, 31, 0.6);
  background: rgba(232, 154, 31, 0.14);
}

.entry-name {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
}

.entry-desc {
  font-size: 11.5px;
  line-height: 1.55;
  color: #b8c0cf;
}

.entry-arrow {
  color: var(--app-accent);
  font-size: 13px;
}

.notice-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.045);
  border: 1px solid rgba(255, 255, 255, 0.07);
  color: #a0a8b4;
  font-size: 12.5px;
}

.notice-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.32);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-style: normal;
  font-size: 11px;
  font-weight: 600;
  color: #cfd6e2;
}

/* ---------------- 右：表单面板 ---------------- */
.auth-form-side {
  flex: 0 0 auto;
  width: min(460px, 100%);
  display: flex;
  align-items: stretch;
}

.auth-panel {
  width: 100%;
  height: 100%;
  padding: 38px 36px 32px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-radius: 22px;
  background: rgba(18, 24, 38, 0.62);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 10px 36px rgba(0, 0, 0, 0.38);
}

.form-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-bottom: 10px;
}

.form-star {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #f2a82e;
  background: radial-gradient(circle, rgba(242, 168, 46, 0.28) 0%, rgba(232, 154, 31, 0.06) 68%, transparent 100%);
  border: 1px solid rgba(242, 168, 46, 0.32);
  box-shadow: 0 0 30px rgba(232, 154, 31, 0.28);
  margin-bottom: 14px;
}

.form-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 1px;
  line-height: 1.35;
}

.form-subtitle {
  margin: 9px 0 0;
  font-size: 13px;
  color: #8a9bb5;
}

@media (max-width: 1080px) {
  .auth-page {
    padding: 20px;
  }

  .auth-card {
    gap: 30px;
  }

  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 860px) {
  .auth-page {
    overflow: auto;
    padding: 18px 14px;
  }

  .auth-card {
    flex-direction: column;
    gap: 22px;
  }

  .auth-brand {
    max-width: 560px;
    gap: 18px;
  }

  .auth-bg {
    opacity: 0.3;
  }

  .auth-form-side {
    width: 100%;
    max-width: 460px;
  }
}
</style>
