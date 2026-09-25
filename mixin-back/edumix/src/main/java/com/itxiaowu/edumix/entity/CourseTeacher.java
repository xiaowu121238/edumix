package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("course_teacher")
public class CourseTeacher {
  private Long courseId;
  private Long teacherId;
  private String memberRole;
  private LocalDateTime createTime;
}
