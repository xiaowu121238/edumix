package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 案例收藏实体；积分案例只有解锁后才允许创建收藏记录。 */
@Data
@TableName("case_favorite")
public class CaseFavorite {
  private Long userId;
  private Long caseId;
  private LocalDateTime createTime;
}
