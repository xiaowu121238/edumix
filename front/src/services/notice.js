import { reactive } from 'vue'

/** 全局消息状态。所有接口错误都通过这里显示，避免每个页面重复实现弹窗。 */
export const noticeState = reactive({ items: [] })

let sequence = 0
const recent = new Map()

export function dismissNotice(id) {
  const index = noticeState.items.findIndex((item) => item.id === id)
  if (index >= 0) noticeState.items.splice(index, 1)
}

export function notifyError(message, options = {}) {
  const text = String(message || '操作失败，请稍后重试').trim()
  const now = Date.now()
  if (now - (recent.get(text) || 0) < 1500) return
  recent.set(text, now)

  const item = {
    id: ++sequence,
    type: 'error',
    title: options.title || '操作未完成',
    message: text,
    status: options.status || null,
  }
  noticeState.items.push(item)
  if (noticeState.items.length > 3) noticeState.items.shift()
  window.setTimeout(() => dismissNotice(item.id), options.duration || 6500)
}

/** 创建已经提示过的本地校验错误，交给页面 catch 后不会再次弹出相同消息。 */
export function validationError(message, title = '请检查输入内容') {
  const error = new Error(message)
  error.userNotified = true
  notifyError(message, { title })
  return error
}
