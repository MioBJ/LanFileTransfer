package com.lft.lanfiletransfer.core.hotupdate

import android.content.Context
import com.lft.lanfiletransfer.utils.LFTLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * H5 热更新管理器
 * 
 * 负责检测 H5 版本更新、下载和安装更新包
 */
object LFTHotUpdateManager {
    
    private const val TAG = "LFTHotUpdateManager"
    
    /** H5 目录名 */
    private const val H5_DIR = "h5"
    
    /** 版本信息文件名 */
    private const val VERSION_FILE = "version.json"
    
    /** HTTP 客户端 */
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /**
     * 获取 H5 首页路径
     * 
     * 优先使用热更新版本，否则使用内置版本
     */
    fun getH5IndexPath(context: Context): String {
        val filesH5Dir = File(context.filesDir, H5_DIR)
        val filesH5Index = File(filesH5Dir, "index.html")
        
        return if (filesH5Index.exists()) {
            // 使用热更新版本
            LFTLogger.d(TAG, "使用热更新版本: ${filesH5Index.absolutePath}")
            "file://${filesH5Index.absolutePath}"
        } else {
            // 使用内置版本
            LFTLogger.d(TAG, "使用内置版本")
            "file:///android_asset/h5/index.html"
        }
    }
    
    /**
     * 获取本地 H5 版本
     */
    fun getLocalVersion(context: Context): String? {
        val versionFile = File(context.filesDir, "$H5_DIR/$VERSION_FILE")
        
        return if (versionFile.exists()) {
            try {
                val json = JSONObject(versionFile.readText())
                json.optString("version")
            } catch (e: Exception) {
                LFTLogger.e(TAG, "读取本地版本失败", e)
                null
            }
        } else {
            null
        }
    }
    
    /**
     * 检查更新
     * 
     * @param serverUrl 服务器基础地址
     * @return 如果有新版本，返回版本信息 JSON；否则返回 null
     */
    suspend fun checkUpdate(context: Context, serverUrl: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$serverUrl/api/h5-version")
                    .build()
                
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    LFTLogger.w(TAG, "检查更新失败: ${response.code}")
                    return@withContext null
                }
                
                val json = JSONObject(response.body?.string() ?: "{}")
                if (json.optInt("code") != 200) {
                    return@withContext null
                }
                
                val data = json.optJSONObject("data") ?: return@withContext null
                val remoteVersion = data.optString("version")
                val localVersion = getLocalVersion(context)
                
                if (remoteVersion.isNotEmpty() && remoteVersion != localVersion) {
                    LFTLogger.i(TAG, "发现新版本: $remoteVersion (本地: $localVersion)")
                    data
                } else {
                    LFTLogger.d(TAG, "无需更新 (本地: $localVersion)")
                    null
                }
            } catch (e: Exception) {
                LFTLogger.e(TAG, "检查更新异常", e)
                null
            }
        }
    }
    
    /**
     * 下载并安装更新
     * 
     * @param downloadUrl 下载地址
     * @param newVersion 新版本号
     * @return 是否成功
     */
    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        newVersion: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                LFTLogger.i(TAG, "开始下载更新: $downloadUrl")
                
                // 下载 zip 文件
                val request = Request.Builder().url(downloadUrl).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    LFTLogger.e(TAG, "下载失败: ${response.code}")
                    return@withContext false
                }
                
                // 保存 zip 文件
                val tempZip = File(context.cacheDir, "h5_update.zip")
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(tempZip).use { output ->
                        input.copyTo(output)
                    }
                }
                
                LFTLogger.d(TAG, "下载完成，开始解压")
                
                // 解压到目标目录
                val targetDir = File(context.filesDir, H5_DIR)
                if (targetDir.exists()) {
                    targetDir.deleteRecursively()
                }
                targetDir.mkdirs()
                
                unzip(tempZip, targetDir)
                
                // 写入版本信息
                val versionFile = File(targetDir, VERSION_FILE)
                versionFile.writeText(JSONObject().apply {
                    put("version", newVersion)
                    put("updateTime", System.currentTimeMillis())
                }.toString())
                
                // 清理临时文件
                tempZip.delete()
                
                LFTLogger.i(TAG, "更新安装成功: $newVersion")
                true
            } catch (e: Exception) {
                LFTLogger.e(TAG, "下载更新失败", e)
                false
            }
        }
    }
    
    /**
     * 清除热更新版本（恢复到内置版本）
     */
    fun clearUpdate(context: Context) {
        val h5Dir = File(context.filesDir, H5_DIR)
        if (h5Dir.exists()) {
            h5Dir.deleteRecursively()
            LFTLogger.i(TAG, "已清除热更新版本")
        }
    }
    
    /**
     * 解压 zip 文件
     */
    private fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                
                // 安全检查：防止路径穿越攻击
                if (!file.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    throw SecurityException("路径穿越攻击: ${entry.name}")
                }
                
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                
                entry = zis.nextEntry
            }
        }
    }
}
