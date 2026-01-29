package com.lft.lanfiletransfer.modules.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeCallback
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeDefines
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeHandler
import com.lft.lanfiletransfer.utils.LFTLogger
import org.json.JSONObject
import java.io.File

/**
 * 视频播放 Handler
 * 
 * 调用系统播放器播放视频
 */
class LFTPlayerHandler(
    private val context: Context
) : LFTJSBridgeHandler {
    
    companion object {
        private const val TAG = "LFTPlayerHandler"
    }
    
    override val supportedMethods = listOf(LFTJSBridgeDefines.METHOD_PLAY_VIDEO)
    
    override fun handleMethod(
        method: String,
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        when (method) {
            LFTJSBridgeDefines.METHOD_PLAY_VIDEO -> handlePlayVideo(params, callback)
        }
    }
    
    /**
     * 播放视频
     */
    private fun handlePlayVideo(
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        val url = params.optString("url", "")
        val title = params.optString("title", "")
        
        if (url.isEmpty()) {
            callback(false, null, LFTJSBridgeDefines.CODE_BAD_REQUEST, "缺少参数 url")
            return
        }
        
        try {
            val uri = when {
                url.startsWith("content://") -> Uri.parse(url)
                url.startsWith("file://") -> Uri.parse(url)
                url.startsWith("/") -> Uri.fromFile(File(url))
                url.startsWith("http://") || url.startsWith("https://") -> Uri.parse(url)
                else -> Uri.parse(url)
            }
            
            LFTLogger.i(TAG, "播放视频: $uri, 标题: $title")
            
            // 使用系统播放器
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(intent)
            
            callback(true, null, 0, null)
        } catch (e: Exception) {
            LFTLogger.e(TAG, "播放视频失败", e)
            callback(false, null, LFTJSBridgeDefines.CODE_INTERNAL_ERROR, "播放失败: ${e.message}")
        }
    }
}
