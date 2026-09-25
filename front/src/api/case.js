import http from './http'

// 案例先保存为本地草稿，再显式提交审核；公开状态由后端审核结果决定。
export const getCases = (params) => http.get('/cases', { params })
export const getPublishedCases = (params) => http.get('/cases/published', { params })
export const getMyCases = () => http.get('/cases/mine')
export const createCaseDraft = (data) => http.post('/cases/drafts', data)
export const updateCaseDraft = (id, data) => http.put(`/cases/${id}/draft`, data)
export const getCase = (id) => http.get(`/cases/${id}`)
export const unlockCase = (id) => http.post(`/cases/${id}/unlock`)
export const submitCase = (id) => http.post(`/cases/${id}/submit`)
export const reviewCase = (id, data) => http.post(`/cases/${id}/review`, data)
export const toggleCaseFavorite = (id) => http.post(`/cases/${id}/favorite`)
export const deleteCase = (id) => http.delete(`/cases/${id}`)
