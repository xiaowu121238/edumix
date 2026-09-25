package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 当前用户修改密码请求，必须同时校验原密码。 */
public record ChangePasswordRequest(
    @NotBlank(message = "请输入原密码") String oldPassword,
    @NotBlank(message = "请输入新密码") @Size(min = 8, max = 72, message = "新密码须为8到72个字符")
        String newPassword) {}
