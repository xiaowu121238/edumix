package com.itxiaowu.edumix.dto;

import java.time.LocalDateTime;

/** 资源单次审核历史响应。 */
public record ResourceReviewHistoryResponse(
    Integer submissionNo,
    String result,
    String comment,
    String reviewerName,
    LocalDateTime reviewedAt) {}
