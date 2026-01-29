<template>
  <div class="glass-tabbar">
    <div
      v-for="item in items"
      :key="item.path"
      class="glass-tabbar__item"
      :class="{ 'glass-tabbar__item--active': isActive(item.path) }"
      @click="handleClick(item.path)"
    >
      <div class="glass-tabbar__icon">
        <AppIcon 
          :name="isActive(item.path) ? (item.activeIcon || item.icon) : item.icon" 
          :size="24"
        />
      </div>
      <span class="glass-tabbar__label">{{ item.label }}</span>
    </div>
  </div>
  
  <!-- 占位元素 -->
  <div v-if="placeholder" class="glass-tabbar__placeholder" />
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import AppIcon from './AppIcon.vue'

interface TabBarItem {
  path: string
  icon: string
  activeIcon?: string
  label: string
}

const props = defineProps<{
  items: TabBarItem[]
  placeholder?: boolean
}>()

const router = useRouter()
const route = useRoute()

const isActive = (path: string) => {
  return route.path === path
}

const handleClick = (path: string) => {
  if (route.path !== path) {
    // 使用 replace 而不是 push，避免污染历史栈
    router.replace(path)
  }
}
</script>

<style scoped>
.glass-tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  background: var(--glass-bg-heavy);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  padding: var(--spacing-sm) 0;
  padding-bottom: calc(var(--spacing-sm) + var(--safe-area-bottom));
}

.glass-tabbar__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-xs);
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out);
  -webkit-tap-highlight-color: transparent;
}

.glass-tabbar__item--active {
  color: var(--color-primary);
}

.glass-tabbar__icon {
  transition: transform var(--duration-normal) var(--ease-bounce);
}

.glass-tabbar__item--active .glass-tabbar__icon {
  transform: scale(1.1);
}

.glass-tabbar__label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
}

.glass-tabbar__item--active .glass-tabbar__label {
  font-weight: var(--font-weight-semibold);
}

.glass-tabbar__placeholder {
  height: calc(var(--tabbar-height) + var(--safe-area-bottom));
}
</style>
