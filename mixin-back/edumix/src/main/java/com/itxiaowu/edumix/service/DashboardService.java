package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.security.UserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** 教师工作台聚合服务：计算个人业务统计并生成近期操作入口。 */
@Service
@RequiredArgsConstructor
public class DashboardService {
  private final JdbcTemplate jdbc;
  private final ResourceService resources;
  private final TeachingCaseService cases;

  public record CourseItem(
      Long id,
      String courseName,
      int chapterCount,
      int knowledgePointCount,
      LocalDateTime updateTime) {}

  public record Summary(
      String realName,
      long courseCount,
      long resourceCount,
      long caseCount,
      long resourceFavoriteCount,
      long caseFavoriteCount,
      long pendingResourceCount,
      long pendingCaseCount,
      List<CourseItem> recentCourses) {}

  public Summary summary(UserPrincipal user) {
    Long id = user.getUserId();
    long courseCount = count("SELECT COUNT(*) FROM course WHERE owner_id=? AND deleted=0", id);
    long resourceCount =
        count("SELECT COUNT(*) FROM teaching_resource WHERE uploader_id=? AND deleted=0", id);
    long caseCount =
        count("SELECT COUNT(*) FROM teaching_case WHERE creator_id=? AND deleted=0", id);
    long resourceFavorites = resources.list(null, null, null, true, user).size();
    long caseFavorites = cases.list(null, null, null, true, user).size();
    boolean reviewer = user.roles().contains("RESOURCE_MANAGER") || user.roles().contains("ADMIN");
    long pendingResources =
        reviewer
            ? count(
                """
                SELECT COUNT(*) FROM teaching_resource
                WHERE deleted=0 AND audit_status='PENDING' AND visibility='PUBLIC' AND uploader_id<>?
                """,
                id)
            : 0;
    long pendingCases =
        reviewer
            ? count(
                """
                SELECT COUNT(*) FROM teaching_case
                WHERE deleted=0 AND audit_status='PENDING' AND visibility='PUBLIC' AND creator_id<>?
                """,
                id)
            : 0;
    List<CourseItem> recent =
        jdbc.query(
            """
                SELECT c.id,c.course_name,c.update_time,
                       (SELECT COUNT(*) FROM course_chapter ch WHERE ch.course_id=c.id AND ch.deleted=0) AS chapter_count,
                       (SELECT COUNT(*) FROM knowledge_point p WHERE p.course_id=c.id AND p.deleted=0) AS point_count
                FROM course c WHERE c.owner_id=? AND c.deleted=0
                ORDER BY c.update_time DESC,c.id DESC LIMIT 5
                """,
            (rs, row) ->
                new CourseItem(
                    rs.getLong("id"),
                    rs.getString("course_name"),
                    rs.getInt("chapter_count"),
                    rs.getInt("point_count"),
                    rs.getTimestamp("update_time").toLocalDateTime()),
            id);
    return new Summary(
        user.user().getRealName(),
        courseCount,
        resourceCount,
        caseCount,
        resourceFavorites,
        caseFavorites,
        pendingResources,
        pendingCases,
        recent);
  }

  private long count(String sql, Long id) {
    Long value = jdbc.queryForObject(sql, Long.class, id);
    return value == null ? 0 : value;
  }
}
