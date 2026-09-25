package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 方案资源关联实体，为授课方案绑定所需资源包或独立资源。 */
@Data
@TableName("package_resource")
public class PackageResource {
  private Long packageId;
  private Long resourceId;
  private Integer sortOrder;
  private String note;
  private LocalDateTime createTime;
}
