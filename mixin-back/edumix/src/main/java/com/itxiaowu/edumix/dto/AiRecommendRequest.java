package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

/** AI 推荐请求，可使用关键词和用户选中的上下文缩小公共库候选范围。 */
public record AiRecommendRequest(
    Long courseId,
    List<Long> resourceIds,
    List<Long> caseIds,
    @Size(max = 1000, message = "推荐需求不能超过1000字") String requirement,
    Integer limit) {}
