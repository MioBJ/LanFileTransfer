package com.lft.lanfiletransfer.core.jsbridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.lft.lanfiletransfer.modules.filepicker.LFTFilePickerHandler
import com.lft.lanfiletransfer.modules.player.LFTPlayerHandler
import com.lft.lanfiletransfer.modules.storage.LFTStorageHandler
import com.lft.lanfiletransfer.modules.upload.LFTUploadHandler
import com.lft.lanfiletransfer.modules.wifi.LFTWiFiHandler
import com.lft.lanfiletransfer.utils.LFTLogger
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSBridge 核心类
 * 
 * 负责 H5 与 Native 的双向通信
 * 
 * H5 调用方式：
 * ```javascript
 * window.AndroidBridge.call(JSON.stringify({
 *     method: "selectFiles",
 *     params: { multiple: true },
 *     callbackId: "cb_123456"
 * }))
 * ```
 */
class LFTJSBridge(
    private val webView: WebView,
    private val context: Context
) {
    
    companion object {
        private const val TAG = "LFTJSBridge"
    }
    
    /** Handler 映射表：方法名 -> Handler */
    private val handlers = mutableMapOf<String, LFTJSBridgeHandler>()
    
    /** 主线程 Handler */
    private val mainHandler = Handler(Looper.getMainLooper())
    
    init {
        // 注册所有 Handler
        registerHandlers()
        LFTLogger.i(TAG, "JSBridge 初始化完成")
    }
    
    /**
     * 注册所有 Handler
     */
    private fun registerHandlers() {
        // 文件选择
        val filePickerHandler = LFTFilePickerHandler(context)
        registerHandler(filePickerHandler)
        
        // WiFi 检测
        val wifiHandler = LFTWiFiHandler(context)
        wifiHandler.setJSBridge(this)
        registerHandler(wifiHandler)
        
        // 本地存储
        val storageHandler = LFTStorageHandler(context)
        registerHandler(storageHandler)
        
        // 视频播放
        val playerHandler = LFTPlayerHandler(context)
        registerHandler(playerHandler)
        
        // 后台上传
        val uploadHandler = LFTUploadHandler(context)
        uploadHandler.setJSBridge(this)
        registerHandler(uploadHandler)
    }
    
    /**
     * 注册 Handler
     */
    private fun registerHandler(handler: LFTJSBridgeHandler) {
        handler.supportedMethods.forEach { method ->
            handlers[method] = handler
            LFTLogger.d(TAG, "注册方法: $method")
        }
    }
    
    /**
     * H5 调用 Native 的入口
     * 
     * 通过 @JavascriptInterface 暴露给 JavaScript
     * 
     * @param messageJson 消息 JSON 字符串
     */
    @JavascriptInterface
    fun call(messageJson: String) {
        try {
            LFTLogger.d(TAG, "收到 H5 调用: $messageJson")
            
            val message = JSONObject(messageJson)
            val method = message.getString("method")
            val params = message.optJSONObject("params") ?: JSONObject()
            val callbackId = message.getString("callbackId")
            
            LFTLogger.d(TAG, "解析消息: method=$method, callbackId=$callbackId")
            
            // 分发到对应的 Handler
            val handler = handlers[method]
            if (handler != null) {
                handler.handleMethod(method, params) { success, data, code, msg ->
                    callbackToH5(callbackId, success, data, code, msg)
                }
            } else {
                LFTLogger.w(TAG, "方法未实现: $method")
                callbackToH5(
                    callbackId, 
                    false, 
                    null, 
                    LFTJSBridgeDefines.CODE_NOT_IMPLEMENTED, 
                    "方法未实现: $method"
                )
            }
            
        } catch (e: Exception) {
            LFTLogger.e(TAG, "解析消息失败", e)
        }
    }
    
    /**
     * 回调结果给 H5
     * 
     * @param callbackId 回调 ID
     * @param success 是否成功
     * @param data 返回数据
     * @param code 错误码
     * @param message 消息
     */
    private fun callbackToH5(
        callbackId: String,
        success: Boolean,
        data: Any?,
        code: Int,
        message: String?
    ) {
        val result = JSONObject().apply {
            put("code", if (success) LFTJSBridgeDefines.CODE_SUCCESS else code)
            put("message", message ?: if (success) "success" else "error")
            put("data", data)
        }
        
        val js = "window.JSBridge._callbacks['$callbackId']($result)"
        
        // 必须在主线程执行
        mainHandler.post {
            LFTLogger.d(TAG, "回调 H5: callbackId=$callbackId, success=$success")
            webView.evaluateJavascript(js, null)
        }
    }
    
    /**
     * Native 主动调用 H5 方法
     * 
     * 用于上传进度通知、WiFi 状态变化等场景
     * 
     * @param methodName H5 方法名
     * @param data 数据
     */
    fun callH5Method(methodName: String, data: Any?) {
        val dataJson = when (data) {
            is JSONObject -> data.toString()
            is JSONArray -> data.toString()
            is String -> "\"$data\""
            is Number -> data.toString()
            is Boolean -> data.toString()
            null -> "null"
            else -> JSONObject().apply { put("data", data) }.toString()
        }
        
        val js = "window.JSBridge.$methodName && window.JSBridge.$methodName($dataJson)"
        
        mainHandler.post {
            LFTLogger.d(TAG, "调用 H5 方法: $methodName")
            webView.evaluateJavascript(js, null)
        }
    }
    
    /**
     * 销毁
     */
    fun destroy() {
        handlers.clear()
        LFTLogger.i(TAG, "JSBridge 已销毁")
    }
}
