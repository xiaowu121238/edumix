# EduMix 混合式教学资源平台

项目包含 Vue 3 前端和 Spring Boot 后端：

- `front`：Vue 3、Vite、Vue Router。
- `mixin-back/edumix`：Spring Boot、Spring Security、MyBatis-Plus、MySQL、Spring AI。

后端目录和调用关系见 [后端分层说明](mixin-back/edumix/ARCHITECTURE.md)。

## 本地运行

1. 在 MySQL 中创建 `edumix` 数据库。
2. 参考 `mixin-back/edumix/.env.example`，在 IDEA 运行配置中填写数据库、JWT 和 AI 环境变量。
3. 在 `mixin-back/edumix` 执行 `mvn spring-boot:run`。
4. 在 `front` 执行 `npm install` 和 `npm run dev`。
