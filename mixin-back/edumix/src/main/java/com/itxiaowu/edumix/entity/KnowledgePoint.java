package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("knowledge_point")
public class KnowledgePoint {
  @TableId private Long id;
  private Long courseId;
  private Long chapterId;
  private String pointName;
  private String pointCode;
  private String description;
  private Integer difficulty;
  private BigDecimal teachingHours;
  private Integer sortOrder;
  @TableLogic private Integer deleted;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
