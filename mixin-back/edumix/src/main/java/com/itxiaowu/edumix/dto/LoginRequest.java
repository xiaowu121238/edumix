package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.NotBlank;

/** 用户名和密码登录请求。 */
public record LoginRequest(
    @NotBlank(message = "请输入账号") String username, @NotBlank(message = "请输入密码") String password) {}
