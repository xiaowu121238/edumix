package com.itxiaowu.edumix.dto;

import java.time.LocalDateTime;

public record ResourceResponse(
    Long id,
    String resourceNo,
    String resourceName,
    String resourceType,
    String description,
    Long fileSize,
    Long uploaderId,
    Long categoryId,
    String categoryName,
    String visibility,
    String auditStatus,
    Integer downloadCount,
    Integer referenceCount,
    boolean favorite,
    boolean previewable,
    String fileUrl,
    String latestReviewComment,
    LocalDateTime createTime,
    LocalDateTime updateTime) {}
