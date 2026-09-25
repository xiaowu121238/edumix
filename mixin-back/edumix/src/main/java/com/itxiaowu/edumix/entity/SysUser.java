package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.Data;

/** 系统用户实体，保存登录账号、个人资料、启用状态及最近登录时间。 */
@Data
@TableName("sys_user")
public class SysUser {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String username;
  @JsonIgnore private String password;
  private String realName;
  private String email;
  private String phone;
  private String avatarUrl;
  private String college;
  private String title;
  private Integer status;
  private LocalDateTime lastLoginTime;
  @TableLogic private Integer deleted;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
