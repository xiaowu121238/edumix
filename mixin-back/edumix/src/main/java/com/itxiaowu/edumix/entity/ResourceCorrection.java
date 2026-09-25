package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("resource_correction")
public class ResourceCorrection {
  @TableId private Long id;
  private Long resourceId;
  private Long submitterId;
  private Long handlerId;
  private String correctionContent;
  private String status;
  private String handleComment;
  private LocalDateTime handledAt;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic private Integer deleted;
}
