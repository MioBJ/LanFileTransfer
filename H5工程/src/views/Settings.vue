<template>
  <div class="settings-page">
    <!-- 顶部渐变背景 -->
    <div class="settings-bg" />
    
    <!-- 导航栏 -->
    <GlassNavBar 
      title="服务器配置"
      :show-back="showBackButton"
      @back="handleBack"
    />
    
    <div class="settings-content">
      <!-- WiFi状态卡片 -->
      <GlassCard 
        title="网络状态" 
        icon="wifi"
        class="animate-fade-in-up stagger-1 animate-backwards"
      >
        <div class="wifi-status">
          <div class="wifi-info">
            <span class="wifi-label">WiFi连接</span>
            <span class="wifi-value">
              <span 
                class="wifi-dot" 
                :class="{ 'wifi-dot--connected': wifiInfo.connected }"
              />
              {{ wifiStatusText }}
            </span>
          </div>
          <div v-if="wifiInfo.connected && wifiInfo.ssid" class="wifi-ssid">
            {{ wifiInfo.ssid }}
          </div>
          <div v-else class="wifi-hint">
            请确保手机与服务器连接同一WiFi
          </div>
        </div>
      </GlassCard>

      <!-- 服务器地址卡片 -->
      <GlassCard 
        title="服务器地址" 
        icon="server"
        class="animate-fade-in-up stagger-2 animate-backwards"
      >
        <div class="server-form">
          <div class="form-item">
            <label class="form-label">IP地址</label>
            <input
              v-model="serverIp"
              type="text"
              class="form-input"
              placeholder="例如：192.168.1.100"
              @input="handleIpInput"
            />
          </div>
          
          <div class="form-item">
            <label class="form-label">端口</label>
            <input
              v-model.number="serverPort"
              type="number"
              class="form-input form-input--short"
              placeholder="8000"
            />
          </div>
          
          <GradientButton
            type="primary"
            size="medium"
            block
            :loading="testing"
            icon="link"
            @click="handleTestConnection"
          >
            {{ testing ? '连接中...' : '测试连接' }}
          </GradientButton>
        </div>
      </GlassCard>

      <!-- 服务器信息卡片 -->
      <GlassCard 
        v-if="serverInfo"
        title="服务器信息" 
        icon="check-circle"
        class="server-info-card animate-bounce-in-up"
      >
        <div class="server-info-list">
          <div class="info-row">
            <span class="info-label">版本</span>
            <span class="info-value">{{ serverInfo.data?.version || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">状态</span>
            <span class="info-value info-value--success">
              <span class="status-dot status-dot--online" />
              在线
            </span>
          </div>
          <div class="info-row">
            <span class="info-label">存储空间</span>
            <span class="info-value">
              {{ formatDiskSpaceForCell(serverInfo.data?.disk_space) }}
            </span>
          </div>
          <div v-if="serverInfo.data?.disk_space" class="disk-progress">
            <div class="disk-progress__track">
              <div 
                class="disk-progress__fill"
                :style="{ width: `${serverInfo.data.disk_space.percent}%` }"
                :class="{ 'disk-progress__fill--warning': serverInfo.data.disk_space.percent > 80 }"
              />
            </div>
            <span class="disk-progress__text">
              {{ serverInfo.data.disk_space.percent.toFixed(1) }}% 已使用
            </span>
          </div>
        </div>
      </GlassCard>

      <!-- 历史连接卡片 -->
      <GlassCard 
        v-if="historyList.length > 0"
        title="最近连接" 
        icon="history"
        class="animate-fade-in-up stagger-3 animate-backwards"
      >
        <div class="history-list">
          <div
            v-for="(item, index) in historyList"
            :key="index"
            class="history-item"
            @click="handleSelectHistory(item)"
          >
            <div class="history-info">
              <span class="history-address">{{ item.ip }}:{{ item.port }}</span>
              <span class="history-time">{{ formatLastConnected(item.lastConnected) }}</span>
            </div>
            <AppIcon name="arrow-left" :size="16" class="history-arrow" />
          </div>
        </div>
      </GlassCard>

      <!-- 保存按钮 -->
      <div class="save-button animate-fade-in-up stagger-4 animate-backwards">
        <GradientButton
          type="primary"
          size="large"
          block
          :disabled="!canSave || testing"
          icon="save"
          @click="handleSave"
        >
          保存配置
        </GradientButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { AppIcon, GlassCard, GradientButton, GlassNavBar } from '../components'
import { useConfigStore } from '../stores/config'
import { getWiFiInfo } from '../utils/jsbridge'
import { healthCheck, getServerInfo } from '../api'
import { formatDiskSpace } from '../utils/format'
import { toastText, toastFail } from '../utils/toast'
import type { WiFiInfo, ServerInfoResponse } from '../types'

const router = useRouter()
const route = useRoute()
const configStore = useConfigStore()

// 状态
const serverIp = ref('')
const serverPort = ref(8000)
const wifiInfo = ref<WiFiInfo>({ connected: false })
const testing = ref(false)
const serverInfo = ref<{ data: ServerInfoResponse } | null>(null)

// 连接测试防误弹：用序号让"旧请求的结果"失效
let testSeq = 0
const invalidateTest = () => {
  testSeq++
  testing.value = false
}

// 计算属性
const showBackButton = computed(() => {
  return route.query.from !== 'guide'
})

const wifiStatusText = computed(() => {
  return wifiInfo.value.connected ? '已连接' : '未连接'
})

const historyList = computed(() => {
  return configStore.config.history || []
})

const canSave = computed(() => {
  return serverIp.value && serverPort.value > 0 && serverPort.value < 65536
})

// 初始化
onMounted(async () => {
  if (configStore.config.server.ip) {
    serverIp.value = configStore.config.server.ip
    serverPort.value = configStore.config.server.port
  }

  try {
    wifiInfo.value = await getWiFiInfo()
  } catch (error) {
    console.error('获取WiFi信息失败:', error)
  }
  
  if (!configStore.config.fileNameRule) {
    configStore.config.fileNameRule = 'date-device-random'
  }
  if (!configStore.config.maxConcurrent) {
    configStore.config.maxConcurrent = 2
  }
  if (configStore.config.wifiOnly === undefined) {
    configStore.config.wifiOnly = true
  }
})

// IP输入处理
const handleIpInput = (e: Event) => {
  const input = e.target as HTMLInputElement
  // 只允许数字和点
  input.value = input.value.replace(/[^0-9.]/g, '')
  serverIp.value = input.value
}

// 格式化磁盘空间
const formatDiskSpaceForCell = (diskSpace?: ServerInfoResponse['disk_space']) => {
  if (!diskSpace) return '-'
  return formatDiskSpace(diskSpace.total, diskSpace.used)
}

// 测试连接
const handleTestConnection = async () => {
  if (!serverIp.value || !serverPort.value) {
    toastFail('请先输入IP地址和端口')
    return
  }

  const seq = ++testSeq
  testing.value = true
  serverInfo.value = null

  try {
    const testBaseURL = `http://${serverIp.value}:${serverPort.value}`
    
    const healthResponse = await healthCheck(testBaseURL)
    if (seq !== testSeq) return
    
    if (healthResponse.code === 200) {
      const infoResponse = await getServerInfo(testBaseURL)
      if (seq !== testSeq) return
      serverInfo.value = infoResponse
      toastText('连接成功')
    } else {
      toastFail(healthResponse.message || '连接失败')
    }
  } catch (error: any) {
    console.error('连接测试失败:', error)
    toastFail(error.message || '连接失败，请检查IP和端口')
  } finally {
    if (seq === testSeq) {
      testing.value = false
    }
  }
}

// 选择历史记录
const handleSelectHistory = (item: { ip: string; port: number }) => {
  serverIp.value = item.ip
  serverPort.value = item.port
  toastText('已填充')
}

// 格式化最后连接时间
const formatLastConnected = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (24 * 60 * 60 * 1000))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString()
}

// 保存配置
const handleSave = async () => {
  if (!canSave.value) {
    toastFail('请先完成必填项')
    return
  }

  invalidateTest()
  
  try {
    await configStore.updateServer({
      ip: serverIp.value,
      port: serverPort.value,
      lastConnected: new Date().toISOString()
    })

    if (!configStore.config.fileNameRule) {
      await configStore.updateFileNameRule('date-device-random')
    }
    if (!configStore.config.maxConcurrent || !configStore.config.wifiOnly) {
      await configStore.updateUploadSettings(2, true)
    }
    
    toastText('保存成功')
    
    setTimeout(() => {
      router.push('/home')
    }, 1000)
  } catch (error) {
    console.error('保存配置失败:', error)
    toastFail('保存失败')
  }
}

// 返回
const handleBack = () => {
  invalidateTest()
  router.push('/home')
}
</script>

<style scoped>
.settings-page {
  height: 100vh;
  background: var(--gradient-content);
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 顶部渐变背景 */
.settings-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 200px;
  background: var(--gradient-sunset-soft);
  z-index: 0;
  pointer-events: none;
}

/* 内容区域 - 仅内容超出时可滚动 */
.settings-content {
  flex: 1;
  position: relative;
  z-index: 1;
  padding: var(--spacing-lg);
  padding-top: calc(var(--navbar-height) + var(--safe-area-top) + var(--spacing-lg));
  padding-bottom: var(--spacing-3xl);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
  overflow-x: hidden;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* 隐藏滚动条但保留滚动功能 */
.settings-content::-webkit-scrollbar {
  display: none;
}

.settings-content {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* WiFi状态 */
.wifi-status {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.wifi-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.wifi-label {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
}

.wifi-value {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.wifi-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-danger);
}

.wifi-dot--connected {
  background: var(--color-success);
}

.wifi-ssid {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.wifi-hint {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

/* 服务器表单 */
.server-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.form-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
}

.form-input {
  width: 100%;
  height: 48px;
  padding: 0 var(--spacing-lg);
  background: rgba(255, 255, 255, 0.8);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  font-size: var(--font-size-md);
  color: var(--color-text-primary);
  transition: all var(--duration-normal) var(--ease-out);
}

.form-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.15);
  outline: none;
}

.form-input::placeholder {
  color: var(--color-text-tertiary);
}

.form-input--short {
  width: 120px;
}

/* 服务器信息 */
.server-info-card {
  border-left: 4px solid var(--color-success);
}

.server-info-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
}

.info-value {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.info-value--success {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  color: var(--color-success);
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

/* 磁盘进度条 */
.disk-progress {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.disk-progress__track {
  height: 6px;
  background: rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.disk-progress__fill {
  height: 100%;
  background: var(--color-success);
  border-radius: var(--radius-full);
  transition: width var(--duration-slow) var(--ease-out);
}

.disk-progress__fill--warning {
  background: var(--color-warning);
}

.disk-progress__text {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  text-align: right;
}

/* 历史列表 */
.history-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md);
  background: rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.history-item:active {
  background: rgba(255, 107, 53, 0.1);
  transform: scale(0.98);
}

.history-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.history-address {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.history-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.history-arrow {
  transform: rotate(180deg);
  color: var(--color-text-tertiary);
}

/* 保存按钮 */
.save-button {
  margin-top: var(--spacing-lg);
}

/* 动画 */
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
