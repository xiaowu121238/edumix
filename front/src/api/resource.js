import http from './http'
import { unwrapPage } from './pagination'

// 文件上传与重新提交使用 multipart/form-data，元数据由后端统一校验。

export const getResources = async (params) => unwrapPage(await http.get('/resources', { params }))
export const getMyResources = async (params) => unwrapPage(await http.get('/resources/mine', { params }))
export const getResourceCategories = () => http.get('/resources/categories')
export const uploadResource = (data, onUploadProgress) =>
  http.post('/resources/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress,
  })
export const resubmitResource = (id, metadata, file) => {
  const data = new FormData()
  data.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }))
  if (file) data.append('file', file)
  return http.put(`/resources/${id}/resubmit`, data, { headers: { 'Content-Type': 'multipart/form-data' } })
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
