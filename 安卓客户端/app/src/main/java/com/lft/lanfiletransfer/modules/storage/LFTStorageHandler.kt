package com.lft.lanfiletransfer.modules.storage

import android.content.Context
import com.lft.lanfiletransfer.LFTApplication
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeCallback
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeDefines
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeHandler
import com.lft.lanfiletransfer.modules.storage.database.entity.ConfigEntity
import com.lft.lanfiletransfer.modules.storage.database.entity.HistoryEntity
import com.lft.lanfiletransfer.utils.LFTLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 本地存储 Handler
 * 
 * 负责配置和历史记录的存储
 */
class LFTStorageHandler(
    private val context: Context
) : LFTJSBridgeHandler {
    
    companion object {
        private const val TAG = "LFTStorageHandler"
    }
    
    override val supportedMethods = listOf(
        LFTJSBridgeDefines.METHOD_SAVE_CONFIG,
        LFTJSBridgeDefines.METHOD_GET_CONFIG,
        LFTJSBridgeDefines.METHOD_REMOVE_CONFIG,
        LFTJSBridgeDefines.METHOD_SAVE_HISTORY,
        LFTJSBridgeDefines.METHOD_GET_HISTORY,
        LFTJSBridgeDefines.METHOD_DELETE_HISTORY,
        LFTJSBridgeDefines.METHOD_CLEAR_HISTORY
    )
    
    private val database by lazy { LFTApplication.instance.database }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun handleMethod(
        method: String,
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        scope.launch {
            try {
                when (method) {
                    LFTJSBridgeDefines.METHOD_SAVE_CONFIG -> handleSaveConfig(params, callback)
                    LFTJSBridgeDefines.METHOD_GET_CONFIG -> handleGetConfig(params, callback)
                    LFTJSBridgeDefines.METHOD_REMOVE_CONFIG -> handleRemoveConfig(params, callback)
                    LFTJSBridgeDefines.METHOD_SAVE_HISTORY -> handleSaveHistory(params, callback)
                    LFTJSBridgeDefines.METHOD_GET_HISTORY -> handleGetHistory(params, callback)
                    LFTJSBridgeDefines.METHOD_DELETE_HISTORY -> handleDeleteHistory(params, callback)
                    LFTJSBridgeDefines.METHOD_CLEAR_HISTORY -> handleClearHistory(callback)
                }
            } catch (e: Exception) {
                LFTLogger.e(TAG, "存储操作失败", e)
                callback(false, null, LFTJSBridgeDefines.CODE_INTERNAL_ERROR, e.message)
            }
        }
    }
    
    // ========== 配置操作 ==========
    
    private suspend fun handleSaveConfig(
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        val key = params.optString("key")
        val value = params.opt("value")
        
        if (key.isEmpty()) {
            callback(false, null, LFTJSBridgeDefines.CODE_BAD_REQUEST, "缺少参数 key")
            return
        }
        
        val jsonValue = when (value) {
            is JSONObject -> value.toString()
            is JSONArray -> value.toString()
            else -> value?.toString() ?: ""
        }
        
        database.configDao().insert(ConfigEntity(key, jsonValue))
        LFTLogger.d(TAG, "保存配置: $key")
        callback(true, null, 0, null)
    }
    
    private suspend fun handleGetConfig(
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        val key = params.optString("key")
        
        if (key.isEmpty()) {
            callback(false, null, LFTJSBridgeDefines.CODE_BAD_REQUEST, "缺少参数 key")
            return
        }
        
        val config = database.configDao().get(key)
        
        val value = config?.value?.let {
            try {
                // 尝试解析为 JSON
                when {
                    it.startsWith("{") -> JSONObject(it)
                    it.startsWith("[") -> JSONArray(it)
                    else -> it
                }
            } catch (e: Exception) {
                it
            }
        }
        
        LFTLogger.d(TAG, "获取配置: $key = ${value != null}")
        callback(true, value, 0, null)
    }
    
    private suspend fun handleRemoveConfig(
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        val key = params.optString("key")
        database.configDao().delete(key)
        LFTLogger.d(TAG, "删除配置: $key")
        callback(true, null, 0, null)
    }
    
    // ========== 历史记录操作 ==========
    
    private suspend fun handleSaveHistory(
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        val entity = HistoryEntity(
            id = params.optString("id", UUID.randomUUID().toString()),
            fileName = params.optString("fileName"),
            fileSize = params.optLong("fileSize"),
            fileType = params.optString("fileType").takeIf { it.isNotEmpty() },
            thumbnail = params.optString("thumbnail").takeIf { it.isNotEmpty() },
            uploadTime = params.optString("uploadTime"),
            uploadDuration = params.optLong("uploadDuration").takeIf { it > 0 },
            serverIp = params.optString("serverIp").takeIf { it.isNotEmpty() },
            deviceName = params.optString("deviceName").takeIf { it.isNotEmpty() }
        )
        
        database.historyDao().insert(entity)
        LFTLogger.d(TAG, "保存历史记录: ${entity.fileName}")
        callback(true, null, 0, null)
    }
    
    private suspend fun handleGetHistory(
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        val limit = params.optInt("limit", 100)
        val offset = params.optInt("offset", 0)
        val search = params.optString("search").takeIf { it.isNotEmpty() }
        
        val records = database.historyDao().getHistory(limit, offset, search)
        
        val jsonArray = JSONArray()
        records.forEach { entity ->
            jsonArray.put(JSONObject().apply {
                put("id", entity.id)
                put("fileName", entity.fileName)
                put("fileSize", entity.fileSize)
                put("fileType", entity.fileType)
                put("thumbnail", entity.thumbnail)
                put("uploadTime", entity.uploadTime)
                put("uploadDuration", entity.uploadDuration)
                put("serverIp", entity.serverIp)
                put("deviceName", entity.deviceName)
            })
        }
        
        LFTLogger.d(TAG, "获取历史记录: ${records.size} 条")
        callback(true, jsonArray, 0, null)
    }
    
    private suspend fun handleDeleteHistory(
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        val idsArray = params.optJSONArray("ids")
        val ids = (0 until (idsArray?.length() ?: 0))
            .map { idsArray!!.getString(it) }
        
        if (ids.isNotEmpty()) {
            database.historyDao().deleteByIds(ids)
            LFTLogger.d(TAG, "删除历史记录: ${ids.size} 条")
        }
        callback(true, null, 0, null)
    }
    
    private suspend fun handleClearHistory(callback: LFTJSBridgeCallback) {
        database.historyDao().deleteAll()
        LFTLogger.d(TAG, "清空历史记录")
        callback(true, null, 0, null)
    }
}
