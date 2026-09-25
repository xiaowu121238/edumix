package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** AI 推荐记录实体，保存推荐对象、推荐理由和用户反馈。 */
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
