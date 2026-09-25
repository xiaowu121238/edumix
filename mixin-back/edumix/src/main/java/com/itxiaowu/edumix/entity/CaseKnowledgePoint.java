package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 案例知识点关联实体，用于为案例绑定一个或多个知识点标签。 */
@Data
@TableName("case_knowledge_point")
public class CaseKnowledgePoint {
  private Long caseId;
  private Long knowledgePointId;
  private LocalDateTime createTime;
}
