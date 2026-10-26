package com.itxiaowu.edumix.dto;

import java.util.List;

/** 公共搜索结果，资源与案例分组返回，便于前端明确区分内容类型。 */
public record PublicSearchResponse(
    String keyword, List<PublicResourceSummary> resources, List<PublicCaseSummary> cases) {}
