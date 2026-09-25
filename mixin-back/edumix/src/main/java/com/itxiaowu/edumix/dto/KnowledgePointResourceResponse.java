package com.itxiaowu.edumix.dto;

public record KnowledgePointResourceResponse(
    Long id, String resourceName, String resourceType, String categoryName, boolean available) {}
