package com.lft.lanfiletransfer.model

import org.json.JSONObject

/**
 * 上传进度数据类
 */
data class UploadProgress(
    /** 任务 ID */
    val taskId: String,
    
    /** 进度百分比 0-100 */
    val progress: Float,
    
    /** 已上传字节数 */
    val uploadedSize: Long,
    
    /** 总字节数 */
    val totalSize: Long,
    
    /** 上传速度 bytes/s */
    val speed: Long,
    
    /** 已完成分片数 */
    val uploadedChunks: Int = 0,
    
    /** 总分片数 */
    val totalChunks: Int = 0
) {
    /**
     * 转换为 JSON 对象
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("taskId", taskId)
            put("progress", progress)
            put("uploadedSize", uploadedSize)
            put("totalSize", totalSize)
            put("speed", speed)
            put("uploadedChunks", uploadedChunks)
            put("totalChunks", totalChunks)
        }
    }
}
