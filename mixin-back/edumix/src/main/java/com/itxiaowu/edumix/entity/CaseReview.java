package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 案例审核记录实体，保留负责人每次通过或驳回的处理信息。 */
@Data
@TableName("case_review")
public class CaseReview {
  @TableId private Long id;
  private Long caseId;
  private Long reviewerId;
  private Integer submissionNo;
  private String reviewResult;
  private String reviewComment;
  private LocalDateTime reviewedAt;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic private Integer deleted;
}
