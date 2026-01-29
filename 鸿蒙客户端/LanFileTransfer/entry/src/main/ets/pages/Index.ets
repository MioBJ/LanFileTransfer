/**
 * 局域网文件传输 - 鸿蒙客户端
 * 主页面（WebView 容器）
 *
 * 职责：
 * - 承载 WKWebView 加载 H5 页面
 * - 初始化和管理 JSBridge
 * - 处理页面生命周期
 */

import { webview } from '@kit.ArkWeb';
import { common } from '@kit.AbilityKit';
import { window, display } from '@kit.ArkUI';
import { resourceManager } from '@kit.LocalizationKit';
import { util } from '@kit.ArkTS';
import { createLogger, LFTModuleLogger } from '../utils/LFTLogger';
import { LFTJSBridge, createJSBridge } from '../bridge/LFTJSBridge';
import { getJSBridgeInjectScript, getConsoleForwardScript } from '../bridge/LFTJSBridgeInject';
import { LFTFilePickerHandler } from '../modules/filepicker/LFTFilePickerHandler';
import { LFTWiFiHandler } from '../modules/wifi/LFTWiFiHandler';
import { LFTStorageHandler } from '../modules/storage/LFTStorageHandler';
import { LFTPlayerHandler } from '../modules/player/LFTPlayerHandler';
import { LFTDeviceHandler } from '../modules/device/LFTDeviceHandler';

const logger: LFTModuleLogger = createLogger('IndexPage');

/** H5 资源基础路径 */
const H5_BASE_PATH = 'h5/';

/**
 * Console 日志数据接口
 */
interface ConsoleLogData {
  type?: string;
  message?: string;
}

/**
 * 主页面组件
 */
@Entry
@Component
struct Index {
  /** WebView 控制器 */
  private webController: webview.WebviewController = new webview.WebviewController();

  /** JSBridge 实例 */
  private jsBridge: LFTJSBridge = createJSBridge();

  /** 上下文 - 从 AppStorage 获取 */
  private context: common.UIAbilityContext | undefined = AppStorage.get<common.UIAbilityContext>('context');

  /** 加载进度 */
  @State loadProgress: number = 0;

  /** 是否显示进度条 */
  @State showProgress: boolean = false;

  // 诊断状态已移除（生产版不显示调试信息）

  /** 状态栏高度 */
  @State statusBarHeight: number = 0;

  /** 导航栏高度 */
  @State navBarHeight: number = 0;

  /**
   * 页面即将显示
   */
  aboutToAppear(): void {
    logger.info('页面即将显示');

    // 获取安全区域
    this.getSafeAreaInsets();

    // 初始化 WebView 配置
    this.initWebViewConfig();

    // 注册 JSBridge 处理器
    this.registerHandlers();
  }

  /**
   * 页面即将消失
   */
  aboutToDisappear(): void {
    logger.info('页面即将消失');

    // 清理 JSBridge
    this.jsBridge.destroy();
  }

  /**
   * 获取安全区域
   */
  private getSafeAreaInsets(): void {
    try {
      const windowStage = AppStorage.get<window.WindowStage>('windowStage');
      if (windowStage) {
        windowStage.getMainWindow((err, mainWindow) => {
          if (err.code) {
            logger.error('获取主窗口失败: %{public}s', JSON.stringify(err));
            return;
          }

          // 获取屏幕密度用于转换
          const displayInfo = display.getDefaultDisplaySync();
          const density = displayInfo.densityPixels;

          const avoidArea = mainWindow.getWindowAvoidArea(window.AvoidAreaType.TYPE_SYSTEM);
          this.statusBarHeight = avoidArea.topRect.height / density;

          const navArea = mainWindow.getWindowAvoidArea(window.AvoidAreaType.TYPE_NAVIGATION_INDICATOR);
          this.navBarHeight = navArea.bottomRect.height / density;

          logger.info('安全区域: statusBar=%{public}d, navBar=%{public}d',
            this.statusBarHeight, this.navBarHeight);
        });
      }
    } catch (e) {
      logger.error('获取安全区域失败: %{public}s', JSON.stringify(e));
    }
  }

  /**
   * 初始化 WebView 配置
   */
  private initWebViewConfig(): void {
    try {
      // 启用 JavaScript
      webview.WebviewController.setWebDebuggingAccess(true);

      logger.info('WebView 配置初始化完成');
    } catch (e) {
      logger.error('WebView 配置初始化失败: %{public}s', JSON.stringify(e));
    }
  }

  /**
   * 注册 JSBridge 处理器
   */
  private registerHandlers(): void {
    logger.info('开始注册 JSBridge 处理器');

    if (!this.context) {
      logger.error('上下文未初始化，无法注册处理器');
      return;
    }

    // 文件选择处理器
    const filePickerHandler = new LFTFilePickerHandler(this.context);
    this.jsBridge.registerHandler(filePickerHandler);

    // WiFi 处理器
    const wifiHandler = new LFTWiFiHandler();
    this.jsBridge.registerHandler(wifiHandler);

    // 存储处理器
    const storageHandler = new LFTStorageHandler(this.context);
    this.jsBridge.registerHandler(storageHandler);

    // 播放器处理器
    const playerHandler = new LFTPlayerHandler(this.context);
    this.jsBridge.registerHandler(playerHandler);

    // 设备信息处理器
    const deviceHandler = new LFTDeviceHandler();
    this.jsBridge.registerHandler(deviceHandler);

    logger.info('JSBridge 处理器注册完成');
  }

  /**
   * 获取初始空白页 URL
   * 实际内容通过 loadData 加载
   */
  private getInitialUrl(): string {
    // 使用 http://localhost/ 作为统一源，确保 history 路由命中 "/"
    return 'http://localhost/';
  }

  /**
   * 获取文件的 MIME 类型
   */
  private getMimeType(fileName: string): string {
    const ext = fileName.split('.').pop()?.toLowerCase() || '';
    const mimeMap = new Map<string, string>([
      ['html', 'text/html'],
      ['htm', 'text/html'],
      ['js', 'application/javascript'],
      ['mjs', 'application/javascript'],
      ['css', 'text/css'],
      ['json', 'application/json'],
      ['png', 'image/png'],
      ['jpg', 'image/jpeg'],
      ['jpeg', 'image/jpeg'],
      ['gif', 'image/gif'],
      ['svg', 'image/svg+xml'],
      ['ico', 'image/x-icon'],
      ['woff', 'font/woff'],
      ['woff2', 'font/woff2'],
      ['ttf', 'font/ttf'],
      ['eot', 'application/vnd.ms-fontobject']
    ]);
    return mimeMap.get(ext) || 'application/octet-stream';
  }

  /**
   * 处理本地资源请求
   */
  private handleLocalResource(url: string, isMainFrame: boolean): WebResourceResponse | null {
    try {
      // 解析 URL，提取文件名
      let fileName = '';

      // 处理 http://localhost/ 协议（模拟本地服务器）
      if (url === 'http://localhost' || url === 'http://localhost/' ||
        url.startsWith('http://localhost/') || url.startsWith('http://localhost:')) {
        // http://localhost/index.html -> h5/index.html
        // http://localhost/xxx.js -> h5/xxx.js
        let path = url.replace(/^http:\/\/localhost(:\d+)?\/?/, '');
        if (!path || path === '/') {
          path = 'index.html';
        }
        fileName = H5_BASE_PATH + path;
      }
      // 处理 resource://rawfile/ 协议（鸿蒙原生协议）
      else if (url.startsWith('resource://rawfile/')) {
        fileName = url.replace('resource://rawfile/', '');
      }
      // 处理 lft:// 自定义协议（兼容）
      else if (url.startsWith('lft://')) {
        let path = url.replace('lft://local/', '').replace('lft://local', '');
        if (!path || path === '/') {
          path = 'index.html';
        }
        if (path.startsWith('/')) {
          path = path.substring(1);
        }
        fileName = H5_BASE_PATH + path;
      }
      // 处理 internal://app/ 协议
      else if (url.startsWith('internal://app/')) {
        const path = url.replace('internal://app/', '');
        fileName = H5_BASE_PATH + path;
      }
      // 处理 file:// 协议
      else if (url.startsWith('file://')) {
        const path = url.replace('file://', '');
        if (path.includes('/rawfile/')) {
          fileName = path.split('/rawfile/')[1] || '';
        } else if (path.includes('/h5/')) {
          fileName = H5_BASE_PATH + path.split('/h5/')[1];
        }
      }

      if (!fileName) {
        logger.warn('无法解析资源路径: %{public}s', url);
        return null;
      }

      // 去除查询参数
      if (fileName.includes('?')) {
        fileName = fileName.split('?')[0];
      }

      // 去除开头的斜杠
      if (fileName.startsWith('/')) {
        fileName = fileName.substring(1);
      }

      logger.debug('加载本地资源: %{public}s -> %{public}s', url, fileName);

      // 获取资源管理器
      const resMgr = this.context?.resourceManager;
      if (!resMgr) {
        logger.error('资源管理器不可用');
        return null;
      }

      // 读取 rawfile 资源
      const rawFileContent = resMgr.getRawFileContentSync(fileName);
      if (!rawFileContent || rawFileContent.byteLength === 0) {
        logger.warn('资源不存在或为空: %{public}s', fileName);
        return null;
      }

      logger.info('读取资源成功: %{public}s, 大小: %{public}d 字节', fileName, rawFileContent.byteLength);

      // 处理 HTML：注入 JSBridge 脚本，确保在业务脚本前执行
      let responseData: Uint8Array = rawFileContent;
      if (fileName.endsWith('index.html')) {
        try {
          const decoder = new util.TextDecoder('utf-8');
          const html = decoder.decode(rawFileContent);
          const injectedScript = `<script>${getConsoleForwardScript()}${getJSBridgeInjectScript()}</script>`;
          const modifiedHtml = html.includes('<head>')
            ? html.replace('<head>', `<head>${injectedScript}`)
            : `${injectedScript}${html}`;
          const encoder = new util.TextEncoder();
          responseData = encoder.encode(modifiedHtml);
          logger.info('已注入 JSBridge 脚本到 index.html');
        } catch (e) {
          logger.error('注入 JSBridge 脚本失败: %{public}s', JSON.stringify(e));
        }
      }

      // 获取 MIME 类型
      const mimeType = this.getMimeType(fileName);

      // 创建响应头（关键：添加 CORS 头解决跨域问题）
      const headers: Array<Header> = [
        { headerKey: 'Content-Type', headerValue: mimeType + '; charset=utf-8' },
        { headerKey: 'Content-Length', headerValue: String(responseData.byteLength) },
        { headerKey: 'Access-Control-Allow-Origin', headerValue: '*' },
        { headerKey: 'Access-Control-Allow-Methods', headerValue: 'GET, POST, OPTIONS' },
        { headerKey: 'Access-Control-Allow-Headers', headerValue: '*' },
        { headerKey: 'Cache-Control', headerValue: 'no-store, no-cache, must-revalidate' },
        { headerKey: 'Pragma', headerValue: 'no-cache' },
        { headerKey: 'Expires', headerValue: '0' },
        { headerKey: 'X-Content-Type-Options', headerValue: 'nosniff' }
      ];

      // 创建响应
      const response = new WebResourceResponse();
      response.setResponseHeader(headers);
      const buffer = responseData.buffer.slice(
        responseData.byteOffset,
        responseData.byteOffset + responseData.byteLength
      );
      response.setResponseData(buffer);
      response.setResponseEncoding('utf-8');
      response.setResponseMimeType(mimeType);
      response.setResponseCode(200);
      response.setReasonMessage('OK');
      response.setResponseIsReady(true);

      return response;

    } catch (e) {
      logger.error('加载本地资源失败: %{public}s, error: %{public}s', url, JSON.stringify(e));
      return null;
    }
  }

  /**
   * 注入 JSBridge 脚本
   */
  private injectJSBridge(): void {
    // 注入 Console 转发脚本
    this.webController.runJavaScript(getConsoleForwardScript())
      .catch((e: Error) => {
        logger.error('注入 Console 脚本失败: %{public}s', e.message);
      });

    // 注入 JSBridge 脚本
    this.webController.runJavaScript(getJSBridgeInjectScript())
      .then(() => {
        logger.info('JSBridge 脚本注入成功');
      })
      .catch((e: Error) => {
        logger.error('注入 JSBridge 脚本失败: %{public}s', e.message);
      });
  }

  // 调试用的 DOM 探针/错误收集已移除

  /**
   * 构建页面
   */
  build() {
    Column() {
      // 状态栏占位
      Row()
        .width('100%')
        .height(this.statusBarHeight)
        .backgroundColor('#FFFFFF')

      // 加载进度条
      if (this.showProgress && this.loadProgress < 100) {
        Progress({ value: this.loadProgress, total: 100, type: ProgressType.Linear })
          .width('100%')
          .height(2)
          .color('#007AFF')
          .backgroundColor(Color.Transparent)
      }

      // WebView 容器
      Web({
        src: this.getInitialUrl(),
        controller: this.webController
      })
        .width('100%')
        .layoutWeight(1)
        .javaScriptAccess(true)
        .domStorageAccess(true)
        .fileAccess(true)
        .imageAccess(true)
        .mixedMode(MixedMode.All)
        .cacheMode(CacheMode.None)
        .onControllerAttached(() => {
          logger.info('WebView 控制器已绑定');

          // 设置 WebView 控制器到 JSBridge
          this.jsBridge.setWebController(this.webController);

          // 注册 JavaScript 代理对象，供 H5 调用
          this.webController.registerJavaScriptProxy(
            {
              postMessage: (message: string) => {
                logger.debug('收到 H5 消息: %{public}s', message);
                this.jsBridge.handleMessage(message);
              }
            },
            'ohosJSBridge',
            ['postMessage']
          );

          // 注册 Console 日志代理
          this.webController.registerJavaScriptProxy(
            {
              log: (message: string) => {
                try {
                  const data: ConsoleLogData = JSON.parse(message) as ConsoleLogData;
                  const type: string = data.type || 'log';
                  const text: string = data.message || '';
                  if (type === 'error') {
                    logger.error('JSConsole[%{public}s]: %{public}s', type, text);
                  } else if (type === 'warn') {
                    logger.warn('JSConsole[%{public}s]: %{public}s', type, text);
                  } else {
                    logger.debug('JSConsole[%{public}s]: %{public}s', type, text);
                  }
                } catch (e) {
                  logger.debug('JSConsole: %{public}s', message);
                }
              }
            },
            'ohosConsole',
            ['log']
          );
        })
        .onPageBegin(() => {
          logger.info('页面开始加载');
          this.showProgress = true;
          this.loadProgress = 0;
        })
        .onPageEnd(() => {
          logger.info('页面加载完成');
          this.showProgress = false;
          this.loadProgress = 100;

          // 注入 JSBridge 脚本
          this.injectJSBridge();

          // 重新派发 JSBridgeReady，避免 H5 监听时序问题
          this.webController.runJavaScript('window.__dispatchJSBridgeReady && window.__dispatchJSBridgeReady()')
            .then(() => {
              logger.info('JSBridgeReady 事件已重发');
            })
            .catch((e: Error) => {
              logger.error('重发 JSBridgeReady 失败: %{public}s', e.message);
            });

        })
        .onProgressChange((event) => {
          if (event) {
            this.loadProgress = event.newProgress;
          }
        })
        .onErrorReceive((event) => {
          if (event) {
            const url = event.request?.getRequestUrl?.() ?? 'unknown';
            logger.error('页面加载错误: %{public}s, url=%{public}s',
              JSON.stringify(event.error), url);
          }
        })
        .onHttpErrorReceive((event) => {
          if (event) {
            const url = event.request?.getRequestUrl?.() ?? 'unknown';
            logger.error('HTTP 错误: %{public}d %{public}s',
              event.response.getResponseCode(),
              event.response.getReasonMessage());
            logger.error('HTTP 错误 URL: %{public}s', url);
          }
        })
        .onConsole((event) => {
          if (event) {
            const message = event.message;
            const level = message.getMessageLevel();
            const text = message.getMessage();
            const sourceId = message.getSourceId();
            const lineNumber = message.getLineNumber();

            switch (level) {
              case MessageLevel.Error:
                logger.error('Console[error]: %{public}s (%{public}s:%{public}d)',
                  text, sourceId, lineNumber);
                break;
              case MessageLevel.Warn:
                logger.warn('Console[warn]: %{public}s', text);
                break;
              default:
                logger.debug('Console[log]: %{public}s', text);
            }
          }
          return false; // 继续默认处理
        })
        .onInterceptRequest((event) => {
          // 拦截 http://localhost/ 请求，从本地 rawfile 加载资源
          if (event) {
            const request = event.request;
            const url = request.getRequestUrl();

            const method = request.getRequestMethod?.() ?? 'GET';
            const isMainFrame = request.isMainFrame?.() ?? false;
            logger.info('【请求拦截】%{public}s method=%{public}s main=%{public}s',
              url, method, String(isMainFrame));

            const headers = request.getRequestHeader?.();
            if (headers) {
              logger.debug('【请求头】%{public}s', JSON.stringify(headers));
            }

            // 拦截 http://localhost/ 请求（模拟本地服务器）
            if (url.startsWith('http://localhost/') || url.startsWith('http://localhost:')) {
              const response = this.handleLocalResource(url, isMainFrame);
              if (response) {
                logger.info('【资源加载成功】%{public}s', url);
                return response;
              } else {
                logger.error('【资源加载失败】%{public}s', url);
              }
            }
          }
          return null;
        })

      // 底部安全区域占位
      Row()
        .width('100%')
        .height(this.navBarHeight)
        .backgroundColor('#FFFFFF')
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}
