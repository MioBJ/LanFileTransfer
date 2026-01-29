package com.lft.lanfiletransfer.model

import org.json.JSONObject

/**
 * 上传结果数据类
 */
data class UploadResult(
    /** 任务 ID */
    val taskId: String,
    
    /** 是否成功 */
    val success: Boolean,
    
    /** 结果消息 */
    val message: String,
    
    /** 服务端 upload_id（成功时） */
    val uploadId: String? = null,
    
    /** 错误码（失败时） */
    val errorCode: Int? = null,
    
    /** 错误详情（失败时） */
    val error: String? = null
) {
    /**
     * 转换为 JSON 对象
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("taskId", taskId)
            put("success", success)
            put("message", message)
            uploadId?.let { put("uploadId", it) }
            if (!success) {
                errorCode?.let { put("errorCode", it) }
                error?.let { put("error", it) }
            }
        }
    }
}
