package com.itxiaowu.edumix.dto;

import java.time.LocalDateTime;

/** 公共首页使用的资源摘要，不返回文件存储地址和上传者隐私信息。 */
public record PublicResourceSummary(
    Long id,
    String name,
    String type,
    String description,
    String uploaderName,
    Integer downloadCount,
    Integer referenceCount,
    LocalDateTime updateTime) {}
