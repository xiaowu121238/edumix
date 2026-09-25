package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 资源审核记录实体，保留每轮审核结果、意见、负责人和审核时间。 */
@Data
@TableName("resource_review")
public class ResourceReview {
  @TableId private Long id;
  private Long resourceId;
  private Long reviewerId;
  private Integer submissionNo;
  private String reviewResult;
  private String reviewComment;
  private LocalDateTime reviewedAt;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic private Integer deleted;
}
