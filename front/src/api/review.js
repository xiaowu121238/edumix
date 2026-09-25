import http from './http'

// 审核队列仅对资源负责人和管理员开放。

export const getPendingReviews = () => http.get('/reviews/pending')
export const reviewResource = (id, data) => http.post(`/resources/${id}/review`, data)
