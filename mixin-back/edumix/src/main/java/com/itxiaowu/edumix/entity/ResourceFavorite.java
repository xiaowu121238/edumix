package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 资源收藏实体，记录用户收藏的已审核资源。 */
@Data
@TableName("resource_favorite")
public class ResourceFavorite {
  private Long userId;
  private Long resourceId;
  private LocalDateTime createTime;
}
