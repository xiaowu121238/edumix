package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("case_knowledge_point")
public class CaseKnowledgePoint {
  private Long caseId;
  private Long knowledgePointId;
  private LocalDateTime createTime;
}
