/**
 * 格式化工具函数
 */

/**
 * 格式化文件大小
 * @param bytes 字节数
 * @returns 格式化后的字符串
 */
export function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes}B`
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(2)}KB`
  if (bytes < 1024 ** 3) return `${(bytes / (1024 ** 2)).toFixed(2)}MB`
  return `${(bytes / (1024 ** 3)).toFixed(2)}GB`
}

/**
 * 格式化磁盘空间
 * @param total 总空间（字节）
 * @param used 已用空间（字节）
 * @returns 格式化后的字符串
 */
export function formatDiskSpace(total: number, used: number): string {
  const totalGB = (total / (1024 ** 3)).toFixed(2)
  const usedGB = (used / (1024 ** 3)).toFixed(2)
  return `${usedGB}GB / ${totalGB}GB`
}

/**
 * 格式化上传速度
 * @param bytesPerSecond 每秒字节数
 * @returns 格式化后的字符串
 */
export function formatSpeed(bytesPerSecond: number): string {
  if (bytesPerSecond < 1024) return `${bytesPerSecond.toFixed(0)}B/s`
  if (bytesPerSecond < 1024 ** 2) return `${(bytesPerSecond / 1024).toFixed(2)}KB/s`
  return `${(bytesPerSecond / (1024 ** 2)).toFixed(2)}MB/s`
}

/**
 * 格式化时间
 * @param date 日期对象或ISO字符串
 * @returns 格式化后的字符串
 */
export function formatTime(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 0) {
    return `${days}天前`
  } else if (hours > 0) {
    return `${hours}小时前`
  } else if (minutes > 0) {
    return `${minutes}分钟前`
  } else {
    return '刚刚'
  }
}

/**
 * 格式化日期
 * @param date 日期对象或ISO字符串
 * @returns 格式化后的字符串（YYYY-MM-DD）
 */
export function formatDate(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 格式化日期时间
 * @param date 日期对象或ISO字符串
 * @returns 格式化后的字符串（YYYY-MM-DD HH:mm）
 */
export function formatDateTime(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

/**
 * 格式化耗时
 * @param seconds 秒数
 * @returns 格式化后的字符串
 */
export function formatDuration(seconds: number): string {
  if (seconds < 60) {
    return `${seconds}秒`
  } else if (seconds < 3600) {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return secs > 0 ? `${mins}分${secs}秒` : `${mins}分钟`
  } else {
    const hours = Math.floor(seconds / 3600)
    const mins = Math.floor((seconds % 3600) / 60)
    return mins > 0 ? `${hours}小时${mins}分钟` : `${hours}小时`
  }
}

