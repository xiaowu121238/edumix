package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

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
