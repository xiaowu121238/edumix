package com.itxiaowu.edumix.dto;

import java.util.List;

/** 完整课程树响应，由课程信息及其章节列表组成。 */
public record CourseTreeResponse(
    CourseSummaryResponse course, List<ChapterTreeResponse> chapters) {}
