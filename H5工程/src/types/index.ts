/**
 * 类型定义文件
 * 定义项目中使用的所有TypeScript类型
 */

// ============================================
// JSBridge 相关类型
// ============================================

/**
 * 文件选择器选项
 */
export interface SelectFilesOptions {
  multiple?: boolean
  mediaTypes?: ('image' | 'video')[]
  maxCount?: number
}

/**
 * 选中的文件
 */
export interface SelectedFile {
  id: string
  name: string
  size: number
  type: string
  path: string
  thumbnail?: string
  duration?: number
}

/**
 * WiFi信息
 */
export interface WiFiInfo {
  connected: boolean
  ssid?: string
  bssid?: string
}

/**
 * 播放视频选项
 */
export interface PlayVideoOptions {
  url: string
  title?: string
}

/**
 * 历史记录
 */
export interface HistoryRecord {
  id: string
  fileName: string
  fileSize: number
  fileType: string
  thumbnail?: string
  uploadTime: string
  uploadDuration: number
  serverIp: string
  deviceName: string
}

// ============================================
// 上传任务相关类型
// ============================================

/**
 * 上传任务状态
 */
export type UploadTaskStatus = 'waiting' | 'uploading' | 'paused' | 'success' | 'failed'

/**
 * 上传任务
 */
export interface UploadTask {
  id: string
  file: File
  fileName: string
  originalName: string
  fileSize: number
  fileType: string
  thumbnail?: string
  
  status: UploadTaskStatus
  progress: number
  uploadedSize: number
  speed: number
  
  // 分片上传相关
  uploadId?: string
  totalChunks?: number
  uploadedChunks?: number[]
  
  // 时间信息
  createdAt: string
  startedAt?: string
  completedAt?: string
  
  // 错误信息
  error?: string
  retryCount: number
}

// ============================================
// 配置相关类型
// ============================================

/**
 * 服务器配置
 */
export interface ServerConfig {
  ip: string
  port: number
  lastConnected?: string
}

/**
 * 应用配置
 */
export interface AppConfig {
  server: ServerConfig
  history: ServerConfig[]
  fileNameRule: 'original' | 'date-device-random' | 'custom'
  customPrefix?: string
  maxConcurrent: number
  wifiOnly: boolean
}

// ============================================
// API 响应类型
// ============================================

/**
 * 统一API响应格式
 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T | null
}

/**
 * 健康检查响应
 */
export interface HealthCheckResponse {
  status: string
  timestamp: string
  version: string
}

/**
 * 服务器信息响应
 */
export interface ServerInfoResponse {
  server_ip: string
  server_port: number
  storage_path: string
  disk_space: {
    total: number
    used: number
    free: number
    percent: number
  }
  upload_limits: {
    chunk_size: number
    chunk_threshold: number
    max_concurrent_uploads: number
  }
  server_time: string
}

/**
 * 上传初始化响应
 */
export interface UploadInitResponse {
  upload_id: string
  uploaded_chunks: number[]
  total_chunks: number
  chunk_size: number
}

/**
 * 分片上传响应
 */
export interface ChunkUploadResponse {
  chunk_index: number
  uploaded: boolean
}

/**
 * 上传完成响应
 */
export interface UploadCompleteResponse {
  upload_id: string
  success: boolean
  message: string
}

// ============================================
// 后台上传相关类型（JSBridge）
// ============================================

/**
 * 后台上传任务参数
 * H5 调用 onUploadTask 时传递给 Native
 */
export interface BackgroundUploadTask {
  // 任务标识
  taskId: string              // 任务 ID（与 H5 uploadStore 中的任务 ID 一致）
  
  // 文件信息
  filePath: string            // 原生文件路径（PHPicker 返回的）
  fileName: string            // 上传使用的文件名（可能是重命名后的）
  originalName: string        // 原始文件名
  fileSize: number            // 文件大小（字节）
  fileType: string            // MIME 类型
  
  // 服务器配置
  serverUrl: string           // 服务器基础地址，如 http://192.168.1.100:8000
  
  // 设备信息
  deviceName: string          // 设备名称
  deviceType: string          // 设备类型，如 "iOS"
  
  // 断点续传（可选，如果 H5 已经开始上传了）
  uploadId?: string           // 已有的 upload_id（服务端返回的）
  uploadedChunks?: number[]   // 已上传成功的分片索引
  totalChunks?: number        // 总分片数
  chunkSize?: number          // 分片大小（字节）
}

/**
 * 后台上传进度事件
 * Native 通过 _onUploadProgress 回调通知 H5
 */
export interface BackgroundUploadProgress {
  taskId: string              // 任务 ID
  progress: number            // 上传进度 0-100
  uploadedSize: number        // 已上传字节数
  totalSize: number           // 总字节数
  speed: number               // 上传速度 bytes/s
  
  // 分片进度（可选）
  uploadedChunks?: number     // 已完成的分片数
  totalChunks?: number        // 总分片数
}

/**
 * 后台上传完成事件
 * Native 通过 _onUploadComplete 回调通知 H5
 */
export interface BackgroundUploadComplete {
  taskId: string              // 任务 ID
  success: boolean            // 是否成功
  message: string             // 结果消息
  
  // 成功时返回
  uploadId?: string           // 服务端 upload_id
  
  // 失败时返回
  errorCode?: number          // 错误码
  error?: string              // 错误详情
}

