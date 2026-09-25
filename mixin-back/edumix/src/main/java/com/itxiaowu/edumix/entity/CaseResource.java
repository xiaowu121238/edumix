package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 案例资源关联实体，记录富文本案例引用的图片、视频和其他教学资源。 */
@Data
@TableName("case_resource")
public class CaseResource {
  private Long caseId;
  private Long resourceId;
  private Integer sortOrder;
  private String useDescription;
  private LocalDateTime createTime;
}
