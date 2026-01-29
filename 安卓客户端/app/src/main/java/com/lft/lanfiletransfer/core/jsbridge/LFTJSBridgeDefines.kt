package com.lft.lanfiletransfer.core.jsbridge

/**
 * JSBridge 常量定义
 */
object LFTJSBridgeDefines {
    
    // ========== 方法名常量 ==========
    
    /** 选择文件 */
    const val METHOD_SELECT_FILES = "selectFiles"
    
    /** 获取 WiFi 信息 */
    const val METHOD_GET_WIFI_INFO = "getWiFiInfo"
    
    /** 保存配置 */
    const val METHOD_SAVE_CONFIG = "saveConfig"
    
    /** 获取配置 */
    const val METHOD_GET_CONFIG = "getConfig"
    
    /** 删除配置 */
    const val METHOD_REMOVE_CONFIG = "removeConfig"
    
    /** 保存历史记录 */
    const val METHOD_SAVE_HISTORY = "saveHistory"
    
    /** 获取历史记录 */
    const val METHOD_GET_HISTORY = "getHistory"
    
    /** 删除历史记录 */
    const val METHOD_DELETE_HISTORY = "deleteHistory"
    
    /** 清空历史记录 */
    const val METHOD_CLEAR_HISTORY = "clearHistory"
    
    /** 播放视频 */
    const val METHOD_PLAY_VIDEO = "playVideo"
    
    /** 启动后台上传 */
    const val METHOD_START_BACKGROUND_UPLOAD = "startBackgroundUpload"
    
    /** 取消后台上传 */
    const val METHOD_CANCEL_BACKGROUND_UPLOAD = "cancelBackgroundUpload"
    
    // ========== 回调方法名（Native → H5）==========
    
    /** WiFi 状态变化回调 */
    const val CALLBACK_WIFI_CHANGED = "_wifiChangedCallback"
    
    /** 上传进度回调 */
    const val CALLBACK_UPLOAD_PROGRESS = "_onUploadProgress"
    
    /** 上传完成回调 */
    const val CALLBACK_UPLOAD_COMPLETE = "_onUploadComplete"
    
    // ========== 错误码 ==========
    
    /** 成功 */
    const val CODE_SUCCESS = 200
    
    /** 参数错误 */
    const val CODE_BAD_REQUEST = 400
    
    /** 权限拒绝 */
    const val CODE_FORBIDDEN = 403
    
    /** 资源不存在 */
    const val CODE_NOT_FOUND = 404
    
    /** 内部错误 */
    const val CODE_INTERNAL_ERROR = 500
    
    /** 功能未实现 */
    const val CODE_NOT_IMPLEMENTED = 501
}
