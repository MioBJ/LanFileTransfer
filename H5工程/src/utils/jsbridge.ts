/**
 * JSBridge 工具
 * 与原生App通信的桥梁
 */

import type {
  SelectFilesOptions,
  SelectedFile,
  WiFiInfo,
  PlayVideoOptions,
  HistoryRecord,
  BackgroundUploadTask,
  BackgroundUploadProgress,
  BackgroundUploadComplete
} from '../types'

// ============================================
// JSBridge 接口定义
// ============================================

interface JSBridge {
  // 文件选择器
  selectFiles: (options: SelectFilesOptions) => Promise<SelectedFile[]>
  
  // WiFi状态
  getWiFiInfo: () => Promise<WiFiInfo>
  onWiFiChanged: (callback: (info: WiFiInfo) => void) => void
  
  // 本地存储 - 配置
  saveConfig: (key: string, value: any) => Promise<void>
  getConfig: (key: string) => Promise<any>
  removeConfig: (key: string) => Promise<void>
  
  // 历史记录
  saveHistory: (record: HistoryRecord) => Promise<void>
  getHistory: (options?: {
    limit?: number
    offset?: number
    search?: string
  }) => Promise<HistoryRecord[]>
  deleteHistory: (ids: string[]) => Promise<void>
  clearHistory: () => Promise<void>
  
  // 文件预览
  playVideo: (options: PlayVideoOptions) => Promise<void>
  
  // 后台上传（H5 → Native）
  startBackgroundUpload: (task: BackgroundUploadTask) => Promise<void>
  cancelBackgroundUpload: (taskId: string) => Promise<void>
  
  // 后台上传回调（Native → H5，由 Native 注入时设置）
  _onUploadProgress?: (data: BackgroundUploadProgress) => void
  _onUploadComplete?: (data: BackgroundUploadComplete) => void
}

// ============================================
// 声明全局JSBridge
// ============================================

declare global {
  interface Window {
    JSBridge?: JSBridge
  }
}

// ============================================
// Mock JSBridge (用于浏览器测试)
// ============================================

const mockJSBridge: JSBridge = {
  // 文件选择器 - 返回模拟的文件列表
  selectFiles: async (options: SelectFilesOptions): Promise<SelectedFile[]> => {
    console.log('[Mock] 选择文件:', options)
    
    // 模拟延迟
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // 返回模拟的文件列表
    return [
      {
        id: 'mock-file-1',
        name: 'IMG_0001.jpg',
        size: 2.5 * 1024 * 1024, // 2.5MB
        type: 'image/jpeg',
        path: '/mock/path/IMG_0001.jpg',
        thumbnail: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iIzRBOTBFMiIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LXNpemU9IjE4IiBmaWxsPSJ3aGl0ZSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuWbvueJhzE8L3RleHQ+PC9zdmc+'
      },
      {
        id: 'mock-file-2',
        name: 'VID_0001.mp4',
        size: 15 * 1024 * 1024, // 15MB
        type: 'video/mp4',
        path: '/mock/path/VID_0001.mp4',
        thumbnail: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI0ZGNkI2QiIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LXNpemU9IjE4IiBmaWxsPSJ3aGl0ZSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuinhumihjE8L3RleHQ+PC9zdmc+',
        duration: 120 // 120秒
      }
    ]
  },
  
  // WiFi状态
  getWiFiInfo: async (): Promise<WiFiInfo> => {
    console.log('[Mock] 获取WiFi信息')
    return {
      connected: true,
      ssid: '我的WiFi', // Mock环境显示，真机会显示实际WiFi名称
      bssid: '00:11:22:33:44:55'
    }
  },
  
  onWiFiChanged: (callback: (info: WiFiInfo) => void): void => {
    console.log('[Mock] 监听WiFi变化')
    // 在Mock环境中不做实际监听
  },
  
  // 配置存储
  saveConfig: async (key: string, value: any): Promise<void> => {
    console.log('[Mock] 保存配置:', key, value)
    localStorage.setItem(key, JSON.stringify(value))
  },
  
  getConfig: async (key: string): Promise<any> => {
    console.log('[Mock] 读取配置:', key)
    const value = localStorage.getItem(key)
    return value ? JSON.parse(value) : null
  },
  
  removeConfig: async (key: string): Promise<void> => {
    console.log('[Mock] 删除配置:', key)
    localStorage.removeItem(key)
  },
  
  // 历史记录
  saveHistory: async (record: HistoryRecord): Promise<void> => {
    console.log('[Mock] 保存历史记录:', record)
    const historyKey = 'mock_history'
    const historyStr = localStorage.getItem(historyKey)
    const history: HistoryRecord[] = historyStr ? JSON.parse(historyStr) : []
    history.unshift(record)
    localStorage.setItem(historyKey, JSON.stringify(history))
  },
  
  getHistory: async (options?: {
    limit?: number
    offset?: number
    search?: string
  }): Promise<HistoryRecord[]> => {
    console.log('[Mock] 获取历史记录:', options)
    const historyKey = 'mock_history'
    const historyStr = localStorage.getItem(historyKey)
    let history: HistoryRecord[] = historyStr ? JSON.parse(historyStr) : []
    
    // 搜索过滤
    if (options?.search) {
      const keyword = options.search.toLowerCase()
      history = history.filter(r => r.fileName.toLowerCase().includes(keyword))
    }
    
    // 分页
    const offset = options?.offset || 0
    const limit = options?.limit || 100
    return history.slice(offset, offset + limit)
  },
  
  deleteHistory: async (ids: string[]): Promise<void> => {
    console.log('[Mock] 删除历史记录:', ids)
    const historyKey = 'mock_history'
    const historyStr = localStorage.getItem(historyKey)
    const history: HistoryRecord[] = historyStr ? JSON.parse(historyStr) : []
    const filtered = history.filter(r => !ids.includes(r.id))
    localStorage.setItem(historyKey, JSON.stringify(filtered))
  },
  
  clearHistory: async (): Promise<void> => {
    console.log('[Mock] 清空历史记录')
    localStorage.removeItem('mock_history')
  },
  
  // 文件预览
  playVideo: async (options: PlayVideoOptions): Promise<void> => {
    console.log('[Mock] 播放视频:', options)
    alert(`[Mock] 播放视频:\n标题: ${options.title}\nURL: ${options.url}`)
  },
  
  // 后台上传（Mock 模拟）
  startBackgroundUpload: async (task: BackgroundUploadTask): Promise<void> => {
    console.log('[Mock] 启动后台上传任务:', task)
    
    // 模拟上传进度
    let progress = 0
    const interval = setInterval(() => {
      progress += 10
      if (progress <= 100) {
        // 模拟进度回调
        if (mockJSBridge._onUploadProgress) {
          mockJSBridge._onUploadProgress({
            taskId: task.taskId,
            progress,
            uploadedSize: Math.floor(task.fileSize * progress / 100),
            totalSize: task.fileSize,
            speed: 1024 * 1024 // 模拟 1MB/s
          })
        }
      }
      
      if (progress >= 100) {
        clearInterval(interval)
        // 模拟完成回调
        if (mockJSBridge._onUploadComplete) {
          mockJSBridge._onUploadComplete({
            taskId: task.taskId,
            success: true,
            message: '上传成功',
            uploadId: task.uploadId || `mock-upload-${Date.now()}`
          })
        }
      }
    }, 500)
  },
  
  cancelBackgroundUpload: async (taskId: string): Promise<void> => {
    console.log('[Mock] 取消后台上传任务:', taskId)
  },
  
  // 后台上传回调（由 H5 设置，Native 调用）
  _onUploadProgress: undefined,
  _onUploadComplete: undefined
}

// 如果没有原生JSBridge，则使用Mock版本
if (typeof window !== 'undefined' && !window.JSBridge) {
  console.log('[JSBridge] 使用Mock版本（浏览器测试环境）')
  window.JSBridge = mockJSBridge
}

// ============================================
// JSBridge 封装函数
// ============================================

/**
 * 检查JSBridge是否可用
 */
export function isJSBridgeAvailable(): boolean {
  return typeof window !== 'undefined' && typeof window.JSBridge !== 'undefined'
}

/**
 * 文件选择器
 */
export async function selectFiles(options: SelectFilesOptions = {}): Promise<SelectedFile[]> {
  if (!isJSBridgeAvailable()) {
    throw new Error('JSBridge不可用，请在原生App中使用')
  }
  
  const defaultOptions: SelectFilesOptions = {
    multiple: true,
    mediaTypes: ['image', 'video'],
    ...options
  }
  
  return await window.JSBridge!.selectFiles(defaultOptions)
}

/**
 * 获取WiFi信息
 */
export async function getWiFiInfo(): Promise<WiFiInfo> {
  if (!isJSBridgeAvailable()) {
    return {
      connected: false
    }
  }
  
  return await window.JSBridge!.getWiFiInfo()
}

/**
 * 监听WiFi状态变化
 */
export function onWiFiChanged(callback: (info: WiFiInfo) => void): void {
  if (!isJSBridgeAvailable()) {
    return
  }
  
  window.JSBridge!.onWiFiChanged(callback)
}

/**
 * 保存配置
 */
export async function saveConfig(key: string, value: any): Promise<void> {
  // 无论是否有 JSBridge，都尽量镜像到 localStorage（router 守卫 / API baseURL 读取依赖它）
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch (e) {
    console.warn('[JSBridge] localStorage 写入失败（可忽略）:', e)
  }

  if (!isJSBridgeAvailable()) {
    return
  }
  
  await window.JSBridge!.saveConfig(key, value)
}

/**
 * 读取配置
 */
export async function getConfig(key: string): Promise<any> {
  // 优先读 localStorage（同步、稳定），再用 JSBridge 补全/刷新
  try {
    const cached = localStorage.getItem(key)
    if (cached) {
      return JSON.parse(cached)
    }
  } catch (e) {
    console.warn('[JSBridge] localStorage 读取失败（可忽略）:', e)
  }

  if (!isJSBridgeAvailable()) {
    return null
  }

  // JSBridge 读取加超时兜底，避免卡死影响页面状态更新
  const timeoutMs = 1500
  const bridgePromise = window.JSBridge!.getConfig(key)
  const timeoutPromise = new Promise<null>((resolve) => {
    setTimeout(() => resolve(null), timeoutMs)
  })

  const result = await Promise.race<any>([bridgePromise, timeoutPromise])
  if (result !== null && result !== undefined) {
    // 写回 localStorage 做镜像
    try {
      localStorage.setItem(key, JSON.stringify(result))
    } catch (e) {
      console.warn('[JSBridge] localStorage 写入失败（可忽略）:', e)
    }
  }

  return result
}

/**
 * 删除配置
 */
export async function removeConfig(key: string): Promise<void> {
  if (!isJSBridgeAvailable()) {
    localStorage.removeItem(key)
    return
  }
  
  await window.JSBridge!.removeConfig(key)
}

/**
 * 保存历史记录
 */
export async function saveHistory(record: HistoryRecord): Promise<void> {
  if (!isJSBridgeAvailable()) {
    throw new Error('JSBridge不可用，无法保存历史记录')
  }
  
  await window.JSBridge!.saveHistory(record)
}

/**
 * 获取历史记录
 */
export async function getHistory(options?: {
  limit?: number
  offset?: number
  search?: string
}): Promise<HistoryRecord[]> {
  if (!isJSBridgeAvailable()) {
    return []
  }
  
  return await window.JSBridge!.getHistory(options)
}

/**
 * 删除历史记录
 */
export async function deleteHistory(ids: string[]): Promise<void> {
  if (!isJSBridgeAvailable()) {
    throw new Error('JSBridge不可用，无法删除历史记录')
  }
  
  await window.JSBridge!.deleteHistory(ids)
}

/**
 * 清空历史记录
 */
export async function clearHistory(): Promise<void> {
  if (!isJSBridgeAvailable()) {
    throw new Error('JSBridge不可用，无法清空历史记录')
  }
  
  await window.JSBridge!.clearHistory()
}

/**
 * 播放视频
 */
export async function playVideo(options: PlayVideoOptions): Promise<void> {
  if (!isJSBridgeAvailable()) {
    throw new Error('JSBridge不可用，无法播放视频')
  }
  
  await window.JSBridge!.playVideo(options)
}

// ============================================
// 后台上传相关
// ============================================

/**
 * 启动后台上传任务
 * 将上传任务交给 Native 处理，支持 App 进入后台后继续上传
 */
export async function startBackgroundUpload(task: BackgroundUploadTask): Promise<void> {
  if (!isJSBridgeAvailable()) {
    throw new Error('JSBridge不可用，无法启动后台上传')
  }
  
  await window.JSBridge!.startBackgroundUpload(task)
}

/**
 * 取消后台上传任务
 */
export async function cancelBackgroundUpload(taskId: string): Promise<void> {
  if (!isJSBridgeAvailable()) {
    throw new Error('JSBridge不可用，无法取消后台上传')
  }
  
  await window.JSBridge!.cancelBackgroundUpload(taskId)
}

/**
 * 注册后台上传进度回调
 * @param callback 进度回调函数
 */
export function onBackgroundUploadProgress(
  callback: (data: BackgroundUploadProgress) => void
): void {
  if (!isJSBridgeAvailable()) {
    console.warn('[JSBridge] 不可用，进度回调未注册')
    return
  }
  
  window.JSBridge!._onUploadProgress = callback
}

/**
 * 注册后台上传完成回调
 * @param callback 完成回调函数
 */
export function onBackgroundUploadComplete(
  callback: (data: BackgroundUploadComplete) => void
): void {
  if (!isJSBridgeAvailable()) {
    console.warn('[JSBridge] 不可用，完成回调未注册')
    return
  }
  
  window.JSBridge!._onUploadComplete = callback
}

