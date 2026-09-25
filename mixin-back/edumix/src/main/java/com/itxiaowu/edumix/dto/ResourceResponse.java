package com.itxiaowu.edumix.dto;

import java.time.LocalDateTime;

/** 教学资源响应，包含文件元数据、审核状态、上传者和当前用户收藏状态。 */
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
