# EduMix 认证与权限接口

## 测试账号

| 账号       | 密码     | 角色               |
| ---------- | -------- | ------------------ |
| `teacher`  | `123456` | `TEACHER`          |
| `reviewer` | `123456` | `RESOURCE_MANAGER` |
| `admin`    | `123456` | `ADMIN`            |

测试密码首次启动后会自动迁移为 BCrypt，数据库不保存明文密码。

## 接口

- `POST /api/auth/login`：登录并签发 JWT。
- `POST /api/auth/logout`：注销当前 JWT。
- `GET /api/auth/me`：获取当前用户、角色和积分余额。
- `GET /api/profile`：获取个人资料。
- `PUT /api/profile`：修改个人资料。

请求受保护接口时添加：

```http
Authorization: Bearer <token>
```

## 权限验证样例

- `GET /api/permission-test/teacher`：仅教师。
- `GET /api/permission-test/resource-manager`：仅资源负责人。
- `GET /api/permission-test/admin`：仅管理员。

业务 Controller 可使用：

```java
@PreAuthorize("hasRole('TEACHER')")
```

未登录返回 HTTP 401，无权访问返回 HTTP 403。

## 生产注意事项

- 通过环境变量 `JWT_SECRET` 注入至少 256 位随机密钥。
- 当前注销黑名单存放于单机内存，单体部署可用；多实例部署时应迁移至 Redis。
- 演示账号上线前必须删除或强制修改密码。
