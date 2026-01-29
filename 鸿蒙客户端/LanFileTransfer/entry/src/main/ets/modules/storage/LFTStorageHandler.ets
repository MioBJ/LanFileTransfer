/**
 * 局域网文件传输 - 鸿蒙客户端
 * 存储处理器
 *
 * 职责：
 * - 处理配置存储（saveConfig, getConfig, removeConfig）
 * - 处理历史记录（saveHistory, getHistory, deleteHistory, clearHistory）
 */

import { preferences } from '@kit.ArkData';
import { common } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { createLogger, LFTModuleLogger } from '../../utils/LFTLogger';
import {
  LFTBridgeHandler,
  LFTBridgeCallback,
  HistoryRecord,
  GetHistoryParams
} from '../../bridge/LFTBridgeTypes';
import {
  JS_METHOD_SAVE_CONFIG,
  JS_METHOD_GET_CONFIG,
  JS_METHOD_REMOVE_CONFIG,
  JS_METHOD_SAVE_HISTORY,
  JS_METHOD_GET_HISTORY,
  JS_METHOD_DELETE_HISTORY,
  JS_METHOD_CLEAR_HISTORY,
  JSBRIDGE_CODE_BAD_REQUEST,
  JSBRIDGE_CODE_INTERNAL_ERROR,
  STORAGE_CONFIG_PREFIX,
  STORAGE_HISTORY_KEY
} from '../../bridge/LFTConstants';

const logger: LFTModuleLogger = createLogger('StorageHandler');

/** Preferences 存储名称 */
const PREFERENCES_NAME = 'lft_storage';

/**
 * 存储处理器
 * 处理配置和历史记录的存储
 */
export class LFTStorageHandler implements LFTBridgeHandler {
  /** 上下文 */
  private context: common.UIAbilityContext;

  /** Preferences 实例 */
  private dataPreferences: preferences.Preferences | null = null;

  /**
   * 构造函数
   * @param context - UIAbility 上下文
   */
  constructor(context: common.UIAbilityContext) {
    this.context = context;
    this.initPreferences();
    logger.info('StorageHandler 初始化');
  }

  /**
   * 初始化 Preferences
   */
  private async initPreferences(): Promise<void> {
    try {
      this.dataPreferences = await preferences.getPreferences(this.context, PREFERENCES_NAME);
      logger.info('Preferences 初始化成功');
    } catch (e) {
      logger.error('Preferences 初始化失败: %{public}s', JSON.stringify(e));
    }
  }

  /**
   * 获取支持的方法列表
   */
  getSupportedMethods(): string[] {
    return [
      JS_METHOD_SAVE_CONFIG,
      JS_METHOD_GET_CONFIG,
      JS_METHOD_REMOVE_CONFIG,
      JS_METHOD_SAVE_HISTORY,
      JS_METHOD_GET_HISTORY,
      JS_METHOD_DELETE_HISTORY,
      JS_METHOD_CLEAR_HISTORY
    ];
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
      case JS_METHOD_SAVE_CONFIG:
        this.handleSaveConfig(params, callback);
        break;
      case JS_METHOD_GET_CONFIG:
        this.handleGetConfig(params, callback);
        break;
      case JS_METHOD_REMOVE_CONFIG:
        this.handleRemoveConfig(params, callback);
        break;
      case JS_METHOD_SAVE_HISTORY:
        this.handleSaveHistory(params, callback);
        break;
      case JS_METHOD_GET_HISTORY:
        // 从 params 中提取 GetHistoryParams 字段
        const historyParams: GetHistoryParams = {
          limit: params['limit'] as number | undefined,
          offset: params['offset'] as number | undefined,
          search: params['search'] as string | undefined
        };
        this.handleGetHistory(historyParams, callback);
        break;
      case JS_METHOD_DELETE_HISTORY:
        this.handleDeleteHistory(params, callback);
        break;
      case JS_METHOD_CLEAR_HISTORY:
        this.handleClearHistory(callback);
        break;
      default:
        callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '方法未实现');
    }
  }

  // ============================================
  // MARK: - 配置存储
  // ============================================

  /**
   * 处理保存配置
   */
  private async handleSaveConfig(
    params: Record<string, Object>,
    callback: LFTBridgeCallback
  ): Promise<void> {
    try {
      const key = params['key'] as string;
      const value: Object | undefined = params['value'];

      if (!key) {
        callback(false, null, JSBRIDGE_CODE_BAD_REQUEST, '缺少参数 key');
        return;
      }

      if (!this.dataPreferences) {
        await this.initPreferences();
      }

      const storageKey = STORAGE_CONFIG_PREFIX + key;
      const valueStr = JSON.stringify(value);

      await this.dataPreferences!.put(storageKey, valueStr);
      await this.dataPreferences!.flush();

      logger.debug('保存配置: %{public}s', key);
      callback(true, null, 0, null);

    } catch (e) {
      const error = e as BusinessError;
      logger.error('保存配置失败: %{public}s', JSON.stringify(error));
      callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '保存配置失败');
    }
  }

  /**
   * 处理获取配置
   */
  private async handleGetConfig(
    params: Record<string, Object>,
    callback: LFTBridgeCallback
  ): Promise<void> {
    try {
      const key = params['key'] as string;

      if (!key) {
        callback(false, null, JSBRIDGE_CODE_BAD_REQUEST, '缺少参数 key');
        return;
      }

      if (!this.dataPreferences) {
        await this.initPreferences();
      }

      const storageKey = STORAGE_CONFIG_PREFIX + key;
      const valueStr = await this.dataPreferences!.get(storageKey, '') as string;

      if (!valueStr) {
        callback(true, null, 0, null);
        return;
      }

      const value: object = JSON.parse(valueStr) as object;
      logger.debug('获取配置: %{public}s', key);
      callback(true, value, 0, null);

    } catch (e) {
      const error = e as BusinessError;
      logger.error('获取配置失败: %{public}s', JSON.stringify(error));
      callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '获取配置失败');
    }
  }

  /**
   * 处理删除配置
   */
  private async handleRemoveConfig(
    params: Record<string, Object>,
    callback: LFTBridgeCallback
  ): Promise<void> {
    try {
      const key = params['key'] as string;

      if (!key) {
        callback(false, null, JSBRIDGE_CODE_BAD_REQUEST, '缺少参数 key');
        return;
      }

      if (!this.dataPreferences) {
        await this.initPreferences();
      }

      const storageKey = STORAGE_CONFIG_PREFIX + key;
      await this.dataPreferences!.delete(storageKey);
      await this.dataPreferences!.flush();

      logger.debug('删除配置: %{public}s', key);
      callback(true, null, 0, null);

    } catch (e) {
      const error = e as BusinessError;
      logger.error('删除配置失败: %{public}s', JSON.stringify(error));
      callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '删除配置失败');
    }
  }

  // ============================================
  // MARK: - 历史记录
  // ============================================

  /**
   * 处理保存历史记录
   */
  private async handleSaveHistory(
    params: Record<string, Object>,
    callback: LFTBridgeCallback
  ): Promise<void> {
    try {
      // 从 params 中提取 HistoryRecord 字段
      const record: HistoryRecord = {
        id: params['id'] as string,
        fileName: params['fileName'] as string,
        fileSize: params['fileSize'] as number,
        fileType: params['fileType'] as string | undefined,
        thumbnail: params['thumbnail'] as string | undefined,
        uploadTime: params['uploadTime'] as string,
        uploadDuration: params['uploadDuration'] as number | undefined,
        serverIp: params['serverIp'] as string | undefined,
        deviceName: params['deviceName'] as string | undefined
      };

      if (!record.id || !record.fileName) {
        callback(false, null, JSBRIDGE_CODE_BAD_REQUEST, '缺少必要参数 id 或 fileName');
        return;
      }

      if (!this.dataPreferences) {
        await this.initPreferences();
      }

      // 获取现有历史记录
      const historyStr = await this.dataPreferences!.get(STORAGE_HISTORY_KEY, '[]') as string;
      const history: HistoryRecord[] = JSON.parse(historyStr);

      // 检查是否已存在（避免重复）
      const existIndex = history.findIndex(r => r.id === record.id);
      if (existIndex >= 0) {
        history[existIndex] = record;
      } else {
        // 添加到开头
        history.unshift(record);
      }

      // 限制历史记录数量（最多 1000 条）
      const maxHistory = 1000;
      if (history.length > maxHistory) {
        history.splice(maxHistory);
      }

      // 保存
      await this.dataPreferences!.put(STORAGE_HISTORY_KEY, JSON.stringify(history));
      await this.dataPreferences!.flush();

      logger.debug('保存历史记录: %{public}s', record.fileName);
      callback(true, null, 0, null);

    } catch (e) {
      const error = e as BusinessError;
      logger.error('保存历史记录失败: %{public}s', JSON.stringify(error));
      callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '保存历史记录失败');
    }
  }

  /**
   * 处理获取历史记录
   */
  private async handleGetHistory(
    params: GetHistoryParams,
    callback: LFTBridgeCallback
  ): Promise<void> {
    try {
      if (!this.dataPreferences) {
        await this.initPreferences();
      }

      const limit = params.limit || 100;
      const offset = params.offset || 0;
      const search = params.search || '';

      // 获取所有历史记录
      const historyStr = await this.dataPreferences!.get(STORAGE_HISTORY_KEY, '[]') as string;
      let history: HistoryRecord[] = JSON.parse(historyStr);

      // 搜索过滤
      if (search) {
        const searchLower = search.toLowerCase();
        history = history.filter(r =>
          r.fileName.toLowerCase().includes(searchLower)
        );
      }

      // 分页
      const result = history.slice(offset, offset + limit);

      logger.debug('获取历史记录: %{public}d 条', result.length);
      callback(true, result as object, 0, null);

    } catch (e) {
      const error = e as BusinessError;
      logger.error('获取历史记录失败: %{public}s', JSON.stringify(error));
      callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '获取历史记录失败');
    }
  }

  /**
   * 处理删除历史记录
   */
  private async handleDeleteHistory(
    params: Record<string, Object>,
    callback: LFTBridgeCallback
  ): Promise<void> {
    try {
      const ids = params['ids'] as string[];

      if (!ids || !Array.isArray(ids)) {
        callback(false, null, JSBRIDGE_CODE_BAD_REQUEST, '缺少参数 ids');
        return;
      }

      if (!this.dataPreferences) {
        await this.initPreferences();
      }

      // 获取现有历史记录
      const historyStr = await this.dataPreferences!.get(STORAGE_HISTORY_KEY, '[]') as string;
      let history: HistoryRecord[] = JSON.parse(historyStr);

      // 过滤掉要删除的记录
      const idSet = new Set(ids);
      history = history.filter(r => !idSet.has(r.id));

      // 保存
      await this.dataPreferences!.put(STORAGE_HISTORY_KEY, JSON.stringify(history));
      await this.dataPreferences!.flush();

      logger.debug('删除历史记录: %{public}d 条', ids.length);
      callback(true, null, 0, null);

    } catch (e) {
      const error = e as BusinessError;
      logger.error('删除历史记录失败: %{public}s', JSON.stringify(error));
      callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '删除历史记录失败');
    }
  }

  /**
   * 处理清空历史记录
   */
  private async handleClearHistory(callback: LFTBridgeCallback): Promise<void> {
    try {
      if (!this.dataPreferences) {
        await this.initPreferences();
      }

      await this.dataPreferences!.put(STORAGE_HISTORY_KEY, '[]');
      await this.dataPreferences!.flush();

      logger.info('清空历史记录');
      callback(true, null, 0, null);

    } catch (e) {
      const error = e as BusinessError;
      logger.error('清空历史记录失败: %{public}s', JSON.stringify(error));
      callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '清空历史记录失败');
    }
  }
}
