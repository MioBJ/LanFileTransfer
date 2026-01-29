package com.lft.lanfiletransfer.core.jsbridge

import org.json.JSONObject

/**
 * JSBridge 回调类型别名
 * 
 * @param success 是否成功
 * @param data 返回数据
 * @param code 错误码（失败时）
 * @param message 错误消息（失败时）
 */
typealias LFTJSBridgeCallback = (success: Boolean, data: Any?, code: Int, message: String?) -> Unit

/**
 * JSBridge Handler 接口
 * 
 * 所有功能模块的 Handler 需要实现此接口
 */
interface LFTJSBridgeHandler {
    
    /**
     * 支持的方法列表
     */
    val supportedMethods: List<String>
    
    /**
     * 处理方法调用
     * 
     * @param method 方法名
     * @param params 参数
     * @param callback 回调函数
     */
    fun handleMethod(
        method: String,
        params: JSONObject,
        callback: LFTJSBridgeCallback
    )
    
    /**
     * 设置 JSBridge 引用（可选实现）
     * 
     * 用于 Native 主动调用 H5 的场景
     */
    fun setJSBridge(bridge: LFTJSBridge) {
        // 默认空实现
    }
}
