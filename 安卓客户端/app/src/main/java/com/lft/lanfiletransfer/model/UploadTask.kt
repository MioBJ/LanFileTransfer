package com.lft.lanfiletransfer.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * 上传任务数据类
 */
data class UploadTask(
    /** 任务 ID */
    val taskId: String,
    
    /** 文件路径（content:// URI） */
    val filePath: String,
    
    /** 上传使用的文件名 */
    val fileName: String,
    
    /** 原始文件名 */
    val originalName: String,
    
    /** 文件大小（字节） */
    val fileSize: Long,
    
    /** MIME 类型 */
    val fileType: String,
    
    /** 服务器基础地址 */
    val serverUrl: String,
    
    /** 设备名称 */
    val deviceName: String,
    
    /** 设备类型 */
    val deviceType: String,
    
    /** 服务端 upload_id（断点续传时使用） */
    var uploadId: String? = null,
    
    /** 已上传的分片索引（断点续传时使用） */
    var uploadedChunks: List<Int>? = null,
    
    /** 总分片数 */
    var totalChunks: Int = 0,
    
    /** 分片大小（字节） */
    var chunkSize: Long = 0
) {
    /**
     * 转换为 JSON 对象
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("taskId", taskId)
            put("filePath", filePath)
            put("fileName", fileName)
            put("originalName", originalName)
            put("fileSize", fileSize)
            put("fileType", fileType)
            put("serverUrl", serverUrl)
            put("deviceName", deviceName)
            put("deviceType", deviceType)
            uploadId?.let { put("uploadId", it) }
            uploadedChunks?.let { put("uploadedChunks", JSONArray(it)) }
            if (totalChunks > 0) put("totalChunks", totalChunks)
            if (chunkSize > 0) put("chunkSize", chunkSize)
        }
    }
    
    companion object {
        /**
         * 从 JSON 对象创建
         */
        fun fromJson(json: JSONObject): UploadTask {
            val uploadedChunksArray = json.optJSONArray("uploadedChunks")
            val uploadedChunks = uploadedChunksArray?.let { array ->
                (0 until array.length()).map { array.getInt(it) }
            }
            
            return UploadTask(
                taskId = json.optString("taskId"),
                filePath = json.optString("filePath"),
                fileName = json.optString("fileName"),
                originalName = json.optString("originalName"),
                fileSize = json.optLong("fileSize"),
                fileType = json.optString("fileType"),
                serverUrl = json.optString("serverUrl"),
                deviceName = json.optString("deviceName"),
                deviceType = json.optString("deviceType"),
                uploadId = json.optString("uploadId").takeIf { it.isNotEmpty() },
                uploadedChunks = uploadedChunks,
                totalChunks = json.optInt("totalChunks"),
                chunkSize = json.optLong("chunkSize")
            )
        }
    }
}
