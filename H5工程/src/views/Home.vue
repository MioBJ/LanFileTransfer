<template>
  <div class="home-page">
    <!-- 导航栏 -->
    <GlassNavBar title="开心快传">
      <template #right>
        <AppIcon 
          name="settings" 
          :size="22" 
          @click="$router.push('/settings')" 
        />
      </template>
    </GlassNavBar>

    <!-- 状态栏 -->
    <div class="status-bar">
      <div class="status-bar__content">
        <div class="status-item">
          <AppIcon 
            :name="wifiInfo.connected ? 'wifi' : 'wifi-off'" 
            :size="16" 
          />
          <span>{{ wifiInfo.connected ? (wifiInfo.ssid || 'WiFi已连接') : '未连接WiFi' }}</span>
        </div>
        <div class="status-item">
          <span 
            class="status-dot" 
            :class="serverConnected ? 'status-dot--online' : 'status-dot--offline'"
          />
          <span>{{ serverStatusText }}</span>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="home-content">
      <!-- 空状态 -->
      <div v-if="allTasks.length === 0" class="empty-state">
        <div class="empty-icon animate-float">
          <AppIcon name="package" :size="64" color="var(--color-text-tertiary)" />
        </div>
        <p class="empty-title">还没有传输任务</p>
        <p class="empty-desc">点击下方按钮开始传输吧~</p>
      </div>

      <!-- 任务列表 -->
      <div v-else class="task-list">
        <TransitionGroup name="task">
          <div
            v-for="task in sortedTasks"
            :key="task.id"
            class="task-item-wrapper"
          >
            <GlassCard
              :variant="getTaskVariant(task.status)"
              class="task-card"
              :class="{ 'task-card--success': task.status === 'success' }"
            >
              <div class="task-content">
                <!-- 缩略图 -->
                <div class="task-thumbnail">
                  <img v-if="task.thumbnail" :src="task.thumbnail" alt="" />
                  <AppIcon 
                    v-else 
                    :name="task.fileType?.startsWith('video') ? 'video' : 'image'" 
                    :size="32" 
                    color="var(--color-text-tertiary)"
                  />
                </div>

                <!-- 文件信息 -->
                <div class="task-info">
                  <div class="task-name">{{ task.fileName }}</div>
                  <div class="task-meta">
                    <span class="task-size">{{ formatSize(task.fileSize) }}</span>
                    <StatusTag 
                      v-if="task.status !== 'uploading'" 
                      :status="task.status"
                      :show-icon="true"
                      :pulse="task.status === 'waiting'"
                    />
                  </div>
                  
                  <!-- 进度条（上传中显示） -->
                  <div v-if="task.status === 'uploading'" class="task-progress">
                    <ProgressBar
                      :progress="task.progress"
                      :speed="task.speed"
                      :animated="true"
                      :show-text="true"
                    />
                  </div>

                  <!-- 操作按钮 -->
                  <div class="task-actions">
                    <template v-if="task.status === 'uploading'">
                      <GradientButton type="ghost" size="small" @click="handlePause(task.id)">
                        <AppIcon name="pause" :size="14" />
                        暂停
                      </GradientButton>
                      <GradientButton type="danger" size="small" @click="handleCancel(task.id)">
                        <AppIcon name="close" :size="14" />
                        取消
                      </GradientButton>
                    </template>
                    <template v-else-if="task.status === 'paused'">
                      <GradientButton type="primary" size="small" @click="handleResume(task.id)">
                        <AppIcon name="play" :size="14" />
                        继续
                      </GradientButton>
                      <GradientButton type="danger" size="small" @click="handleCancel(task.id)">
                        <AppIcon name="close" :size="14" />
                        取消
                      </GradientButton>
                    </template>
                    <template v-else-if="task.status === 'waiting'">
                      <GradientButton type="danger" size="small" @click="handleCancel(task.id)">
                        <AppIcon name="close" :size="14" />
                        取消
                      </GradientButton>
                    </template>
                    <template v-else-if="task.status === 'failed'">
                      <GradientButton type="primary" size="small" @click="handleRetry(task.id)">
                        <AppIcon name="refresh" :size="14" />
                        重试
                      </GradientButton>
                      <GradientButton type="danger" size="small" @click="handleCancel(task.id)">
                        <AppIcon name="trash" :size="14" />
                        删除
                      </GradientButton>
                    </template>
                  </div>
                </div>
              </div>
            </GlassCard>
          </div>
        </TransitionGroup>
      </div>
    </div>

    <!-- 选择文件按钮 -->
    <div class="select-button-wrapper">
      <GradientButton
        type="primary"
        size="large"
        block
        :breathe="allTasks.length === 0"
        icon="plus"
        @click="handleSelectFiles"
      >
        选择照片视频
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
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  AppIcon, 
  GlassCard, 
  GradientButton, 
  GlassNavBar, 
  GlassTabBar,
  StatusTag,
  ProgressBar
} from '../components'
import { useUploadStore } from '../stores/upload'
import { useConfigStore } from '../stores/config'
import { getWiFiInfo, onWiFiChanged, selectFiles, saveHistory } from '../utils/jsbridge'
import { formatSize, formatSpeed } from '../utils/format'
import { calculateFileHash, calculateChunkHash } from '../utils/hash'
import { toastText, toastFail } from '../utils/toast'
import type { WiFiInfo, UploadTask, HistoryRecord } from '../types'

const router = useRouter()
const route = useRoute()
const uploadStore = useUploadStore()
const configStore = useConfigStore()

// TabBar配置
const tabBarItems = [
  { path: '/home', icon: 'home', label: '首页' },
  { path: '/history', icon: 'history', label: '历史记录' }
]

// 状态
const wifiInfo = ref<WiFiInfo>({ connected: false })
const uploadSpeedTimers = new Map<string, { lastSize: number; lastTime: number }>()
const isStartingUpload = ref(false)

// 计算属性
const serverConnected = computed(() => configStore.serverConnected)
const allTasks = computed(() => uploadStore.allTasks)
const sortedTasks = computed(() => {
  const statusOrder = { uploading: 1, waiting: 2, paused: 3, failed: 4, success: 5 }
  return [...allTasks.value].sort((a, b) => {
    return (statusOrder[a.status] || 99) - (statusOrder[b.status] || 99)
  })
})

const serverStatusText = computed(() => {
  const serverIp = configStore.config.server?.ip
  if (configStore.serverChecking) return '检查中...'
  if (!serverIp) return '未配置'
  return serverConnected.value ? '服务器在线' : '服务器离线'
})

// 获取任务变体样式
const getTaskVariant = (status: UploadTask['status']) => {
  const variantMap: Record<string, string> = {
    uploading: 'uploading',
    waiting: 'waiting',
    paused: 'paused',
    failed: 'failed',
    success: 'success'
  }
  return variantMap[status] || 'default'
}

// 初始化
onMounted(async () => {
  try {
    wifiInfo.value = await getWiFiInfo()
  } catch (error) {
    console.error('获取WiFi信息失败:', error)
  }

  onWiFiChanged((info) => {
    wifiInfo.value = info
    if (!info.connected && configStore.config.wifiOnly) {
      pauseAllUploads()
      toastFail('已切换到移动网络，上传已暂停')
    }
  })

  startServerCheck()
  window.addEventListener('start-upload', handleStartUploadEvent)
})

onUnmounted(() => {
  window.removeEventListener('start-upload', handleStartUploadEvent)
  stopServerCheck()
  if (configStore.serverChecking) {
    configStore.setServerChecking(false)
  }
})

watch(
  () => route.path,
  (newPath, oldPath) => {
    if ((newPath === '/home' || newPath === '/') && oldPath !== newPath) {
      checkServerConnection()
    }
  },
  { immediate: false }
)

const handleStartUploadEvent = () => {
  startUpload()
}

// 服务器连接检查
let isCheckingServer = false
let checkFailCount = 0

const checkServerConnection = async () => {
  if (isCheckingServer) return
  
  isCheckingServer = true
  configStore.setServerChecking(true)

  const forceEndTimer = window.setTimeout(() => {
    if (isCheckingServer && configStore.serverChecking) {
      configStore.setServerChecking(false)
      isCheckingServer = false
    }
  }, 8000)
  
  try {
    const { healthCheck } = await import('../api')
    const response = await healthCheck()
    
    if (response.code === 200) {
      configStore.setServerConnected(true)
      checkFailCount = 0
    } else {
      checkFailCount++
      if (checkFailCount >= 3) {
        configStore.setServerConnected(false)
      } else {
        configStore.setServerChecking(false)
      }
    }
  } catch (error: any) {
    checkFailCount++
    if (checkFailCount >= 3) {
      configStore.setServerConnected(false)
    } else {
      configStore.setServerChecking(false)
    }
  } finally {
    clearTimeout(forceEndTimer)
    isCheckingServer = false
    setTimeout(() => {
      if (configStore.serverChecking) {
        configStore.setServerChecking(false)
      }
    }, 6000)
  }
}

let serverCheckInterval: number | null = null

const startServerCheck = () => {
  checkServerConnection()
  if (serverCheckInterval) clearInterval(serverCheckInterval)
  serverCheckInterval = window.setInterval(() => {
    checkServerConnection()
  }, 15000)
}

const stopServerCheck = () => {
  if (serverCheckInterval) {
    clearInterval(serverCheckInterval)
    serverCheckInterval = null
  }
}

// 选择文件
const handleSelectFiles = async () => {
  try {
    const files = await selectFiles({
      multiple: true,
      mediaTypes: ['image', 'video']
    })

    if (files.length === 0) return

    router.push({
      path: '/confirm',
      query: { files: encodeURIComponent(JSON.stringify(files)) }
    })
  } catch (error: any) {
    toastFail(error.message || '选择文件失败')
  }
}

// 暂停上传
const handlePause = (id: string) => {
  uploadStore.updateTask(id, { status: 'paused' })
  toastText('已暂停')
}

// 继续上传
const handleResume = (id: string) => {
  uploadStore.updateTask(id, { status: 'waiting' })
  toastText('已加入队列')
  startUpload()
}

// 取消上传
const handleCancel = (id: string) => {
  uploadStore.removeTask(id)
  toastText('已取消')
}

// 重试上传
const handleRetry = (id: string) => {
  const task = uploadStore.getTask(id)
  if (task && task.retryCount < 1) {
    uploadStore.updateTask(id, {
      status: 'waiting',
      error: undefined,
      retryCount: task.retryCount + 1
    })
    toastText('已加入重试队列')
    startUpload()
  } else {
    toastFail('已达到最大重试次数')
  }
}

// 暂停所有上传
const pauseAllUploads = () => {
  uploadStore.activeTasks.forEach(task => {
    if (task.status === 'uploading') {
      uploadStore.updateTask(task.id, { status: 'paused' })
    }
  })
}

// 开始上传（队列管理）
const startUpload = async () => {
  if (isStartingUpload.value) return
  
  try {
    isStartingUpload.value = true

    if (configStore.config.wifiOnly && !wifiInfo.value.connected) return
    if (!serverConnected.value) return

    const waitingTasksSnapshot = [...uploadStore.waitingTasks]
    const activeTasksCount = uploadStore.activeTasks.length
    const maxConcurrent = configStore.config.maxConcurrent || 2
    const canStartCount = Math.min(maxConcurrent - activeTasksCount, waitingTasksSnapshot.length)
    
    if (canStartCount <= 0) return
    
    for (let i = 0; i < canStartCount; i++) {
      const task = waitingTasksSnapshot[i]
      if (task) {
        uploadStore.updateTask(task.id, { status: 'uploading' })
        uploadFile(task).catch(console.error)
      }
    }
  } finally {
    setTimeout(() => { isStartingUpload.value = false }, 100)
  }
}

// 上传文件
const uploadFile = async (task: UploadTask) => {
  const currentTask = uploadStore.getTask(task.id)
  if (!currentTask || currentTask.status !== 'uploading') return
  
  uploadStore.updateTask(task.id, { startedAt: new Date().toISOString() })

  try {
    const chunkThreshold = 10 * 1024 * 1024
    if (task.fileSize < chunkThreshold) {
      await uploadSmallFile(task)
    } else {
      await uploadLargeFile(task)
    }
  } catch (error: any) {
    uploadStore.updateTask(task.id, {
      status: 'failed',
      error: error.message || '上传失败'
    })
    
    if (task.retryCount < 1) {
      setTimeout(() => handleRetry(task.id), 30000)
    }
  }
}

// 小文件上传
const uploadSmallFile = async (task: UploadTask) => {
  const { uploadFile: uploadFileAPI } = await import('../api')
  const startTime = Date.now()

  uploadSpeedTimers.set(task.id, { lastSize: 0, lastTime: startTime })

  await uploadFileAPI(
    task.file as File,
    'Device',
    'H5',
    (progress) => {
      const now = Date.now()
      const uploadedSize = (task.fileSize * progress) / 100
      const timer = uploadSpeedTimers.get(task.id)
      
      if (timer) {
        const timeDiff = (now - timer.lastTime) / 1000
        const sizeDiff = uploadedSize - timer.lastSize
        
        if (timeDiff > 0) {
          const speed = sizeDiff / timeDiff
          uploadStore.updateTask(task.id, { progress, uploadedSize, speed })
          timer.lastSize = uploadedSize
          timer.lastTime = now
        }
      } else {
        uploadStore.updateTask(task.id, { progress, uploadedSize })
      }
    }
  )

  const duration = Math.floor((Date.now() - startTime) / 1000)
  uploadSpeedTimers.delete(task.id)

  uploadStore.updateTask(task.id, {
    status: 'success',
    progress: 100,
    uploadedSize: task.fileSize,
    speed: 0,
    completedAt: new Date().toISOString()
  })

  await saveToHistory(task, duration)
  toastText('上传成功')
  setTimeout(() => startUpload(), 500)
}

// 大文件上传
const uploadLargeFile = async (task: UploadTask) => {
  const { initUpload, uploadChunk, completeUpload } = await import('../api')
  const startTime = Date.now()

  uploadSpeedTimers.set(task.id, { lastSize: 0, lastTime: startTime })

  const fileHash = await calculateFileHash(task.file as File)
  
  const initResponse = await initUpload({
    filename: task.fileName,
    filesize: task.fileSize,
    fileHash,
    deviceName: 'Device',
    deviceType: 'H5'
  })

  if (initResponse.code !== 200 || !initResponse.data) {
    throw new Error(initResponse.message || '初始化失败')
  }

  const { upload_id, uploaded_chunks, total_chunks, chunk_size } = initResponse.data!
  uploadStore.updateTask(task.id, {
    uploadId: upload_id,
    totalChunks: total_chunks,
    uploadedChunks: uploaded_chunks || []
  })

  const file = task.file as File
  for (let i = 0; i < total_chunks; i++) {
    if (uploaded_chunks.includes(i)) continue

    const currentTask = uploadStore.getTask(task.id)
    if (currentTask?.status === 'paused') break

    const start = i * chunk_size
    const end = Math.min(start + chunk_size, file.size)
    const chunk = file.slice(start, end)
    const chunkHash = await calculateChunkHash(chunk)

    await uploadChunk(
      upload_id,
      i,
      chunkHash,
      chunk,
      (progress) => {
        const now = Date.now()
        const uploadedSize = Math.min(i * chunk_size + (progress * chunk_size / 100), file.size)
        const totalProgress = (uploadedSize / file.size) * 100
        const timer = uploadSpeedTimers.get(task.id)
        
        if (timer) {
          const timeDiff = (now - timer.lastTime) / 1000
          const sizeDiff = uploadedSize - timer.lastSize
          
          if (timeDiff > 0) {
            const speed = sizeDiff / timeDiff
            uploadStore.updateTask(task.id, {
              progress: Math.min(totalProgress, 100),
              uploadedSize,
              speed
            })
            timer.lastSize = uploadedSize
            timer.lastTime = now
          }
        }
      }
    )

    const updatedChunks = [...(uploadStore.getTask(task.id)?.uploadedChunks || []), i]
    uploadStore.updateTask(task.id, { uploadedChunks: updatedChunks })
  }

  const completeResponse = await completeUpload(upload_id, total_chunks)
  const duration = Math.floor((Date.now() - startTime) / 1000)
  uploadSpeedTimers.delete(task.id)
  
  if (completeResponse.code === 200 && completeResponse.data?.success) {
    uploadStore.updateTask(task.id, {
      status: 'success',
      progress: 100,
      uploadedSize: task.fileSize,
      speed: 0,
      completedAt: new Date().toISOString()
    })
    
    await saveToHistory(task, duration)
    toastText('上传成功')
  } else {
    throw new Error(completeResponse.message || '合并失败')
  }

  setTimeout(() => startUpload(), 500)
}

// 保存历史记录
const saveToHistory = async (task: UploadTask, duration: number) => {
  try {
    const serverConfig = configStore.config.server
    const uniqueId = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    
    const record: HistoryRecord = {
      id: uniqueId,
      fileName: task.fileName,
      fileSize: task.fileSize,
      fileType: task.fileType,
      thumbnail: task.thumbnail,
      uploadTime: task.completedAt || new Date().toISOString(),
      uploadDuration: duration,
      serverIp: serverConfig.ip,
      deviceName: 'Device'
    }
    
    await saveHistory(record)
  } catch (error) {
    console.error('保存历史记录失败:', error)
  }
}
</script>

<style scoped>
.home-page {
  height: 100vh;
  background: var(--gradient-content);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 状态栏 */
.status-bar {
  position: fixed;
  top: calc(var(--navbar-height) + var(--safe-area-top));
  left: 0;
  right: 0;
  z-index: 99;
  background: var(--gradient-sunset-soft);
  border-bottom: 1px solid var(--glass-border);
}

.status-bar__content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-lg);
}

.status-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot--online {
  background: var(--color-success);
  animation: pulse 1.5s ease-in-out infinite;
}

.status-dot--offline {
  background: var(--color-danger);
}

/* 内容区域 - 列表可滚动 */
.home-content {
  flex: 1;
  padding: var(--spacing-lg);
  padding-top: calc(var(--navbar-height) + var(--safe-area-top) + var(--status-bar-height) + var(--spacing-lg));
  padding-bottom: calc(80px + var(--tabbar-height) + var(--safe-area-bottom));
  overflow-x: hidden;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* 隐藏滚动条但保留滚动功能 */
.home-content::-webkit-scrollbar {
  display: none;
}

.home-content {
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

/* 任务列表 */
.task-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.task-item-wrapper {
  transition: all var(--duration-normal) var(--ease-out);
}

.task-card {
  padding: var(--spacing-md);
}

/* 传输完成的卡片 - 不需要特殊效果，只保留绿色边框 */
.task-card--success {
  /* 移除模糊/透明效果，保持清晰 */
}

.task-content {
  display: flex;
  gap: var(--spacing-md);
}

/* 缩略图 */
.task-thumbnail {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.task-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 任务信息 */
.task-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.task-name {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.task-size {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.task-progress {
  margin-top: var(--spacing-xs);
}

.task-actions {
  display: flex;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-sm);
}

/* 选择按钮 */
.select-button-wrapper {
  position: fixed;
  bottom: calc(var(--tabbar-height) + var(--safe-area-bottom) + var(--spacing-sm));
  left: 0;
  right: 0;
  padding: var(--spacing-lg);
  background: linear-gradient(to top, rgba(255, 255, 255, 0.95) 80%, transparent);
  z-index: 50;
}

/* 任务列表动画 */
.task-enter-active,
.task-leave-active {
  transition: all var(--duration-normal) var(--ease-out);
}

.task-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.task-leave-to {
  opacity: 0;
  transform: translateX(100px);
}

.task-move {
  transition: transform var(--duration-normal) var(--ease-out);
}

/* 动画 */
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
