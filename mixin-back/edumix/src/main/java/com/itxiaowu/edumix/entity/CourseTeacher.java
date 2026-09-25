package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 课程教师关联实体，记录课程的创建者和参与教师。 */
@Data
@TableName("course_teacher")
public class CourseTeacher {
  private Long courseId;
  private Long teacherId;
  private String memberRole;
  private LocalDateTime createTime;
}
