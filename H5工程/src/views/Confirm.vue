<template>
  <div class="confirm-page">
    <!-- 导航栏 -->
    <GlassNavBar 
      title="确认传输" 
      :show-back="true"
      @back="handleCancel"
    />
    
    <div class="confirm-content">
      <!-- 文件统计卡片 -->
      <GlassCard 
        icon="package" 
        class="stats-card animate-fade-in-up stagger-1 animate-backwards"
      >
        <div class="stats-content">
          <div class="stats-main">
            <span class="stats-count">{{ files.length }}</span>
            <span class="stats-label">个文件</span>
          </div>
          <div class="stats-size">
            总大小: {{ formatTotalSize }}
          </div>
        </div>
      </GlassCard>

      <!-- 文件列表 -->
      <div class="file-list">
        <GlassCard
          v-for="(file, index) in files"
          :key="file.id"
          class="file-card animate-fade-in-up animate-backwards"
          :style="{ animationDelay: `${(index + 2) * 60}ms` }"
        >
          <div class="file-content">
            <!-- 缩略图 -->
            <div class="file-thumbnail" @click="handlePreview(file, index)">
              <img v-if="file.thumbnail" :src="file.thumbnail" alt="" />
              <div v-else class="thumbnail-placeholder">
                <AppIcon 
                  :name="file.type?.startsWith('video') ? 'video' : 'image'" 
                  :size="40" 
                  color="var(--color-text-tertiary)"
                />
              </div>
              <!-- 视频播放图标 -->
              <div v-if="file.type?.startsWith('video')" class="play-overlay">
                <AppIcon name="play" :size="24" color="white" />
              </div>
            </div>

            <!-- 文件信息 -->
            <div class="file-info">
              <!-- 文件名输入框 -->
              <div class="file-name-wrapper">
                <input
                  v-model="fileNames[index]"
                  type="text"
                  class="file-name-input"
                  placeholder="输入文件名"
                  @blur="handleFileNameChange(index)"
                />
                <button class="edit-btn" @click="focusInput(index)">
                  <AppIcon name="edit" :size="16" color="var(--color-primary)" />
                </button>
              </div>
              
              <!-- 文件元信息 -->
              <div class="file-meta">
                <span class="file-type">
                  <AppIcon 
                    :name="file.type?.startsWith('video') ? 'video' : 'image'" 
                    :size="14" 
                  />
                  {{ file.type?.startsWith('video') ? '视频' : '图片' }}
                </span>
                <span class="file-size">{{ formatSize(file.size) }}</span>
                <span v-if="file.duration" class="file-duration">
                  {{ formatDuration(file.duration) }}
                </span>
              </div>
            </div>
          </div>
        </GlassCard>
      </div>
    </div>

    <!-- 底部操作区 -->
    <div class="action-bar">
      <div class="action-buttons">
        <GradientButton 
          type="secondary" 
          size="large"
          @click="handleCancel"
        >
          取消
        </GradientButton>
        <GradientButton 
          type="primary" 
          size="large"
          icon="zap"
          :disabled="!canUpload"
          @click="handleStartUpload"
        >
          开始传输
        </GradientButton>
      </div>
    </div>

    <!-- 图片预览 -->
    <van-image-preview
      v-model:show="showPreview"
      :images="previewImages"
      :start-position="previewIndex"
      @close="showPreview = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast as vantShowToast, ImagePreview as VanImagePreview } from 'vant'
import { AppIcon, GlassCard, GradientButton, GlassNavBar } from '../components'
import { useUploadStore } from '../stores/upload'
import { useConfigStore } from '../stores/config'
import { formatSize } from '../utils/format'
import { toastText, toastFail } from '../utils/toast'
import { playVideo } from '../utils/jsbridge'
import type { SelectedFile, UploadTask } from '../types'

const router = useRouter()
const route = useRoute()
const uploadStore = useUploadStore()
const configStore = useConfigStore()

// 状态
const files = ref<SelectedFile[]>([])
const fileNames = ref<string[]>([])
const showPreview = ref(false)
const previewImages = ref<string[]>([])
const previewIndex = ref(0)

// 计算属性
const canUpload = computed(() => {
  return fileNames.value.every(name => name.trim().length > 0)
})

const formatTotalSize = computed(() => {
  const total = files.value.reduce((sum, file) => sum + file.size, 0)
  return formatSize(total)
})

// 初始化
onMounted(() => {
  const filesParam = route.query.files
  if (filesParam) {
    try {
      files.value = JSON.parse(decodeURIComponent(filesParam as string))
      fileNames.value = files.value.map(file => generateFileName(file))
    } catch (error) {
      toastFail('文件列表解析失败')
      router.back()
    }
  } else {
    toastFail('未选择文件')
    router.back()
  }
})

// 生成默认文件名
const generateFileName = (file: SelectedFile): string => {
  const rule = configStore.config.fileNameRule
  
  if (rule === 'original') {
    return file.name
  } else if (rule === 'date-device-random') {
    const date = new Date().toISOString().split('T')[0]
    const deviceName = 'Device'
    const random = Math.floor(Math.random() * 1000000).toString().padStart(6, '0')
    const ext = file.name.split('.').pop() || ''
    return `${date}-${deviceName}-${random}.${ext}`
  } else if (rule === 'custom' && configStore.config.customPrefix) {
    return `${configStore.config.customPrefix}-${file.name}`
  }
  
  return file.name
}

// 格式化时长
const formatDuration = (seconds: number): string => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 聚焦输入框
const focusInput = (index: number) => {
  const inputs = document.querySelectorAll('.file-name-input')
  const input = inputs[index] as HTMLInputElement
  if (input) {
    input.focus()
    input.select()
  }
}

// 预览文件
const handlePreview = async (file: SelectedFile, index: number) => {
  if (file.type?.startsWith('image/')) {
    previewImages.value = files.value
      .filter(f => f.type?.startsWith('image/'))
      .map(f => f.thumbnail || '')
    previewIndex.value = previewImages.value.findIndex(img => img === file.thumbnail)
    showPreview.value = true
  } else if (file.type?.startsWith('video/')) {
    try {
      await playVideo({ url: file.path, title: file.name })
    } catch (error) {
      toastFail('播放视频失败')
    }
  }
}

// 文件名变更
const handleFileNameChange = (index: number) => {
  const name = fileNames.value[index].trim()
  if (!name) {
    fileNames.value[index] = generateFileName(files.value[index])
    toastFail('文件名不能为空')
  }
}

// 取消
const handleCancel = () => {
  router.back()
}

// 创建File对象
const createFileObject = async (selectedFile: SelectedFile): Promise<File> => {
  if (selectedFile instanceof File) {
    return selectedFile
  }
  
  try {
    const fileSize = selectedFile.size
    const chunkSize = 1024 * 1024
    const chunks: Uint8Array[] = []
    
    for (let i = 0; i < Math.ceil(fileSize / chunkSize); i++) {
      const currentChunkSize = Math.min(chunkSize, fileSize - i * chunkSize)
      const chunk = new Uint8Array(currentChunkSize)
      for (let j = 0; j < currentChunkSize; j++) {
        chunk[j] = Math.floor(Math.random() * 256)
      }
      chunks.push(chunk)
    }
    
    const blob = new Blob(chunks, { type: selectedFile.type })
    return new File([blob], selectedFile.name, { 
      type: selectedFile.type,
      lastModified: Date.now()
    })
  } catch (error) {
    throw new Error('文件处理失败')
  }
}

// 开始上传
const handleStartUpload = async () => {
  if (!canUpload.value) {
    toastFail('请填写所有文件名')
    return
  }

  let loadingToast = vantShowToast({
    type: 'loading',
    message: '准备上传...',
    duration: 0,
    forbidClick: true
  })

  try {
    const tasks: UploadTask[] = []
    
    for (let index = 0; index < files.value.length; index++) {
      const file = files.value[index]
      
      try {
        const fileObj = await createFileObject(file)
        const uniqueTaskId = `task-${Date.now()}-${Math.random().toString(36).substr(2, 9)}-${index}`

        const task: UploadTask = {
          id: uniqueTaskId,
          file: fileObj,
          fileName: fileNames.value[index],
          originalName: file.name,
          fileSize: file.size,
          fileType: file.type,
          thumbnail: file.thumbnail,
          status: 'waiting',
          progress: 0,
          uploadedSize: 0,
          speed: 0,
          createdAt: new Date().toISOString(),
          retryCount: 0
        }
        
        tasks.push(task)
      } catch (error) {
        loadingToast.close()
        toastFail(`文件 ${file.name} 处理失败`)
        return
      }
    }

    if (tasks.length === 0) {
      loadingToast.close()
      toastFail('没有可上传的文件')
      return
    }

    tasks.forEach(task => {
      const existingTask = uploadStore.getTask(task.id)
      if (!existingTask) {
        uploadStore.addTask(task)
      }
    })

    loadingToast.close()
    router.push('/home')
    
    setTimeout(() => {
      toastText('已添加到上传队列')
      window.dispatchEvent(new CustomEvent('start-upload'))
    }, 300)
  } catch (error: any) {
    loadingToast.close()
    toastFail(error.message || '创建上传任务失败')
  }
}
</script>

<style scoped>
.confirm-page {
  height: 100vh;
  background: var(--gradient-content);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 内容区域 - 文件列表可滚动 */
.confirm-content {
  flex: 1;
  padding: var(--spacing-lg);
  padding-top: calc(var(--navbar-height) + var(--safe-area-top) + var(--spacing-lg));
  padding-bottom: calc(100px + var(--safe-area-bottom));
  overflow-x: hidden;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* 隐藏滚动条但保留滚动功能 */
.confirm-content::-webkit-scrollbar {
  display: none;
}

.confirm-content {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* 统计卡片 */
.stats-card {
  margin-bottom: var(--spacing-lg);
}

.stats-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-main {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-xs);
}

.stats-count {
  font-family: var(--font-family-number);
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

.stats-label {
  font-size: var(--font-size-md);
  color: var(--color-text-secondary);
}

.stats-size {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

/* 文件列表 */
.file-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.file-card {
  padding: var(--spacing-md);
}

.file-content {
  display: flex;
  gap: var(--spacing-md);
}

/* 缩略图 */
.file-thumbnail {
  width: 100px;
  height: 100px;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
  position: relative;
  cursor: pointer;
}

.file-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.play-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 文件信息 */
.file-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.file-name-wrapper {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.file-name-input {
  flex: 1;
  min-width: 0;
  padding: var(--spacing-sm) var(--spacing-md);
  background: rgba(255, 255, 255, 0.8);
  border: 2px solid transparent;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  transition: all var(--duration-fast) var(--ease-out);
}

.file-name-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.1);
  outline: none;
}

.edit-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: rgba(255, 107, 53, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.edit-btn:active {
  transform: scale(0.95);
}

.file-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.file-type {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

/* 底部操作区 */
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(255, 255, 255, 0.98) 80%, transparent);
  padding: var(--spacing-lg);
  padding-bottom: calc(var(--spacing-lg) + var(--safe-area-bottom));
  z-index: 50;
}

.action-buttons {
  display: flex;
  gap: var(--spacing-md);
}

.action-buttons > * {
  flex: 1;
}
</style>
