import http from './http'

// 公共接口不需要登录，只返回已审核公开内容的摘要。
export const getPublicHome = () => http.get('/public/home')
export const searchPublicContent = (keyword) => http.get('/public/search', { params: { keyword } })
