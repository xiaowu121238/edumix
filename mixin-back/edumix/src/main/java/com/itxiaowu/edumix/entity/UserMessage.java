package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 站内通知实体，保存系统公告、审核结果、纠错进度等用户消息。 */
@Data
@TableName("user_message")
public class UserMessage {
  @TableId private Long id;
  private Long userId;
  private Long messageId;
  private Integer readStatus;
  private LocalDateTime readTime;
  @TableLogic private Integer deleted;
  private LocalDateTime createTime;
}
