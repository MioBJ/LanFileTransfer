package com.lft.lanfiletransfer.utils

import android.content.Context
import android.util.Log

/**
 * 日志工具类
 * 
 * 统一管理应用日志输出
 */
object LFTLogger {
    
    private const val TAG_PREFIX = "LFT_"
    
    /** 是否启用日志 */
    private var enabled = true
    
    /** 最小日志级别 */
    private var minLevel = Log.DEBUG
    
    /**
     * 初始化日志
     */
    fun init(context: Context) {
        // 可以根据 BuildConfig 设置日志级别
        enabled = true
        minLevel = Log.DEBUG
    }
    
    /**
     * 设置是否启用日志
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
    
    /**
     * 设置最小日志级别
     */
    fun setMinLevel(level: Int) {
        this.minLevel = level
    }
    
    /**
     * Verbose 日志
     */
    fun v(tag: String, message: String) {
        if (enabled && minLevel <= Log.VERBOSE) {
            Log.v(TAG_PREFIX + tag, message)
        }
    }
    
    /**
     * Debug 日志
     */
    fun d(tag: String, message: String) {
        if (enabled && minLevel <= Log.DEBUG) {
            Log.d(TAG_PREFIX + tag, message)
        }
    }
    
    /**
     * Info 日志
     */
    fun i(tag: String, message: String) {
        if (enabled && minLevel <= Log.INFO) {
            Log.i(TAG_PREFIX + tag, message)
        }
    }
    
    /**
     * Warning 日志
     */
    fun w(tag: String, message: String) {
        if (enabled && minLevel <= Log.WARN) {
            Log.w(TAG_PREFIX + tag, message)
        }
    }
    
    /**
     * Warning 日志（带异常）
     */
    fun w(tag: String, message: String, throwable: Throwable) {
        if (enabled && minLevel <= Log.WARN) {
            Log.w(TAG_PREFIX + tag, message, throwable)
        }
    }
    
    /**
     * Error 日志
     */
    fun e(tag: String, message: String) {
        if (enabled && minLevel <= Log.ERROR) {
            Log.e(TAG_PREFIX + tag, message)
        }
    }
    
    /**
     * Error 日志（带异常）
     */
    fun e(tag: String, message: String, throwable: Throwable) {
        if (enabled && minLevel <= Log.ERROR) {
            Log.e(TAG_PREFIX + tag, message, throwable)
        }
    }
}
