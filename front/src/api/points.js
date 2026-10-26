import http from './http'

// 积分变动只能由后端规则产生，前端不直接计算或修改余额。
export const getWallet = (config) => http.get('/points/wallet', config)
export const getPointHistory = () => http.get('/points/history')
export const checkIn = () => http.post('/points/check-in')
export const getCheckInDates = (year, month) => http.get('/points/check-ins', { params: { year, month } })
export const grantPoints = (data) => http.post('/points/grant', data)
