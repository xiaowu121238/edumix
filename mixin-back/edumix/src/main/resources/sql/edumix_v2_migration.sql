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

CREATE TABLE IF NOT EXISTS case_review (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    case_id BIGINT UNSIGNED NOT NULL,
    reviewer_id BIGINT UNSIGNED NOT NULL,
    submission_no INT UNSIGNED NOT NULL DEFAULT 1,
    review_result VARCHAR(20) NOT NULL COMMENT 'APPROVED REJECTED',
    review_comment TEXT,
    reviewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_case_review_submission (case_id, submission_no),
    KEY idx_case_review_reviewer (reviewer_id, reviewed_at),
    CONSTRAINT fk_case_review_case FOREIGN KEY (case_id) REFERENCES teaching_case(id),
    CONSTRAINT fk_case_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Case audit history';

CREATE TABLE IF NOT EXISTS resource_review (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    resource_id BIGINT UNSIGNED NOT NULL,
    reviewer_id BIGINT UNSIGNED NOT NULL,
    submission_no INT UNSIGNED NOT NULL DEFAULT 1,
    review_result VARCHAR(20) NOT NULL COMMENT 'APPROVED REJECTED',
    review_comment TEXT,
    reviewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_resource_review_submission (resource_id, submission_no),
    KEY idx_resource_review_reviewer (reviewer_id, reviewed_at),
    CONSTRAINT fk_resource_review_resource FOREIGN KEY (resource_id) REFERENCES teaching_resource(id),
    CONSTRAINT fk_resource_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Resource audit history';

CALL add_column_if_missing('resource_package','visibility',"`visibility` VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' AFTER `creator_id`");

CREATE TABLE IF NOT EXISTS package_resource (
    package_id BIGINT UNSIGNED NOT NULL,
    resource_id BIGINT UNSIGNED NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    note VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (package_id, resource_id),
    KEY idx_package_resource_order (package_id, sort_order),
    CONSTRAINT fk_package_resource_package FOREIGN KEY (package_id) REFERENCES resource_package(id),
    CONSTRAINT fk_package_resource_resource FOREIGN KEY (resource_id) REFERENCES teaching_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Resources in packages';

CREATE TABLE IF NOT EXISTS package_case (
    package_id BIGINT UNSIGNED NOT NULL,
    case_id BIGINT UNSIGNED NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    note VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (package_id, case_id),
    KEY idx_package_case_order (package_id, sort_order),
    CONSTRAINT fk_package_case_package FOREIGN KEY (package_id) REFERENCES resource_package(id),
    CONSTRAINT fk_package_case_case FOREIGN KEY (case_id) REFERENCES teaching_case(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Cases in packages';

INSERT IGNORE INTO package_resource (package_id, resource_id, sort_order, note)
SELECT package_id, item_id, sort_order, note FROM resource_package_item WHERE item_type = 'RESOURCE';
INSERT IGNORE INTO package_case (package_id, case_id, sort_order, note)
SELECT package_id, item_id, sort_order, note FROM resource_package_item WHERE item_type = 'CASE';

CALL add_column_if_missing('teaching_plan','teaching_method','`teaching_method` TEXT NULL AFTER `teaching_objectives`');
CALL add_column_if_missing('teaching_plan','total_minutes','`total_minutes` INT UNSIGNED NOT NULL DEFAULT 0 AFTER `difficult_points`');
CALL add_column_if_missing('teaching_plan','visibility',"`visibility` VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' AFTER `total_minutes`");

CREATE TABLE IF NOT EXISTS plan_package (
    plan_id BIGINT UNSIGNED NOT NULL,
    package_id BIGINT UNSIGNED NOT NULL,
    use_stage VARCHAR(30) NOT NULL DEFAULT 'IN_CLASS',
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (plan_id, package_id),
    KEY idx_plan_package_order (plan_id, sort_order),
    CONSTRAINT fk_plan_package_plan FOREIGN KEY (plan_id) REFERENCES teaching_plan(id),
    CONSTRAINT fk_plan_package_package FOREIGN KEY (package_id) REFERENCES resource_package(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Packages used by lesson plans';

INSERT IGNORE INTO plan_package (plan_id, package_id, use_stage, sort_order)
SELECT id, package_id, 'IN_CLASS', 1 FROM teaching_plan WHERE package_id IS NOT NULL;

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

CREATE TABLE IF NOT EXISTS point_rule (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(50) NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    points_value INT NOT NULL,
    daily_limit INT UNSIGNED,
    enabled TINYINT NOT NULL DEFAULT 1,
    description VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_point_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Configurable point rules';

CREATE TABLE IF NOT EXISTS daily_checkin (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    checkin_date DATE NOT NULL,
    reward_points INT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_daily_checkin (user_id, checkin_date),
    CONSTRAINT fk_daily_checkin_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Daily check-ins';

CREATE TABLE IF NOT EXISTS point_log (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    rule_id BIGINT UNSIGNED,
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
    CONSTRAINT fk_point_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_point_log_rule FOREIGN KEY (rule_id) REFERENCES point_rule(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Immutable point ledger';

CREATE TABLE IF NOT EXISTS case_unlock (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    case_id BIGINT UNSIGNED NOT NULL,
    cost_points INT UNSIGNED NOT NULL,
    author_income INT UNSIGNED NOT NULL DEFAULT 0,
    unlocked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_case_unlock (user_id, case_id),
    KEY idx_case_unlock_case (case_id),
    CONSTRAINT fk_case_unlock_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_case_unlock_case FOREIGN KEY (case_id) REFERENCES teaching_case(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Point-paid case access';

CALL add_column_if_missing('ai_generation_task','package_id','`package_id` BIGINT UNSIGNED NULL AFTER `course_id`');
CALL add_column_if_missing('ai_generation_task','plan_id','`plan_id` BIGINT UNSIGNED NULL AFTER `package_id`');
CALL add_column_if_missing('ai_generation_task','requirement_text','`requirement_text` TEXT NULL AFTER `task_type`');

-- Backfill V1 tasks before enforcing the exclusive target constraint.
UPDATE ai_generation_task
SET package_id=1,
    requirement_text='为Spring Boot微服务实践推荐适合课中使用的资源与案例'
WHERE id=1 AND package_id IS NULL AND plan_id IS NULL;

SET @has_ai_package_fk = (SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='ai_generation_task' AND constraint_name='fk_ai_package');
SET @sql = IF(@has_ai_package_fk=0,'ALTER TABLE ai_generation_task ADD CONSTRAINT fk_ai_package FOREIGN KEY (package_id) REFERENCES resource_package(id)','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_ai_plan_fk = (SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='ai_generation_task' AND constraint_name='fk_ai_plan');
SET @sql = IF(@has_ai_plan_fk=0,'ALTER TABLE ai_generation_task ADD CONSTRAINT fk_ai_plan FOREIGN KEY (plan_id) REFERENCES teaching_plan(id)','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_ai_target_ck = (SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='ai_generation_task' AND constraint_name='ck_ai_task_target');
SET @sql = IF(@has_ai_target_ck=0,'ALTER TABLE ai_generation_task ADD CONSTRAINT ck_ai_task_target CHECK ((package_id IS NULL) <> (plan_id IS NULL))','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS ai_recommendation (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT UNSIGNED NOT NULL,
    resource_id BIGINT UNSIGNED,
    case_id BIGINT UNSIGNED,
    relevance_score DECIMAL(5,4) NOT NULL,
    use_stage VARCHAR(30),
    recommendation_reason TEXT,
    use_suggestion TEXT,
    selected TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_ai_recommendation_task_score (task_id, relevance_score),
    CONSTRAINT fk_ai_recommendation_task FOREIGN KEY (task_id) REFERENCES ai_generation_task(id),
    CONSTRAINT fk_ai_recommendation_resource FOREIGN KEY (resource_id) REFERENCES teaching_resource(id),
    CONSTRAINT fk_ai_recommendation_case FOREIGN KEY (case_id) REFERENCES teaching_case(id),
    CONSTRAINT ck_ai_recommendation_target CHECK ((resource_id IS NULL) <> (case_id IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI candidate recommendations confirmed by teachers';

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
  points_price = CASE WHEN id = 4 THEN 20 ELSE 0 END;

INSERT IGNORE INTO case_resource (case_id, resource_id, sort_order, use_description) VALUES
  (3,4,1,'课堂分组练习材料'),(4,5,1,'并发实验配套指导');

INSERT IGNORE INTO case_review (id,case_id,reviewer_id,submission_no,review_result,review_comment) VALUES
  (1,1,2,1,'APPROVED','案例结构完整，同意公开。'),(2,4,2,1,'APPROVED','问题典型，建议标记为优秀案例。');
INSERT IGNORE INTO resource_review (id,resource_id,reviewer_id,submission_no,review_result,review_comment) VALUES
  (1,1,2,1,'APPROVED','课件内容完整。'),(2,5,2,1,'REJECTED','视频或实验资料需要补充字幕说明。');

INSERT INTO point_rule (id,rule_code,rule_name,points_value,daily_limit,enabled,description) VALUES
  (1,'DAILY_CHECKIN','每日签到',5,1,1,'教师每日首次签到奖励'),
  (2,'CASE_APPROVED','公共案例审核通过',25,NULL,1,'案例公开并审核通过后奖励作者'),
  (3,'CORRECTION_ACCEPTED','有效纠错',10,NULL,1,'纠错建议被采纳后奖励'),
  (4,'CASE_UNLOCK','积分案例解锁',-20,NULL,1,'首次解锁积分案例扣除')
ON DUPLICATE KEY UPDATE rule_name=VALUES(rule_name),points_value=VALUES(points_value),daily_limit=VALUES(daily_limit),enabled=VALUES(enabled);

INSERT IGNORE INTO point_account (user_id,balance,total_income,total_expense,version) VALUES
  (1,80,100,20,0),(2,125,125,0,0),(3,0,0,0,0)
;

INSERT IGNORE INTO daily_checkin (id,user_id,checkin_date,reward_points) VALUES (1,1,CURRENT_DATE,5);
INSERT IGNORE INTO point_log (id,user_id,rule_id,business_type,business_id,change_amount,balance_after,remark) VALUES
  (1,1,1,'DAILY_CHECKIN',DATE_FORMAT(CURRENT_DATE,'%Y%m%d'),5,80,'每日签到奖励'),
  (2,1,2,'CASE_APPROVED',4,25,100,'优秀公共案例审核通过'),
  (3,1,4,'CASE_UNLOCK',2,-20,80,'解锁积分案例');
INSERT IGNORE INTO case_unlock (id,user_id,case_id,cost_points,author_income) VALUES (1,1,2,20,16);

INSERT IGNORE INTO ai_recommendation (id,task_id,resource_id,case_id,relevance_score,use_stage,recommendation_reason,use_suggestion,selected) VALUES
  (1,1,4,NULL,0.9400,'IN_CLASS','与设计模式和工程实践知识点高度相关','用于课堂分组练习',1),
  (2,1,NULL,1,0.9100,'INTRODUCTION','案例背景适合作为微服务课程导入','课前阅读并在课堂讨论',0);

INSERT IGNORE INTO resource_favorite (user_id,resource_id) VALUES (1,3);
INSERT IGNORE INTO case_favorite (user_id,case_id) VALUES (1,1);

-- resource_package_item and teaching_plan.package_id are retained only for V1 compatibility.
-- New code should use package_resource/package_case and plan_package.
DROP PROCEDURE IF EXISTS add_column_if_missing;
