<template>
  <div class="history-page">
    <!-- 导航栏 -->
    <GlassNavBar title="历史记录" />
    
    <div class="history-content">
      <!-- 空状态 -->
      <div v-if="allRecords.length === 0" class="empty-state">
        <div class="empty-icon animate-float">
          <AppIcon name="history" :size="64" color="var(--color-text-tertiary)" />
        </div>
        <p class="empty-title">暂无传输记录</p>
        <p class="empty-desc">完成传输后会显示在这里~</p>
      </div>

      <!-- 历史记录列表 -->
      <div v-else class="record-list">
        <template v-for="(records, date) in groupedRecords" :key="date">
          <!-- 日期标签 -->
          <div class="date-tag">
            <span class="date-tag__icon">📅</span>
            <span class="date-tag__text">{{ formatDateTitle(date) }}</span>
          </div>

          <!-- 该日期下的记录 -->
          <GlassCard
            v-for="record in records"
            :key="record.id"
            variant="success"
            class="record-card"
          >
            <div class="record-content">
              <!-- 缩略图 -->
              <div class="record-thumbnail">
                <img v-if="record.thumbnail" :src="record.thumbnail" alt="" />
                <AppIcon 
                  v-else 
                  :name="record.fileType?.startsWith('video') ? 'video' : 'image'" 
                  :size="28" 
                  color="var(--color-text-tertiary)"
                />
              </div>

              <!-- 文件信息 -->
              <div class="record-info">
                <div class="record-name">{{ record.fileName }}</div>
                <div class="record-meta">
                  <span class="record-size">{{ formatSize(record.fileSize) }}</span>
                  <span class="record-time">{{ formatTime(record.uploadTime) }}</span>
                </div>
                <div class="record-extra">
                  <span class="record-duration">
                    <AppIcon name="zap" :size="12" />
                    耗时 {{ formatUploadDuration(record.uploadDuration) }}
                  </span>
                  <span class="record-status">
                    <AppIcon name="check-circle" :size="12" color="var(--color-success)" />
                    已完成
                  </span>
                </div>
              </div>
            </div>
          </GlassCard>
        </template>
      </div>
    </div>

    <!-- 清空按钮 -->
    <div v-if="allRecords.length > 0" class="clear-button-wrapper">
      <GradientButton
        type="danger"
        size="large"
        block
        icon="trash"
        @click="handleClearAll"
      >
        清空历史记录
      </GradientButton>
    </div>

    <!-- 底部导航 -->
    <GlassTabBar
      :items="tabBarItems"
      :placeholder="true"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { showConfirmDialog } from 'vant'
import { AppIcon, GlassCard, GradientButton, GlassNavBar, GlassTabBar } from '../components'
import { getHistory, clearHistory } from '../utils/jsbridge'
import { formatSize, formatDate } from '../utils/format'
import { toastText, toastFail } from '../utils/toast'
import type { HistoryRecord } from '../types'

// TabBar配置
const tabBarItems = [
  { path: '/home', icon: 'home', label: '首页' },
  { path: '/history', icon: 'history', label: '历史记录' }
]

// 状态
const allRecords = ref<HistoryRecord[]>([])

// 计算属性 - 按日期分组
const groupedRecords = computed(() => {
  const groups: Record<string, HistoryRecord[]> = {}
  
  allRecords.value.forEach(record => {
    const date = formatDate(record.uploadTime)
    if (!groups[date]) {
      groups[date] = []
    }
    groups[date].push(record)
  })
  
  // 按日期倒序排序
  const sortedGroups: Record<string, HistoryRecord[]> = {}
  Object.keys(groups)
    .sort((a, b) => new Date(b).getTime() - new Date(a).getTime())
    .forEach(date => {
      sortedGroups[date] = groups[date].sort(
        (a, b) => new Date(b.uploadTime).getTime() - new Date(a.uploadTime).getTime()
      )
    })
  
  return sortedGroups
})

// 初始化
onMounted(async () => {
  await loadHistory()
})

// 加载历史记录
const loadHistory = async () => {
  try {
    allRecords.value = await getHistory()
  } catch (error) {
    console.error('加载历史记录失败:', error)
    toastFail('加载历史记录失败')
  }
}

// 格式化日期标题
const formatDateTitle = (date: string): string => {
  const today = formatDate(new Date().toISOString())
  const yesterday = formatDate(new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString())
  
  if (date === today) {
    return '今天'
  } else if (date === yesterday) {
    return '昨天'
  } else {
    // 格式化为 "1月27日" 形式
    const d = new Date(date)
    return `${d.getMonth() + 1}月${d.getDate()}日`
  }
}

// 格式化时间（只显示时:分）
const formatTime = (dateStr: string): string => {
  const date = new Date(dateStr)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

// 格式化上传耗时
const formatUploadDuration = (seconds: number): string => {
  if (!seconds || seconds < 0) return '-'
  
  if (seconds < 60) {
    return `${seconds}秒`
  } else {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    if (secs === 0) {
      return `${mins}分钟`
    }
    return `${mins}分${secs}秒`
  }
}

// 清空所有记录
const handleClearAll = async () => {
  try {
    await showConfirmDialog({
      title: '确认清空',
      message: '确定要清空所有历史记录吗？\n此操作不可恢复。',
      confirmButtonText: '清空',
      confirmButtonColor: 'var(--color-danger)',
      cancelButtonText: '取消'
    })
    
    await clearHistory()
    allRecords.value = []
    toastText('历史记录已清空')
  } catch (error) {
    // 用户取消
    if (error !== 'cancel') {
      console.error('清空失败:', error)
      toastFail('清空失败')
    }
  }
}
</script>

<style scoped>
.history-page {
  height: 100vh;
  background: var(--gradient-content);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 内容区域 - 列表可滚动 */
.history-content {
  flex: 1;
  padding: var(--spacing-lg);
  padding-top: calc(var(--navbar-height) + var(--safe-area-top) + var(--spacing-lg));
  padding-bottom: calc(100px + var(--tabbar-height) + var(--safe-area-bottom));
  overflow-x: hidden;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* 隐藏滚动条但保留滚动功能 */
.history-content::-webkit-scrollbar {
  display: none;
}

.history-content {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-4xl) var(--spacing-lg);
  text-align: center;
}

.empty-icon {
  width: 120px;
  height: 120px;
  background: var(--glass-bg);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--spacing-xl);
}

.empty-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
  margin: 0 0 var(--spacing-sm);
}

.empty-desc {
  font-size: var(--font-size-base);
  color: var(--color-text-tertiary);
  margin: 0;
}

/* 记录列表 */
.record-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

/* 日期标签 */
.date-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  padding: var(--spacing-xs) var(--spacing-md);
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-sm);
  width: fit-content;
}

.date-tag__icon {
  font-size: var(--font-size-sm);
}

/* 记录卡片 */
.record-card {
  padding: var(--spacing-md);
}

.record-content {
  display: flex;
  gap: var(--spacing-md);
}

/* 缩略图 */
.record-thumbnail {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.record-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 记录信息 */
.record-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.record-name {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.record-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.record-extra {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.record-duration,
.record-status {
  display: flex;
  align-items: center;
  gap: 2px;
}

.record-status {
  color: var(--color-success);
}

/* 清空按钮 */
.clear-button-wrapper {
  position: fixed;
  bottom: calc(var(--tabbar-height) + var(--safe-area-bottom));
  left: 0;
  right: 0;
  padding: var(--spacing-lg);
  background: linear-gradient(to top, rgba(255, 255, 255, 0.95) 80%, transparent);
  z-index: 50;
}
</style>
