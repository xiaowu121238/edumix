package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("package_case")
public class PackageCase {
  private Long packageId;
  private Long caseId;
  private Integer sortOrder;
  private String note;
  private LocalDateTime createTime;
}
