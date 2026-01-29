package com.lft.lanfiletransfer.modules.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lft.lanfiletransfer.modules.storage.database.entity.ConfigEntity

/**
 * 配置 DAO
 * 
 * 提供配置表的增删改查操作
 */
@Dao
interface ConfigDao {
    
    /**
     * 根据 key 获取配置
     */
    @Query("SELECT * FROM t_config WHERE `key` = :key")
    suspend fun get(key: String): ConfigEntity?
    
    /**
     * 插入或更新配置
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ConfigEntity)
    
    /**
     * 删除配置
     */
    @Query("DELETE FROM t_config WHERE `key` = :key")
    suspend fun delete(key: String)
    
    /**
     * 获取所有配置
     */
    @Query("SELECT * FROM t_config")
    suspend fun getAll(): List<ConfigEntity>
    
    /**
     * 清空所有配置
     */
    @Query("DELETE FROM t_config")
    suspend fun deleteAll()
}
