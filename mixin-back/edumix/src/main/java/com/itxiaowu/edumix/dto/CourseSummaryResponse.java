package com.itxiaowu.edumix.dto;

import java.time.LocalDateTime;

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
