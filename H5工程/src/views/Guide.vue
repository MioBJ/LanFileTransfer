<template>
  <div class="guide-page">
    <!-- 背景装饰 -->
    <div class="guide-bg">
      <div class="guide-bg__circle guide-bg__circle--1" />
      <div class="guide-bg__circle guide-bg__circle--2" />
      <div class="guide-bg__circle guide-bg__circle--3" />
    </div>

    <!-- 内容区域 -->
    <div class="guide-content">
      <!-- Logo动画区域 -->
      <div class="guide-logo animate-fade-in-up stagger-1 animate-backwards">
        <div class="guide-logo__icon animate-float">
          <AppIcon name="sun" :size="80" color="#FFFFFF" />
        </div>
      </div>

      <!-- 标题 -->
      <h1 class="guide-title animate-fade-in-up stagger-2 animate-backwards">
        开心快传
      </h1>
      
      <p class="guide-subtitle animate-fade-in-up stagger-3 animate-backwards">
        轻松传输你的珍贵回忆<br>
        照片、视频，一键送达
      </p>

      <!-- 功能介绍卡片 -->
      <GlassCard class="guide-features animate-fade-in-up stagger-4 animate-backwards">
        <div class="feature-list">
          <div class="feature-item">
            <div class="feature-icon">
              <AppIcon name="image" :size="24" color="var(--color-primary)" />
            </div>
            <div class="feature-text">
              <span class="feature-title">选择照片视频</span>
              <span class="feature-desc">支持批量选择相册内容</span>
            </div>
          </div>
          
          <div class="feature-item">
            <div class="feature-icon">
              <AppIcon name="server" :size="24" color="var(--color-primary)" />
            </div>
            <div class="feature-text">
              <span class="feature-title">连接家庭服务器</span>
              <span class="feature-desc">同一WiFi环境下自动发现</span>
            </div>
          </div>
          
          <div class="feature-item">
            <div class="feature-icon">
              <AppIcon name="zap" :size="24" color="var(--color-primary)" />
            </div>
            <div class="feature-text">
              <span class="feature-title">一键快速传输</span>
              <span class="feature-desc">断点续传、秒传、大文件支持</span>
            </div>
          </div>
        </div>
      </GlassCard>

      <!-- 开始按钮 -->
      <div class="guide-action animate-fade-in-up stagger-5 animate-backwards">
        <GradientButton 
          type="white" 
          size="large" 
          block 
          :breathe="true"
          @click="handleStart"
        >
          开始使用
        </GradientButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { AppIcon, GlassCard, GradientButton } from '../components'

const router = useRouter()

const handleStart = () => {
  router.push({
    path: '/settings',
    query: { from: 'guide' }
  })
}
</script>

<style scoped>
.guide-page {
  height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-xl);
  background: var(--gradient-sunset);
  position: relative;
  overflow: hidden;
}

/* 背景装饰圆形 */
.guide-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.guide-bg__circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.guide-bg__circle--1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -80px;
  animation: float 8s ease-in-out infinite;
}

.guide-bg__circle--2 {
  width: 200px;
  height: 200px;
  bottom: 10%;
  left: -60px;
  animation: float 6s ease-in-out infinite reverse;
}

.guide-bg__circle--3 {
  width: 150px;
  height: 150px;
  top: 40%;
  right: -40px;
  animation: float 7s ease-in-out infinite;
  animation-delay: 1s;
}

/* 内容区域 */
.guide-content {
  width: 100%;
  max-width: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  z-index: 1;
}

/* Logo */
.guide-logo {
  margin-bottom: var(--spacing-2xl);
}

.guide-logo__icon {
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(255, 107, 53, 0.3);
}

/* 标题 */
.guide-title {
  font-family: var(--font-family-title);
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-white);
  margin: 0 0 var(--spacing-md);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  letter-spacing: 4px;
}

.guide-subtitle {
  font-size: var(--font-size-md);
  color: rgba(255, 255, 255, 0.9);
  text-align: center;
  line-height: var(--line-height-relaxed);
  margin: 0 0 var(--spacing-2xl);
}

/* 功能卡片 */
.guide-features {
  width: 100%;
  margin-bottom: var(--spacing-2xl);
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.feature-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.feature-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 107, 53, 0.1);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.feature-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.feature-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

/* 操作按钮 */
.guide-action {
  width: 100%;
}

/* 动画定义 */
@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20px);
  }
}
</style>
