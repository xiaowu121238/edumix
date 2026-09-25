package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 积分规则实体，配置注册、签到、发布案例等行为对应的积分值。 */
@Data
@TableName("point_rule")
public class PointRule {
  @TableId private Long id;
  private String ruleCode;
  private String ruleName;
  private Integer pointsValue;
  private Integer dailyLimit;
  private Integer enabled;
  private String description;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic private Integer deleted;
}
