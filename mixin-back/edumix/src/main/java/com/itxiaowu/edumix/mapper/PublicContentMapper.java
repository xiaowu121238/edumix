package com.itxiaowu.edumix.mapper;

import com.itxiaowu.edumix.dto.PublicCaseSummary;
import com.itxiaowu.edumix.dto.PublicResourceSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 公共内容数据访问层。
 *
 * <p>所有查询都固定限制为“公开且审核通过”，避免公共接口读取草稿、待审核内容或私有内容。
 */
@Repository
@RequiredArgsConstructor
public class PublicContentMapper {
  private final JdbcTemplate jdbc;

  public long countActiveTeachers() {
    return jdbc.queryForObject(
        """
        SELECT COUNT(DISTINCT u.id)
        FROM sys_user u
        JOIN sys_user_role ur ON ur.user_id=u.id
        JOIN sys_role r ON r.id=ur.role_id
        WHERE u.status=1 AND u.deleted=0 AND r.role_code='TEACHER'
        """,
        Long.class);
  }

  public long countPublicResources() {
    return count(
        "SELECT COUNT(*) FROM teaching_resource WHERE visibility='PUBLIC' AND audit_status='APPROVED' AND deleted=0");
  }

  public long countPublicCases() {
    return count(
        "SELECT COUNT(*) FROM teaching_case WHERE visibility='PUBLIC' AND audit_status='APPROVED' AND deleted=0");
  }

  public List<PublicCaseSummary> findFeaturedCases(int limit) {
    return jdbc.query(
        caseSelect()
            + """
             WHERE tc.visibility='PUBLIC' AND tc.audit_status='APPROVED' AND tc.deleted=0
             ORDER BY tc.excellent DESC, tc.reference_count DESC, tc.update_time DESC, tc.id DESC
             LIMIT ?
            """,
        (rs, rowNum) ->
            new PublicCaseSummary(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("summary"),
                rs.getString("course_name"),
                rs.getString("creator_name"),
                rs.getInt("excellent") == 1,
                rs.getString("access_type"),
                rs.getInt("points_price"),
                rs.getInt("reference_count"),
                rs.getTimestamp("update_time").toLocalDateTime()),
        limit);
  }

  public List<PublicCaseSummary> searchCases(String keyword, int limit) {
    String pattern = "%" + keyword + "%";
    return jdbc.query(
        caseSelect()
            + """
             WHERE tc.visibility='PUBLIC' AND tc.audit_status='APPROVED' AND tc.deleted=0
               AND (tc.case_name LIKE ? OR tc.summary LIKE ? OR c.course_name LIKE ? OR u.real_name LIKE ?)
             ORDER BY tc.excellent DESC, tc.reference_count DESC, tc.update_time DESC, tc.id DESC
             LIMIT ?
            """,
        (rs, rowNum) ->
            new PublicCaseSummary(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("summary"),
                rs.getString("course_name"),
                rs.getString("creator_name"),
                rs.getInt("excellent") == 1,
                rs.getString("access_type"),
                rs.getInt("points_price"),
                rs.getInt("reference_count"),
                rs.getTimestamp("update_time").toLocalDateTime()),
        pattern,
        pattern,
        pattern,
        pattern,
        limit);
  }

  public List<PublicResourceSummary> searchResources(String keyword, int limit) {
    String pattern = "%" + keyword + "%";
    return jdbc.query(
        """
        SELECT tr.id, tr.resource_name AS name, tr.resource_type AS type, tr.description,
               u.real_name AS uploader_name, tr.download_count, tr.reference_count, tr.update_time
        FROM teaching_resource tr
        JOIN sys_user u ON u.id=tr.uploader_id AND u.deleted=0
        LEFT JOIN resource_category rc ON rc.id=tr.category_id
        WHERE tr.visibility='PUBLIC' AND tr.audit_status='APPROVED' AND tr.deleted=0
          AND (tr.resource_name LIKE ? OR tr.description LIKE ? OR tr.resource_type LIKE ?
               OR rc.category_name LIKE ? OR u.real_name LIKE ?)
        ORDER BY tr.reference_count DESC, tr.download_count DESC, tr.update_time DESC, tr.id DESC
        LIMIT ?
        """,
        (rs, rowNum) ->
            new PublicResourceSummary(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getString("description"),
                rs.getString("uploader_name"),
                rs.getInt("download_count"),
                rs.getInt("reference_count"),
                rs.getTimestamp("update_time").toLocalDateTime()),
        pattern,
        pattern,
        pattern,
        pattern,
        pattern,
        limit);
  }

  private long count(String sql) {
    Long result = jdbc.queryForObject(sql, Long.class);
    return result == null ? 0 : result;
  }

  private String caseSelect() {
    return """
        SELECT tc.id, tc.case_name AS name, tc.summary, c.course_name,
               u.real_name AS creator_name, tc.excellent, tc.access_type,
               tc.points_price, tc.reference_count, tc.update_time
        FROM teaching_case tc
        JOIN sys_user u ON u.id=tc.creator_id AND u.deleted=0
        LEFT JOIN course c ON c.id=tc.course_id AND c.deleted=0
        """;
  }
}
