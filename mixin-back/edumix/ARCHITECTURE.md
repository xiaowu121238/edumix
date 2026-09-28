# EduMix 后端分层说明

后端统一采用以下调用方向：

```text
Controller → Service → ServiceImpl → Mapper → MySQL
```

## 各层职责

- `controller`：接收和校验 HTTP 参数、读取登录用户并返回统一响应，不直接访问数据库。
- `service`：定义业务接口和跨层使用的业务结果类型。
- `service.impl`：实现业务接口，负责事务、权限、状态流转和跨模块编排。
- `mapper`：负责数据库读写；单表 CRUD 使用 MyBatis-Plus，复杂查询通过专用 Mapper 或 `SqlQueryMapper` 执行。
- `entity`：数据库表对应的持久化对象。
- `dto`：前后端请求和响应对象，不直接作为数据库实体使用。
- `security`：JWT、登录用户和请求认证。
- `config`：Spring Security、WebSocket 等应用配置。
- `exception`：业务异常和统一异常响应。

## 示例

资源上传请求的调用顺序：

```text
ResourceController
  → ResourceService
    → ResourceServiceImpl
      → TeachingResourceMapper / ResourceCategoryMapper
        → MySQL
```

Controller 只能依赖 Service 或认证等基础组件，不应依赖 Mapper、`JdbcTemplate` 或数据源。
ServiceImpl 不直接持有 `JdbcTemplate`；需要执行复杂 SQL 时统一委托 Mapper 层。
