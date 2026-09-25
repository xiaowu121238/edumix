package com.itxiaowu.edumix.dto;

public record LoginResponse(
    String token, String tokenType, long expiresIn, UserProfileResponse user) {}
