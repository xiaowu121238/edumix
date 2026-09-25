-- Demo data for frontend/backend integration. Safe for repeated execution.
SET NAMES utf8mb4;

INSERT INTO sys_role (id, role_code, role_name, description) VALUES
  (1, 'ADMIN', '系统管理员', '平台配置与用户管理'),
  (2, 'TEACHER', '教师', '课程、资源和授课方案管理'),
  (3, 'AUDITOR', '资源审核员', '教学资源与案例审核')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description);

INSERT INTO sys_user (id, username, password, real_name, email, college, title, status) VALUES
  (1, 'teacher', '{noop}123456', '李教授', 'teacher@edumix.local', '计算机学院', '教授', 1),
  (2, 'zhang', '{noop}123456', '张主任', 'zhang@edumix.local', '计算机学院', '教研室主任', 1),
  (3, 'admin', '{noop}123456', '系统管理员', 'admin@edumix.local', '教务处', '管理员', 1)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), college = VALUES(college), title = VALUES(title);

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1,2),(2,2),(2,3),(3,1);

INSERT INTO course (id, course_code, course_name, description, college, semester, owner_id, status) VALUES
  (1, 'JAVA-OOP-2026', 'Java面向对象编程', 'Java语言基础、面向对象设计与工程实践', '计算机学院', '2026-2027-1', 1, 1),
  (2, 'SE-ADV-2026', '高级软件工程', '软件架构、设计模式和团队工程实践', '计算机学院', '2026-2027-1', 1, 1),
  (3, 'JAVA-CONC-2026', 'Java并发编程深度解析', 'Java并发模型、线程安全和性能优化', '计算机学院', '2026-2027-1', 1, 1)
ON DUPLICATE KEY UPDATE course_name = VALUES(course_name), description = VALUES(description), semester = VALUES(semester);

INSERT IGNORE INTO course_teacher (course_id, teacher_id, member_role) VALUES (1,1,'OWNER'),(2,1,'OWNER'),(2,2,'COLLABORATOR'),(3,1,'OWNER');

INSERT INTO course_chapter (id, course_id, chapter_name, chapter_no, sort_order) VALUES
  (1,1,'第一章 面向对象程序设计基础','1',1),
  (2,1,'第二章 Java 集合框架','2',2),
  (3,1,'第三章 异常处理与泛型','3',3),
  (4,2,'第一章 软件架构基础','1',1),
  (5,3,'第一章 Java 并发基础','1',1)
ON DUPLICATE KEY UPDATE chapter_name = VALUES(chapter_name), sort_order = VALUES(sort_order);

INSERT INTO knowledge_point (id, course_id, chapter_id, point_name, point_code, difficulty, teaching_hours, sort_order) VALUES
  (1,1,1,'类与对象的基本概念','OOP-01',1,2,1),
  (2,1,1,'封装、继承与多态','OOP-02',2,4,2),
  (3,1,1,'抽象类与接口','OOP-03',2,2,3),
  (4,1,2,'Collection 体系结构','COL-01',2,2,1),
  (5,1,2,'List、Set 与 Map','COL-02',2,4,2),
  (6,1,3,'异常分类与处理机制','EX-01',2,2,1),
  (7,2,4,'常用软件设计模式','SE-01',3,4,1),
  (8,3,5,'线程生命周期与同步','CONC-01',3,4,1)
ON DUPLICATE KEY UPDATE point_name = VALUES(point_name), difficulty = VALUES(difficulty), teaching_hours = VALUES(teaching_hours);

INSERT INTO teaching_resource (id, resource_no, resource_name, resource_type, description, file_url, uploader_id, visibility, audit_status, download_count, reference_count) VALUES
  (1,'RES-2026-001','面向对象基础教学课件','COURSEWARE','类、对象与消息传递核心概念','/uploads/demo/oop-basics.pptx',1,'PUBLIC','APPROVED',126,42),
  (2,'RES-2026-002','继承与多态微课视频','VIDEO','通过订单计价案例演示多态','/uploads/demo/polymorphism.mp4',1,'PUBLIC','APPROVED',203,56),
  (3,'RES-2026-003','Java集合框架知识图谱','IMAGE','Collection、Map及常用实现关系','/uploads/demo/collection-map.png',1,'PUBLIC','APPROVED',180,35),
  (4,'RES-2026-004','设计模式课堂练习','DOCUMENT','策略模式与工厂模式分组练习','/uploads/demo/design-pattern.pdf',2,'PUBLIC','APPROVED',98,27),
  (5,'RES-2026-005','并发编程实验指导','DOCUMENT','线程同步、锁与死锁实验','/uploads/demo/concurrency-lab.pdf',1,'PUBLIC','PENDING',45,18)
ON DUPLICATE KEY UPDATE resource_name = VALUES(resource_name), description = VALUES(description), audit_status = VALUES(audit_status);

INSERT IGNORE INTO resource_knowledge_point (resource_id, knowledge_point_id, relevance_score, source) VALUES
  (1,1,1.0000,'MANUAL'),(1,2,0.8200,'AI'),(2,2,1.0000,'MANUAL'),(3,4,0.9500,'AI'),(3,5,1.0000,'MANUAL'),(4,7,1.0000,'MANUAL'),(5,8,1.0000,'MANUAL');

INSERT INTO teaching_case (id, case_no, case_name, summary, content, subject, creator_id, visibility, audit_status, excellent, reference_count) VALUES
  (1,'CASE-2026-001','Spring Boot 微服务实战案例','从单体应用到微服务的课堂项目案例','包含问题背景、架构拆分、课堂活动和评价方案。','软件工程',1,'PUBLIC','APPROVED',1,86),
  (2,'CASE-2026-002','AI辅助的混合式教学设计','利用AI完成课前诊断与课堂资源推荐','包含课前、课中和课后的混合式教学流程。','教学创新',2,'PUBLIC','APPROVED',1,64),
  (3,'CASE-2026-003','设计模式协作课堂实践','通过小组协作重构订单系统','学生分组识别坏味道并使用设计模式完成重构。','计算机科学',2,'PUBLIC','APPROVED',0,48),
  (4,'CASE-2026-004','多线程死锁案例','通过银行转账场景定位与修复死锁','包含死锁复现、线程转储分析和修复策略。','计算机科学',1,'PUBLIC','APPROVED',1,39)
ON DUPLICATE KEY UPDATE case_name = VALUES(case_name), summary = VALUES(summary), excellent = VALUES(excellent);

INSERT IGNORE INTO case_knowledge_point (case_id, knowledge_point_id) VALUES (3,7),(4,8);

INSERT INTO resource_package (id, package_no, package_name, description, course_id, creator_id, generation_type, status, progress) VALUES
  (1,'PKG-2026-001','Spring Boot 实战资源包','面向微服务实践教学的课件、案例与练习合集',2,1,'AI','PUBLISHED',100),
  (2,'PKG-2026-002','Java面向对象基础资源包','面向第一章教学的资源组合',1,1,'AI','DRAFT',75)
ON DUPLICATE KEY UPDATE package_name = VALUES(package_name), description = VALUES(description), status = VALUES(status), progress = VALUES(progress);

INSERT IGNORE INTO resource_package_item (package_id, item_type, item_id, sort_order, note) VALUES
  (1,'RESOURCE',4,1,'设计模式课前练习'),(1,'CASE',1,2,'项目导入案例'),
  (2,'RESOURCE',1,1,'课堂讲授课件'),(2,'RESOURCE',2,2,'课后微课'),(2,'CASE',3,3,'协作实践案例');

INSERT INTO teaching_plan (id, plan_no, plan_name, course_id, chapter_id, package_id, creator_id, teaching_objectives, key_points, difficult_points, status, progress) VALUES
  (1,'PLAN-2026-001','Spring Boot 核心原理与微服务实践',2,4,1,1,'理解微服务架构并完成基础服务拆分','服务边界与接口设计','分布式数据一致性','DRAFT',75),
  (2,'PLAN-2026-002','高级软件工程：设计模式实验',2,4,1,1,'能够识别并应用常用设计模式','策略模式、工厂模式','模式选择与权衡','DRAFT',30),
  (3,'PLAN-2026-003','Java 并发编程第三周教学预案',3,5,NULL,1,'理解线程同步并定位常见并发问题','锁与线程安全','死锁诊断','DRAFT',0)
ON DUPLICATE KEY UPDATE plan_name = VALUES(plan_name), status = VALUES(status), progress = VALUES(progress);

INSERT INTO teaching_plan_item (id, plan_id, stage_name, stage_type, duration_minutes, content, sort_order) VALUES
  (1,1,'案例导入','INTRODUCTION',10,'展示传统单体应用扩展问题',1),
  (2,1,'核心概念讲授','TEACHING',25,'讲解服务拆分、注册发现与配置中心',2),
  (3,1,'分组架构设计','PRACTICE',35,'学生完成业务边界划分',3),
  (4,1,'总结与评价','SUMMARY',10,'汇总方案并完成同伴评价',4)
ON DUPLICATE KEY UPDATE stage_name = VALUES(stage_name), duration_minutes = VALUES(duration_minutes), content = VALUES(content);

INSERT INTO ai_generation_task (id, task_no, user_id, course_id, task_type, input_json, result_json, status, started_time, finished_time) VALUES
  (1,'AI-2026-001',1,2,'RESOURCE_MATCH',JSON_OBJECT('keywords',JSON_ARRAY('Spring Boot','微服务')),JSON_OBJECT('matchedCount',12,'packageId',1),'SUCCESS',NOW() - INTERVAL 5 MINUTE,NOW() - INTERVAL 4 MINUTE)
ON DUPLICATE KEY UPDATE status = VALUES(status), result_json = VALUES(result_json);

INSERT INTO message (id, message_type, title, content, sender_id, business_type, business_id, create_time) VALUES
  (1,'SYSTEM','V2.0 版本更新通知：AI 教学助手全新上线','平台已完成升级，新增资源包一键生成、智能知识点提取等功能。',3,NULL,NULL,NOW() - INTERVAL 10 MINUTE),
  (2,'SYSTEM','下周全校停机维护公告','因核心机房网络设备升级，本平台将于本周日凌晨暂停服务。',3,NULL,NULL,NOW() - INTERVAL 1 DAY),
  (3,'COLLAB','张主任共享了授课方案《高级软件工程》','张主任将您添加为协作教师，您可以查看并编辑相关案例资源。',2,'COURSE',2,NOW() - INTERVAL 2 HOUR),
  (4,'AUDIT','《多线程死锁案例》已被标记为优秀资源','教务处审核反馈：该案例设计巧妙，建议在公共资源库中重点推荐。',3,'CASE',4,NOW() - INTERVAL 1 DAY),
  (5,'AUDIT','《Spring Boot 实战》已通过院系审核','您提交的公开教学资源包现已上架至全校资源库。',3,'PACKAGE',1,NOW() - INTERVAL 3 DAY),
  (6,'AUDIT','资源格式反馈：视频缺少字幕','建议为微课视频补充字幕文件，以便于 AI 更好地进行文本检索。',3,'RESOURCE',2,NOW() - INTERVAL 5 DAY)
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content);

INSERT IGNORE INTO user_message (user_id, message_id, read_status, read_time) VALUES
  (1,1,0,NULL),(1,2,0,NULL),(1,3,0,NULL),(1,4,0,NULL),(1,5,1,NOW() - INTERVAL 2 DAY),(1,6,1,NOW() - INTERVAL 4 DAY);
