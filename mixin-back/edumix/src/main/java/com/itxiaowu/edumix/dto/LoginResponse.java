package com.itxiaowu.edumix.dto;

/** 登录成功响应，返回 JWT、有效期和当前用户信息。 */
public record LoginResponse(
    String token, String tokenType, long expiresIn, UserProfileResponse user) {}
