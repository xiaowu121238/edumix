package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "姓名不能为空") @Size(max = 50, message = "姓名不能超过50个字符") String realName,
    @Email(message = "邮箱格式不正确") @Size(max = 100, message = "邮箱不能超过100个字符") String email,
    @Size(max = 20, message = "手机号不能超过20个字符") String phone,
    @Size(max = 500, message = "头像地址不能超过500个字符") String avatarUrl,
    @Size(max = 100, message = "学院不能超过100个字符") String college,
    @Size(max = 50, message = "职称不能超过50个字符") String title) {}
