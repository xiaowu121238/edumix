package com.itxiaowu.edumix.dto;

import java.time.LocalDateTime;

/** 资源纠错详情响应，包含提交人、处理进度和处理意见。 */
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
