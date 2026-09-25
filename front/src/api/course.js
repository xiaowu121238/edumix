import http from './http'

// 课程、章节、知识点及知识点资源关联接口。

export const getCourses = () => http.get('/courses')
export const getCourseTree = (courseId) => http.get(`/courses/${courseId}/knowledge-tree`)
export const createCourse = (data) => http.post('/courses', data)
export const updateCourse = (id, data) => http.put(`/courses/${id}`, data)
export const deleteCourse = (id) => http.delete(`/courses/${id}`)
export const createChapter = (courseId, data) => http.post(`/courses/${courseId}/chapters`, data)
export const updateChapter = (id, data) => http.put(`/chapters/${id}`, data)
export const deleteChapter = (id) => http.delete(`/chapters/${id}`)
export const createKnowledgePoint = (chapterId, data) =>
  http.post(`/chapters/${chapterId}/knowledge-points`, data)
export const updateKnowledgePoint = (id, data) => http.put(`/knowledge-points/${id}`, data)
export const deleteKnowledgePoint = (id) => http.delete(`/knowledge-points/${id}`)
export const getKnowledgePointResources = (id) => http.get(`/knowledge-points/${id}/resources`)
export const getKnowledgePointResourceCandidates = (id) =>
  http.get(`/knowledge-points/${id}/resource-candidates`)
export const linkKnowledgePointResources = (id, resourceIds) =>
  http.post(`/knowledge-points/${id}/resources`, resourceIds)
export const unlinkKnowledgePointResource = (id, resourceId) =>
  http.delete(`/knowledge-points/${id}/resources/${resourceId}`)
