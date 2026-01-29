/**
 * 局域网文件传输 - 鸿蒙客户端
 * 应用入口 Ability
 *
 * 职责：
 * - 应用生命周期管理
 * - 窗口创建和配置
 * - 全局上下文管理
 */

import { AbilityConstant, UIAbility, Want } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { window } from '@kit.ArkUI';

// 日志标签
const TAG = 'LFT_EntryAbility';
const DOMAIN = 0x0000;

/**
 * 应用入口 Ability
 * 管理应用的生命周期和窗口
 */
export default class EntryAbility extends UIAbility {
  /**
   * Ability 创建时调用
   * 初始化全局资源
   */
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    hilog.info(DOMAIN, TAG, '%{public}s', 'Ability onCreate');

    // 保存全局上下文，供其他模块使用
    AppStorage.setOrCreate('context', this.context);
  }

  /**
   * Ability 销毁时调用
   * 释放全局资源
   */
  onDestroy(): void {
    hilog.info(DOMAIN, TAG, '%{public}s', 'Ability onDestroy');
  }

  /**
   * 窗口创建完成回调
   * 配置窗口属性并加载主页面
   */
  onWindowStageCreate(windowStage: window.WindowStage): void {
    hilog.info(DOMAIN, TAG, '%{public}s', 'Ability onWindowStageCreate');

    // 保存 windowStage 供后续使用
    AppStorage.setOrCreate('windowStage', windowStage);

    // 获取主窗口并配置
    windowStage.getMainWindow((err, mainWindow) => {
      if (err.code) {
        hilog.error(DOMAIN, TAG, 'Failed to get main window: %{public}s', JSON.stringify(err));
        return;
      }

      // 设置全屏显示（避免状态栏遮挡 WebView）
      mainWindow.setWindowLayoutFullScreen(true);

      // 设置状态栏和导航栏属性
      const systemBarProperties: window.SystemBarProperties = {
        statusBarColor: '#FFFFFF',
        navigationBarColor: '#FFFFFF',
        statusBarContentColor: '#000000',
        navigationBarContentColor: '#000000'
      };
      mainWindow.setWindowSystemBarProperties(systemBarProperties);

      hilog.info(DOMAIN, TAG, 'Window configured successfully');
    });

    // 加载主页面
    windowStage.loadContent('pages/Index', (err) => {
      if (err.code) {
        hilog.error(DOMAIN, TAG, 'Failed to load content: %{public}s', JSON.stringify(err));
        return;
      }
      hilog.info(DOMAIN, TAG, 'Content loaded successfully');
    });
  }

  /**
   * 窗口销毁回调
   */
  onWindowStageDestroy(): void {
    hilog.info(DOMAIN, TAG, '%{public}s', 'Ability onWindowStageDestroy');
  }

  /**
   * Ability 进入前台
   */
  onForeground(): void {
    hilog.info(DOMAIN, TAG, '%{public}s', 'Ability onForeground');
  }

  /**
   * Ability 进入后台
   */
  onBackground(): void {
    hilog.info(DOMAIN, TAG, '%{public}s', 'Ability onBackground');
  }
}
