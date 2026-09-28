package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.security.UserPrincipal;
import java.time.LocalDateTime;
import java.util.List;

/** 教师工作台业务接口，定义个人业务统计汇总能力。 */
public interface DashboardService {
  Summary summary(UserPrincipal user);

  record CourseItem(
      Long id,
      String courseName,
      int chapterCount,
      int knowledgePointCount,
      LocalDateTime updateTime) {}

  record Summary(
      String realName,
      long courseCount,
      long resourceCount,
      long caseCount,
      long resourceFavoriteCount,
      long caseFavoriteCount,
      long pendingResourceCount,
      long pendingCaseCount,
      List<CourseItem> recentCourses) {}
}
