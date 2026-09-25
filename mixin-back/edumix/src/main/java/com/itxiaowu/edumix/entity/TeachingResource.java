package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 教学资源实体，保存文件元数据、上传者、可见范围及审核状态。 */
@Data
@TableName("teaching_resource")
public class TeachingResource {
  @TableId private Long id;
  private String resourceNo;
  private String resourceName;
  private String resourceType;
  private String description;
  private String fileUrl;
  private String coverUrl;
  private Long fileSize;
  private Integer durationSeconds;
  private Long uploaderId;
  private Long categoryId;
  private String visibility;
  private String auditStatus;
  private Integer downloadCount;
  private Integer referenceCount;
  @TableLogic private Integer deleted;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
