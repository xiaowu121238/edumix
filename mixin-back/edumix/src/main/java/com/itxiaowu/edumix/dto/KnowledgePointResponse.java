package com.itxiaowu.edumix.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 知识点详情响应。 */
public record KnowledgePointResponse(
    Long id,
    Long courseId,
    Long chapterId,
    String pointName,
    String pointCode,
    String description,
    Integer difficulty,
    BigDecimal teachingHours,
    Integer sortOrder,
    int resourceCount,
    int caseCount,
    LocalDateTime updateTime) {}
