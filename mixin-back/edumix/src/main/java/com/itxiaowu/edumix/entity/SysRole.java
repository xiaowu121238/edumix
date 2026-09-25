package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 系统角色实体，定义教师、资源负责人和管理员等权限身份。 */
@Data
@TableName("sys_role")
public class SysRole {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String roleCode;
  private String roleName;
  private String description;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
