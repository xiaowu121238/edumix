package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 资源包实体，用于把多个教学资源组织为可复用的备课集合。 */
@Data
@TableName("resource_package")
public class ResourcePackage {
  @TableId private Long id;
  private String packageNo;
  private String packageName;
  private String description;
  private Long courseId;
  private Long creatorId;
  private String visibility;
  private String generationType;
  private String status;
  private Integer progress;
  @TableLogic private Integer deleted;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
