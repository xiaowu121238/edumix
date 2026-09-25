package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 管理员发布全站系统通知的请求。 */
public record SystemNoticeRequest(
    @NotBlank(message = "请输入公告标题") @Size(max = 255, message = "标题不能超过255字") String title,
    @NotBlank(message = "请输入公告内容") @Size(max = 2000, message = "内容不能超过2000字") String content) {}
