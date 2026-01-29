package com.lft.lanfiletransfer.modules.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridge
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeCallback
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeDefines
import com.lft.lanfiletransfer.core.jsbridge.LFTJSBridgeHandler
import com.lft.lanfiletransfer.model.WiFiInfo
import com.lft.lanfiletransfer.utils.LFTLogger
import org.json.JSONObject

/**
 * WiFi 检测 Handler
 * 
 * 获取 WiFi 状态并监听网络变化
 */
class LFTWiFiHandler(
    private val context: Context
) : LFTJSBridgeHandler {
    
    companion object {
        private const val TAG = "LFTWiFiHandler"
    }
    
    override val supportedMethods = listOf(LFTJSBridgeDefines.METHOD_GET_WIFI_INFO)
    
    private var jsBridge: LFTJSBridge? = null
    
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    
    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    
    /** 网络回调 */
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    override fun setJSBridge(bridge: LFTJSBridge) {
        this.jsBridge = bridge
        // 开始监听网络变化
        startMonitoring()
    }
    
    override fun handleMethod(
        method: String,
        params: JSONObject,
        callback: LFTJSBridgeCallback
    ) {
        when (method) {
            LFTJSBridgeDefines.METHOD_GET_WIFI_INFO -> handleGetWiFiInfo(callback)
        }
    }
    
    /**
     * 获取 WiFi 信息
     */
    private fun handleGetWiFiInfo(callback: LFTJSBridgeCallback) {
        val wifiInfo = getWiFiInfo()
        callback(true, wifiInfo.toJson(), 0, null)
    }
    
    /**
     * 获取当前 WiFi 信息
     */
    fun getWiFiInfo(): WiFiInfo {
        // 检查 WiFi 是否连接
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val isWiFi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        
        if (!isWiFi) {
            return WiFiInfo(connected = false, ssid = "", bssid = "")
        }
        
        // 获取 WiFi 详情
        @Suppress("DEPRECATION")
        val connectionInfo = wifiManager.connectionInfo
        var ssid = connectionInfo?.ssid ?: ""
        
        // 移除引号
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length - 1)
        }
        
        // 未知 SSID（需要定位权限）
        if (ssid == "<unknown ssid>") {
            ssid = ""
        }
        
        return WiFiInfo(
            connected = true,
            ssid = ssid,
            bssid = connectionInfo?.bssid ?: ""
        )
    }
    
    /**
     * 开始监听网络变化
     */
    private fun startMonitoring() {
        if (networkCallback != null) {
            return
        }
        
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                LFTLogger.d(TAG, "WiFi 连接")
                notifyWiFiChanged()
            }
            
            override fun onLost(network: Network) {
                LFTLogger.d(TAG, "WiFi 断开")
                jsBridge?.callH5Method(
                    LFTJSBridgeDefines.CALLBACK_WIFI_CHANGED,
                    WiFiInfo(connected = false).toJson()
                )
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                notifyWiFiChanged()
            }
        }
        
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
            LFTLogger.d(TAG, "开始监听网络变化")
        } catch (e: Exception) {
            LFTLogger.e(TAG, "注册网络回调失败", e)
        }
    }
    
    /**
     * 停止监听
     */
    fun stopMonitoring() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                LFTLogger.e(TAG, "注销网络回调失败", e)
            }
        }
        networkCallback = null
        LFTLogger.d(TAG, "停止监听网络变化")
    }
    
    /**
     * 通知 WiFi 变化
     */
    private fun notifyWiFiChanged() {
        val wifiInfo = getWiFiInfo()
        jsBridge?.callH5Method(LFTJSBridgeDefines.CALLBACK_WIFI_CHANGED, wifiInfo.toJson())
    }
}
