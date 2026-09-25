package com.itxiaowu.edumix.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 教学案例详情响应，包含作者、课程、标签、资源和当前用户访问状态。 */
public record CaseResponse(
    Long id,
    String caseNo,
    String caseName,
    String summary,
    String teachingObjective,
    String keyPoints,
    String difficultPoints,
    String teachingProcess,
    String teachingReflection,
    String content,
    Long creatorId,
    String creatorName,
    String creatorCollege,
    Long courseId,
    String courseName,
    List<Long> knowledgePointIds,
    List<String> knowledgePointNames,
    List<Long> resourceIds,
    List<String> resourceNames,
    String visibility,
    String accessType,
    Integer pointsPrice,
    String auditStatus,
    Integer excellent,
    Integer referenceCount,
    boolean favorite,
    String latestReviewComment,
    LocalDateTime createTime,
    LocalDateTime updateTime,
    boolean contentUnlocked) {}
