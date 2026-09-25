package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 资源包条目实体，记录资源在资源包中的顺序和使用说明。 */
@Data
@TableName("resource_package_item")
public class ResourcePackageItem {
  @TableId private Long id;
  private Long packageId;
  private String itemType;
  private Long itemId;
  private Integer sortOrder;
  private String note;
  private LocalDateTime createTime;
}
