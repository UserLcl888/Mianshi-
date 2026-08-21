<template>
  <div class="auth-page">
    <div class="auth-card">
      <!-- 左：品牌区 -->
      <aside class="auth-brand">
        <div class="brand-head">
          <img src="/logo.png" alt="logo" class="brand-logo" />
          <div class="brand-text">
            <div class="brand-name">知识分享平台</div>
            <div class="brand-sub">Java · AI · 场景 · 工具 · 计算机基础</div>
          </div>
        </div>

        <div class="brand-main">
          <div class="brand-slogan">
            <div class="slogan-sub">每一次坚持，</div>
            <div class="slogan-main">都在为更好的自己铺路</div>
          </div>
          <p class="brand-desc">
            这里收录作者总结的 Java 与 AI 问题、业务场景题、<br />
            实用文档与工具，以及计算机四大核心知识，持续更新。
          </p>
        </div>

        <div class="feature-grid">
          <div class="feature-card">
            <span class="feature-icon"><el-icon><Cpu /></el-icon></span>
            <div class="feature-name">Java 和 AI 问题</div>
            <div class="feature-desc">高频考点、原理梳理与实战问答</div>
          </div>
          <div class="feature-card">
            <span class="feature-icon"><el-icon><Opportunity /></el-icon></span>
            <div class="feature-name">场景问题</div>
            <div class="feature-desc">抢单、优惠券等真实业务场景题</div>
          </div>
          <div class="feature-card">
            <span class="feature-icon"><el-icon><Tools /></el-icon></span>
            <div class="feature-name">文档和工具</div>
            <div class="feature-desc">好用的文档链接与工具合集</div>
          </div>
          <div class="feature-card">
            <span class="feature-icon"><el-icon><Monitor /></el-icon></span>
            <div class="feature-name">计算机四大核心</div>
            <div class="feature-desc">网络、操作系统、数据结构等基础</div>
          </div>
        </div>

        <button class="guest-btn" type="button" @click="goHome">
          <span>不登录仅能以游客身份浏览首页</span>
          <el-icon class="guest-arrow"><Right /></el-icon>
        </button>
      </aside>

      <!-- 右：表单区 -->
      <main class="auth-form-panel">
        <div class="form-head">
          <span class="form-head-icon"><el-icon><Sunny /></el-icon></span>
          <span class="form-head-text">{{ greeting }}</span>
        </div>
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Cpu, Opportunity, Tools, Monitor, Right, Sunny } from '@element-plus/icons-vue'

withDefaults(defineProps<{ greeting?: string }>(), {
  greeting: '一起加油，一起进步'
})

const router = useRouter()

function goHome() {
  router.push('/')
}
</script>

<style scoped>
/* 流光边框角度属性（支持旋转动画的渐变） */
@property --border-angle {
  syntax: '<angle>';
  initial-value: 0deg;
  inherits: false;
}

.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-bg);
  padding: 24px;
}

.auth-card {
  position: relative;
  display: flex;
  width: 880px;
  max-width: 100%;
  min-height: 560px;
  background: var(--app-card);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 40px rgba(217, 167, 22, 0.14);
}

/* 四周边框流光（mask 只保留外圈 1.5px 的渐变环） */
.auth-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  padding: 1.5px;
  background: conic-gradient(
    from var(--border-angle),
    #e9d28e,
    #d9a716,
    #fdf3d8,
    #d9a716,
    #e9d28e
  );
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  mask-composite: exclude;
  animation: border-spin 5s linear infinite;
  pointer-events: none;
}

@keyframes border-spin {
  to {
    --border-angle: 360deg;
  }
}

/* 不支持 mask 时退化为普通边框 */
@supports not ((mask-composite: exclude) or (-webkit-mask-composite: xor)) {
  .auth-card {
    border: 1px solid var(--app-border);
  }
}

/* 左品牌区 */
.auth-brand {
  width: 50%;
  display: flex;
  flex-direction: column;
  gap: 22px;
  padding: 34px 28px;
  background: var(--app-accent-soft);
}

.brand-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid var(--app-border);
  box-shadow: 0 4px 12px rgba(217, 167, 22, 0.18);
  flex-shrink: 0;
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  color: #6b5208;
}

.brand-sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.brand-slogan {
  padding: 2px 0 4px;
}

.slogan-sub {
  font-size: 15px;
  letter-spacing: 2px;
  color: var(--app-text-secondary);
}

.slogan-main {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  color: #6b5208;
  line-height: 1.4;
  letter-spacing: 1px;
  position: relative;
}

.slogan-main::after {
  content: '';
  display: block;
  margin-top: 12px;
  width: 46px;
  height: 3px;
  border-radius: 2px;
  background: var(--app-accent);
}

.brand-desc {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--app-text-secondary);
}

.feature-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.feature-card {
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 12px;
  transition: all 0.2s;
}

.feature-card:hover {
  border-color: var(--app-accent);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(217, 167, 22, 0.14);
}

.feature-icon {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #a87f18;
  background: var(--app-accent-soft);
}

.feature-name {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text);
}

.feature-desc {
  margin-top: 3px;
  font-size: 11px;
  line-height: 1.5;
  color: var(--app-text-secondary);
}

.guest-btn {
  margin-top: auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 10px 14px;
  border: 1px solid rgba(217, 167, 22, 0.4);
  border-radius: 8px;
  background: rgba(217, 167, 22, 0.1);
  color: #a87f18;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.guest-btn:hover {
  background: rgba(217, 167, 22, 0.18);
  border-color: var(--app-accent);
}

.guest-arrow {
  transition: transform 0.2s;
}

.guest-btn:hover .guest-arrow {
  transform: translateX(3px);
}

/* 右表单区 */
.auth-form-panel {
  position: relative;
  width: 50%;
  padding: 42px 36px;
  display: flex;
  flex-direction: column;
}

.form-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.form-head-icon {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #fff;
  background: var(--app-accent-soft);
  background: linear-gradient(135deg, #d9a716, #f0cd6a);
  border: 1px solid rgba(217, 167, 22, 0.5);
  animation: icon-pulse 1.8s ease-in-out infinite;
}

.form-head-text {
  font-size: 17px;
  font-weight: 600;
  color: #6b5208;
  animation: text-fade 0.6s ease both;
}

@keyframes icon-pulse {
  0%,
  100% {
    transform: scale(1) rotate(0deg);
    box-shadow: 0 0 0 0 rgba(217, 167, 22, 0.4);
  }
  50% {
    transform: scale(1.16) rotate(8deg);
    box-shadow: 0 0 0 12px rgba(217, 167, 22, 0);
  }
}

@keyframes text-fade {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 767px) {
  .auth-brand {
    display: none;
  }

  .auth-card {
    width: 94%;
    min-height: 0;
  }

  .auth-form-panel {
    width: 100%;
    padding: 30px 22px;
  }
}
</style>
