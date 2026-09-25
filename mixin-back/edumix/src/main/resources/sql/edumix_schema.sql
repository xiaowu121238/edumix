-- EduMix database schema (MySQL 8+/9+)
-- Safe for repeated execution: existing tables are preserved.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    college VARCHAR(100),
    title VARCHAR(50),
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0 disabled, 1 enabled',
    last_login_time DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email),
    KEY idx_user_college_status (college, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Platform users';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Roles';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User role mappings';

CREATE TABLE IF NOT EXISTS course (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    course_code VARCHAR(50) NOT NULL,
    course_name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    knowledge_objective TEXT COMMENT 'Knowledge objectives',
    ability_objective TEXT COMMENT 'Ability objectives',
    quality_objective TEXT COMMENT 'Quality objectives',
    cover_url VARCHAR(500),
    college VARCHAR(100),
    semester VARCHAR(50),
    owner_id BIGINT UNSIGNED NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0 draft, 1 active, 2 archived',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_code (course_code),
    KEY idx_course_owner_status (owner_id, status),
    KEY idx_course_name (course_name),
    CONSTRAINT fk_course_owner FOREIGN KEY (owner_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Courses';

CREATE TABLE IF NOT EXISTS course_teacher (
    course_id BIGINT UNSIGNED NOT NULL,
    teacher_id BIGINT UNSIGNED NOT NULL,
    member_role VARCHAR(30) NOT NULL DEFAULT 'COLLABORATOR',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (course_id, teacher_id),
    CONSTRAINT fk_course_teacher_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_course_teacher_user FOREIGN KEY (teacher_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Course collaborators';

CREATE TABLE IF NOT EXISTS course_chapter (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT UNSIGNED NOT NULL,
    parent_id BIGINT UNSIGNED,
    chapter_name VARCHAR(200) NOT NULL,
    chapter_no VARCHAR(30),
    description VARCHAR(1000),
    sort_order INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_chapter_course_sort (course_id, sort_order),
    KEY idx_chapter_parent (parent_id),
    CONSTRAINT fk_chapter_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_chapter_parent FOREIGN KEY (parent_id) REFERENCES course_chapter(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Course chapters';

CREATE TABLE IF NOT EXISTS knowledge_point (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT UNSIGNED NOT NULL,
    chapter_id BIGINT UNSIGNED NOT NULL,
    point_name VARCHAR(200) NOT NULL,
    point_code VARCHAR(50),
    description TEXT,
    difficulty TINYINT NOT NULL DEFAULT 2 COMMENT '1 easy, 2 medium, 3 hard',
    teaching_hours DECIMAL(5,2) DEFAULT 1.00,
    sort_order INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_point_course_code (course_id, point_code),
    KEY idx_point_chapter_sort (chapter_id, sort_order),
    KEY idx_point_name (point_name),
    CONSTRAINT fk_point_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_point_chapter FOREIGN KEY (chapter_id) REFERENCES course_chapter(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Knowledge points';

CREATE TABLE IF NOT EXISTS teaching_resource (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    resource_no VARCHAR(50) NOT NULL,
    resource_name VARCHAR(200) NOT NULL,
    resource_type VARCHAR(30) NOT NULL COMMENT 'DOCUMENT VIDEO AUDIO IMAGE LINK COURSEWARE',
    description VARCHAR(1000),
    file_url VARCHAR(1000),
    cover_url VARCHAR(500),
    file_size BIGINT UNSIGNED DEFAULT 0,
    duration_seconds INT UNSIGNED,
    uploader_id BIGINT UNSIGNED NOT NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE PUBLIC',
    audit_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING APPROVED REJECTED',
    download_count INT UNSIGNED NOT NULL DEFAULT 0,
    reference_count INT UNSIGNED NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_resource_no (resource_no),
    KEY idx_resource_type_audit (resource_type, audit_status),
    KEY idx_resource_uploader (uploader_id),
    KEY idx_resource_name (resource_name),
    CONSTRAINT fk_resource_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Teaching resources';

CREATE TABLE IF NOT EXISTS resource_knowledge_point (
    resource_id BIGINT UNSIGNED NOT NULL,
    knowledge_point_id BIGINT UNSIGNED NOT NULL,
    relevance_score DECIMAL(5,4) DEFAULT 1.0000,
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL AI',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (resource_id, knowledge_point_id),
    KEY idx_rkp_point (knowledge_point_id),
    CONSTRAINT fk_rkp_resource FOREIGN KEY (resource_id) REFERENCES teaching_resource(id),
    CONSTRAINT fk_rkp_point FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Resource knowledge point mappings';

CREATE TABLE IF NOT EXISTS teaching_case (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    case_no VARCHAR(50) NOT NULL,
    case_name VARCHAR(200) NOT NULL,
    summary VARCHAR(1000),
    teaching_objective TEXT,
    key_points TEXT,
    difficult_points TEXT,
    teaching_process TEXT,
    teaching_reflection TEXT,
    content LONGTEXT,
    subject VARCHAR(100),
    cover_url VARCHAR(500),
    creator_id BIGINT UNSIGNED NOT NULL,
    course_id BIGINT UNSIGNED,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    access_type VARCHAR(20) NOT NULL DEFAULT 'FREE',
    points_price INT UNSIGNED NOT NULL DEFAULT 0,
    audit_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    excellent TINYINT NOT NULL DEFAULT 0,
    reference_count INT UNSIGNED NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_case_no (case_no),
    KEY idx_case_subject_audit (subject, audit_status),
    KEY idx_case_creator (creator_id),
    KEY idx_case_name (case_name),
    CONSTRAINT fk_case_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id),
    CONSTRAINT fk_case_course FOREIGN KEY (course_id) REFERENCES course(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Teaching cases';

CREATE TABLE IF NOT EXISTS case_knowledge_point (
    case_id BIGINT UNSIGNED NOT NULL,
    knowledge_point_id BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (case_id, knowledge_point_id),
    KEY idx_ckp_point (knowledge_point_id),
    CONSTRAINT fk_ckp_case FOREIGN KEY (case_id) REFERENCES teaching_case(id),
    CONSTRAINT fk_ckp_point FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Case knowledge point mappings';

CREATE TABLE IF NOT EXISTS resource_package (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    package_no VARCHAR(50) NOT NULL,
    package_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    course_id BIGINT UNSIGNED,
    creator_id BIGINT UNSIGNED NOT NULL,
    generation_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL AI',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT PUBLISHED ARCHIVED',
    progress TINYINT UNSIGNED NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_package_no (package_no),
    KEY idx_package_course (course_id),
    KEY idx_package_creator_status (creator_id, status),
    CONSTRAINT fk_package_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_package_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Resource packages';

CREATE TABLE IF NOT EXISTS resource_package_item (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    package_id BIGINT UNSIGNED NOT NULL,
    item_type VARCHAR(20) NOT NULL COMMENT 'RESOURCE CASE',
    item_id BIGINT UNSIGNED NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    note VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_package_item (package_id, item_type, item_id),
    KEY idx_package_item_order (package_id, sort_order),
    CONSTRAINT fk_package_item_package FOREIGN KEY (package_id) REFERENCES resource_package(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Items in resource packages';

CREATE TABLE IF NOT EXISTS teaching_plan (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    plan_no VARCHAR(50) NOT NULL,
    plan_name VARCHAR(200) NOT NULL,
    course_id BIGINT UNSIGNED NOT NULL,
    chapter_id BIGINT UNSIGNED,
    package_id BIGINT UNSIGNED,
    creator_id BIGINT UNSIGNED NOT NULL,
    teaching_objectives TEXT,
    key_points TEXT,
    difficult_points TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT REVIEW PUBLISHED',
    progress TINYINT UNSIGNED NOT NULL DEFAULT 0,
    version INT UNSIGNED NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_plan_no (plan_no),
    KEY idx_plan_course_status (course_id, status),
    KEY idx_plan_creator (creator_id),
    CONSTRAINT fk_plan_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_plan_chapter FOREIGN KEY (chapter_id) REFERENCES course_chapter(id),
    CONSTRAINT fk_plan_package FOREIGN KEY (package_id) REFERENCES resource_package(id),
    CONSTRAINT fk_plan_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Teaching plans';

CREATE TABLE IF NOT EXISTS teaching_plan_item (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT UNSIGNED NOT NULL,
    stage_name VARCHAR(100) NOT NULL,
    stage_type VARCHAR(30) NOT NULL COMMENT 'INTRODUCTION TEACHING PRACTICE SUMMARY HOMEWORK',
    duration_minutes INT UNSIGNED NOT NULL DEFAULT 0,
    content TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_plan_item_order (plan_id, sort_order),
    CONSTRAINT fk_plan_item_plan FOREIGN KEY (plan_id) REFERENCES teaching_plan(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Teaching plan stages';

CREATE TABLE IF NOT EXISTS ai_generation_task (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    task_no VARCHAR(50) NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    course_id BIGINT UNSIGNED,
    task_type VARCHAR(30) NOT NULL COMMENT 'RESOURCE_MATCH PACKAGE PLAN KNOWLEDGE_EXTRACT',
    input_json JSON,
    result_json JSON,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING RUNNING SUCCESS FAILED',
    error_message VARCHAR(1000),
    started_time DATETIME,
    finished_time DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ai_task_no (task_no),
    KEY idx_ai_user_status (user_id, status),
    CONSTRAINT fk_ai_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_ai_course FOREIGN KEY (course_id) REFERENCES course(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI generation tasks';

CREATE TABLE IF NOT EXISTS message (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    message_type VARCHAR(30) NOT NULL COMMENT 'SYSTEM COLLAB AUDIT',
    title VARCHAR(255) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    sender_id BIGINT UNSIGNED,
    business_type VARCHAR(30),
    business_id BIGINT UNSIGNED,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_message_type_time (message_type, create_time),
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Message content';

CREATE TABLE IF NOT EXISTS user_message (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    message_id BIGINT UNSIGNED NOT NULL,
    read_status TINYINT NOT NULL DEFAULT 0,
    read_time DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_message (user_id, message_id),
    KEY idx_user_message_status (user_id, read_status, create_time),
    CONSTRAINT fk_user_message_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_user_message_message FOREIGN KEY (message_id) REFERENCES message(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User inbox';

SET FOREIGN_KEY_CHECKS = 1;
