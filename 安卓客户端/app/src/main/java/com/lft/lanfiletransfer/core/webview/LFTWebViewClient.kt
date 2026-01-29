package com.lft.lanfiletransfer.core.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lft.lanfiletransfer.utils.LFTLogger

/**
 * WebViewClient
 * 
 * 处理页面加载事件、URL 拦截等
 */
class LFTWebViewClient : WebViewClient() {
    
    companion object {
        private const val TAG = "LFTWebViewClient"
    }
    
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        LFTLogger.d(TAG, "页面开始加载: $url")
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        LFTLogger.d(TAG, "页面加载完成: $url")
    }
    
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        
        LFTLogger.d(TAG, "URL 加载: $url")
        
        // 处理特殊 URL scheme
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> {
                // 普通 HTTP/HTTPS 请求，由 WebView 处理
                false
            }
            url.startsWith("file://") -> {
                // 本地文件，由 WebView 处理
                false
            }
            else -> {
                // 其他 scheme，可以根据需要处理
                LFTLogger.w(TAG, "未处理的 URL scheme: $url")
                false
            }
        }
    }
    
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        
        val url = request?.url?.toString() ?: "unknown"
        val errorCode = error?.errorCode ?: -1
        val description = error?.description?.toString() ?: "unknown"
        
        LFTLogger.e(TAG, "页面加载错误: $url, code=$errorCode, desc=$description")
    }
    
    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        // 开发环境下可以忽略 SSL 错误
        // 注意：生产环境不应该忽略 SSL 错误
        LFTLogger.w(TAG, "SSL 错误: ${error?.primaryError}")
        
        // 局域网环境，允许继续
        handler?.proceed()
    }
}
