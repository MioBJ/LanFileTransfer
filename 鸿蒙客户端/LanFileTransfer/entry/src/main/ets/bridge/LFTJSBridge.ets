/**
 * 局域网文件传输 - 鸿蒙客户端
 * JSBridge 核心类
 *
 * 职责：
 * - 接收 H5 发来的消息
 * - 分发消息给对应的处理器
 * - 将处理结果回调给 H5
 * - 提供 Native -> H5 的通信能力
 */

import { webview } from '@kit.ArkWeb';
import { hilog } from '@kit.PerformanceAnalysisKit';
import {
  LFTBridgeHandler,
  LFTBridgeCallback,
  LFTJSBridgeInterface,
  H5Message,
  NativeResponse
} from './LFTBridgeTypes';
import {
  LOG_DOMAIN,
  LOG_TAG_PREFIX,
  JSBRIDGE_CODE_SUCCESS,
  JSBRIDGE_CODE_NOT_IMPLEMENTED
} from './LFTConstants';

const TAG = LOG_TAG_PREFIX + 'JSBridge';

/**
 * JSBridge 核心类
 * 负责 H5 ↔ Native 的双向通信
 */
export class LFTJSBridge implements LFTJSBridgeInterface {
  /** WebView 控制器引用 */
  private webController: webview.WebviewController | null = null;

  /** 方法名 -> 处理器 的映射表 */
  private handlerMap: Map<string, LFTBridgeHandler> = new Map();

  /**
   * 构造函数
   */
  constructor() {
    hilog.info(LOG_DOMAIN, TAG, 'JSBridge 初始化');
  }

  /**
   * 设置 WebView 控制器
   * @param controller - WebView 控制器
   */
  setWebController(controller: webview.WebviewController): void {
    this.webController = controller;
    hilog.info(LOG_DOMAIN, TAG, 'WebView 控制器已设置');
  }

  // ============================================
  // MARK: - 处理器注册
  // ============================================

  /**
   * 注册处理器
   * @param handler - 处理器对象
   */
  registerHandler(handler: LFTBridgeHandler): void {
    const methods = handler.getSupportedMethods();

    for (const method of methods) {
      if (this.handlerMap.has(method)) {
        hilog.warn(LOG_DOMAIN, TAG, `方法 ${method} 已被注册，将被覆盖`);
      }
      this.handlerMap.set(method, handler);
      hilog.debug(LOG_DOMAIN, TAG, `注册方法: ${method}`);
    }

    // 设置 bridge 引用（如果 handler 需要）
    if (handler.setBridge) {
      handler.setBridge(this);
    }
  }

  /**
   * 批量注册处理器
   * @param handlers - 处理器数组
   */
  registerHandlers(handlers: LFTBridgeHandler[]): void {
    for (const handler of handlers) {
      this.registerHandler(handler);
    }
  }

  // ============================================
  // MARK: - H5 -> Native 消息处理
  // ============================================

  /**
   * 处理来自 H5 的消息
   * 此方法由 WebView 的 onMessageReceive 回调触发
   * @param messageStr - JSON 格式的消息字符串
   */
  handleMessage(messageStr: string): void {
    hilog.debug(LOG_DOMAIN, TAG, `收到 H5 消息: ${messageStr}`);

    // 1. 解析消息
    let message: H5Message;
    try {
      message = JSON.parse(messageStr) as H5Message;
    } catch (e) {
      hilog.error(LOG_DOMAIN, TAG, `消息解析失败: ${messageStr}`);
      return;
    }

    // 2. 验证消息格式
    if (!message.method || !message.callbackId) {
      hilog.error(LOG_DOMAIN, TAG, '消息格式错误：缺少 method 或 callbackId');
      return;
    }

    // ArkTS 不支持解构赋值，逐个提取
    const method: string = message.method;
    const params: Record<string, Object> = message.params;
    const callbackId: string = message.callbackId;
    hilog.info(LOG_DOMAIN, TAG, `处理方法: ${method}, callbackId: ${callbackId}`);

    // 3. 查找处理器
    const handler = this.handlerMap.get(method);
    if (!handler) {
      hilog.warn(LOG_DOMAIN, TAG, `未找到方法处理器: ${method}`);
      this.invokeCallback(
        callbackId,
        false,
        null,
        JSBRIDGE_CODE_NOT_IMPLEMENTED,
        `方法 '${method}' 未实现`
      );
      return;
    }

    // 4. 调用处理器
    const callback: LFTBridgeCallback = (
      success: boolean,
      data: object | null,
      errorCode: number,
      errorMessage: string | null
    ) => {
      this.invokeCallback(callbackId, success, data, errorCode, errorMessage);
    };

    handler.handleMethod(method, params || {}, callback);
  }

  // ============================================
  // MARK: - Native -> H5 通信
  // ============================================

  /**
   * 调用 H5 的方法
   * @param method - 方法名（如 '_onUploadProgress'）
   * @param data - 传递的数据
   */
  callH5Method(method: string, data: object | null): void {
    if (!this.webController) {
      hilog.error(LOG_DOMAIN, TAG, 'WebView 控制器未设置，无法调用 H5 方法');
      return;
    }

    const jsonString = data ? JSON.stringify(data) : 'null';
    const js = `window.JSBridge.${method} && window.JSBridge.${method}(${jsonString})`;

    hilog.debug(LOG_DOMAIN, TAG, `调用 H5 方法: ${method}`);

    this.webController.runJavaScript(js)
      .then(() => {
        hilog.debug(LOG_DOMAIN, TAG, `调用 H5 方法成功: ${method}`);
      })
      .catch((error: Error) => {
        hilog.error(LOG_DOMAIN, TAG, `调用 H5 方法失败: ${method}, error: ${error.message}`);
      });
  }

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
  ): void {
    if (!this.webController) {
      hilog.error(LOG_DOMAIN, TAG, 'WebView 控制器未设置，无法回调 H5');
      return;
    }

    // 构造响应数据
    const response: NativeResponse = {
      code: success ? JSBRIDGE_CODE_SUCCESS : errorCode,
      message: success ? 'success' : (errorMessage || '未知错误'),
      data: data
    };

    const jsonString = JSON.stringify(response);
    const js = `window.JSBridge._callbacks['${callbackId}'](${jsonString})`;

    hilog.debug(LOG_DOMAIN, TAG, `回调 H5: callbackId=${callbackId}, success=${success}`);

    this.webController.runJavaScript(js)
      .then(() => {
        hilog.debug(LOG_DOMAIN, TAG, `回调 H5 成功: ${callbackId}`);
      })
      .catch((error: Error) => {
        hilog.error(LOG_DOMAIN, TAG, `回调 H5 失败: ${callbackId}, error: ${error.message}`);
      });
  }

  // ============================================
  // MARK: - 资源清理
  // ============================================

  /**
   * 销毁 JSBridge，释放资源
   */
  destroy(): void {
    this.handlerMap.clear();
    this.webController = null;
    hilog.info(LOG_DOMAIN, TAG, 'JSBridge 已销毁');
  }
}

// 导出单例（可选，也可以每个 WebView 创建一个实例）
let bridgeInstance: LFTJSBridge | null = null;

/**
 * 获取 JSBridge 单例
 * @returns JSBridge 实例
 */
export function getJSBridge(): LFTJSBridge {
  if (!bridgeInstance) {
    bridgeInstance = new LFTJSBridge();
  }
  return bridgeInstance;
}

/**
 * 创建新的 JSBridge 实例
 * @returns 新的 JSBridge 实例
 */
export function createJSBridge(): LFTJSBridge {
  return new LFTJSBridge();
}
