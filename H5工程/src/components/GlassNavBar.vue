<template>
  <div class="glass-navbar" :class="{ 'glass-navbar--transparent': transparent }">
    <div class="glass-navbar__content">
      <!-- 左侧 -->
      <div class="glass-navbar__left" @click="handleBack">
        <slot name="left">
          <template v-if="showBack">
            <AppIcon name="arrow-left" :size="22" />
            <span v-if="backText" class="glass-navbar__back-text">{{ backText }}</span>
          </template>
        </slot>
      </div>

      <!-- 标题 -->
      <div class="glass-navbar__title">
        <slot name="title">
          {{ title }}
        </slot>
      </div>

      <!-- 右侧 -->
      <div class="glass-navbar__right">
        <slot name="right" />
      </div>
    </div>
  </div>
  
  <!-- 占位元素 -->
  <div v-if="placeholder" class="glass-navbar__placeholder" />
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAttrs } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps<{
  title?: string
  showBack?: boolean
  backText?: string
  transparent?: boolean
  placeholder?: boolean
}>()

const emit = defineEmits<{
  (e: 'back'): void
}>()

const router = useRouter()
const attrs = useAttrs()

const handleBack = () => {
  if (props.showBack) {
    // 检查父组件是否监听了 @back 事件
    const hasBackListener = 'onBack' in attrs || attrs['onBack'] !== undefined
    
    if (hasBackListener) {
      // 如果有监听器，只 emit 事件，让父组件处理
      emit('back')
    } else {
      // 如果没有监听器，自动执行 router.back()
      router.back()
    }
  }
}
</script>

<style scoped>
.glass-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: var(--glass-bg-light);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border-bottom: 1px solid var(--glass-border);
  padding-top: var(--safe-area-top);
}

.glass-navbar--transparent {
  background: transparent;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  border-bottom: none;
}

.glass-navbar__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--navbar-height);
  padding: 0 var(--spacing-lg);
}

.glass-navbar__left {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  min-width: 60px;
  color: var(--color-primary);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.glass-navbar__back-text {
  font-size: var(--font-size-base);
}

.glass-navbar__title {
  flex: 1;
  text-align: center;
  font-family: var(--font-family-title);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.glass-navbar__right {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  min-width: 60px;
  justify-content: flex-end;
  color: var(--color-primary);
}

.glass-navbar__placeholder {
  height: calc(var(--navbar-height) + var(--safe-area-top));
}
</style>
