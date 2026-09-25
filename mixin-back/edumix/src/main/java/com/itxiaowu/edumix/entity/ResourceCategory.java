package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 资源分类实体，用于资源库筛选和同类内容推荐。 */
@Data
@TableName("resource_category")
public class ResourceCategory {
  @TableId private Long id;
  private Long parentId;
  private String categoryName;
  private String categoryCode;
  private Integer sortOrder;
  private Integer status;
  @TableLogic private Integer deleted;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
