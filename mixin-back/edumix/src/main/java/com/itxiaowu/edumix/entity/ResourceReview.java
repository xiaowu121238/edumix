package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

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
