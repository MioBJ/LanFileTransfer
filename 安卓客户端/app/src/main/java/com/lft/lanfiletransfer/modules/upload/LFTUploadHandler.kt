package com.lft.lanfiletransfer.modules.upload

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridge
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeCallback
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeDefines
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeHandler
import com.lft.lanfiletransfer.model.UploadTask
import com.lft.lanfiletransfer.utils.LFTLogger
import org.json.JSONObject

/**
 * 上传 Handler
 * 
 * 处理后台上传相关的 JSBridge 调用
 */
class LFTUploadHandler(
    private val context: Context
) : LFTJSBridgeHandler {
    
    companion object {
        private const val TAG = "LFTUploadHandler"
    }
    
    override val supportedMethods = listOf(
        LFTJSBridgeDefines.METHOD_START_BACKGROUND_UPLOAD,
        LFTJSBridgeDefines.METHOD_CANCEL_BACKGROUND_UPLOAD
    )
    
    private var jsBridge: LFTJSBridge? = null
    
    override fun setJSBridge(bridge: LFTJSBridge) {
        this.jsBridge = bridge
        
        // 设置上传管理器的回调
        val uploadManager = LFTBackgroundUploadManager.getInstance()
        
        uploadManager.onProgressCallback = { progress ->
            jsBridge?.callH5Method(
                LFTJSBridgeDefines.CALLBACK_UPLOAD_PROGRESS,
                progress.toJson()
            )
        }
        
        uploadManager.onCompleteCallback = { result ->
            jsBridge?.callH5Method(
                LFTJSBridgeDefines.CALLBACK_UPLOAD_COMPLETE,
                result.toJson()
            )
        }
    }
    
    override fun handleMethod(
        method: String,
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        when (method) {
            LFTJSBridgeDefines.METHOD_START_BACKGROUND_UPLOAD -> 
                handleStartBackgroundUpload(params, callback)
            LFTJSBridgeDefines.METHOD_CANCEL_BACKGROUND_UPLOAD -> 
                handleCancelBackgroundUpload(params, callback)
        }
    }
    
    /**
     * 启动后台上传
     */
    private fun handleStartBackgroundUpload(
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        val taskId = params.optString("taskId")
        val filePath = params.optString("filePath")
        val serverUrl = params.optString("serverUrl")
        
        if (taskId.isEmpty()) {
            callback(false, null, LFTJSBridgeDefines.CODE_BAD_REQUEST, "缺少参数 taskId")
            return
        }
        
        if (filePath.isEmpty()) {
            callback(false, null, LFTJSBridgeDefines.CODE_BAD_REQUEST, "缺少参数 filePath")
            return
        }
        
        if (serverUrl.isEmpty()) {
            callback(false, null, LFTJSBridgeDefines.CODE_BAD_REQUEST, "缺少参数 serverUrl")
            return
        }
        
        // 创建上传任务
        val task = UploadTask.fromJson(params)
        
        LFTLogger.i(TAG, "启动后台上传: $taskId, 文件: ${task.fileName}")
        
        // 启动前台服务
        val intent = Intent(context, LFTUploadService::class.java).apply {
            action = LFTUploadService.ACTION_START_UPLOAD
            putExtra(LFTUploadService.EXTRA_TASK_JSON, params.toString())
        }
        ContextCompat.startForegroundService(context, intent)
        
        callback(true, null, 0, null)
    }
    
    /**
     * 取消后台上传
     */
    private fun handleCancelBackgroundUpload(
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        val taskId = params.optString("taskId")
        
        if (taskId.isEmpty()) {
            callback(false, null, LFTJSBridgeDefines.CODE_BAD_REQUEST, "缺少参数 taskId")
            return
        }
        
        LFTLogger.i(TAG, "取消后台上传: $taskId")
        
        // 取消上传
        LFTBackgroundUploadManager.getInstance().cancelUpload(taskId)
        
        // 通知服务
        val intent = Intent(context, LFTUploadService::class.java).apply {
            action = LFTUploadService.ACTION_CANCEL_UPLOAD
            putExtra(LFTUploadService.EXTRA_TASK_ID, taskId)
        }
        context.startService(intent)
        
        callback(true, null, 0, null)
    }
}
