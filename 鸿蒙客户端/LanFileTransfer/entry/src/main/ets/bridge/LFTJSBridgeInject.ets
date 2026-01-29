/**
 * 局域网文件传输 - 鸿蒙客户端
 * JSBridge 注入脚本
 *
 * 此脚本由 Native 端在 WebView 加载页面时注入
 * 提供 H5 与 Native 通信的桥接接口
 */

/**
 * 获取 JSBridge 注入的 JavaScript 代码
 * @returns 注入脚本字符串
 */
export function getJSBridgeInjectScript(): string {
  return `
(function() {
    'use strict';

    // 防止重复注入
    if (window.JSBridge && window.JSBridge._initialized) {
        console.log('[JSBridge] Already initialized, skip injection');
        window.__JSBRIDGE_READY__ = true;
        return;
    }

    console.log('[JSBridge] Initializing for HarmonyOS...');

    window.JSBridge = {
        // ========== 内部状态 ==========
        _initialized: true,
        _callbacks: {},
        _callbackId: 0,

        // ========== 回调函数（H5 设置，Native 调用）==========
        _wifiChangedCallback: null,
        _onUploadProgress: null,
        _onUploadComplete: null,

        // ========== 内部方法 ==========

        _generateCallbackId: function() {
            return 'cb_' + Date.now() + '_' + (++this._callbackId);
        },

        _sanitizeParams: function(params) {
            if (!params || typeof params !== 'object') {
                return params || {};
            }
            try {
                return JSON.parse(JSON.stringify(params));
            } catch (e) {
                console.error('[JSBridge] Params not serializable:', e);
                return {};
            }
        },

        _callNative: function(method, params) {
            var self = this;
            var callbackId = this._generateCallbackId();
            var safeParams = this._sanitizeParams(params);
            var message = {
                method: method,
                params: safeParams || {},
                callbackId: callbackId
            };

            console.log('[JSBridge] Call native:', method, JSON.stringify(params));

            return new Promise(function(resolve, reject) {
                self._callbacks[callbackId] = function(result) {
                    delete self._callbacks[callbackId];

                    console.log('[JSBridge] Native response:', method, JSON.stringify(result));

                    if (result.code === 200) {
                        resolve(result.data);
                    } else {
                        var error = new Error(result.message || '调用失败');
                        error.code = result.code;
                        reject(error);
                    }
                };

                // 鸿蒙 WebView 的消息通道
                // 使用 jsBridge 端口发送消息
                try {
                    if (window.ohosJSBridge && window.ohosJSBridge.postMessage) {
                        window.ohosJSBridge.postMessage(JSON.stringify(message));
                    } else {
                        console.error('[JSBridge] ohosJSBridge not available');
                        delete self._callbacks[callbackId];
                        reject(new Error('JSBridge 通信失败，请确保在 App 内运行'));
                    }
                } catch (e) {
                    console.error('[JSBridge] Post message failed:', e);
                    delete self._callbacks[callbackId];
                    reject(new Error('JSBridge 通信失败'));
                }
            });
        },

        // ========== 文件选择 ==========

        selectFiles: function(options) {
            return this._callNative('selectFiles', options);
        },

        // ========== WiFi ==========

        getWiFiInfo: function() {
            return this._callNative('getWiFiInfo');
        },

        onWiFiChanged: function(callback) {
            this._wifiChangedCallback = callback;
            this._callNative('onWiFiChanged').catch(function() {});
        },

        // ========== 配置存储 ==========

        saveConfig: function(key, value) {
            return this._callNative('saveConfig', { key: key, value: value });
        },

        getConfig: function(key) {
            return this._callNative('getConfig', { key: key });
        },

        removeConfig: function(key) {
            return this._callNative('removeConfig', { key: key });
        },

        // ========== 历史记录 ==========

        saveHistory: function(record) {
            return this._callNative('saveHistory', record);
        },

        getHistory: function(options) {
            return this._callNative('getHistory', options || {});
        },

        deleteHistory: function(ids) {
            return this._callNative('deleteHistory', { ids: ids });
        },

        clearHistory: function() {
            return this._callNative('clearHistory');
        },

        // ========== 视频播放 ==========

        playVideo: function(options) {
            return this._callNative('playVideo', options);
        },

        // ========== 后台上传 ==========

        startBackgroundUpload: function(task) {
            return this._callNative('startBackgroundUpload', task);
        },

        cancelBackgroundUpload: function(taskId) {
            return this._callNative('cancelBackgroundUpload', { taskId: taskId });
        },

        // ========== 设备信息 ==========

        getDeviceInfo: function() {
            return this._callNative('getDeviceInfo');
        },

        // ========== 工具方法 ==========

        isInApp: function() {
            return !!(window.ohosJSBridge && window.ohosJSBridge.postMessage);
        }
    };

    window.__JSBRIDGE_READY__ = true;
    console.log('[JSBridge] Initialized successfully, isInApp:', window.JSBridge.isInApp());

    // 统一事件派发函数，便于 Native 端重发
    window.__dispatchJSBridgeReady = function() {
        try {
            var event = new CustomEvent('JSBridgeReady', { detail: { version: '1.0.0', platform: 'HarmonyOS' } });
            window.dispatchEvent(event);
        } catch (e) {
            console.log('[JSBridge] CustomEvent not supported');
        }
    };

    // 触发初始化完成事件
    window.__dispatchJSBridgeReady();

})();
`;
}

/**
 * 获取 Console 日志转发脚本
 * 用于在 Native 端接收 H5 的 console 日志
 * @returns Console 转发脚本
 */
export function getConsoleForwardScript(): string {
  return `
(function() {
    if (window.__lftConsoleHooked) return;
    window.__lftConsoleHooked = true;

    function send(type, args) {
        try {
            if (window.ohosConsole && window.ohosConsole.log) {
                window.ohosConsole.log(JSON.stringify({
                    type: type,
                    message: Array.prototype.slice.call(args).map(function(a) {
                        try { return typeof a === 'string' ? a : JSON.stringify(a); }
                        catch(e) { return String(a); }
                    }).join(' '),
                    url: location.href
                }));
            }
        } catch(e) {}
    }

    ['log', 'info', 'warn', 'error'].forEach(function(level) {
        var original = console[level];
        console[level] = function() {
            send(level, arguments);
            if (original) original.apply(console, arguments);
        };
    });

    window.addEventListener('error', function(e) {
        send('error', [e.message || 'Script error', e.filename || '', e.lineno || 0, e.colno || 0]);
    });

    window.addEventListener('unhandledrejection', function(e) {
        var reason = e.reason && (e.reason.message || e.reason) || 'Unhandled rejection';
        send('error', [reason]);
    });

    send('info', ['Console forwarding ready', document.readyState]);
})();
`;
}
