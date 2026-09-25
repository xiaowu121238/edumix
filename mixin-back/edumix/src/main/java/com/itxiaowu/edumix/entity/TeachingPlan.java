package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

/** 授课方案实体，描述一门课程的教学安排、课时结构和发布状态。 */
@Data
@TableName("teaching_plan")
public class TeachingPlan {
  @TableId private Long id;
  private String planNo;
  private String planName;
  private Long courseId;
  private Long chapterId;
  private Long packageId;
  private Long creatorId;
  private String teachingObjectives;
  private String teachingMethod;
  private String keyPoints;
  private String difficultPoints;
  private Integer totalMinutes;
  private String visibility;
  private String status;
  private Integer progress;
  @Version private Integer version;
  @TableLogic private Integer deleted;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
