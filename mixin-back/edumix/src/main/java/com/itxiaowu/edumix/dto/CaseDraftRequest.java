package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

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
