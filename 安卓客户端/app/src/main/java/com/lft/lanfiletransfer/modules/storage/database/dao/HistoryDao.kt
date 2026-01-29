package com.lft.lanfiletransfer.modules.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lft.lanfiletransfer.modules.storage.database.entity.HistoryEntity

/**
 * 历史记录 DAO
 * 
 * 提供历史记录表的增删改查操作
 */
@Dao
interface HistoryDao {
    
    /**
     * 获取历史记录列表
     * 
     * @param limit 返回条数
     * @param offset 偏移量
     * @param search 搜索关键词（按文件名模糊搜索）
     */
    @Query("""
        SELECT * FROM t_history 
        WHERE (:search IS NULL OR file_name LIKE '%' || :search || '%')
        ORDER BY upload_time DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getHistory(
        limit: Int = 100,
        offset: Int = 0,
        search: String? = null
    ): List<HistoryEntity>
    
    /**
     * 根据 ID 获取历史记录
     */
    @Query("SELECT * FROM t_history WHERE id = :id")
    suspend fun getById(id: String): HistoryEntity?
    
    /**
     * 插入或更新历史记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)
    
    /**
     * 批量插入历史记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<HistoryEntity>)
    
    /**
     * 根据 ID 列表删除历史记录
     */
    @Query("DELETE FROM t_history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
    
    /**
     * 清空所有历史记录
     */
    @Query("DELETE FROM t_history")
    suspend fun deleteAll()
    
    /**
     * 获取历史记录数量
     */
    @Query("SELECT COUNT(*) FROM t_history")
    suspend fun count(): Int
}
