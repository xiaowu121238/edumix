import axios from 'axios'
import { notifyError } from '../services/notice'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('edumix-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    // 页面切换主动取消的请求不是业务失败，不向用户显示错误。
    if (axios.isCancel(error)) return Promise.reject(error)
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      localStorage.removeItem('edumix-token')
      localStorage.removeItem('edumix-user')
      window.dispatchEvent(new Event('edumix-auth-expired'))
    }

    const normalized = await normalizeError(error)
    if (!error.config?.skipErrorNotice && !normalized.userNotified) {
      notifyError(normalized.message, {
        title: errorTitle(normalized.status),
        status: normalized.status,
      })
      normalized.userNotified = true
    }
    return Promise.reject(normalized)
  },
)

async function normalizeError(error) {
  const status = error.response?.status || null
  let data = error.response?.data

  // 下载接口使用 blob；即使后端返回 JSON 错误，Axios 也会把响应包装成 Blob。
  if (data instanceof Blob) {
    try {
      const text = await data.text()
      data = JSON.parse(text)
    } catch {
      data = null
    }
  }

  let message = typeof data === 'string' ? data : data?.message
  if (!message) {
    if (error.code === 'ECONNABORTED') message = '请求处理超时，请检查网络后重试'
    else if (!error.response && containsOversizedFile(error.config?.data))
      message = '文件不能超过200MB，请压缩文件或选择较小的文件后重新上传'
    else if (!error.response) message = '无法连接后端服务，请确认后端已启动，并检查网络或代理配置'
    else
      message =
        {
          400: '请求内容不正确，请检查填写信息',
          401: '登录状态已失效，请重新登录',
          403: '当前账号没有执行此操作的权限',
          404: '请求的内容不存在或已被删除',
          405: '当前接口不支持此操作',
          409: '数据已存在或已被其他操作修改',
          413: '文件不能超过200MB',
          415: '文件或请求格式不受支持',
          500: '服务器处理失败，请稍后重试',
          503: '服务暂时不可用，请稍后重试',
        }[status] || `请求失败（${status || '未知状态'}）`
  }

  const normalized = new Error(message)
  normalized.status = status
  normalized.code = data?.code || error.code
  normalized.data = data?.data
  normalized.response = error.response
  normalized.cause = error
  return normalized
}

function containsOversizedFile(data) {
  if (!(data instanceof FormData)) return false
  return [...data.values()].some((value) => value instanceof File && value.size > 200 * 1024 * 1024)
}

function errorTitle(status) {
  if (status === 401) return '需要重新登录'
  if (status === 403) return '权限不足'
  if (status === 413) return '文件过大'
  if (status && status >= 500) return '服务器暂时不可用'
  return '操作未完成'
}

export default http
