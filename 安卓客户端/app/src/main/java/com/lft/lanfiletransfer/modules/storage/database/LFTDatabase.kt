package com.lft.lanfiletransfer.modules.storage.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lft.lanfiletransfer.modules.storage.database.dao.ConfigDao
import com.lft.lanfiletransfer.modules.storage.database.dao.HistoryDao
import com.lft.lanfiletransfer.modules.storage.database.entity.ConfigEntity
import com.lft.lanfiletransfer.modules.storage.database.entity.HistoryEntity

/**
 * 应用数据库
 * 
 * 使用 Room 进行数据持久化
 */
@Database(
    entities = [ConfigEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LFTDatabase : RoomDatabase() {
    
    /**
     * 配置 DAO
     */
    abstract fun configDao(): ConfigDao
    
    /**
     * 历史记录 DAO
     */
    abstract fun historyDao(): HistoryDao
    
    companion object {
        private const val DB_NAME = "lan_file_transfer.db"
        
        @Volatile
        private var instance: LFTDatabase? = null
        
        /**
         * 获取数据库实例（单例）
         */
        fun getInstance(context: Context): LFTDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }
        
        private fun buildDatabase(context: Context): LFTDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                LFTDatabase::class.java,
                DB_NAME
            )
                .fallbackToDestructiveMigration() // 版本升级时销毁重建（开发阶段使用）
                .build()
        }
    }
}
