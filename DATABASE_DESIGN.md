# EduMix 数据库设计说明

## 1. 当前业务范围

数据库只围绕项目已经实现的功能设计。当前核心业务包括：

- 教师、资源负责人、管理员账号与角色权限；
- 课程—章节—知识点三级课程体系；
- 教学资源上传、审核、预览、收藏与纠错；
- 教学案例草稿、富文本编辑、审核、积分解锁与收藏；
- 积分账户、签到、奖励、消费与管理员发放；
- 系统通知、审核通知和用户私信；
- AI 公共内容推荐与基于收藏内容的案例整合。

## 2. 核心数据模块

### 用户与权限

`sys_user`、`sys_role`、`sys_user_role`

用户与角色采用多对多关系。普通注册账号自动获得教师角色；资源负责人同时拥有教师和资源负责人角色。

### 课程体系

`course`、`course_teacher`、`course_chapter`、`knowledge_point`

课程包含知识、能力、素质三个维度的教学目标。章节和知识点均采用逻辑删除，资源与案例通过知识点建立教学语义关联。

### 教学资源库

`resource_category`、`teaching_resource`、`resource_knowledge_point`、`resource_review`、`resource_favorite`、`resource_correction`

文件本体保存在服务器上传目录，数据库只保存文件元数据和相对访问地址。公开资源必须满足 `visibility=PUBLIC`、`audit_status=APPROVED`、`deleted=0`。

### 教学案例库

`teaching_case`、`case_knowledge_point`、`case_resource`、`case_review`、`case_favorite`、`case_unlock`

案例正文采用经过清洗的富文本 HTML，可嵌入已关联的图片、视频和其他教学资源。草稿仅作者可见；提交并审核通过后才进入公共案例库。积分案例价格限制为 1～3 分。

### 积分与签到

`point_account`、`point_rule`、`point_log`、`point_grant`、`daily_checkin`

所有积分变化必须写入流水。案例解锁时在同一事务中完成购买者扣分、作者收入和解锁记录写入。

### 消息与私信

`message`、`user_message`、`chat_conversation`、`chat_message`、`chat_read_cursor`

平台消息用于系统公告和审核结果；私信使用独立会话及已读游标，避免与系统通知混用。

### AI 记录

`ai_generation_task`、`ai_recommendation`

AI 业务面向资源推荐和案例草稿整合。候选内容只允许来自公开且审核通过的资源、案例，或当前用户已经收藏并有权访问的内容。

## 3. 关键约束

1. 公共查询必须同时检查公开范围、审核状态和逻辑删除状态。
2. 待审核、驳回、私有资源以及个人案例草稿不得出现在公共接口中。
3. 积分案例未解锁前不返回正文，也不能加入收藏。
4. 审核记录、积分流水和解锁记录保留历史，不以当前主表状态代替过程数据。
5. 文件真实存储路径、密码、JWT 密钥和 AI API Key 不写入公共响应或版本库。
