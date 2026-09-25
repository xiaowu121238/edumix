package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户发起资源纠错时提交的问题描述。 */
public record CorrectionRequest(
    @NotBlank(message = "纠错内容不能为空") @Size(min = 20, max = 2000, message = "纠错内容须为20至2000个字符")
        String content) {}
