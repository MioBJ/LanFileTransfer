<template>
  <div 
    class="glass-card"
    :class="[
      `glass-card--${variant}`,
      { 'glass-card--hoverable': hoverable },
      { 'glass-card--clickable': clickable }
    ]"
    :style="cardStyle"
    @click="handleClick"
  >
    <!-- 标题区域 -->
    <div v-if="title || $slots.header" class="glass-card__header">
      <slot name="header">
        <div class="glass-card__title">
          <AppIcon v-if="icon" :name="icon" :size="20" class="glass-card__icon" />
          <span>{{ title }}</span>
        </div>
      </slot>
    </div>

    <!-- 内容区域 -->
    <div class="glass-card__body">
      <slot />
    </div>

    <!-- 底部区域 -->
    <div v-if="$slots.footer" class="glass-card__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps<{
  title?: string
  icon?: string
  variant?: 'default' | 'uploading' | 'waiting' | 'failed' | 'success' | 'paused'
  hoverable?: boolean
  clickable?: boolean
  padding?: string
  noPadding?: boolean
}>()

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
}>()

const cardStyle = computed(() => {
  if (props.noPadding) {
    return { padding: '0' }
  }
  if (props.padding) {
    return { padding: props.padding }
  }
  return {}
})

const handleClick = (event: MouseEvent) => {
  if (props.clickable) {
    emit('click', event)
  }
}
</script>

<style scoped>
.glass-card {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-light);
  border-radius: var(--radius-xl);
  box-shadow: var(--glass-shadow-soft);
  padding: var(--spacing-lg);
  transition: all var(--duration-normal) var(--ease-out);
}

/* 可悬浮 */
.glass-card--hoverable:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
}

/* 可点击 */
.glass-card--clickable {
  cursor: pointer;
}

.glass-card--clickable:active {
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
}

/* 状态变体 - 左侧彩条 */
.glass-card--uploading {
  border-left: 4px solid var(--color-primary);
}

.glass-card--waiting {
  border-left: 4px solid var(--color-warning);
}

.glass-card--failed {
  border-left: 4px solid var(--color-danger);
}

.glass-card--success {
  border-left: 4px solid var(--color-success);
}

.glass-card--paused {
  border-left: 4px solid var(--color-muted);
}

/* 标题区域 */
.glass-card__header {
  margin-bottom: var(--spacing-md);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--glass-border);
}

.glass-card__title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-family: var(--font-family-title);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.glass-card__icon {
  color: var(--color-primary);
}

/* 内容区域 */
.glass-card__body {
  /* 默认无样式 */
}

/* 底部区域 */
.glass-card__footer {
  margin-top: var(--spacing-md);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--glass-border);
}
</style>
