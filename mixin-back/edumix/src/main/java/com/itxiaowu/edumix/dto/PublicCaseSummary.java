package com.itxiaowu.edumix.dto;

import java.time.LocalDateTime;

/** 公共首页使用的案例摘要，不包含正文、资源附件等需要登录或积分解锁的数据。 */
public record PublicCaseSummary(
    Long id,
    String name,
    String summary,
    String courseName,
    String creatorName,
    boolean excellent,
    String accessType,
    Integer pointsPrice,
    Integer referenceCount,
    LocalDateTime updateTime) {}
