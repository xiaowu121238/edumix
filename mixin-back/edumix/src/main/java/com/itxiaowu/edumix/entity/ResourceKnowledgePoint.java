package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 资源知识点关联实体，建立教学资源与课程知识点之间的多对多关系。 */
@Data
@TableName("resource_knowledge_point")
public class ResourceKnowledgePoint {
  private Long resourceId;
  private Long knowledgePointId;
  private BigDecimal relevanceScore;
  private String source;
  private LocalDateTime createTime;
}
