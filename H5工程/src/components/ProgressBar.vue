<template>
  <div class="progress-bar">
    <div class="progress-bar__track">
      <div 
        class="progress-bar__fill"
        :class="{ 'progress-bar__fill--animated': animated }"
        :style="{ width: `${clampedProgress}%` }"
      />
    </div>
    <div v-if="showText" class="progress-bar__info">
      <span class="progress-bar__percentage">{{ clampedProgress }}%</span>
      <span v-if="speed && speed > 0" class="progress-bar__speed">
        <AppIcon name="zap" :size="12" />
        {{ formatSpeed(speed) }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps<{
  progress: number
  speed?: number
  showText?: boolean
  animated?: boolean
}>()

const clampedProgress = computed(() => {
  return Math.min(100, Math.max(0, Math.round(props.progress)))
})

const formatSpeed = (bytesPerSecond: number): string => {
  if (bytesPerSecond < 1024) {
    return `${bytesPerSecond.toFixed(0)} B/s`
  } else if (bytesPerSecond < 1024 * 1024) {
    return `${(bytesPerSecond / 1024).toFixed(1)} KB/s`
  } else {
    return `${(bytesPerSecond / (1024 * 1024)).toFixed(1)} MB/s`
  }
}
</script>

<style scoped>
.progress-bar {
  width: 100%;
}

.progress-bar__track {
  width: 100%;
  height: 8px;
  background: rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-bar__fill {
  height: 100%;
  background: var(--gradient-progress);
  background-size: 200% 100%;
  border-radius: var(--radius-full);
  transition: width var(--duration-normal) var(--ease-out);
}

.progress-bar__fill--animated {
  animation: progressShimmer 2s ease-in-out infinite;
}

.progress-bar__info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--spacing-xs);
  font-size: var(--font-size-sm);
}

.progress-bar__percentage {
  color: var(--color-text-secondary);
  font-family: var(--font-family-number);
}

.progress-bar__speed {
  display: flex;
  align-items: center;
  gap: 2px;
  color: var(--color-primary);
  font-family: var(--font-family-number);
}

@keyframes progressShimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}
</style>
