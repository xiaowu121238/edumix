package com.itxiaowu.edumix.dto;

/** 知识点已关联资源响应，包含资源信息和关联时间。 */
public record KnowledgePointResourceResponse(
    Long id, String resourceName, String resourceType, String categoryName, boolean available) {}
