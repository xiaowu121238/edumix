package com.itxiaowu.edumix.dto;

import java.time.LocalDateTime;

public record CorrectionResponse(
    Long id,
    Long resourceId,
    String resourceName,
    String resourceType,
    Long uploaderId,
    String uploaderName,
    Long submitterId,
    String submitterName,
    String content,
    String status,
    Long handlerId,
    String handlerName,
    String handleComment,
    LocalDateTime handledAt,
    LocalDateTime createTime) {}
