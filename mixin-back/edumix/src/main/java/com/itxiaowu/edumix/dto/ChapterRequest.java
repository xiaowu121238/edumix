package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChapterRequest(
    @NotBlank(message = "章节名称不能为空") @Size(max = 200, message = "章节名称不能超过200个字符") String chapterName,
    @Size(max = 30, message = "章节编号不能超过30个字符") String chapterNo,
    @Size(max = 1000, message = "章节简介不能超过1000个字符") String description,
    @Min(value = 0, message = "章节排序不能为负数") Integer sortOrder) {}
