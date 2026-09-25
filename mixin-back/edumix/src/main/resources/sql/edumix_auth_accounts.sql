-- Three isolated accounts for authentication and authorization tests.
-- Passwords use {noop} only for local integration. Replace with BCrypt before deployment.
SET NAMES utf8mb4;

UPDATE sys_role
SET role_code = 'RESOURCE_MANAGER', role_name = '资源负责人', description = '公共资源、案例审核与纠错处理'
WHERE id = 3;

INSERT INTO sys_role (id, role_code, role_name, description) VALUES
  (1, 'ADMIN', '管理员', '用户、角色、规则与系统管理'),
  (2, 'TEACHER', '教师', '课程、资源、案例与备课业务'),
  (3, 'RESOURCE_MANAGER', '资源负责人', '公共资源、案例审核与纠错处理')
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name), description=VALUES(description);

INSERT INTO sys_user (id, username, password, real_name, email, college, title, status) VALUES
  (1, 'teacher', '{noop}123456', '李教授', 'teacher@edumix.local', '计算机学院', '教授', 1),
  (2, 'reviewer', '{noop}123456', '张主任', 'reviewer@edumix.local', '教务处', '资源负责人', 1),
  (3, 'admin', '{noop}123456', '系统管理员', 'admin@edumix.local', '信息化中心', '管理员', 1)
ON DUPLICATE KEY UPDATE
  username=VALUES(username), real_name=VALUES(real_name),
  email=VALUES(email), college=VALUES(college), title=VALUES(title), status=VALUES(status);

DELETE FROM sys_user_role WHERE user_id IN (1,2,3);
INSERT INTO sys_user_role (user_id, role_id) VALUES (1,2),(2,2),(2,3),(3,1);
