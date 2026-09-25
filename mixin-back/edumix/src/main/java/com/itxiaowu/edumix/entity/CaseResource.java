package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("case_resource")
public class CaseResource {
  private Long caseId;
  private Long resourceId;
  private Integer sortOrder;
  private String useDescription;
  private LocalDateTime createTime;
}
