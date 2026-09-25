package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 负责人审核教学案例时提交的结果和审核意见。 */
public record CaseReviewRequest(
    @NotBlank(message = "请选择审核结果") @Pattern(regexp = "APPROVED|REJECTED", message = "审核结果不合法")
        String result,
    String comment,
    Boolean excellent) {}
