/**
 * 局域网文件传输 - 鸿蒙客户端
 * 设备信息处理器
 *
 * 职责：
 * - 处理 getDeviceInfo 方法调用
 * - 获取设备名称、型号、系统版本等信息
 */

import { deviceInfo } from '@kit.BasicServicesKit';
import { createLogger, LFTModuleLogger } from '../../utils/LFTLogger';
import {
  LFTBridgeHandler,
  LFTBridgeCallback,
  DeviceInfo
} from '../../bridge/LFTBridgeTypes';
import {
  JS_METHOD_GET_DEVICE_INFO,
  JSBRIDGE_CODE_INTERNAL_ERROR
} from '../../bridge/LFTConstants';

const logger: LFTModuleLogger = createLogger('DeviceHandler');

/**
 * 设备信息处理器
 * 处理来自 H5 的设备信息请求
 */
export class LFTDeviceHandler implements LFTBridgeHandler {
  /**
   * 构造函数
   */
  constructor() {
    logger.info('DeviceHandler 初始化');
  }

  /**
   * 获取支持的方法列表
   */
  getSupportedMethods(): string[] {
    return [JS_METHOD_GET_DEVICE_INFO];
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

    if (method === JS_METHOD_GET_DEVICE_INFO) {
      this.handleGetDeviceInfo(callback);
    } else {
      callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '方法未实现');
    }
  }

  /**
   * 处理获取设备信息
   */
  private handleGetDeviceInfo(callback: LFTBridgeCallback): void {
    try {
      const info: DeviceInfo = {
        // 设备名称（用户设置的设备名）
        deviceName: this.getDeviceName(),
        // 设备类型
        deviceType: 'HarmonyOS',
        // 系统版本
        osVersion: this.getOSVersion(),
        // 设备型号
        model: this.getDeviceModel()
      };

      logger.info('获取设备信息: %{public}s', JSON.stringify(info));
      callback(true, info as object, 0, null);

    } catch (e) {
      logger.error('获取设备信息失败: %{public}s', JSON.stringify(e));
      callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '获取设备信息失败');
    }
  }

  /**
   * 获取设备名称
   * @returns 设备名称
   */
  private getDeviceName(): string {
    try {
      // 尝试获取设备型号作为名称
      const model = deviceInfo.productModel || deviceInfo.deviceType || 'HarmonyOS Device';
      return model;
    } catch (e) {
      return 'HarmonyOS Device';
    }
  }

  /**
   * 获取系统版本
   * @returns 系统版本字符串
   */
  private getOSVersion(): string {
    try {
      const osFullName = deviceInfo.osFullName || '';
      const sdkApiVersion = deviceInfo.sdkApiVersion || 0;

      if (osFullName) {
        return osFullName;
      }

      return `HarmonyOS API ${sdkApiVersion}`;
    } catch (e) {
      return 'Unknown';
    }
  }

  /**
   * 获取设备型号
   * @returns 设备型号
   */
  private getDeviceModel(): string {
    try {
      const model = deviceInfo.productModel || '';
      const brand = deviceInfo.brand || '';

      if (brand && model) {
        return `${brand} ${model}`;
      }

      return model || brand || 'Unknown';
    } catch (e) {
      return 'Unknown';
    }
  }
}
