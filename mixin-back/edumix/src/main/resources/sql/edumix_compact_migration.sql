-- EduMix 数据库精简迁移：31 张业务表精简为 22 张。
-- 执行前请先备份数据库。本脚本会先迁移数据，再删除被合并或停用的旧表。
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
    IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 资源审核与案例审核合并为统一审核历史。
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
    UNIQUE KEY uk_content_review_submission (target_type, target_id, submission_no),
    KEY idx_content_review_reviewer (reviewer_id, reviewed_at),
    KEY idx_content_review_target (target_type, target_id, reviewed_at),
    CONSTRAINT fk_content_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一内容审核历史';

INSERT IGNORE INTO content_review
    (target_type,target_id,reviewer_id,submission_no,review_result,review_comment,
     reviewed_at,create_time,update_time,deleted)
SELECT 'RESOURCE',resource_id,reviewer_id,submission_no,review_result,review_comment,
       reviewed_at,create_time,update_time,deleted
FROM resource_review;

INSERT IGNORE INTO content_review
    (target_type,target_id,reviewer_id,submission_no,review_result,review_comment,
     reviewed_at,create_time,update_time,deleted)
SELECT 'CASE',case_id,reviewer_id,submission_no,review_result,review_comment,
       reviewed_at,create_time,update_time,deleted
FROM case_review;

-- 每个会话只有两名固定成员，已读游标直接放在会话记录中。
CALL add_column_if_missing('chat_conversation','low_last_read_message_id',
    'BIGINT UNSIGNED NULL AFTER `last_message_at`');
CALL add_column_if_missing('chat_conversation','high_last_read_message_id',
    'BIGINT UNSIGNED NULL AFTER `low_last_read_message_id`');

UPDATE chat_conversation c
LEFT JOIN chat_read_cursor low_cursor
       ON low_cursor.conversation_id=c.id AND low_cursor.user_id=c.user_low_id
LEFT JOIN chat_read_cursor high_cursor
       ON high_cursor.conversation_id=c.id AND high_cursor.user_id=c.user_high_id
SET c.low_last_read_message_id=low_cursor.last_read_message_id,
    c.high_last_read_message_id=high_cursor.last_read_message_id;

-- 管理员发放信息并入积分流水；签到与案例解锁事实也由流水唯一键表达。
CALL add_column_if_missing('point_log','operator_id',
    'BIGINT UNSIGNED NULL AFTER `user_id`');

UPDATE point_log pl
JOIN point_grant pg
  ON pl.user_id=pg.recipient_id
 AND pl.business_type='ADMIN_GRANT'
 AND pl.business_id=pg.id
SET pl.operator_id=pg.operator_id;

INSERT IGNORE INTO point_log
    (user_id,business_type,business_id,change_amount,balance_after,remark,create_time)
SELECT dc.user_id,'DAILY_CHECKIN',CAST(DATE_FORMAT(dc.checkin_date,'%Y%m%d') AS UNSIGNED),
       dc.reward_points,pa.balance,'由旧签到记录迁移',dc.create_time
FROM daily_checkin dc
JOIN point_account pa ON pa.user_id=dc.user_id
WHERE dc.deleted=0;

INSERT IGNORE INTO point_log
    (user_id,business_type,business_id,change_amount,balance_after,remark,create_time)
SELECT cu.user_id,'CASE_UNLOCK',cu.case_id,-cu.cost_points,pa.balance,
       '由旧案例解锁记录迁移',cu.unlocked_at
FROM case_unlock cu
JOIN point_account pa ON pa.user_id=cu.user_id
WHERE cu.deleted=0
  AND NOT EXISTS (
      SELECT 1 FROM point_log pl
      WHERE pl.user_id=cu.user_id AND pl.business_id=cu.case_id
        AND pl.business_type IN ('CASE_UNLOCK','CASE_UNLOCK_SPEND') AND pl.deleted=0
  );

-- point_rule 未参与运行时规则计算，移除流水中的冗余外键列后再删表。
SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema=DATABASE() AND table_name='point_log'
      AND constraint_name='fk_point_log_rule' AND constraint_type='FOREIGN KEY'
);
SET @ddl = IF(@fk_exists>0,
    'ALTER TABLE point_log DROP FOREIGN KEY fk_point_log_rule','SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='point_log' AND column_name='rule_id'
);
SET @ddl = IF(@column_exists>0,
    'ALTER TABLE point_log DROP COLUMN rule_id','SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 数据已迁入保留表后，删除 9 张旧表。
SET FOREIGN_KEY_CHECKS=0;
DROP TABLE IF EXISTS ai_recommendation;
DROP TABLE IF EXISTS ai_generation_task;
DROP TABLE IF EXISTS course_teacher;
DROP TABLE IF EXISTS point_rule;
DROP TABLE IF EXISTS daily_checkin;
DROP TABLE IF EXISTS point_grant;
DROP TABLE IF EXISTS case_unlock;
DROP TABLE IF EXISTS resource_review;
DROP TABLE IF EXISTS case_review;
DROP TABLE IF EXISTS chat_read_cursor;
SET FOREIGN_KEY_CHECKS=1;

DROP PROCEDURE IF EXISTS add_column_if_missing;
