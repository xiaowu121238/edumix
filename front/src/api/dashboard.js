import http from './http'

// 工作台使用聚合接口，避免首页分别请求多个业务模块。

export const getDashboardSummary = () => http.get('/dashboard/summary')
