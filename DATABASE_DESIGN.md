# EduMix 数据库设计审查（V2）

本设计以开题报告确定的业务范围为基准：教师、资源负责人、管理员三类角色；课程—章节—知识点组织体系；资源库与案例库；资源审核；资源包；授课方案；积分激励；AI 辅助推荐。

## 审查结论

V1 的 18 张表能够支持用户、课程、资源、案例、资源包、授课方案和消息等基础流程，但存在以下缺口：

- 缺少资源分类以及资源、案例的审核历史。
- 案例缺少课程、教学目标、实施过程、反思、访问方式与积分价格。
- 通用 `resource_package_item` 使用类型字段关联不同主表，无法建立内容外键。
- 授课方案只能关联一个资源包，不符合“一个方案可关联多个资源包”。
- 缺少签到、积分账户、积分流水和案例解锁记录。
- AI 任务没有明确的资源包/授课方案分析对象，也没有逐项推荐与教师确认记录。
- 开题报告中的收藏、有效纠错功能没有数据支撑。

V2 采用增量迁移补齐上述缺口，保留 V1 数据。

## 分层表结构

### 基础与权限

`sys_user`、`sys_role`、`sys_user_role`

### 课程体系

`course`、`course_teacher`、`course_chapter`、`knowledge_point`

### 资源与案例

`resource_category`、`teaching_resource`、`resource_knowledge_point`、`resource_review`、`teaching_case`、`case_knowledge_point`、`case_resource`、`case_review`、`resource_favorite`、`case_favorite`、`resource_correction`

### 辅助备课

`resource_package`、`package_resource`、`package_case`、`teaching_plan`、`plan_package`、`teaching_plan_item`

### 积分激励

`point_account`、`point_rule`、`daily_checkin`、`point_log`、`case_unlock`

### AI 与消息

`ai_generation_task`、`ai_recommendation`、`message`、`user_message`

## 关键设计决定

1. 资源包中的资源和案例分别使用 `package_resource`、`package_case`，保证真实外键约束。
2. 授课方案通过 `plan_package` 关联多个资源包。
3. `point_log(user_id,business_type,business_id)` 唯一，保证奖励和扣分接口幂等。
4. `daily_checkin(user_id,checkin_date)` 唯一，避免重复签到。
5. `case_unlock(user_id,case_id)` 唯一，避免重复扣除解锁积分。
6. 积分余额使用 `point_account.version` 实现乐观锁，扣分、作者分成和流水必须放在同一事务。
7. AI 任务的 `package_id` 与 `plan_id` 必须且只能存在一个。
8. AI 推荐的 `resource_id` 与 `case_id` 必须且只能存在一个；`selected` 记录教师是否确认，AI 不直接修改资源包或方案。
9. 审核状态字段保存当前状态，`resource_review`、`case_review` 保存完整审核历史。
10. V1 的 `resource_package_item` 和 `teaching_plan.package_id` 暂留兼容，新代码不再写入。

## 事务边界

- 案例审核通过：更新案例状态、写审核记录、发放作者积分、写积分流水、发送消息。
- 案例解锁：校验余额、扣除积分、写解锁记录和流水、发放作者分成。
- AI 推荐确认：更新推荐项 `selected`，再将所选资源/案例写入目标资源包或方案。

以上流程应在 Spring `@Transactional` 方法中完成。
