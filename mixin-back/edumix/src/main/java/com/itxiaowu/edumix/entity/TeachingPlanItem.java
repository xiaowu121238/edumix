package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

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
