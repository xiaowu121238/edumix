package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 授课方案条目实体，记录每个教学环节的内容、时长和排序。 */
@Data
@TableName("teaching_plan_item")
public class TeachingPlanItem {
  @TableId private Long id;
  private Long planId;
  private String stageName;
  private String stageType;
  private Integer durationMinutes;
  private String content;
  private Integer sortOrder;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
