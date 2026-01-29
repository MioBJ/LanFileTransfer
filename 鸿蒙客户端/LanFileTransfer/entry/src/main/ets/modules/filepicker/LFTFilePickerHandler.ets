/**
 * 局域网文件传输 - 鸿蒙客户端
 * 文件选择处理器
 *
 * 职责：
 * - 处理 selectFiles 方法调用
 * - 调用系统相册选择照片/视频
 * - 生成缩略图并返回文件信息
 */

import { photoAccessHelper } from '@kit.MediaLibraryKit';
import { fileIo } from '@kit.CoreFileKit';
import { image } from '@kit.ImageKit';
import { common } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { createLogger, LFTModuleLogger } from '../../utils/LFTLogger';
import {
  LFTBridgeHandler,
  LFTBridgeCallback,
  LFTJSBridgeInterface,
  SelectFilesParams,
  SelectedFile
} from '../../bridge/LFTBridgeTypes';
import {
  JS_METHOD_SELECT_FILES,
  JSBRIDGE_CODE_FORBIDDEN,
  JSBRIDGE_CODE_INTERNAL_ERROR,
  THUMBNAIL_MAX_WIDTH,
  THUMBNAIL_MAX_HEIGHT,
  THUMBNAIL_QUALITY
} from '../../bridge/LFTConstants';

const logger: LFTModuleLogger = createLogger('FilePicker');

/**
 * 文件选择处理器
 * 处理来自 H5 的文件选择请求
 */
export class LFTFilePickerHandler implements LFTBridgeHandler {
  /** 上下文 */
  private context: common.UIAbilityContext;

  /** JSBridge 引用 */
  private bridge: LFTJSBridgeInterface | null = null;

  /**
   * 构造函数
   * @param context - UIAbility 上下文
   */
  constructor(context: common.UIAbilityContext) {
    this.context = context;
    logger.info('FilePickerHandler 初始化');
  }

  /**
   * 获取支持的方法列表
   */
  getSupportedMethods(): string[] {
    return [JS_METHOD_SELECT_FILES];
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
    logger.debug('处理方法: %{public}s, params: %{public}s', method, JSON.stringify(params));

    if (method === JS_METHOD_SELECT_FILES) {
      // 从 params 中提取 SelectFilesParams 字段
      const selectParams: SelectFilesParams = {
        multiple: params['multiple'] as boolean | undefined,
        mediaTypes: params['mediaTypes'] as string[] | undefined,
        maxCount: params['maxCount'] as number | undefined
      };
      this.handleSelectFiles(selectParams, callback);
    } else {
      callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '方法未实现');
    }
  }

  /**
   * 处理文件选择
   */
  private async handleSelectFiles(
    params: SelectFilesParams,
    callback: LFTBridgeCallback
  ): Promise<void> {
    try {
      // 解析参数
      const multiple = params.multiple !== false;
      const maxCount = params.maxCount || (multiple ? 99 : 1);
      const mediaTypes = params.mediaTypes || ['image', 'video'];

      logger.info('开始选择文件: multiple=%{public}s, maxCount=%{public}d, types=%{public}s',
        multiple, maxCount, JSON.stringify(mediaTypes));

      // 创建相册选择器
      const photoPicker = new photoAccessHelper.PhotoViewPicker();

      // 配置选择器选项
      const options: photoAccessHelper.PhotoSelectOptions = new photoAccessHelper.PhotoSelectOptions();
      options.maxSelectNumber = maxCount;

      // 设置媒体类型过滤
      if (mediaTypes.includes('image') && mediaTypes.includes('video')) {
        options.MIMEType = photoAccessHelper.PhotoViewMIMETypes.IMAGE_VIDEO_TYPE;
      } else if (mediaTypes.includes('image')) {
        options.MIMEType = photoAccessHelper.PhotoViewMIMETypes.IMAGE_TYPE;
      } else if (mediaTypes.includes('video')) {
        options.MIMEType = photoAccessHelper.PhotoViewMIMETypes.VIDEO_TYPE;
      }

      // 显示选择器
      const result = await photoPicker.select(options);

      // 用户取消选择
      if (!result.photoUris || result.photoUris.length === 0) {
        logger.info('用户取消选择');
        callback(true, [], 0, null);
        return;
      }

      logger.info('用户选择了 %{public}d 个文件', result.photoUris.length);

      // 处理选中的文件
      const selectedFiles: SelectedFile[] = [];
      for (const uri of result.photoUris) {
        try {
          const fileInfo = await this.processSelectedFile(uri);
          if (fileInfo) {
            selectedFiles.push(fileInfo);
          }
        } catch (e) {
          logger.error('处理文件失败: %{public}s, error: %{public}s', uri, JSON.stringify(e));
        }
      }

      logger.info('文件处理完成，共 %{public}d 个文件', selectedFiles.length);
      callback(true, selectedFiles as object, 0, null);

    } catch (e) {
      const error = e as BusinessError;
      logger.error('文件选择失败: %{public}s', JSON.stringify(error));

      if (error.code === 12500001) {
        // 权限被拒绝
        callback(false, null, JSBRIDGE_CODE_FORBIDDEN, '相册权限被拒绝，请在设置中允许访问');
      } else {
        callback(false, null, JSBRIDGE_CODE_INTERNAL_ERROR, '文件选择失败: ' + error.message);
      }
    }
  }

  /**
   * 处理单个选中的文件
   * @param uri - 文件 URI
   * @returns 文件信息
   */
  private async processSelectedFile(uri: string): Promise<SelectedFile | null> {
    try {
      // 打开文件获取信息
      const file = await fileIo.open(uri, fileIo.OpenMode.READ_ONLY);
      const stat = await fileIo.stat(file.fd);

      // 生成文件 ID
      const fileId = this.generateUUID();

      // 获取文件名
      const fileName = this.getFileNameFromUri(uri);

      // 获取 MIME 类型
      const mimeType = this.getMimeType(fileName);

      // 构建文件信息
      const fileInfo: SelectedFile = {
        id: fileId,
        name: fileName,
        size: stat.size,
        type: mimeType,
        path: uri
      };

      // 生成缩略图
      const isVideo = mimeType.startsWith('video/');
      if (isVideo) {
        // 视频：获取时长和缩略图
        fileInfo.duration = await this.getVideoDuration(uri);
        fileInfo.thumbnail = await this.generateVideoThumbnail(uri);
      } else {
        // 图片：生成缩略图
        fileInfo.thumbnail = await this.generateImageThumbnail(file.fd);
      }

      // 关闭文件
      await fileIo.close(file);

      logger.debug('处理文件完成: %{public}s, size=%{public}d', fileName, stat.size);
      return fileInfo;

    } catch (e) {
      logger.error('处理文件失败: %{public}s', JSON.stringify(e));
      return null;
    }
  }

  /**
   * 生成图片缩略图
   * @param fd - 文件描述符
   * @returns Base64 缩略图
   */
  private async generateImageThumbnail(fd: number): Promise<string | undefined> {
    try {
      // 创建图片源
      const imageSource = image.createImageSource(fd);
      if (!imageSource) {
        logger.warn('创建图片源失败');
        return undefined;
      }

      // 获取图片信息
      const imageInfo = await imageSource.getImageInfo();
      const originalWidth = imageInfo.size.width;
      const originalHeight = imageInfo.size.height;

      // 计算缩略图尺寸（保持宽高比）
      let targetWidth = THUMBNAIL_MAX_WIDTH;
      let targetHeight = THUMBNAIL_MAX_HEIGHT;

      if (originalWidth > originalHeight) {
        targetHeight = Math.round(originalHeight * (THUMBNAIL_MAX_WIDTH / originalWidth));
      } else {
        targetWidth = Math.round(originalWidth * (THUMBNAIL_MAX_HEIGHT / originalHeight));
      }

      // 解码选项
      const decodingOptions: image.DecodingOptions = {
        desiredSize: {
          width: targetWidth,
          height: targetHeight
        }
      };

      // 创建 PixelMap
      const pixelMap = await imageSource.createPixelMap(decodingOptions);

      // 编码为 JPEG
      const packer = image.createImagePacker();
      const packingOptions: image.PackingOption = {
        format: 'image/jpeg',
        quality: THUMBNAIL_QUALITY
      };

      const arrayBuffer = await packer.packing(pixelMap, packingOptions);

      // 转换为 Base64
      const base64 = this.arrayBufferToBase64(arrayBuffer);

      // 释放资源
      pixelMap.release();
      imageSource.release();
      packer.release();

      return 'data:image/jpeg;base64,' + base64;

    } catch (e) {
      logger.error('生成图片缩略图失败: %{public}s', JSON.stringify(e));
      return undefined;
    }
  }

  /**
   * 生成视频缩略图
   * @param uri - 视频 URI
   * @returns Base64 缩略图
   */
  private async generateVideoThumbnail(uri: string): Promise<string | undefined> {
    try {
      // 使用 AVMetadataExtractor 获取视频帧
      // 注意：此功能需要 API 12+
      // 这里先返回 undefined，后续可以使用 media 模块的 AVMetadataExtractor
      logger.debug('视频缩略图生成暂未实现');
      return undefined;

    } catch (e) {
      logger.error('生成视频缩略图失败: %{public}s', JSON.stringify(e));
      return undefined;
    }
  }

  /**
   * 获取视频时长
   * @param uri - 视频 URI
   * @returns 时长（秒）
   */
  private async getVideoDuration(uri: string): Promise<number | undefined> {
    try {
      // 使用 AVMetadataExtractor 获取视频信息
      // 注意：此功能需要 API 12+
      // 这里先返回 undefined
      return undefined;

    } catch (e) {
      logger.error('获取视频时长失败: %{public}s', JSON.stringify(e));
      return undefined;
    }
  }

  /**
   * 从 URI 中提取文件名
   * @param uri - 文件 URI
   * @returns 文件名
   */
  private getFileNameFromUri(uri: string): string {
    const parts = uri.split('/');
    return parts[parts.length - 1] || 'unknown';
  }

  /**
   * 根据文件名获取 MIME 类型
   * @param fileName - 文件名
   * @returns MIME 类型
   */
  private getMimeType(fileName: string): string {
    const ext = fileName.split('.').pop()?.toLowerCase() || '';

    const mimeTypes: Record<string, string> = {
      'jpg': 'image/jpeg',
      'jpeg': 'image/jpeg',
      'png': 'image/png',
      'gif': 'image/gif',
      'heic': 'image/heic',
      'heif': 'image/heif',
      'webp': 'image/webp',
      'mp4': 'video/mp4',
      'm4v': 'video/mp4',
      'mov': 'video/quicktime',
      'avi': 'video/x-msvideo',
      '3gp': 'video/3gpp'
    };

    return mimeTypes[ext] || 'application/octet-stream';
  }

  /**
   * 生成 UUID
   * @returns UUID 字符串
   */
  private generateUUID(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }

  /**
   * ArrayBuffer 转 Base64
   * @param buffer - ArrayBuffer
   * @returns Base64 字符串
   */
  private arrayBufferToBase64(buffer: ArrayBuffer): string {
    const uint8Array = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < uint8Array.length; i++) {
      binary += String.fromCharCode(uint8Array[i]);
    }
    return btoa(binary);
  }
}

/**
 * btoa 实现（Base64 编码）
 * @param str - 输入字符串
 * @returns Base64 编码字符串
 */
function btoa(str: string): string {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
  let output = '';

  for (let i = 0; i < str.length; i += 3) {
    const a = str.charCodeAt(i);
    const b = i + 1 < str.length ? str.charCodeAt(i + 1) : 0;
    const c = i + 2 < str.length ? str.charCodeAt(i + 2) : 0;

    const index1 = a >> 2;
    const index2 = ((a & 3) << 4) | (b >> 4);
    const index3 = ((b & 15) << 2) | (c >> 6);
    const index4 = c & 63;

    // ArkTS 不支持字符串索引访问，使用 charAt 方法
    output += chars.charAt(index1);
    output += chars.charAt(index2);
    output += i + 1 < str.length ? chars.charAt(index3) : '=';
    output += i + 2 < str.length ? chars.charAt(index4) : '=';
  }

  return output;
}
