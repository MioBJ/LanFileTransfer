package com.lft.lanfiletransfer

import android.app.Application
import com.lft.lanfiletransfer.modules.storage.database.LFTDatabase
import com.lft.lanfiletransfer.utils.LFTLogger

/**
 * 应用 Application 类
 * 
 * 负责全局初始化工作
 */
class LFTApplication : Application() {
    
    companion object {
        private const val TAG = "LFTApplication"
        
        /** Application 实例 */
        lateinit var instance: LFTApplication
            private set
    }
    
    /** 数据库实例 */
    val database: LFTDatabase by lazy {
        LFTDatabase.getInstance(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 初始化日志
        LFTLogger.init(this)
        LFTLogger.i(TAG, "Application 初始化")
        
        // 初始化数据库（延迟加载，首次访问时创建）
        // database 会在首次访问时自动初始化
    }
}
