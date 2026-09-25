package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

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
