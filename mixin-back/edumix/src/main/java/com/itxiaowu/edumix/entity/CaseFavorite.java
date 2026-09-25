package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("case_favorite")
public class CaseFavorite {
  private Long userId;
  private Long caseId;
  private LocalDateTime createTime;
}
