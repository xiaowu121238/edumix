package com.itxiaowu.edumix.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChapterTreeResponse(
    Long id,
    Long courseId,
    String chapterName,
    String chapterNo,
    String description,
    Integer sortOrder,
    LocalDateTime updateTime,
    List<KnowledgePointResponse> knowledgePoints) {}
