package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 课程章节实体，用于组成课程下的有序章节结构。 */
@Data
@TableName("course_chapter")
public class CourseChapter {
  @TableId private Long id;
  private Long courseId;
  private Long parentId;
  private String chapterName;
  private String chapterNo;
  private String description;
  private Integer sortOrder;
  @TableLogic private Integer deleted;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
