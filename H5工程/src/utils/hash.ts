/**
 * Hash计算工具
 * 用于计算文件和分片的MD5 hash值
 */

/**
 * 计算文件MD5 hash
 * 注意：浏览器环境无法直接计算MD5，这里使用SHA-256的前32位作为替代
 * 实际项目中应该由原生App计算MD5，或使用Web Worker + wasm实现
 */
export async function calculateFileHash(file: File): Promise<string> {
  // 使用SHA-256计算，取前32位作为hash
  const buffer = await file.arrayBuffer()
  const hashBuffer = await crypto.subtle.digest('SHA-256', buffer)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
  
  // 返回32位hash（MD5长度）
  return hashHex.substring(0, 32)
}

/**
 * 计算分片MD5 hash
 */
export async function calculateChunkHash(chunk: Blob): Promise<string> {
  const buffer = await chunk.arrayBuffer()
  const hashBuffer = await crypto.subtle.digest('SHA-256', buffer)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
  
  return hashHex.substring(0, 32)
}

