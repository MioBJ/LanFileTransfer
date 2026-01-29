/**
 * Toast 工具（防“串台”）
 *
 * 之前页面里用 closeToast() + setTimeout(...) 的方式避免重叠，
 * 但如果用户在延迟窗口内触发了多次提示，会出现“旧 setTimeout 回调覆盖新提示”的问题。
 *
 * 这个文件通过清理 pending timer，保证只会展示最后一次触发的提示。
 */

import { showToast as vantShowToast, closeToast, type ToastOptions } from 'vant'

let pendingTimer: number | null = null

function clearPending() {
  if (pendingTimer !== null) {
    clearTimeout(pendingTimer)
    pendingTimer = null
  }
}

export function toastClose() {
  clearPending()
  closeToast()
}

export function toast(options: ToastOptions) {
  // 关闭当前 toast，并取消任何“延迟显示中的旧 toast”
  toastClose()

  // 轻微延迟，给 closeToast 一点时间完成 DOM 更新；更关键的是我们会清理旧 timer，避免串台
  pendingTimer = window.setTimeout(() => {
    pendingTimer = null
    vantShowToast(options)
  }, 50)
}

export function toastText(message: string, duration = 500) {
  // 精确定位“连接成功”到底是谁触发的（用户反馈误弹但代码里只有 Settings 调用）
  if (message === '连接成功') {
    console.trace('[ToastTrace] 触发 toastText("连接成功")')
  }
  toast({ message, duration })
}

export function toastFail(message: string, duration = 1500) {
  toast({ type: 'fail', message, duration })
}


