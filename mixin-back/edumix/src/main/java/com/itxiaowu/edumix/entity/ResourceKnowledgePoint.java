package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("resource_knowledge_point")
public class ResourceKnowledgePoint {
  private Long resourceId;
  private Long knowledgePointId;
  private BigDecimal relevanceScore;
  private String source;
  private LocalDateTime createTime;
}
