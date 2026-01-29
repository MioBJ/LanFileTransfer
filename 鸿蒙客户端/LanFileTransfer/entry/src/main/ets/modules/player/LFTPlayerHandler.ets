/**
 * 局域网文件传输 - 鸿蒙客户端
 * 视频播放处理器
 *
 * 职责：
 * - 处理 playVideo 方法调用
 * - 调用系统视频播放器播放视频
 */

import { common, Want } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { createLogger, LFTModuleLogger } from '../../utils/LFTLogger';
import {
  LFTBridgeHandler,
  LFTBridgeCallback,
  PlayVideoParams
} from '../../bridge/LFTBridgeTypes';
import {
  JS_METHOD_PLAY_VIDEO,
  JSBRIDGE_CODE_BAD_REQUEST,
  JSBRIDGE_CODE_INTERNAL_ERROR
} from '../../bridge/LFTConstants';

const logger: LFTModuleLogger = createLogger('PlayerHandler');

/**
 * 视频播放处理器
 * 处理来自 H5 的视频播放请求
 */
export class LFTPlayerHandler implements LFTBridgeHandler {
  /** 上下文 */
  private context: common.UIAbilityContext;

  /**
   * 构造函数
   * @param context - UIAbility 上下文
   */
  constructor(context: common.UIAbilityContext) {
    this.context = context;
    logger.info('PlayerHandler 初始化');
  }

  /**
   * 获取支持的方法列表
   */
  getSupportedMethods(): string[] {
    return [JS_METHOD_PLAY_VIDEO];
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

    if (method === JS_METHOD_PLAY_VIDEO) {
      // 从 params 中提取 PlayVideoParams 字段
      const playParams: PlayVideoParams = {
        url: params['url'] as string,
        title: params['title'] as string | undefined
      };
      this.handlePlayVideo(playParams, callback);
    } else {
      callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '方法未实现');
    }
  }

  /**
   * 处理视频播放
   */
  private async handlePlayVideo(
    params: PlayVideoParams,
    callback: LFTBridgeCallback
  ): Promise<void> {
    try {
      const url = params.url;
      const title = params.title || '';

      if (!url) {
        callback(false, null, JSBRIDGE_CODE_BAD_REQUEST, '缺少参数 url');
        return;
      }

      logger.info('播放视频: %{public}s, title: %{public}s', url, title);

      // 构建 Want 对象，调用系统视频播放器
      const want: Want = {
        action: 'ohos.want.action.viewData',
        type: 'video/*',
        uri: url,
        parameters: {
          title: title
        }
      };

      // 启动播放器
      await this.context.startAbility(want);

      logger.info('视频播放器已启动');
      callback(true, null, 0, null);

    } catch (e) {
      const error = e as BusinessError;
      logger.error('播放视频失败: %{public}s', JSON.stringify(error));

      // 尝试使用备选方式播放
      this.fallbackPlay(params, callback);
    }
  }

  /**
   * 备选播放方式
   * 当系统播放器无法使用时，尝试其他方式
   */
  private async fallbackPlay(
    params: PlayVideoParams,
    callback: LFTBridgeCallback
  ): Promise<void> {
    try {
      const url = params.url;

      // 尝试使用 openLink 打开
      const want: Want = {
        action: 'ohos.want.action.viewData',
        entities: ['entity.system.browsable'],
        uri: url
      };

      await this.context.startAbility(want);
      callback(true, null, 0, null);

    } catch (e) {
      const error = e as BusinessError;
      logger.error('备选播放失败: %{public}s', JSON.stringify(error));
      callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '无法播放视频');
    }
  }
}
