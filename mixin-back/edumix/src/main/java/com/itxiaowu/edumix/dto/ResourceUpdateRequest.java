package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResourceUpdateRequest(
    @NotBlank(message = "资源名称不能为空") @Size(max = 200, message = "资源名称不能超过200个字符")
        String resourceName,
    @Size(max = 1000, message = "资源简介不能超过1000个字符") String description,
    Long categoryId,
    String visibility) {}
