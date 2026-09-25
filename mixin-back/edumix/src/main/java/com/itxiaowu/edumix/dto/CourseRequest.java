package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseRequest(
    @Size(max = 50, message = "课程编码不能超过50个字符") String courseCode,
    @NotBlank(message = "课程名称不能为空") @Size(max = 150, message = "课程名称不能超过150个字符") String courseName,
    @Size(max = 1000, message = "课程简介不能超过1000个字符") String description,
    @Size(max = 2000, message = "知识目标不能超过2000个字符") String knowledgeObjective,
    @Size(max = 2000, message = "能力目标不能超过2000个字符") String abilityObjective,
    @Size(max = 2000, message = "素质目标不能超过2000个字符") String qualityObjective,
    @Size(max = 500, message = "封面地址不能超过500个字符") String coverUrl,
    @Size(max = 100, message = "学院不能超过100个字符") String college,
    @Size(max = 50, message = "学期不能超过50个字符") String semester,
    Integer status) {}
