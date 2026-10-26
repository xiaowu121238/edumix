package com.itxiaowu.edumix.dto;

import java.util.List;

/** 公共首页聚合数据，包括真实统计和优先展示的优秀案例。 */
public record PublicHomeResponse(
    long teacherCount, long resourceCount, long caseCount, List<PublicCaseSummary> featuredCases) {}
