-- Private chat data, separate from platform notification tables.
CREATE TABLE IF NOT EXISTS point_grant (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT UNSIGNED NOT NULL,
  recipient_id BIGINT UNSIGNED NOT NULL,
  amount INT UNSIGNED NOT NULL,
  reason VARCHAR(500) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_point_grant_recipient (recipient_id, create_time),
  CONSTRAINT fk_point_grant_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id),
  CONSTRAINT fk_point_grant_recipient FOREIGN KEY (recipient_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO point_rule(rule_code,rule_name,points_value,daily_limit,enabled,description) VALUES
  ('REGISTER','教师注册奖励',20,NULL,1,'自行注册教师账号后一次性发放'),
  ('DAILY_CHECKIN','每日签到',5,1,1,'每日首次签到奖励'),
  ('CASE_APPROVED_FREE','免费案例审核通过',20,NULL,1,'同一案例仅奖励一次'),
  ('CASE_APPROVED_PAID','积分案例审核通过',10,NULL,1,'同一案例仅奖励一次'),
  ('CASE_UNLOCK_SPEND','积分案例解锁支出',0,NULL,1,'按案例实际定价扣除'),
  ('CASE_UNLOCK_INCOME','积分案例解锁收入',0,NULL,1,'按实际成交价格全额给作者'),
  ('ADMIN_GRANT','管理员发放',0,NULL,1,'由管理员核验密码后手动发放')
ON DUPLICATE KEY UPDATE rule_name=VALUES(rule_name),points_value=VALUES(points_value),daily_limit=VALUES(daily_limit),description=VALUES(description);

CREATE TABLE IF NOT EXISTS chat_conversation (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_low_id BIGINT UNSIGNED NOT NULL,
  user_high_id BIGINT UNSIGNED NOT NULL,
  last_message_id BIGINT UNSIGNED NULL,
  last_message_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_chat_pair (user_low_id, user_high_id),
  KEY idx_chat_low_recent (user_low_id, last_message_at),
  KEY idx_chat_high_recent (user_high_id, last_message_at),
  CONSTRAINT fk_chat_low FOREIGN KEY (user_low_id) REFERENCES sys_user(id),
  CONSTRAINT fk_chat_high FOREIGN KEY (user_high_id) REFERENCES sys_user(id),
  CONSTRAINT ck_chat_distinct_users CHECK (user_low_id < user_high_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT UNSIGNED NOT NULL,
  sender_id BIGINT UNSIGNED NOT NULL,
  content VARCHAR(2000) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_chat_message_page (conversation_id, id),
  CONSTRAINT fk_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversation(id),
  CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS chat_read_cursor (
  conversation_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  last_read_message_id BIGINT UNSIGNED NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (conversation_id, user_id),
  CONSTRAINT fk_chat_cursor_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversation(id),
  CONSTRAINT fk_chat_cursor_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- The current product has only two visibility scopes. Normalize legacy data left
-- by early school/college builds while preserving every row's audit status.
UPDATE teaching_resource SET visibility = 'PUBLIC' WHERE visibility IN ('COLLEGE', 'SCHOOL');
UPDATE teaching_case SET visibility = 'PUBLIC' WHERE visibility IN ('COLLEGE', 'SCHOOL');

ALTER TABLE teaching_resource ALTER COLUMN visibility SET DEFAULT 'PRIVATE';
ALTER TABLE teaching_case ALTER COLUMN visibility SET DEFAULT 'PUBLIC';

-- 为资源库和案例库的权限筛选、排序和物理分页补充联合索引。
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'teaching_resource' AND index_name = 'idx_resource_public_page');
SET @ddl = IF(@idx_exists = 0, 'ALTER TABLE teaching_resource ADD INDEX idx_resource_public_page (visibility, audit_status, deleted, create_time, id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'teaching_resource' AND index_name = 'idx_resource_owner_page');
SET @ddl = IF(@idx_exists = 0, 'ALTER TABLE teaching_resource ADD INDEX idx_resource_owner_page (uploader_id, deleted, create_time, id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'teaching_case' AND index_name = 'idx_case_public_page');
SET @ddl = IF(@idx_exists = 0, 'ALTER TABLE teaching_case ADD INDEX idx_case_public_page (visibility, audit_status, deleted, excellent, update_time, id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'teaching_case' AND index_name = 'idx_case_course_page');
SET @ddl = IF(@idx_exists = 0, 'ALTER TABLE teaching_case ADD INDEX idx_case_course_page (course_id, visibility, audit_status, deleted, excellent, update_time, id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
