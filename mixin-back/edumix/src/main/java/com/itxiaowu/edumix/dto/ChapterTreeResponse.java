package com.itxiaowu.edumix.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 章节树响应，包含章节信息和其下的有序知识点。 */
public record ChapterTreeResponse(
    Long id,
    Long courseId,
    String chapterName,
    String chapterNo,
    String description,
    Integer sortOrder,
    LocalDateTime updateTime,
    List<KnowledgePointResponse> knowledgePoints) {}
