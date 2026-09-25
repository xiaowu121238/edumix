package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("resource_favorite")
public class ResourceFavorite {
  private Long userId;
  private Long resourceId;
  private LocalDateTime createTime;
}
