/**
 * 局域网文件传输 - 鸿蒙客户端
 * WiFi 处理器
 *
 * 职责：
 * - 处理 getWiFiInfo 方法调用
 * - 处理 onWiFiChanged 方法调用
 * - 监听网络状态变化并通知 H5
 */

import { wifiManager } from '@kit.ConnectivityKit';
import { connection } from '@kit.NetworkKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { createLogger, LFTModuleLogger } from '../../utils/LFTLogger';
import {
  LFTBridgeHandler,
  LFTBridgeCallback,
  LFTJSBridgeInterface,
  WiFiInfo
} from '../../bridge/LFTBridgeTypes';
import {
  JS_METHOD_GET_WIFI_INFO,
  JS_METHOD_ON_WIFI_CHANGED,
  JSBRIDGE_CODE_INTERNAL_ERROR
} from '../../bridge/LFTConstants';

const logger: LFTModuleLogger = createLogger('WiFiHandler');

/**
 * WiFi 处理器
 * 处理 WiFi 相关的 JSBridge 调用
 */
export class LFTWiFiHandler implements LFTBridgeHandler {
  /** JSBridge 引用 */
  private bridge: LFTJSBridgeInterface | null = null;

  /** 网络连接对象 */
  private netConnection: connection.NetConnection | null = null;

  /** 是否已开始监听 */
  private isMonitoring: boolean = false;

  /**
   * 构造函数
   */
  constructor() {
    logger.info('WiFiHandler 初始化');
  }

  /**
   * 获取支持的方法列表
   */
  getSupportedMethods(): string[] {
    return [JS_METHOD_GET_WIFI_INFO, JS_METHOD_ON_WIFI_CHANGED];
  }

  /**
   * 设置 JSBridge 引用
   */
  setBridge(bridge: LFTJSBridgeInterface): void {
    this.bridge = bridge;
  }

  /**
   * 处理方法调用
   */
  handleMethod(
    method: string,
    params: Record<string, Object>,
    callback: LFTBridgeCallback
  ): void {
    logger.debug('处理方法: %{public}s', method);

    switch (method) {
      case JS_METHOD_GET_WIFI_INFO:
        this.handleGetWiFiInfo(callback);
        break;
      case JS_METHOD_ON_WIFI_CHANGED:
        this.handleOnWiFiChanged(callback);
        break;
      default:
        callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '方法未实现');
    }
  }

  /**
   * 处理获取 WiFi 信息
   */
  private async handleGetWiFiInfo(callback: LFTBridgeCallback): Promise<void> {
    try {
      const wifiInfo = await this.getWiFiInfoAsync();
      logger.info('获取 WiFi 信息: %{public}s', JSON.stringify(wifiInfo));
      callback(true, wifiInfo as object, 0, null);
    } catch (e) {
      const error = e as BusinessError;
      logger.error('获取 WiFi 信息失败: %{public}s', JSON.stringify(error));
      callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '获取 WiFi 信息失败: ' + error.message);
    }
  }

  /**
   * 处理监听 WiFi 变化
   */
  private handleOnWiFiChanged(callback: LFTBridgeCallback): void {
    try {
      this.startMonitoring();
      logger.info('开始监听 WiFi 变化');
      callback(true, null, 0, null);
    } catch (e) {
      const error = e as BusinessError;
      logger.error('开始监听 WiFi 失败: %{public}s', JSON.stringify(error));
      callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '监听 WiFi 失败: ' + error.message);
    }
  }

  /**
   * 获取当前 WiFi 信息（同步版本，返回基础信息）
   * @returns WiFi 信息
   */
  private getWiFiInfo(): WiFiInfo {
    try {
      // 检查 WiFi 是否开启
      const isWifiActive = wifiManager.isWifiActive();
      if (!isWifiActive) {
        return { connected: false };
      }

      // 检查是否已连接
      const isConnected = wifiManager.isConnected();
      if (!isConnected) {
        return { connected: false };
      }

      // 返回已连接状态，详细信息通过异步获取
      return {
        connected: true,
        ssid: '',
        bssid: ''
      };

    } catch (e) {
      logger.error('获取 WiFi 信息异常: %{public}s', JSON.stringify(e));
      return { connected: false };
    }
  }

  /**
   * 异步获取详细 WiFi 信息
   * @returns WiFi 信息 Promise
   */
  private async getWiFiInfoAsync(): Promise<WiFiInfo> {
    try {
      // 检查 WiFi 是否开启
      const isWifiActive = wifiManager.isWifiActive();
      if (!isWifiActive) {
        return { connected: false };
      }

      // 检查是否已连接
      const isConnected = wifiManager.isConnected();
      if (!isConnected) {
        return { connected: false };
      }

      // 异步获取连接信息
      const linkedInfo = await wifiManager.getLinkedInfo();
      if (!linkedInfo) {
        return { connected: false };
      }

      return {
        connected: true,
        ssid: linkedInfo.ssid || '',
        bssid: linkedInfo.bssid || ''
      };

    } catch (e) {
      logger.error('获取 WiFi 信息异常: %{public}s', JSON.stringify(e));
      return { connected: false };
    }
  }

  /**
   * 开始监听网络变化
   */
  private startMonitoring(): void {
    if (this.isMonitoring) {
      logger.debug('已在监听中，跳过');
      return;
    }

    try {
      // 创建网络连接监听
      this.netConnection = connection.createNetConnection();

      // 监听网络可用
      this.netConnection.on('netAvailable', (netHandle: connection.NetHandle) => {
        logger.info('网络可用: %{public}d', netHandle.netId);
        this.notifyWiFiChanged();
      });

      // 监听网络不可用
      this.netConnection.on('netUnavailable', () => {
        logger.info('网络不可用');
        this.notifyWiFiChanged();
      });

      // 监听网络能力变化
      this.netConnection.on('netCapabilitiesChange', () => {
        logger.debug('网络能力变化');
        this.notifyWiFiChanged();
      });

      // 监听网络丢失
      this.netConnection.on('netLost', () => {
        logger.info('网络丢失');
        this.notifyWiFiChanged();
      });

      // 注册监听
      this.netConnection.register((err) => {
        if (err) {
          logger.error('注册网络监听失败: %{public}s', JSON.stringify(err));
          return;
        }
        logger.info('网络监听注册成功');
      });

      this.isMonitoring = true;

    } catch (e) {
      logger.error('启动网络监听失败: %{public}s', JSON.stringify(e));
    }
  }

  /**
   * 通知 H5 WiFi 状态变化
   */
  private notifyWiFiChanged(): void {
    if (!this.bridge) {
      logger.warn('Bridge 未设置，无法通知 H5');
      return;
    }

    const wifiInfo = this.getWiFiInfo();
    logger.info('通知 H5 WiFi 变化: %{public}s', JSON.stringify(wifiInfo));

    // 调用 H5 的回调函数
    this.bridge.callH5Method('_wifiChangedCallback', wifiInfo);
  }

  /**
   * 停止监听
   */
  stopMonitoring(): void {
    if (!this.isMonitoring || !this.netConnection) {
      return;
    }

    try {
      this.netConnection.unregister((err) => {
        if (err) {
          logger.error('取消网络监听失败: %{public}s', JSON.stringify(err));
          return;
        }
        logger.info('网络监听已取消');
      });

      this.isMonitoring = false;
      this.netConnection = null;

    } catch (e) {
      logger.error('停止网络监听失败: %{public}s', JSON.stringify(e));
    }
  }
}
