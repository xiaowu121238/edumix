-- 统一保存资源和案例的审核历史。
CREATE TABLE IF NOT EXISTS content_review (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  target_type VARCHAR(20) NOT NULL COMMENT 'RESOURCE CASE',
  target_id BIGINT UNSIGNED NOT NULL,
  reviewer_id BIGINT UNSIGNED NOT NULL,
  submission_no INT UNSIGNED NOT NULL DEFAULT 1,
  review_result VARCHAR(20) NOT NULL COMMENT 'APPROVED REJECTED',
  review_comment TEXT,
  reviewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_content_review_submission (target_type,target_id,submission_no),
  KEY idx_content_review_reviewer (reviewer_id,reviewed_at),
  KEY idx_content_review_target (target_type,target_id,reviewed_at),
  CONSTRAINT fk_content_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 私信数据与平台通知分开保存；两名成员的已读位置直接记录在会话表中。

CREATE TABLE IF NOT EXISTS chat_conversation (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_low_id BIGINT UNSIGNED NOT NULL,
  user_high_id BIGINT UNSIGNED NOT NULL,
  last_message_id BIGINT UNSIGNED NULL,
  last_message_at DATETIME NULL,
  low_last_read_message_id BIGINT UNSIGNED NULL,
  high_last_read_message_id BIGINT UNSIGNED NULL,
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

-- 兼容已存在的会话表和积分流水表。
SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='chat_conversation' AND column_name='low_last_read_message_id');
SET @ddl = IF(@column_exists=0,'ALTER TABLE chat_conversation ADD COLUMN low_last_read_message_id BIGINT UNSIGNED NULL AFTER last_message_at','SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='chat_conversation' AND column_name='high_last_read_message_id');
SET @ddl = IF(@column_exists=0,'ALTER TABLE chat_conversation ADD COLUMN high_last_read_message_id BIGINT UNSIGNED NULL AFTER low_last_read_message_id','SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='point_log' AND column_name='operator_id');
SET @ddl = IF(@column_exists=0,'ALTER TABLE point_log ADD COLUMN operator_id BIGINT UNSIGNED NULL AFTER user_id','SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

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

-- 注销版本并入用户表，避免为 JWT 黑名单单独增加一张技术表。
SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sys_user' AND column_name='token_version');
SET @ddl = IF(@column_exists=0,'ALTER TABLE sys_user ADD COLUMN token_version INT UNSIGNED NOT NULL DEFAULT 0 AFTER status','SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
