package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

/** 用户积分账户实体，保存当前可用积分余额。 */
@Data
@TableName("point_account")
public class PointAccount {
  @TableId private Long userId;
  private Integer balance;
  private Integer totalIncome;
  private Integer totalExpense;
  @Version private Integer version;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
