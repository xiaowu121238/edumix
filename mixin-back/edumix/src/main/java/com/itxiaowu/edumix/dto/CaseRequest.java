package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.*;
import java.util.List;

/** 教学案例创建或正式发布请求。 */
public record CaseRequest(
    @NotBlank(message = "案例名称不能为空") @Size(max = 200, message = "案例名称不能超过200个字符") String caseName,
    @NotNull(message = "请选择所属课程") Long courseId,
    @NotEmpty(message = "请至少选择一个知识点") List<Long> knowledgePointIds,
    String teachingObjective,
    String keyPoints,
    String difficultPoints,
    String teachingProcess,
    String teachingReflection,
    @Size(max = 1000, message = "案例摘要不能超过1000个字符") String summary,
    @NotBlank(message = "帖子正文不能为空") String content,
    List<Long> resourceIds,
    String visibility,
    String accessType,
    @Min(value = 0, message = "积分价格不能为负数") Integer pointsPrice) {}
