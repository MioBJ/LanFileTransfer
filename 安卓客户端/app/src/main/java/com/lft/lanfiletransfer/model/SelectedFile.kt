package com.lft.lanfiletransfer.model

import org.json.JSONObject

/**
 * 选中文件数据类
 * 
 * 表示用户选择的文件信息
 */
data class SelectedFile(
    /** 文件唯一标识（UUID） */
    val id: String,
    
    /** 文件名 */
    val name: String,
    
    /** 文件大小（字节） */
    val size: Long,
    
    /** MIME 类型 */
    val type: String,
    
    /** 文件路径（content:// URI） */
    val path: String,
    
    /** 缩略图（Base64，包含 data URI 前缀） */
    val thumbnail: String?,
    
    /** 视频时长（秒，仅视频文件） */
    val duration: Long? = null
) {
    /**
     * 转换为 JSON 对象
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("size", size)
            put("type", type)
            put("path", path)
            put("thumbnail", thumbnail)
            if (duration != null) {
                put("duration", duration)
            }
        }
    }
    
    companion object {
        /**
         * 从 JSON 对象创建
         */
        fun fromJson(json: JSONObject): SelectedFile {
            return SelectedFile(
                id = json.optString("id"),
                name = json.optString("name"),
                size = json.optLong("size"),
                type = json.optString("type"),
                path = json.optString("path"),
                thumbnail = json.optString("thumbnail").takeIf { it.isNotEmpty() },
                duration = json.optLong("duration").takeIf { it > 0 }
            )
        }
    }
}
