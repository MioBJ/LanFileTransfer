package com.lft.lanfiletransfer.modules.storage.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 配置实体
 * 
 * 用于存储应用配置（服务器地址、用户偏好等）
 */
@Entity(tableName = "t_config")
data class ConfigEntity(
    /** 配置键名 */
    @PrimaryKey
    val key: String,
    
    /** 配置值（JSON 字符串） */
    @ColumnInfo(name = "value")
    val value: String,
    
    /** 更新时间（毫秒时间戳） */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
