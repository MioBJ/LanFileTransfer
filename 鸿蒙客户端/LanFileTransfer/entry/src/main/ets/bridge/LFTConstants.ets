/**
 * 局域网文件传输 - 鸿蒙客户端
 * 常量定义
 *
 * 包含 JSBridge 方法名、错误码等常量定义
 */

// ============================================
// MARK: - JSBridge 方法名常量
// ============================================

/** 文件选择 */
export const JS_METHOD_SELECT_FILES = 'selectFiles';

/** 获取 WiFi 信息 */
export const JS_METHOD_GET_WIFI_INFO = 'getWiFiInfo';

/** 监听 WiFi 变化 */
export const JS_METHOD_ON_WIFI_CHANGED = 'onWiFiChanged';

/** 保存配置 */
export const JS_METHOD_SAVE_CONFIG = 'saveConfig';

/** 获取配置 */
export const JS_METHOD_GET_CONFIG = 'getConfig';

/** 删除配置 */
export const JS_METHOD_REMOVE_CONFIG = 'removeConfig';

/** 保存历史记录 */
export const JS_METHOD_SAVE_HISTORY = 'saveHistory';

/** 获取历史记录 */
export const JS_METHOD_GET_HISTORY = 'getHistory';

/** 删除历史记录 */
export const JS_METHOD_DELETE_HISTORY = 'deleteHistory';

/** 清空历史记录 */
export const JS_METHOD_CLEAR_HISTORY = 'clearHistory';

/** 播放视频 */
export const JS_METHOD_PLAY_VIDEO = 'playVideo';

/** 获取设备信息 */
export const JS_METHOD_GET_DEVICE_INFO = 'getDeviceInfo';

/** 启动后台上传 */
export const JS_METHOD_START_BACKGROUND_UPLOAD = 'startBackgroundUpload';

/** 取消后台上传 */
export const JS_METHOD_CANCEL_BACKGROUND_UPLOAD = 'cancelBackgroundUpload';

// ============================================
// MARK: - JSBridge 错误码
// ============================================

/** 成功 */
export const JSBRIDGE_CODE_SUCCESS = 200;

/** 参数错误 */
export const JSBRIDGE_CODE_BAD_REQUEST = 400;

/** 权限拒绝 */
export const JSBRIDGE_CODE_FORBIDDEN = 403;

/** 资源不存在 */
export const JSBRIDGE_CODE_NOT_FOUND = 404;

/** 内部错误 */
export const JSBRIDGE_CODE_INTERNAL_ERROR = 500;

/** 功能未实现 */
export const JSBRIDGE_CODE_NOT_IMPLEMENTED = 501;

// ============================================
// MARK: - 日志相关
// ============================================

/** 日志 Domain */
export const LOG_DOMAIN = 0x0000;

/** 日志 Tag 前缀 */
export const LOG_TAG_PREFIX = 'LFT_';

// ============================================
// MARK: - 存储相关
// ============================================

/** 配置存储键前缀 */
export const STORAGE_CONFIG_PREFIX = 'lft_config_';

/** 历史记录存储键 */
export const STORAGE_HISTORY_KEY = 'lft_history';

// ============================================
// MARK: - 缩略图配置
// ============================================

/** 缩略图最大宽度 */
export const THUMBNAIL_MAX_WIDTH = 200;

/** 缩略图最大高度 */
export const THUMBNAIL_MAX_HEIGHT = 200;

/** 缩略图 JPEG 质量 (0-100) */
export const THUMBNAIL_QUALITY = 70;
