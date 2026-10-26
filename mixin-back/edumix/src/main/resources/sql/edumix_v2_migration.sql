-- EduMix V2 migration
-- Aligns the implementation database with the thesis scope.
-- This migration is additive and preserves all V1 data.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS resource_category (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT UNSIGNED,
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_resource_category_code (category_code),
    KEY idx_resource_category_parent (parent_id, sort_order),
    CONSTRAINT fk_resource_category_parent FOREIGN KEY (parent_id) REFERENCES resource_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Resource categories';

DELIMITER //
DROP PROCEDURE IF EXISTS add_column_if_missing//
CREATE PROCEDURE add_column_if_missing(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column
    ) THEN
        SET @column_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_definition);
        PREPARE column_stmt FROM @column_ddl;
        EXECUTE column_stmt;
        DEALLOCATE PREPARE column_stmt;
    END IF;
END//
DELIMITER ;

CALL add_column_if_missing('teaching_resource','category_id','`category_id` BIGINT UNSIGNED NULL AFTER `uploader_id`');
CALL add_column_if_missing('course','knowledge_objective','`knowledge_objective` TEXT NULL AFTER `description`');
CALL add_column_if_missing('course','ability_objective','`ability_objective` TEXT NULL AFTER `knowledge_objective`');
CALL add_column_if_missing('course','quality_objective','`quality_objective` TEXT NULL AFTER `ability_objective`');

SET @has_resource_category_fk = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'teaching_resource'
      AND constraint_name = 'fk_resource_category'
);
SET @sql = IF(@has_resource_category_fk = 0,
    'ALTER TABLE teaching_resource ADD CONSTRAINT fk_resource_category FOREIGN KEY (category_id) REFERENCES resource_category(id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CALL add_column_if_missing('teaching_case','course_id','`course_id` BIGINT UNSIGNED NULL AFTER `creator_id`');
CALL add_column_if_missing('teaching_case','teaching_objective','`teaching_objective` TEXT NULL AFTER `summary`');
CALL add_column_if_missing('teaching_case','key_points','`key_points` TEXT NULL AFTER `teaching_objective`');
CALL add_column_if_missing('teaching_case','difficult_points','`difficult_points` TEXT NULL AFTER `key_points`');
CALL add_column_if_missing('teaching_case','teaching_process','`teaching_process` TEXT NULL AFTER `teaching_objective`');
CALL add_column_if_missing('teaching_case','teaching_reflection','`teaching_reflection` TEXT NULL AFTER `teaching_process`');
CALL add_column_if_missing('teaching_case','access_type',"`access_type` VARCHAR(20) NOT NULL DEFAULT 'FREE' AFTER `visibility`");
CALL add_column_if_missing('teaching_case','points_price','`points_price` INT UNSIGNED NOT NULL DEFAULT 0 AFTER `access_type`');

SET @has_case_course_fk = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'teaching_case' AND constraint_name = 'fk_case_course'
);
SET @sql = IF(@has_case_course_fk = 0,
    'ALTER TABLE teaching_case ADD CONSTRAINT fk_case_course FOREIGN KEY (course_id) REFERENCES course(id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS case_resource (
    case_id BIGINT UNSIGNED NOT NULL,
    resource_id BIGINT UNSIGNED NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    use_description VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (case_id, resource_id),
    KEY idx_case_resource_order (case_id, sort_order),
    CONSTRAINT fk_case_resource_case FOREIGN KEY (case_id) REFERENCES teaching_case(id),
    CONSTRAINT fk_case_resource_resource FOREIGN KEY (resource_id) REFERENCES teaching_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Resources used by cases';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Unified content audit history';

CREATE TABLE IF NOT EXISTS point_account (
    user_id BIGINT UNSIGNED PRIMARY KEY,
    balance INT UNSIGNED NOT NULL DEFAULT 0,
    total_income INT UNSIGNED NOT NULL DEFAULT 0,
    total_expense INT UNSIGNED NOT NULL DEFAULT 0,
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_point_account_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT ck_point_account_balance CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User point accounts';

CREATE TABLE IF NOT EXISTS point_log (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    operator_id BIGINT UNSIGNED,
    business_type VARCHAR(30) NOT NULL,
    business_id BIGINT UNSIGNED NOT NULL,
    change_amount INT NOT NULL,
    balance_after INT UNSIGNED NOT NULL,
    remark VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_point_log_business (user_id, business_type, business_id),
    KEY idx_point_log_user_time (user_id, create_time),
    CONSTRAINT fk_point_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Immutable point ledger';

CREATE TABLE IF NOT EXISTS resource_favorite (
    user_id BIGINT UNSIGNED NOT NULL,
    resource_id BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, resource_id),
    CONSTRAINT fk_resource_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_resource_favorite_resource FOREIGN KEY (resource_id) REFERENCES teaching_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Resource favorites';

CREATE TABLE IF NOT EXISTS case_favorite (
    user_id BIGINT UNSIGNED NOT NULL,
    case_id BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, case_id),
    CONSTRAINT fk_case_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_case_favorite_case FOREIGN KEY (case_id) REFERENCES teaching_case(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Case favorites';

CREATE TABLE IF NOT EXISTS resource_correction (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    resource_id BIGINT UNSIGNED NOT NULL,
    submitter_id BIGINT UNSIGNED NOT NULL,
    handler_id BIGINT UNSIGNED,
    correction_content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING ACCEPTED REJECTED RESOLVED',
    handle_comment TEXT,
    handled_at DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_correction_status_time (status, create_time),
    CONSTRAINT fk_correction_resource FOREIGN KEY (resource_id) REFERENCES teaching_resource(id),
    CONSTRAINT fk_correction_submitter FOREIGN KEY (submitter_id) REFERENCES sys_user(id),
    CONSTRAINT fk_correction_handler FOREIGN KEY (handler_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Resource correction workflow';

INSERT INTO resource_category (id, parent_id, category_name, category_code, sort_order) VALUES
  (1,NULL,'课件','COURSEWARE',1),(2,NULL,'教学视频','VIDEO',2),(3,NULL,'教学文档','DOCUMENT',3),(4,NULL,'图片与图谱','IMAGE',4)
ON DUPLICATE KEY UPDATE category_name=VALUES(category_name), sort_order=VALUES(sort_order);

UPDATE teaching_resource SET category_id = CASE resource_type
  WHEN 'COURSEWARE' THEN 1 WHEN 'VIDEO' THEN 2 WHEN 'DOCUMENT' THEN 3 WHEN 'IMAGE' THEN 4 ELSE category_id END
WHERE category_id IS NULL;

UPDATE teaching_case SET
  course_id = CASE id WHEN 1 THEN 2 WHEN 3 THEN 2 WHEN 4 THEN 3 ELSE course_id END,
  teaching_objective = COALESCE(teaching_objective, summary),
  teaching_process = COALESCE(teaching_process, content),
  access_type = CASE WHEN id = 4 THEN 'POINTS' ELSE 'FREE' END,
  points_price = CASE WHEN id = 4 THEN 3 ELSE 0 END;

INSERT IGNORE INTO case_resource (case_id, resource_id, sort_order, use_description) VALUES
  (3,4,1,'课堂分组练习材料'),(4,5,1,'并发实验配套指导');

INSERT IGNORE INTO content_review (id,target_type,target_id,reviewer_id,submission_no,review_result,review_comment) VALUES
  (1,'CASE',1,2,1,'APPROVED','案例结构完整，同意公开。'),
  (2,'CASE',4,2,1,'APPROVED','问题典型，建议标记为优秀案例。'),
  (3,'RESOURCE',1,2,1,'APPROVED','课件内容完整。'),
  (4,'RESOURCE',5,2,1,'REJECTED','视频或实验资料需要补充字幕说明。');

INSERT IGNORE INTO point_account (user_id,balance,total_income,total_expense,version) VALUES
  (1,80,100,20,0),(2,125,125,0,0),(3,0,0,0,0)
;

INSERT IGNORE INTO point_log (id,user_id,business_type,business_id,change_amount,balance_after,remark) VALUES
  (1,1,'DAILY_CHECKIN',DATE_FORMAT(CURRENT_DATE,'%Y%m%d'),5,80,'每日签到奖励'),
  (2,1,'CASE_APPROVED',4,25,100,'优秀公共案例审核通过'),
  (3,1,'CASE_UNLOCK',2,-20,80,'解锁积分案例');

INSERT IGNORE INTO resource_favorite (user_id,resource_id) VALUES (1,3);
INSERT IGNORE INTO case_favorite (user_id,case_id) VALUES (1,1);

DROP PROCEDURE IF EXISTS add_column_if_missing;
