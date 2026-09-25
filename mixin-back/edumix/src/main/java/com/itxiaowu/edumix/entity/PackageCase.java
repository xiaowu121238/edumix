package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 资源包案例关联实体，为资源包绑定可参考的教学案例。 */
@Data
@TableName("package_case")
public class PackageCase {
  private Long packageId;
  private Long caseId;
  private Integer sortOrder;
  private String note;
  private LocalDateTime createTime;
}
