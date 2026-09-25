import http from './http'

// 管理员账号管理接口。角色分配必须由后端再次校验，不能信任前端传值。

export const listAccounts = () => http.get('/admin/accounts')
export const createAccount = (data) => http.post('/admin/accounts', data)
export const setAccountRoles = (id, roles) => http.put(`/admin/accounts/${id}/roles`, { roles })
export const setAccountStatus = (id, status) => http.put(`/admin/accounts/${id}/status`, { status })
export const resetAccountPassword = (id, password) =>
  http.post(`/admin/accounts/${id}/reset-password`, { password })
