package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("case_unlock")
public class CaseUnlock {
  @TableId private Long id;
  private Long userId;
  private Long caseId;
  private Integer costPoints;
  private Integer authorIncome;
  private LocalDateTime unlockedAt;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic private Integer deleted;
}
