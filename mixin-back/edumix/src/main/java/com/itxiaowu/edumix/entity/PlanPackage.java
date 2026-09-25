package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 授课方案资源包关联实体，记录方案采用的资源包及排列顺序。 */
@Data
@TableName("plan_package")
public class PlanPackage {
  private Long planId;
  private Long packageId;
  private String useStage;
  private Integer sortOrder;
  private LocalDateTime createTime;
}
