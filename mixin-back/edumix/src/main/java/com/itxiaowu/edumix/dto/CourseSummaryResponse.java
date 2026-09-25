package com.itxiaowu.edumix.dto;

import java.time.LocalDateTime;

/** 课程列表摘要响应，提供课程基础信息和章节知识点数量。 */
public record CourseSummaryResponse(
    Long id,
    String courseCode,
    String courseName,
    String description,
    String knowledgeObjective,
    String abilityObjective,
    String qualityObjective,
    String coverUrl,
    String college,
    String semester,
    Integer status,
    int chapterCount,
    int knowledgePointCount,
    LocalDateTime updateTime) {}
