package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 统一内容审核记录。
 *
 * <p>资源和案例共用一张审核表，通过 {@code targetType} 与 {@code targetId} 定位被审核内容，避免两套结构完全相同的审核表。
 */
@Data
@TableName("content_review")
public class ContentReview {
  @TableId private Long id;
  private String targetType;
  private Long targetId;
  private Long reviewerId;
  private Integer submissionNo;
  private String reviewResult;
  private String reviewComment;
  private LocalDateTime reviewedAt;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic private Integer deleted;
}
