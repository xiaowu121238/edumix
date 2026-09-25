# EduMix 前端

基于 Vue 3、Vite、Vue Router 和 Axios 的混合式教学资源平台前端。

## 启动

```bash
npm install
npm run dev
```

开发地址默认为 `http://localhost:5173`，`/api` 请求会代理至 `http://localhost:8080`。

## 已实现页面

- 门户首页
- 登录页（当前为演示登录）
- 教师工作台
- 课程与知识点管理
- 消息中心
- 其他业务模块占位页

## Spring Boot 接口约定

接口封装位于 `src/api`：

- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/courses`
- `GET /api/courses/{courseId}/knowledge-tree`

后端接口完成后，可将登录页中的演示逻辑替换为 `src/api/auth.js` 提供的请求方法。
