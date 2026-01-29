/**
 * API 接口配置
 * 封装所有后端API调用
 */

import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig } from 'axios'
import type {
  ApiResponse,
  HealthCheckResponse,
  ServerInfoResponse,
  UploadInitResponse,
  ChunkUploadResponse,
  UploadCompleteResponse
} from '../types'

// ============================================
// 创建Axios实例
// ============================================

const api: AxiosInstance = axios.create({
  timeout: 30000, // 30秒超时
  headers: {
    'Content-Type': 'application/json'
  }
})

// ============================================
// 请求拦截器：设置基础URL
// ============================================

api.interceptors.request.use(
  (config) => {
    // 如果config中已经设置了baseURL（测试连接时），则不覆盖
    if (!config.baseURL) {
      // 从 localStorage 同步读取（避免 JSBridge getConfig 偶发卡住导致请求永远停在“发送前”）
      try {
        if (typeof window !== 'undefined') {
          const configStr = localStorage.getItem('appConfig')
          if (configStr) {
            const appConfig = JSON.parse(configStr)
            if (appConfig?.server?.ip) {
              const port = appConfig.server.port || 8000
              config.baseURL = `http://${appConfig.server.ip}:${port}`
            }
          }
        }
      } catch (e) {
        console.warn('读取本地服务器配置失败，使用默认地址:', e)
      }

      // 如果仍未拿到 baseURL，使用默认地址
      if (!config.baseURL) {
        console.warn('未找到服务器配置，使用默认地址')
        config.baseURL = 'http://localhost:8000'
      }
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// ============================================
// 响应拦截器：统一处理响应
// ============================================

api.interceptors.response.use(
  (response) => {
    const data: ApiResponse = response.data
    
    // 检查业务状态码
    if (data.code === 200) {
      return data
    } else {
      // 业务错误
      return Promise.reject(new Error(data.message || '请求失败'))
    }
  },
  (error) => {
    // 网络错误或其他错误
    if (error.response) {
      // 服务器返回了错误响应
      const data: ApiResponse = error.response.data || { code: 500, message: '服务器错误', data: null }
      return Promise.reject(new Error(data.message || '服务器错误'))
    } else if (error.request) {
      // 请求已发出但没有收到响应
      return Promise.reject(new Error('网络连接失败，请检查服务器是否运行'))
    } else {
      // 其他错误
      return Promise.reject(error)
    }
  }
)

// ============================================
// API 接口函数
// ============================================

/**
 * 健康检查
 * @param baseURL 可选的服务器地址（用于测试连接）
 */
export async function healthCheck(baseURL?: string): Promise<ApiResponse<HealthCheckResponse>> {
  const config: AxiosRequestConfig = {
    timeout: 5000 // 健康检查使用较短的超时时间（5秒）
  }
  if (baseURL) {
    config.baseURL = baseURL
  }
  return await api.get('/api/health', config)
}

/**
 * 获取服务器信息
 * @param baseURL 可选的服务器地址（用于测试连接）
 */
export async function getServerInfo(baseURL?: string): Promise<ApiResponse<ServerInfoResponse>> {
  const config: AxiosRequestConfig = {}
  if (baseURL) {
    config.baseURL = baseURL
  }
  return await api.get('/api/server-info', config)
}

/**
 * 小文件直接上传
 */
export async function uploadFile(
  file: File,
  deviceName: string,
  deviceType: string,
  onProgress?: (progress: number) => void
): Promise<ApiResponse<UploadCompleteResponse>> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('device_name', deviceName)
  formData.append('device_type', deviceType)
  
  return await api.post('/api/upload/', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = (progressEvent.loaded / progressEvent.total) * 100
        onProgress(percent)
      }
    }
  })
}

/**
 * 初始化分片上传
 */
export async function initUpload(params: {
  filename: string
  filesize: number
  fileHash: string
  deviceName: string
  deviceType: string
}): Promise<ApiResponse<UploadInitResponse>> {
  return await api.post('/api/upload/init', {
    filename: params.filename,
    filesize: params.filesize,
    file_hash: params.fileHash,
    device_name: params.deviceName,
    device_type: params.deviceType
  })
}

/**
 * 上传分片
 */
export async function uploadChunk(
  uploadId: string,
  chunkIndex: number,
  chunkHash: string,
  chunkData: Blob,
  onProgress?: (progress: number) => void
): Promise<ApiResponse<ChunkUploadResponse>> {
  const formData = new FormData()
  formData.append('upload_id', uploadId)
  formData.append('chunk_index', chunkIndex.toString())
  formData.append('chunk_hash', chunkHash)
  formData.append('file', chunkData)
  
  return await api.post('/api/upload/chunk', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = (progressEvent.loaded / progressEvent.total) * 100
        onProgress(percent)
      }
    }
  })
}

/**
 * 完成分片上传
 */
export async function completeUpload(
  uploadId: string,
  totalChunks: number
): Promise<ApiResponse<UploadCompleteResponse>> {
  return await api.post('/api/upload/complete', {
    upload_id: uploadId,
    total_chunks: totalChunks
  })
}

export default api

