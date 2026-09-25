package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 每日签到实体，通过用户与日期唯一约束避免重复领取积分。 */
@Data
@TableName("daily_checkin")
public class DailyCheckin {
  @TableId private Long id;
  private Long userId;
  private LocalDate checkinDate;
  private Integer rewardPoints;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic private Integer deleted;
}
