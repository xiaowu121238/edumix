import http from './http'

// AI 请求可能包含模型推理过程，因此单独使用较长的超时时间。

export const getAiStatus = () => http.get('/ai/status')
export const getAiRecommendations = (data) => http.post('/ai/recommend', data, { timeout: 60000 })
export const chatWithAi = (data) => http.post('/ai/chat', data, { timeout: 60000 })
