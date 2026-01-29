<template>
  <span
    class="status-tag"
    :class="[
      `status-tag--${status}`,
      { 'status-tag--pulse': pulse }
    ]"
  >
    <AppIcon 
      v-if="showIcon" 
      :name="iconName" 
      :size="12" 
      class="status-tag__icon"
    />
    <span class="status-tag__text">{{ text || statusText }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps<{
  status: 'uploading' | 'waiting' | 'paused' | 'success' | 'failed'
  text?: string
  showIcon?: boolean
  pulse?: boolean
}>()

const statusText = computed(() => {
  const textMap: Record<string, string> = {
    uploading: '上传中',
    waiting: '等待中',
    paused: '已暂停',
    success: '已完成',
    failed: '失败'
  }
  return textMap[props.status] || props.status
})

const iconName = computed(() => {
  const iconMap: Record<string, string> = {
    uploading: 'loader',
    waiting: 'loader',
    paused: 'pause',
    success: 'check',
    failed: 'alert-circle'
  }
  return iconMap[props.status] || 'info'
})
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: 4px 10px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  line-height: 1;
  white-space: nowrap;
}

/* 上传中 */
.status-tag--uploading {
  background: var(--color-primary);
  color: var(--color-text-white);
}

.status-tag--uploading .status-tag__icon {
  animation: spin 1s linear infinite;
}

/* 等待中 */
.status-tag--waiting {
  background: var(--color-warning);
  color: var(--color-text-primary);
}

/* 已暂停 */
.status-tag--paused {
  background: var(--color-muted);
  color: var(--color-text-white);
}

/* 已完成 */
.status-tag--success {
  background: var(--color-success);
  color: var(--color-text-white);
}

/* 失败 */
.status-tag--failed {
  background: var(--color-danger);
  color: var(--color-text-white);
}

/* 脉冲效果 */
.status-tag--pulse {
  animation: pulse 1.5s ease-in-out infinite;
}

.status-tag__icon {
  flex-shrink: 0;
}

.status-tag__text {
  /* 默认样式 */
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}
</style>
