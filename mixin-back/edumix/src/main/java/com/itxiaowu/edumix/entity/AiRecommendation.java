package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("ai_recommendation")
public class AiRecommendation {
  @TableId private Long id;
  private Long taskId;
  private Long resourceId;
  private Long caseId;
  private BigDecimal relevanceScore;
  private String useStage;
  private String recommendationReason;
  private String useSuggestion;
  private Integer selected;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic private Integer deleted;
}
