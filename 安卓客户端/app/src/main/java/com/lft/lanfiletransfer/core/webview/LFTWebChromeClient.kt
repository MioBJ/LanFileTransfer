package com.lft.lanfiletransfer.core.webview

import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.lft.lanfiletransfer.utils.LFTLogger

/**
 * WebChromeClient
 * 
 * 处理 JavaScript 对话框、进度条、控制台日志等
 */
class LFTWebChromeClient : WebChromeClient() {
    
    companion object {
        private const val TAG = "LFTWebChromeClient"
    }
    
    /** 页面加载进度回调 */
    var onProgressChanged: ((Int) -> Unit)? = null
    
    /** 页面标题变化回调 */
    var onTitleChanged: ((String?) -> Unit)? = null
    
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged?.invoke(newProgress)
    }
    
    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        onTitleChanged?.invoke(title)
    }
    
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let { msg ->
            val level = when (msg.messageLevel()) {
                ConsoleMessage.MessageLevel.DEBUG -> "DEBUG"
                ConsoleMessage.MessageLevel.LOG -> "LOG"
                ConsoleMessage.MessageLevel.WARNING -> "WARN"
                ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                else -> "INFO"
            }
            
            LFTLogger.d(TAG, "[$level] ${msg.message()} (${msg.sourceId()}:${msg.lineNumber()})")
        }
        return true
    }
    
    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        LFTLogger.d(TAG, "JS Alert: $message")
        // 返回 false 使用默认的 Alert 对话框
        return false
    }
    
    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        LFTLogger.d(TAG, "JS Confirm: $message")
        // 返回 false 使用默认的 Confirm 对话框
        return false
    }
}
