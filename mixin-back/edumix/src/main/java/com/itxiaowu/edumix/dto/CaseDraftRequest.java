package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

/** 本地案例草稿保存请求，允许内容未完整时分阶段保存。 */
public record CaseDraftRequest(
    @Size(max = 200, message = "案例标题不能超过200字") String caseName,
    Long courseId,
    List<Long> knowledgePointIds,
    @Size(max = 1000, message = "摘要不能超过1000字") String summary,
    String content,
    List<Long> resourceIds,
    String visibility,
    String accessType,
    Integer pointsPrice) {}
