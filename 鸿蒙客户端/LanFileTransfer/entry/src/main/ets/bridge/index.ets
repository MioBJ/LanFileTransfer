/**
 * 局域网文件传输 - 鸿蒙客户端
 * JSBridge 模块导出
 */

export { LFTJSBridge, getJSBridge, createJSBridge } from './LFTJSBridge';
export { getJSBridgeInjectScript, getConsoleForwardScript } from './LFTJSBridgeInject';

export {
  LFTBridgeHandler,
  LFTBridgeCallback,
  LFTJSBridgeInterface,
  H5Message,
  NativeResponse,
  SelectFilesParams,
  SelectedFile,
  WiFiInfo,
  HistoryRecord,
  GetHistoryParams,
  PlayVideoParams,
  DeviceInfo
} from './LFTBridgeTypes';

export {
  // 方法名常量
  JS_METHOD_SELECT_FILES,
  JS_METHOD_GET_WIFI_INFO,
  JS_METHOD_ON_WIFI_CHANGED,
  JS_METHOD_SAVE_CONFIG,
  JS_METHOD_GET_CONFIG,
  JS_METHOD_REMOVE_CONFIG,
  JS_METHOD_SAVE_HISTORY,
  JS_METHOD_GET_HISTORY,
  JS_METHOD_DELETE_HISTORY,
  JS_METHOD_CLEAR_HISTORY,
  JS_METHOD_PLAY_VIDEO,
  JS_METHOD_GET_DEVICE_INFO,
  JS_METHOD_START_BACKGROUND_UPLOAD,
  JS_METHOD_CANCEL_BACKGROUND_UPLOAD,

  // 错误码
  JSBRIDGE_CODE_SUCCESS,
  JSBRIDGE_CODE_BAD_REQUEST,
  JSBRIDGE_CODE_FORBIDDEN,
  JSBRIDGE_CODE_NOT_FOUND,
  JSBRIDGE_CODE_INTERNAL_ERROR,
  JSBRIDGE_CODE_NOT_IMPLEMENTED,

  // 其他常量
  LOG_DOMAIN,
  LOG_TAG_PREFIX,
  STORAGE_CONFIG_PREFIX,
  STORAGE_HISTORY_KEY,
  THUMBNAIL_MAX_WIDTH,
  THUMBNAIL_MAX_HEIGHT,
  THUMBNAIL_QUALITY
} from './LFTConstants';
