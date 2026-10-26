import http from './http'
import { unwrapPage } from './pagination'
import { validationError } from '../services/notice'

// 文件上传与重新提交使用 multipart/form-data，元数据由后端统一校验。

export const getResources = async (params) => unwrapPage(await http.get('/resources', { params }))
export const getMyResources = async (params) => unwrapPage(await http.get('/resources/mine', { params }))
export const getResourceCategories = () => http.get('/resources/categories')
export const MAX_RESOURCE_FILE_SIZE = 200 * 1024 * 1024

export function assertResourceFile(file) {
  if (file instanceof File && file.size > MAX_RESOURCE_FILE_SIZE)
    throw validationError('文件不能超过200MB，请压缩文件或选择较小的文件后重新上传', '文件过大')
}

export const uploadResource = (data, onUploadProgress) => {
  assertResourceFile(data.get('file'))
  return http.post('/resources/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress,
    timeout: 0,
  })
}
export const resubmitResource = (id, metadata, file) => {
  assertResourceFile(file)
  const data = new FormData()
  data.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }))
  if (file) data.append('file', file)
  return http.put(`/resources/${id}/resubmit`, data, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 0,
  })
}
export const getResourceReviewHistory = (id) => http.get(`/resources/${id}/reviews`)
export const deleteResource = (id) => http.delete(`/resources/${id}`)
export const toggleResourceFavorite = (id) => http.post(`/resources/${id}/favorite`)
export const submitResourceCorrection = (id, content) =>
  http.post(`/resources/${id}/corrections`, { content })
export const getPendingResourceCorrections = () => http.get('/resources/corrections/pending')
export const handleResourceCorrection = (id, data) => http.post(`/resources/corrections/${id}/handle`, data)
export const getResourceFile = (id, download = false) =>
  http.get(`/resources/${id}/file`, { params: { download }, responseType: 'blob' })

export const getMyResourceStats = () => http.get('/resources/mine/stats')
export async function getResourcesByIds(ids) {
  const unique = [...new Set(ids.map(Number).filter((id) => Number.isSafeInteger(id) && id > 0))]
  const data = []
  for (let i = 0; i < unique.length; i += 100) {
    const result = await http.get('/resources/by-ids', {
      params: { ids: unique.slice(i, i + 100).join(',') },
    })
    data.push(...(result.data || []))
  }
  return { data }
}
