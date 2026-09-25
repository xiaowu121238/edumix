USE edumix;

SET @has_key_points = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='teaching_case' AND column_name='key_points');
SET @sql = IF(@has_key_points=0, 'ALTER TABLE teaching_case ADD COLUMN key_points TEXT NULL AFTER teaching_objective', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_difficult_points = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='teaching_case' AND column_name='difficult_points');
SET @sql = IF(@has_difficult_points=0, 'ALTER TABLE teaching_case ADD COLUMN difficult_points TEXT NULL AFTER key_points', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
