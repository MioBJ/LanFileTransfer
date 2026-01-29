/**
 * 局域网文件传输 - 鸿蒙客户端
 * JSBridge 类型定义
 *
 * 定义 JSBridge 通信过程中使用的各种数据类型
 */

// ============================================
// MARK: - 基础类型
// ============================================

/**
 * JSBridge 回调函数类型
 * @param success - 是否成功
 * @param data - 返回数据（成功时）
 * @param errorCode - 错误码（失败时）
 * @param errorMessage - 错误信息（失败时）
 */
export type LFTBridgeCallback = (
  success: boolean,
  data: object | null,
  errorCode: number,
  errorMessage: string | null
) => void;

/**
 * JSBridge 处理器接口
 * 所有功能模块的处理器都需要实现此接口
 */
export interface LFTBridgeHandler {
  /**
   * 获取处理器支持的方法名列表
   * @returns 方法名数组
   */
  getSupportedMethods(): string[];

  /**
   * 处理方法调用
   * @param method - 方法名
   * @param params - 参数对象
   * @param callback - 回调函数（必须调用）
   */
  handleMethod(method: string, params: Record<string, Object>, callback: LFTBridgeCallback): void;

  /**
   * 设置 JSBridge 引用（可选，用于主动调用 H5）
   * @param bridge - JSBridge 实例
   */
  setBridge?(bridge: LFTJSBridgeInterface): void;
}

/**
 * JSBridge 接口定义
 * 供 Handler 调用，实现 Native -> H5 的通信
 */
export interface LFTJSBridgeInterface {
  /**
   * 调用 H5 方法
   * @param method - 方法名
   * @param data - 数据
   */
  callH5Method(method: string, data: object | null): void;

  /**
   * 触发 H5 回调
   * @param callbackId - 回调 ID
   * @param success - 是否成功
   * @param data - 返回数据
   * @param errorCode - 错误码
   * @param errorMessage - 错误信息
   */
  invokeCallback(
    callbackId: string,
    success: boolean,
    data: object | null,
    errorCode: number,
    errorMessage: string | null
  ): void;
}

// ============================================
// MARK: - H5 消息类型
// ============================================

/**
 * H5 -> Native 消息格式
 */
export interface H5Message {
  /** 方法名 */
  method: string;
  /** 参数对象 */
  params: Record<string, Object>;
  /** 回调 ID */
  callbackId: string;
}

/**
 * Native -> H5 响应格式
 */
export interface NativeResponse {
  /** 状态码 */
  code: number;
  /** 消息 */
  message: string;
  /** 数据 */
  data: object | null;
}

// ============================================
// MARK: - 文件选择相关类型
// ============================================

/**
 * 文件选择参数
 */
export interface SelectFilesParams {
  /** 是否多选 */
  multiple?: boolean;
  /** 媒体类型 ['image', 'video'] */
  mediaTypes?: string[];
  /** 最大选择数量 */
  maxCount?: number;
}

/**
 * 选中的文件信息
 */
export interface SelectedFile {
  /** 文件唯一 ID */
  id: string;
  /** 文件名 */
  name: string;
  /** 文件大小（字节） */
  size: number;
  /** MIME 类型 */
  type: string;
  /** 文件路径 */
  path: string;
  /** 缩略图 Base64 */
  thumbnail?: string;
  /** 视频时长（秒，仅视频） */
  duration?: number;
}

// ============================================
// MARK: - WiFi 相关类型
// ============================================

/**
 * WiFi 信息
 */
export interface WiFiInfo {
  /** 是否连接 WiFi */
  connected: boolean;
  /** WiFi 名称 */
  ssid?: string;
  /** BSSID */
  bssid?: string;
}

// ============================================
// MARK: - 存储相关类型
// ============================================

/**
 * 历史记录
 */
export interface HistoryRecord {
  /** 记录 ID */
  id: string;
  /** 文件名 */
  fileName: string;
  /** 文件大小 */
  fileSize: number;
  /** 文件类型 */
  fileType?: string;
  /** 缩略图 */
  thumbnail?: string;
  /** 上传时间 (ISO 8601) */
  uploadTime: string;
  /** 上传耗时（秒） */
  uploadDuration?: number;
  /** 服务器 IP */
  serverIp?: string;
  /** 设备名称 */
  deviceName?: string;
}

/**
 * 获取历史记录参数
 */
export interface GetHistoryParams {
  /** 限制条数 */
  limit?: number;
  /** 偏移量 */
  offset?: number;
  /** 搜索关键词 */
  search?: string;
}

// ============================================
// MARK: - 视频播放相关类型
// ============================================

/**
 * 视频播放参数
 */
export interface PlayVideoParams {
  /** 视频 URL */
  url: string;
  /** 视频标题 */
  title?: string;
}

// ============================================
// MARK: - 设备信息类型
// ============================================

/**
 * 设备信息
 */
export interface DeviceInfo {
  /** 设备名称 */
  deviceName: string;
  /** 设备类型 */
  deviceType: string;
  /** 系统版本 */
  osVersion: string;
  /** 设备型号 */
  model: string;
}
