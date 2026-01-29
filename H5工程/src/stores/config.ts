/**
 * 配置Store
 * 管理应用配置信息
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { AppConfig, ServerConfig } from '../types'
import { getConfig, saveConfig } from '../utils/jsbridge'

export const useConfigStore = defineStore('config', () => {
  // 状态
  const config = ref<AppConfig>({
    server: {
      ip: '',
      port: 8000
    },
    history: [],
    fileNameRule: 'date-device-random',
    maxConcurrent: 2,
    wifiOnly: true
  })

  // 是否已初始化
  const initialized = ref(false)
  
  // 服务器连接状态（全局共享，避免页面切换时丢失）
  const serverConnected = ref(false)
  
  // 服务器状态是否正在检查中
  const serverChecking = ref(false)

  // ============================================
  // Actions
  // ============================================

  /**
   * 初始化配置（从本地存储加载）
   */
  async function init() {
    try {
      const savedConfig = await getConfig('appConfig')
      if (savedConfig) {
        config.value = { ...config.value, ...savedConfig }
      }
      initialized.value = true
    } catch (error) {
      console.error('加载配置失败:', error)
      initialized.value = true
    }
  }

  /**
   * 保存配置
   */
  async function save() {
    try {
      await saveConfig('appConfig', config.value)
    } catch (error) {
      console.error('保存配置失败:', error)
    }
  }

  /**
   * 更新服务器配置
   */
  async function updateServer(server: ServerConfig) {
    config.value.server = server
    
    // 添加到历史记录（最多3个）
    const history = config.value.history.filter(
      item => !(item.ip === server.ip && item.port === server.port)
    )
    history.unshift(server)
    config.value.history = history.slice(0, 3)
    
    await save()
  }

  /**
   * 更新文件名规则
   */
  async function updateFileNameRule(rule: AppConfig['fileNameRule'], customPrefix?: string) {
    config.value.fileNameRule = rule
    if (customPrefix) {
      config.value.customPrefix = customPrefix
    }
    await save()
  }

  /**
   * 更新上传设置
   */
  async function updateUploadSettings(maxConcurrent: number, wifiOnly: boolean) {
    config.value.maxConcurrent = maxConcurrent
    config.value.wifiOnly = wifiOnly
    await save()
  }

  /**
   * 更新服务器连接状态
   */
  function setServerConnected(connected: boolean) {
    console.log('[ConfigStore] 设置服务器连接状态:', connected)
    serverConnected.value = connected
    serverChecking.value = false
    // 强制触发响应式更新
    console.log('[ConfigStore] 当前serverConnected.value =', serverConnected.value)
  }
  
  /**
   * 设置服务器检查中状态
   */
  function setServerChecking(checking: boolean) {
    console.log('[ConfigStore] 设置检查中状态:', checking)
    serverChecking.value = checking
  }

  return {
    config,
    initialized,
    serverConnected,
    serverChecking,
    init,
    save,
    updateServer,
    updateFileNameRule,
    updateUploadSettings,
    setServerConnected,
    setServerChecking
  }
})

