package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 用户角色关联实体，支持一个账号同时拥有教师和资源负责人等多个角色。 */
@Data
@TableName("sys_user_role")
public class SysUserRole {
  private Long userId;
  private Long roleId;
  private LocalDateTime createTime;
}
