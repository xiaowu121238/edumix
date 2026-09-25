package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("package_resource")
public class PackageResource {
  private Long packageId;
  private Long resourceId;
  private Integer sortOrder;
  private String note;
  private LocalDateTime createTime;
}
