package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 负责人或上传者处理资源纠错时提交的处理状态与说明。 */
public record CorrectionHandleRequest(
    @NotBlank(message = "请选择处理结果") @Pattern(regexp = "ACCEPTED|REJECTED", message = "纠错处理结果不合法")
        String result,
    @NotBlank(message = "请填写处理说明") @Size(max = 1000, message = "处理说明不能超过1000个字符") String comment) {}
