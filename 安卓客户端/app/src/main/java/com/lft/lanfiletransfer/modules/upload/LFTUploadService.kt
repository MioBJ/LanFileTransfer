package com.lft.lanfiletransfer.modules.upload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lft.lanfiletransfer.R
import com.lft.lanfiletransfer.model.UploadTask
import com.lft.lanfiletransfer.utils.LFTLogger
import org.json.JSONObject

/**
 * 上传前台服务
 * 
 * 保证 App 在后台时上传任务继续执行
 */
class LFTUploadService : Service() {
    
    companion object {
        private const val TAG = "LFTUploadService"
        
        private const val CHANNEL_ID = "upload_channel"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_UPLOAD = "com.lft.action.START_UPLOAD"
        const val ACTION_CANCEL_UPLOAD = "com.lft.action.CANCEL_UPLOAD"
        const val EXTRA_TASK_JSON = "extra_task_json"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
    
    private val uploadManager by lazy { LFTBackgroundUploadManager.getInstance() }
    
    /** 当前上传的任务 */
    private var currentTask: UploadTask? = null
    
    override fun onCreate() {
        super.onCreate()
        LFTLogger.d(TAG, "服务创建")
        createNotificationChannel()
        
        // 设置进度回调用于更新通知
        uploadManager.onProgressCallback = { progress ->
            updateNotification(progress.progress.toInt(), progress.taskId)
        }
        
        uploadManager.onCompleteCallback = { result ->
            LFTLogger.i(TAG, "上传完成: ${result.taskId}, 成功: ${result.success}")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_UPLOAD -> {
                val taskJson = intent.getStringExtra(EXTRA_TASK_JSON)
                taskJson?.let {
                    val task = UploadTask.fromJson(JSONObject(it))
                    currentTask = task
                    
                    LFTLogger.i(TAG, "启动上传任务: ${task.taskId}")
                    
                    // 启动前台服务
                    startForeground(NOTIFICATION_ID, createNotification(task.fileName, 0))
                    
                    // 开始上传
                    uploadManager.startUpload(this, task)
                }
            }
            ACTION_CANCEL_UPLOAD -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                taskId?.let {
                    LFTLogger.i(TAG, "取消上传任务: $taskId")
                    uploadManager.cancelUpload(it)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        LFTLogger.d(TAG, "服务销毁")
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "文件上传",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "文件上传进度通知"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建通知
     */
    private fun createNotification(fileName: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在上传文件")
            .setContentText("$fileName - $progress%")
            .setSmallIcon(R.drawable.ic_upload)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
    
    /**
     * 更新通知进度
     */
    private fun updateNotification(progress: Int, taskId: String) {
        val fileName = currentTask?.fileName ?: taskId
        val notification = createNotification(fileName, progress)
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
