package com.lft.lanfiletransfer.modules.filepicker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeCallback
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeDefines
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeHandler
import com.lft.lanfiletransfer.model.SelectedFile
import com.lft.lanfiletransfer.utils.LFTLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 文件选择 Handler
 * 
 * 负责调用系统文件选择器，返回选中文件信息和缩略图
 */
class LFTFilePickerHandler(
    private val context: Context
) : LFTJSBridgeHandler {
    
    companion object {
        private const val TAG = "LFTFilePickerHandler"
        
        /** Activity Result Launcher（需要在 Activity 中设置） */
        private var launcher: ActivityResultLauncher<Intent>? = null
        
        /** 当前回调 */
        private var currentCallback: LFTJSBridgeCallback? = null
        
        /** 当前上下文 */
        private var currentContext: Context? = null
        
        /**
         * 设置 Launcher
         */
        fun setLauncher(launcher: ActivityResultLauncher<Intent>) {
            this.launcher = launcher
            LFTLogger.d(TAG, "Launcher 已设置")
        }
        
        /**
         * 处理选择结果
         */
        fun handlePickResult(result: ActivityResult) {
            val callback = currentCallback ?: return
            val ctx = currentContext ?: return
            
            currentCallback = null
            currentContext = null
            
            if (result.resultCode != android.app.Activity.RESULT_OK) {
                // 用户取消，返回空数组
                callback(true, JSONArray(), 0, null)
                return
            }
            
            val data = result.data
            val uris = mutableListOf<Uri>()
            
            // 获取选中的 URI
            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            } ?: data?.data?.let { uri ->
                uris.add(uri)
            }
            
            if (uris.isEmpty()) {
                callback(true, JSONArray(), 0, null)
                return
            }
            
            // 异步处理文件
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                val files = uris.mapNotNull { uri ->
                    processFile(ctx, uri)
                }
                
                val jsonArray = JSONArray()
                files.forEach { file ->
                    jsonArray.put(file.toJson())
                }
                
                withContext(Dispatchers.Main) {
                    callback(true, jsonArray, 0, null)
                }
            }
        }
        
        /**
         * 处理单个文件
         */
        private fun processFile(context: Context, uri: Uri): SelectedFile? {
            return try {
                val contentResolver = context.contentResolver
                
                // 获取文件信息
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                        
                        val name = if (nameIndex >= 0) it.getString(nameIndex) else "unknown"
                        val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
                        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                        
                        // 生成缩略图
                        val thumbnail = LFTThumbnailGenerator.generateThumbnail(
                            context, uri, mimeType
                        )
                        
                        // 获取视频时长
                        val duration = if (mimeType.startsWith("video/")) {
                            LFTThumbnailGenerator.getVideoDuration(context, uri)
                        } else null
                        
                        SelectedFile(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            size = size,
                            type = mimeType,
                            path = uri.toString(),
                            thumbnail = thumbnail,
                            duration = duration
                        )
                    } else null
                }
            } catch (e: Exception) {
                LFTLogger.e(TAG, "处理文件失败: $uri", e)
                null
            }
        }
    }
    
    override val supportedMethods = listOf(LFTJSBridgeDefines.METHOD_SELECT_FILES)
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun handleMethod(
        method: String,
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        when (method) {
            LFTJSBridgeDefines.METHOD_SELECT_FILES -> handleSelectFiles(params, callback)
        }
    }
    
    /**
     * 处理文件选择
     */
    private fun handleSelectFiles(
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        if (launcher == null) {
            callback(false, null, LFTJSBridgeDefines.CODE_INTERNAL_ERROR, "文件选择器未初始化")
            return
        }
        
        val multiple = params.optBoolean("multiple", true)
        val mediaTypes = params.optJSONArray("mediaTypes")?.let { array ->
            (0 until array.length()).map { array.getString(it) }
        } ?: listOf("image", "video")
        
        // 构建 MIME 类型
        val mimeTypes = mutableListOf<String>()
        if (mediaTypes.contains("image")) {
            mimeTypes.add("image/*")
        }
        if (mediaTypes.contains("video")) {
            mimeTypes.add("video/*")
        }
        
        // 创建选择 Intent
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            
            if (mimeTypes.size == 1) {
                type = mimeTypes[0]
            } else {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
            }
            
            if (multiple) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            
            // 持久化权限
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        
        // 保存回调和上下文
        currentCallback = callback
        currentContext = context
        
        // 启动选择器
        scope.launch {
            try {
                launcher?.launch(intent)
            } catch (e: Exception) {
                LFTLogger.e(TAG, "启动文件选择器失败", e)
                currentCallback = null
                currentContext = null
                callback(false, null, LFTJSBridgeDefines.CODE_INTERNAL_ERROR, "启动文件选择器失败: ${e.message}")
            }
        }
    }
}
