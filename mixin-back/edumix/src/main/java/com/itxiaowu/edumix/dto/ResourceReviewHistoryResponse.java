package com.itxiaowu.edumix.dto;

import java.time.LocalDateTime;

public record ResourceReviewHistoryResponse(
    Integer submissionNo,
    String result,
    String comment,
    String reviewerName,
    LocalDateTime reviewedAt) {}
