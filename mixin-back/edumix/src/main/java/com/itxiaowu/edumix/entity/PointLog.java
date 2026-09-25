package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("point_log")
public class PointLog {
  @TableId private Long id;
  private Long userId;
  private Long ruleId;
  private String businessType;
  private Long businessId;
  private Integer changeAmount;
  private Integer balanceAfter;
  private String remark;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic private Integer deleted;
}
