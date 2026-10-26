package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 积分流水实体，记录每次收入或支出的业务类型、数量和关联对象。 */
@Data
@TableName("point_log")
public class PointLog {
  @TableId private Long id;
  private Long userId;
  private Long operatorId;
  private String businessType;
  private Long businessId;
  private Integer changeAmount;
  private Integer balanceAfter;
  private String remark;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic private Integer deleted;
}
