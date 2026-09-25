import http from './http'

// 登录态由统一的 HTTP 客户端注入 JWT；此处只描述认证与个人资料接口。

export const login = (credentials) => http.post('/auth/login', credentials)
export const register = (data) => http.post('/auth/register', data)
export const getCurrentUser = () => http.get('/auth/me')
export const logout = () => http.post('/auth/logout')
export const updateProfile = (profile) => http.put('/profile', profile)
export const uploadAvatar = (file) => {
  const data = new FormData()
  data.append('file', file)
  return http.post('/profile/avatar', data, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const changePassword = (data) => http.post('/profile/password', data)
