package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgePointRequest(
    @NotBlank(message = "知识点名称不能为空") @Size(max = 200, message = "知识点名称不能超过200个字符") String pointName,
    @Size(max = 50, message = "知识点编码不能超过50个字符") String pointCode,
    String description,
    @Min(value = 1, message = "难度只能是1至3") @Max(value = 3, message = "难度只能是1至3") Integer difficulty,
    @Min(value = 1, message = "建议课时必须是大于等于1的整数") Integer teachingHours,
    @Min(value = 0, message = "知识点排序不能为负数") Integer sortOrder) {}
