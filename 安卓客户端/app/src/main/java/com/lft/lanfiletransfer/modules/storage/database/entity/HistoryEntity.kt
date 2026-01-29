package com.lft.lanfiletransfer.modules.storage.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 历史记录实体
 * 
 * 用于存储上传历史记录
 */
@Entity(tableName = "t_history")
data class HistoryEntity(
    /** 记录 ID（UUID） */
    @PrimaryKey
    val id: String,
    
    /** 文件名 */
    @ColumnInfo(name = "file_name")
    val fileName: String,
    
    /** 文件大小（字节） */
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    
    /** MIME 类型 */
    @ColumnInfo(name = "file_type")
    val fileType: String?,
    
    /** 缩略图（Base64） */
    @ColumnInfo(name = "thumbnail")
    val thumbnail: String?,
    
    /** 上传时间（ISO 8601 格式） */
    @ColumnInfo(name = "upload_time")
    val uploadTime: String,
    
    /** 上传耗时（秒） */
    @ColumnInfo(name = "upload_duration")
    val uploadDuration: Long?,
    
    /** 服务器 IP */
    @ColumnInfo(name = "server_ip")
    val serverIp: String?,
    
    /** 设备名称 */
    @ColumnInfo(name = "device_name")
    val deviceName: String?,
    
    /** 创建时间（毫秒时间戳） */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
