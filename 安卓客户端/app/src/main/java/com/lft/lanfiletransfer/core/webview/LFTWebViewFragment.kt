package com.lft.lanfiletransfer.core.webview

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.fragment.app.Fragment
import com.lft.lanfiletransfer.core.hotupdate.LFTHotUpdateManager
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridge
import com.lft.lanfiletransfer.databinding.FragmentWebviewBinding
import com.lft.lanfiletransfer.utils.LFTLogger

/**
 * WebView 容器 Fragment
 * 
 * 负责承载 H5 页面，管理 WebView 生命周期
 */
class LFTWebViewFragment : Fragment() {
    
    companion object {
        private const val TAG = "LFTWebViewFragment"
    }
    
    private var _binding: FragmentWebviewBinding? = null
    private val binding get() = _binding!!
    
    /** WebView 实例 */
    lateinit var webView: WebView
        private set
    
    /** JSBridge 实例 */
    lateinit var jsBridge: LFTJSBridge
        private set
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        LFTLogger.d(TAG, "onViewCreated")
        
        setupWebView()
        loadLocalH5()
    }
    
    /**
     * 配置 WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = binding.webView
        
        // 配置 WebView Settings
        webView.settings.apply {
            // 启用 JavaScript
            javaScriptEnabled = true
            
            // DOM Storage
            domStorageEnabled = true
            
            // 数据库存储
            databaseEnabled = true
            
            // 允许文件访问
            allowFileAccess = true
            allowContentAccess = true
            
            // 允许通过 file:// 协议加载的页面访问其他 file:// 资源
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            
            // 媒体播放不需要用户手势
            mediaPlaybackRequiresUserGesture = false
            
            // 混合内容模式（允许 HTTPS 页面加载 HTTP 资源）
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            // 缩放设置
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            
            // 视口设置
            useWideViewPort = true
            loadWithOverviewMode = true
            
            // 缓存设置
            cacheMode = WebSettings.LOAD_DEFAULT
            
            // 字体设置
            textZoom = 100
        }
        
        // 设置 WebViewClient
        webView.webViewClient = LFTWebViewClient()
        
        // 设置 WebChromeClient
        webView.webChromeClient = LFTWebChromeClient()
        
        // 创建并注入 JSBridge
        jsBridge = LFTJSBridge(webView, requireContext())
        webView.addJavascriptInterface(jsBridge, "AndroidBridge")
        
        LFTLogger.d(TAG, "WebView 配置完成")
    }
    
    /**
     * 加载本地 H5
     */
    fun loadLocalH5() {
        val h5Path = LFTHotUpdateManager.getH5IndexPath(requireContext())
        LFTLogger.i(TAG, "加载 H5: $h5Path")
        webView.loadUrl(h5Path)
    }
    
    /**
     * 加载指定 URL
     */
    fun loadUrl(url: String) {
        LFTLogger.i(TAG, "加载 URL: $url")
        webView.loadUrl(url)
    }
    
    /**
     * 刷新页面
     */
    fun reload() {
        LFTLogger.d(TAG, "刷新页面")
        webView.reload()
    }
    
    /**
     * 是否可以后退
     */
    fun canGoBack(): Boolean {
        return webView.canGoBack()
    }
    
    /**
     * 后退
     */
    fun goBack() {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }
    
    /**
     * 执行 JavaScript
     */
    fun evaluateJavascript(script: String, callback: ((String?) -> Unit)? = null) {
        webView.evaluateJavascript(script) { result ->
            callback?.invoke(result)
        }
    }
    
    override fun onResume() {
        super.onResume()
        webView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        webView.onPause()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // 清理 WebView
        webView.stopLoading()
        webView.removeJavascriptInterface("AndroidBridge")
        webView.destroy()
        
        _binding = null
        
        LFTLogger.d(TAG, "onDestroyView")
    }
}
