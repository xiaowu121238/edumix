package com.itxiaowu.edumix.service.impl;

import com.itxiaowu.edumix.mapper.SqlQueryMapper;
import com.itxiaowu.edumix.service.ReviewQueueService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 审核队列业务实现，合并资源与案例的待审核记录。 */
@Service
@RequiredArgsConstructor
public class ReviewQueueServiceImpl implements ReviewQueueService {
  private final SqlQueryMapper sql;

  @Override
  public List<PendingItem> pending() {
    List<PendingItem> items =
        new ArrayList<>(
            sql.query(
                """
                SELECT r.id,r.resource_name AS title,r.description AS summary,u.real_name AS author,
                       r.uploader_id AS author_id,r.visibility,r.resource_type,r.update_time
                FROM teaching_resource r JOIN sys_user u ON u.id=r.uploader_id
                WHERE r.deleted=0 AND r.audit_status='PENDING' AND r.visibility='PUBLIC'
                ORDER BY r.update_time DESC LIMIT 200
                """,
                (rs, rowNum) ->
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
        sql.query(
            """
            SELECT c.id,c.case_name AS title,c.summary,u.real_name AS author,
                   c.creator_id AS author_id,c.visibility,c.update_time
            FROM teaching_case c JOIN sys_user u ON u.id=c.creator_id
            WHERE c.deleted=0 AND c.audit_status='PENDING' AND c.visibility='PUBLIC'
            ORDER BY c.update_time DESC LIMIT 200
            """,
            (rs, rowNum) ->
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
    return items;
  }
}
