/**
 * 局域网文件传输 - 鸿蒙客户端
 * 日志工具类
 *
 * 封装 hilog，提供统一的日志接口
 */

import { hilog } from '@kit.PerformanceAnalysisKit';
import { LOG_DOMAIN, LOG_TAG_PREFIX } from '../bridge/LFTConstants';

/**
 * 日志级别枚举
 */
export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3
}

/**
 * 当前日志级别（可通过配置修改）
 */
let currentLogLevel: LogLevel = LogLevel.DEBUG;

/**
 * 设置日志级别
 * @param level - 日志级别
 */
export function setLogLevel(level: LogLevel): void {
  currentLogLevel = level;
}

/**
 * 获取完整的 Tag
 * @param module - 模块名
 * @returns 完整 Tag
 */
function getTag(module: string): string {
  return LOG_TAG_PREFIX + module;
}

/**
 * 输出调试日志
 * @param module - 模块名
 * @param message - 日志内容
 * @param args - 格式化参数
 */
export function logDebug(module: string, message: string, ...args: Object[]): void {
  if (currentLogLevel <= LogLevel.DEBUG) {
    hilog.debug(LOG_DOMAIN, getTag(module), message, ...args);
  }
}

/**
 * 输出信息日志
 * @param module - 模块名
 * @param message - 日志内容
 * @param args - 格式化参数
 */
export function logInfo(module: string, message: string, ...args: Object[]): void {
  if (currentLogLevel <= LogLevel.INFO) {
    hilog.info(LOG_DOMAIN, getTag(module), message, ...args);
  }
}

/**
 * 输出警告日志
 * @param module - 模块名
 * @param message - 日志内容
 * @param args - 格式化参数
 */
export function logWarn(module: string, message: string, ...args: Object[]): void {
  if (currentLogLevel <= LogLevel.WARN) {
    hilog.warn(LOG_DOMAIN, getTag(module), message, ...args);
  }
}

/**
 * 输出错误日志
 * @param module - 模块名
 * @param message - 日志内容
 * @param args - 格式化参数
 */
export function logError(module: string, message: string, ...args: Object[]): void {
  if (currentLogLevel <= LogLevel.ERROR) {
    hilog.error(LOG_DOMAIN, getTag(module), message, ...args);
  }
}

/**
 * 创建模块专用的日志器
 * @param module - 模块名
 * @returns 日志器对象
 */
export function createLogger(module: string): LFTModuleLogger {
  return new LFTModuleLogger(module);
}

/**
 * 模块日志器类
 * 便于在模块中使用，不需要每次传入模块名
 */
export class LFTModuleLogger {
  private module: string;

  constructor(module: string) {
    this.module = module;
  }

  debug(message: string, ...args: Object[]): void {
    logDebug(this.module, message, ...args);
  }

  info(message: string, ...args: Object[]): void {
    logInfo(this.module, message, ...args);
  }

  warn(message: string, ...args: Object[]): void {
    logWarn(this.module, message, ...args);
  }

  error(message: string, ...args: Object[]): void {
    logError(this.module, message, ...args);
  }
}
