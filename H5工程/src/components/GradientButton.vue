<template>
  <button
    class="gradient-button"
    :class="[
      `gradient-button--${type}`,
      `gradient-button--${size}`,
      { 'gradient-button--block': block },
      { 'gradient-button--loading': loading },
      { 'gradient-button--disabled': disabled },
      { 'gradient-button--breathe': breathe && !loading && !disabled }
    ]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <!-- 加载状态 -->
    <span v-if="loading" class="gradient-button__loading">
      <AppIcon name="loader" :size="18" class="animate-spin" />
    </span>
    
    <!-- 图标 -->
    <AppIcon 
      v-if="icon && !loading" 
      :name="icon" 
      :size="iconSize" 
      class="gradient-button__icon"
    />
    
    <!-- 文字 -->
    <span class="gradient-button__text">
      <slot />
    </span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps<{
  type?: 'primary' | 'secondary' | 'danger' | 'ghost' | 'white'
  size?: 'small' | 'medium' | 'large'
  icon?: string
  block?: boolean
  loading?: boolean
  disabled?: boolean
  breathe?: boolean
}>()

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
}>()

const iconSize = computed(() => {
  switch (props.size) {
    case 'small': return 14
    case 'large': return 20
    default: return 16
  }
})

const handleClick = (event: MouseEvent) => {
  if (!props.disabled && !props.loading) {
    emit('click', event)
  }
}
</script>

<style scoped>
.gradient-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  border: none;
  border-radius: var(--radius-lg);
  font-family: var(--font-family-body);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out);
  white-space: nowrap;
  -webkit-tap-highlight-color: transparent;
}

/* ===== 类型变体 ===== */

/* 主按钮 */
.gradient-button--primary {
  background: var(--gradient-button);
  color: var(--color-text-white);
  box-shadow: var(--shadow-button);
}

.gradient-button--primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-button-hover);
}

.gradient-button--primary:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(255, 107, 53, 0.25);
}

/* 次按钮 */
.gradient-button--secondary {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid rgba(255, 107, 53, 0.3);
  color: var(--color-primary);
}

.gradient-button--secondary:hover:not(:disabled) {
  background: rgba(255, 107, 53, 0.1);
  border-color: var(--color-primary);
}

.gradient-button--secondary:active:not(:disabled) {
  background: rgba(255, 107, 53, 0.15);
}

/* 危险按钮 */
.gradient-button--danger {
  background: transparent;
  border: 1px solid var(--color-danger);
  color: var(--color-danger);
}

.gradient-button--danger:hover:not(:disabled) {
  background: rgba(255, 59, 92, 0.1);
}

.gradient-button--danger:active:not(:disabled) {
  background: rgba(255, 59, 92, 0.15);
}

/* 幽灵按钮 */
.gradient-button--ghost {
  background: transparent;
  color: var(--color-text-secondary);
}

.gradient-button--ghost:hover:not(:disabled) {
  color: var(--color-primary);
  background: rgba(255, 107, 53, 0.08);
}

/* 白色按钮 */
.gradient-button--white {
  background: rgba(255, 255, 255, 0.9);
  color: var(--color-primary);
  box-shadow: var(--shadow-md);
}

.gradient-button--white:hover:not(:disabled) {
  background: #fff;
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.gradient-button--white:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
}

/* ===== 尺寸变体 ===== */

.gradient-button--small {
  height: 32px;
  padding: 0 var(--spacing-md);
  font-size: var(--font-size-sm);
  border-radius: var(--radius-sm);
}

.gradient-button--medium {
  height: 44px;
  padding: 0 var(--spacing-xl);
  font-size: var(--font-size-base);
}

.gradient-button--large {
  height: 50px;
  padding: 0 var(--spacing-2xl);
  font-size: var(--font-size-md);
}

/* ===== 修饰符 ===== */

.gradient-button--block {
  display: flex;
  width: 100%;
}

.gradient-button--loading {
  pointer-events: none;
  opacity: 0.8;
}

.gradient-button--disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.gradient-button--breathe {
  animation: breathe 3s ease-in-out infinite;
}

/* ===== 子元素 ===== */

.gradient-button__loading {
  display: flex;
  align-items: center;
}

.gradient-button__icon {
  flex-shrink: 0;
}

.gradient-button__text {
  flex: 1;
}

/* ===== 动画 ===== */

@keyframes breathe {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.02);
  }
}
</style>
