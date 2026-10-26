import http from './http'

// 系统公告和审核通知共用消息中心，私信未读数由后端一并聚合返回。

export const getNotifications = () => http.get('/notifications')
export const getUnreadCounts = (config) => http.get('/notifications/unread-count', config)
export const readNotification = (id) => http.post(`/notifications/${id}/read`)
export const publishSystemNotice = (data) => http.post('/notifications/system', data)
