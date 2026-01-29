package com.lft.lanfiletransfer.modules.filepicker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import com.lft.lanfiletransfer.utils.LFTLogger
import java.io.ByteArrayOutputStream

/**
 * 缩略图生成器
 * 
 * 负责为图片和视频生成缩略图
 */
object LFTThumbnailGenerator {
    
    private const val TAG = "LFTThumbnailGenerator"
    
    /** 缩略图最大尺寸 */
    private const val THUMBNAIL_SIZE = 200
    
    /** JPEG 压缩质量 */
    private const val JPEG_QUALITY = 70
    
    /**
     * 生成缩略图
     * 
     * @param context Context
     * @param uri 文件 URI
     * @param mimeType MIME 类型
     * @return Base64 编码的缩略图（包含 data URI 前缀）
     */
    fun generateThumbnail(context: Context, uri: Uri, mimeType: String): String? {
        return try {
            val bitmap = when {
                mimeType.startsWith("image/") -> generateImageThumbnail(context, uri)
                mimeType.startsWith("video/") -> generateVideoThumbnail(context, uri)
                else -> null
            }
            
            bitmap?.let { bitmapToBase64(it) }
        } catch (e: Exception) {
            LFTLogger.e(TAG, "生成缩略图失败", e)
            null
        }
    }
    
    /**
     * 获取视频时长
     * 
     * @param context Context
     * @param uri 视频 URI
     * @return 视频时长（秒），失败返回 null
     */
    fun getVideoDuration(context: Context, uri: Uri): Long? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull()
            retriever.release()
            duration?.div(1000) // 毫秒转秒
        } catch (e: Exception) {
            LFTLogger.e(TAG, "获取视频时长失败", e)
            null
        }
    }
    
    /**
     * 生成图片缩略图
     */
    private fun generateImageThumbnail(context: Context, uri: Uri): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        
        // 先获取图片尺寸
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()
        
        // 计算采样率
        val sampleSize = calculateSampleSize(
            options.outWidth, options.outHeight,
            THUMBNAIL_SIZE, THUMBNAIL_SIZE
        )
        
        // 重新读取并缩放
        val inputStream2 = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(
            inputStream2, 
            null, 
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
        )
        inputStream2.close()
        
        return bitmap?.let { scaleBitmap(it, THUMBNAIL_SIZE) }
    }
    
    /**
     * 生成视频缩略图（第一帧）
     */
    private fun generateVideoThumbnail(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(0)
            frame?.let { scaleBitmap(it, THUMBNAIL_SIZE) }
        } catch (e: Exception) {
            LFTLogger.e(TAG, "生成视频缩略图失败", e)
            null
        } finally {
            retriever.release()
        }
    }
    
    /**
     * 缩放 Bitmap
     */
    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val scale = minOf(
            maxSize.toFloat() / width,
            maxSize.toFloat() / height
        )
        
        if (scale >= 1f) return bitmap
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * 计算采样率
     */
    private fun calculateSampleSize(
        width: Int, height: Int,
        reqWidth: Int, reqHeight: Int
    ): Int {
        var sampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / sampleSize) >= reqHeight &&
                (halfWidth / sampleSize) >= reqWidth
            ) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }
    
    /**
     * Bitmap 转 Base64
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        val bytes = outputStream.toByteArray()
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
