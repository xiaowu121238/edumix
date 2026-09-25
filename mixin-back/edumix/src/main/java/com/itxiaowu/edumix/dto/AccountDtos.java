package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 账号管理相关请求和响应对象的集合。 */
public final class AccountDtos {
  private AccountDtos() {}

  public record RegisterRequest(
      @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 50, message = "用户名须为3到50个字符")
          String username,
      @NotBlank(message = "密码不能为空") @Size(min = 8, max = 72, message = "密码须为8到72个字符")
          String password,
      @NotBlank(message = "姓名不能为空") @Size(max = 50, message = "姓名不能超过50个字符") String realName,
      String email,
      String college) {}

  public record CreateUserRequest(
      @NotBlank String username,
      @NotBlank String password,
      @NotBlank String realName,
      String email,
      String college,
      String title,
      @NotBlank String accountType) {}

  public record RolesRequest(List<String> roles) {}

  public record StatusRequest(Integer status) {}

  public record ResetPasswordRequest(@NotBlank String password) {}

  public record Account(
      Long id,
      String username,
      String realName,
      String email,
      String college,
      String title,
      Integer status,
      List<String> roles) {}
}
