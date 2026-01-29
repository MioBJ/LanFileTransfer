package com.lft.lanfiletransfer.model

import org.json.JSONObject

/**
 * WiFi 信息数据类
 */
data class WiFiInfo(
    /** 是否连接 WiFi */
    val connected: Boolean,
    
    /** WiFi 名称（SSID） */
    val ssid: String = "",
    
    /** BSSID */
    val bssid: String = ""
) {
    /**
     * 转换为 JSON 对象
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("connected", connected)
            put("ssid", ssid)
            put("bssid", bssid)
        }
    }
    
    companion object {
        /**
         * 从 JSON 对象创建
         */
        fun fromJson(json: JSONObject): WiFiInfo {
            return WiFiInfo(
                connected = json.optBoolean("connected"),
                ssid = json.optString("ssid", ""),
                bssid = json.optString("bssid", "")
            )
        }
    }
}
