package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("plan_package")
public class PlanPackage {
  private Long planId;
  private Long packageId;
  private String useStage;
  private Integer sortOrder;
  private LocalDateTime createTime;
}
