package com.itxiaowu.edumix.dto;

import java.util.List;

public record CourseTreeResponse(
    CourseSummaryResponse course, List<ChapterTreeResponse> chapters) {}
