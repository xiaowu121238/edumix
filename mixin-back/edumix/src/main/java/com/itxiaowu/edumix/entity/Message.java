package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 用户私信实体，保存发送者、接收者、文本内容和已读时间。 */
@Data
@TableName("message")
public class Message {
  @TableId private Long id;
  private String messageType;
  private String title;
  private String content;
  private Long senderId;
  private String businessType;
  private Long businessId;
  private LocalDateTime createTime;
}
