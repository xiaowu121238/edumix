package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 负责人审核接口：查询待审资源与案例，并执行通过或驳回操作。 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESOURCE_MANAGER','ADMIN')")
public class ReviewQueueController {
  private final JdbcTemplate jdbc;

  public record PendingItem(
      String kind,
      Long id,
      String title,
      String summary,
      String author,
      Long authorId,
      String visibility,
      String resourceType,
      LocalDateTime submittedAt) {}

  @GetMapping("/pending")
  public ApiResponse<List<PendingItem>> pending() {
    List<PendingItem> items =
        new ArrayList<>(
            jdbc.query(
                """
            SELECT r.id,r.resource_name AS title,r.description AS summary,u.real_name AS author,
                   r.uploader_id AS author_id,r.visibility,r.resource_type,r.update_time
            FROM teaching_resource r JOIN sys_user u ON u.id=r.uploader_id
            WHERE r.deleted=0 AND r.audit_status='PENDING' AND r.visibility='PUBLIC'
            ORDER BY r.update_time DESC LIMIT 200
            """,
                (rs, n) ->
                    new PendingItem(
                        "RESOURCE",
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("summary"),
                        rs.getString("author"),
                        rs.getLong("author_id"),
                        rs.getString("visibility"),
                        rs.getString("resource_type"),
                        rs.getTimestamp("update_time").toLocalDateTime())));
    items.addAll(
        jdbc.query(
            """
            SELECT c.id,c.case_name AS title,c.summary,u.real_name AS author,
                   c.creator_id AS author_id,c.visibility,c.update_time
            FROM teaching_case c JOIN sys_user u ON u.id=c.creator_id
            WHERE c.deleted=0 AND c.audit_status='PENDING' AND c.visibility='PUBLIC'
            ORDER BY c.update_time DESC LIMIT 200
            """,
            (rs, n) ->
                new PendingItem(
                    "CASE",
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("summary"),
                    rs.getString("author"),
                    rs.getLong("author_id"),
                    rs.getString("visibility"),
                    null,
                    rs.getTimestamp("update_time").toLocalDateTime())));
    items.sort(Comparator.comparing(PendingItem::submittedAt).reversed());
    return ApiResponse.success(items);
  }
}
