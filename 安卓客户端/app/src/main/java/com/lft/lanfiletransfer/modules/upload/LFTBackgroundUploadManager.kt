package com.lft.lanfiletransfer.modules.upload

import android.content.Context
import android.net.Uri
import com.lft.lanfiletransfer.model.UploadProgress
import com.lft.lanfiletransfer.model.UploadResult
import com.lft.lanfiletransfer.model.UploadTask
import com.lft.lanfiletransfer.utils.LFTLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 后台上传管理器
 * 
 * 负责管理后台上传任务，使用 OkHttp 进行分片上传
 */
class LFTBackgroundUploadManager private constructor() {
    
    companion object {
        private const val TAG = "LFTBackgroundUploadManager"
        
        /** 默认分片大小：2MB */
        private const val DEFAULT_CHUNK_SIZE = 2 * 1024 * 1024L
        
        @Volatile
        private var instance: LFTBackgroundUploadManager? = null
        
        fun getInstance(): LFTBackgroundUploadManager {
            return instance ?: synchronized(this) {
                instance ?: LFTBackgroundUploadManager().also { instance = it }
            }
        }
    }
    
    /** HTTP 客户端 */
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /** 任务映射 */
    private val tasks = ConcurrentHashMap<String, UploadTask>()
    
    /** 协程作用域 */
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /** 进度回调 */
    var onProgressCallback: ((UploadProgress) -> Unit)? = null
    
    /** 完成回调 */
    var onCompleteCallback: ((UploadResult) -> Unit)? = null
    
    /**
     * 启动上传任务
     */
    fun startUpload(context: Context, task: UploadTask) {
        tasks[task.taskId] = task
        
        scope.launch {
            try {
                LFTLogger.i(TAG, "开始上传: ${task.taskId}, 文件: ${task.fileName}")
                
                // 如果没有 uploadId，先初始化
                if (task.uploadId.isNullOrEmpty()) {
                    val initResult = initUpload(task)
                    if (!initResult) return@launch
                }
                
                // 上传分片
                uploadChunks(context, task)
                
            } catch (e: CancellationException) {
                LFTLogger.d(TAG, "上传任务被取消: ${task.taskId}")
            } catch (e: Exception) {
                LFTLogger.e(TAG, "上传失败", e)
                notifyComplete(task.taskId, false, e.message ?: "上传失败")
            } finally {
                tasks.remove(task.taskId)
            }
        }
    }
    
    /**
     * 取消上传任务
     */
    fun cancelUpload(taskId: String) {
        LFTLogger.i(TAG, "取消上传: $taskId")
        tasks.remove(taskId)
    }
    
    /**
     * 初始化上传
     */
    private fun initUpload(task: UploadTask): Boolean {
        val params = JSONObject().apply {
            put("file_name", task.fileName)
            put("original_name", task.originalName)
            put("file_size", task.fileSize)
            put("content_type", task.fileType)
            put("chunk_size", task.chunkSize.takeIf { it > 0 } ?: DEFAULT_CHUNK_SIZE)
            put("device_name", task.deviceName)
            put("device_type", task.deviceType)
        }
        
        val body = params.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${task.serverUrl}/api/upload/init")
            .post(body)
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            notifyComplete(task.taskId, false, "初始化上传失败: ${response.code}")
            return false
        }
        
        val json = JSONObject(response.body?.string() ?: "{}")
        if (json.optInt("code") != 200) {
            notifyComplete(task.taskId, false, json.optString("message", "初始化上传失败"))
            return false
        }
        
        val data = json.optJSONObject("data")
        task.uploadId = data?.optString("upload_id")
        task.totalChunks = data?.optInt("total_chunks") ?: 0
        task.chunkSize = data?.optLong("chunk_size") ?: DEFAULT_CHUNK_SIZE
        
        LFTLogger.d(TAG, "初始化成功: uploadId=${task.uploadId}, totalChunks=${task.totalChunks}")
        
        return true
    }
    
    /**
     * 上传所有分片
     */
    private fun uploadChunks(context: Context, task: UploadTask) {
        // 找到起始分片索引
        val uploadedSet = task.uploadedChunks?.toSet() ?: emptySet()
        
        for (i in 0 until task.totalChunks) {
            // 检查是否取消
            if (!tasks.containsKey(task.taskId)) {
                throw CancellationException("任务已取消")
            }
            
            // 跳过已上传的分片
            if (uploadedSet.contains(i)) {
                continue
            }
            
            uploadChunk(context, task, i)
            
            // 通知进度
            val progress = (i + 1).toFloat() / task.totalChunks * 100
            notifyProgress(task.taskId, progress, task)
        }
        
        // 完成上传
        completeUpload(task)
    }
    
    /**
     * 上传单个分片
     */
    private fun uploadChunk(context: Context, task: UploadTask, chunkIndex: Int) {
        // 读取分片数据
        val chunkData = readChunk(context, task, chunkIndex)
        
        // 构建 multipart 请求
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_id", task.uploadId!!)
            .addFormDataPart("chunk_index", chunkIndex.toString())
            .addFormDataPart(
                "chunk",
                "chunk_$chunkIndex",
                chunkData.toRequestBody("application/octet-stream".toMediaType())
            )
            .build()
        
        val request = Request.Builder()
            .url("${task.serverUrl}/api/upload/chunk")
            .post(body)
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("上传分片失败: ${response.code}")
        }
        
        val json = JSONObject(response.body?.string() ?: "{}")
        if (json.optInt("code") != 200) {
            throw IOException(json.optString("message", "上传分片失败"))
        }
        
        LFTLogger.d(TAG, "分片 $chunkIndex/${task.totalChunks} 上传成功")
    }
    
    /**
     * 完成上传
     */
    private fun completeUpload(task: UploadTask) {
        val params = JSONObject().apply {
            put("upload_id", task.uploadId)
        }
        
        val body = params.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${task.serverUrl}/api/upload/complete")
            .post(body)
            .build()
        
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body?.string() ?: "{}")
        
        if (response.isSuccessful && json.optInt("code") == 200) {
            LFTLogger.i(TAG, "上传完成: ${task.taskId}")
            notifyComplete(task.taskId, true, "上传成功", task.uploadId)
        } else {
            notifyComplete(task.taskId, false, json.optString("message", "完成上传失败"))
        }
    }
    
    /**
     * 读取文件分片
     */
    private fun readChunk(context: Context, task: UploadTask, chunkIndex: Int): ByteArray {
        val uri = Uri.parse(task.filePath)
        val offset = chunkIndex.toLong() * task.chunkSize
        val size = minOf(task.chunkSize, task.fileSize - offset).toInt()
        
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("无法打开文件: ${task.filePath}")
        
        return inputStream.use { stream ->
            // 跳过前面的字节
            var skipped = 0L
            while (skipped < offset) {
                val skip = stream.skip(offset - skipped)
                if (skip <= 0) break
                skipped += skip
            }
            
            // 读取分片数据
            val buffer = ByteArray(size)
            var read = 0
            while (read < size) {
                val count = stream.read(buffer, read, size - read)
                if (count <= 0) break
                read += count
            }
            
            if (read < size) {
                buffer.copyOf(read)
            } else {
                buffer
            }
        }
    }
    
    /**
     * 通知进度
     */
    private fun notifyProgress(taskId: String, progress: Float, task: UploadTask) {
        val uploadProgress = UploadProgress(
            taskId = taskId,
            progress = progress,
            uploadedSize = (progress / 100 * task.fileSize).toLong(),
            totalSize = task.fileSize,
            speed = 0, // TODO: 计算速度
            uploadedChunks = (progress / 100 * task.totalChunks).toInt(),
            totalChunks = task.totalChunks
        )
        
        onProgressCallback?.invoke(uploadProgress)
    }
    
    /**
     * 通知完成
     */
    private fun notifyComplete(
        taskId: String,
        success: Boolean,
        message: String,
        uploadId: String? = null
    ) {
        val result = UploadResult(
            taskId = taskId,
            success = success,
            message = message,
            uploadId = uploadId,
            errorCode = if (success) null else -1,
            error = if (success) null else message
        )
        
        onCompleteCallback?.invoke(result)
    }
}
