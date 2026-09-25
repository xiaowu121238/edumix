import http from './http'

// 私信的历史数据通过 HTTP 获取，实时新消息由 ChatView 中的 WebSocket 接收。

export const searchTeachers = (keyword) => http.get('/teachers', { params: { keyword } })
export const getTeacher = (id) => http.get(`/teachers/${id}`)
export const getConversations = () => http.get('/chats')
export const startConversation = (teacherId) => http.post(`/chats/with/${teacherId}`)
export const getChatMessages = (id, beforeId, size = 30) =>
  http.get(`/chats/${id}/messages`, { params: { beforeId, size } })
export const sendChatMessage = (id, content) => http.post(`/chats/${id}/messages`, { content })
export const markChatRead = (id) => http.post(`/chats/${id}/read`)
