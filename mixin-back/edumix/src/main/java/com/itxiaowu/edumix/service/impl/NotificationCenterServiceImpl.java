package com.itxiaowu.edumix.service.impl;

import com.itxiaowu.edumix.mapper.SqlQueryMapper;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.NotificationCenterService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 消息中心业务实现，负责通知查询和已读状态维护。 */
@Service
@RequiredArgsConstructor
public class NotificationCenterServiceImpl implements NotificationCenterService {
  private final SqlQueryMapper sql;

  @Override
  public List<Notice> list(UserPrincipal user) {
    return sql.query(
        """
        SELECT um.id,m.message_type,m.title,m.content,um.read_status,um.create_time
        FROM user_message um JOIN message m ON m.id=um.message_id
        WHERE um.user_id=? AND um.deleted=0 AND m.message_type IN ('SYSTEM','AUDIT')
        ORDER BY um.create_time DESC LIMIT 100
        """,
        (rs, rowNum) ->
            new Notice(
                rs.getLong("id"),
                rs.getString("message_type"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getInt("read_status") == 1,
                rs.getTimestamp("create_time").toLocalDateTime()),
        user.getUserId());
  }

  @Override
  public UnreadCount unreadCount(UserPrincipal user) {
    List<Long> counts =
        sql
            .query(
                """
                SELECT m.message_type,COUNT(*) FROM user_message um JOIN message m ON m.id=um.message_id
                WHERE um.user_id=? AND um.deleted=0 AND um.read_status=0
                  AND m.message_type IN ('SYSTEM','AUDIT')
                GROUP BY m.message_type
                """,
                (rs, rowNum) ->
                    List.of(
                        "SYSTEM".equals(rs.getString(1)) ? rs.getLong(2) : 0L,
                        "AUDIT".equals(rs.getString(1)) ? rs.getLong(2) : 0L),
                user.getUserId())
            .stream()
            .reduce(
                List.of(0L, 0L),
                (left, right) -> List.of(left.get(0) + right.get(0), left.get(1) + right.get(1)));
    Long privateCount =
        user.roles().contains("TEACHER")
            ? sql.queryForObject(
                """
                SELECT COUNT(*) FROM chat_message cm
                JOIN chat_conversation c ON c.id=cm.conversation_id
                LEFT JOIN chat_read_cursor rc ON rc.conversation_id=c.id AND rc.user_id=?
                WHERE (c.user_low_id=? OR c.user_high_id=?) AND cm.sender_id<>?
                  AND cm.id>COALESCE(rc.last_read_message_id,0)
                """,
                Long.class,
                user.getUserId(),
                user.getUserId(),
                user.getUserId(),
                user.getUserId())
            : 0L;
    return new UnreadCount(counts.get(0), counts.get(1), privateCount == null ? 0 : privateCount);
  }

  @Override
  public void readAll(String category, UserPrincipal user) {
    if (category != null && !category.equals("SYSTEM") && !category.equals("AUDIT")) {
      throw new IllegalArgumentException("通知分类不正确");
    }
    sql.update(
        """
        UPDATE user_message um JOIN message m ON m.id=um.message_id
        SET um.read_status=1,um.read_time=COALESCE(um.read_time,NOW())
        WHERE um.user_id=? AND um.deleted=0 AND m.message_type IN ('SYSTEM','AUDIT')
          AND (? IS NULL OR m.message_type=?)
        """,
        user.getUserId(),
        category,
        category);
  }

  @Override
  public void readOne(Long id, UserPrincipal user) {
    int updated =
        sql.update(
            """
            UPDATE user_message um JOIN message m ON m.id=um.message_id
            SET um.read_status=1,um.read_time=COALESCE(um.read_time,NOW())
            WHERE um.id=? AND um.user_id=? AND um.deleted=0
              AND m.message_type IN ('SYSTEM','AUDIT')
            """,
            id,
            user.getUserId());
    if (updated != 0) return;
    Integer owned =
        sql.queryForObject(
            """
            SELECT COUNT(*) FROM user_message um JOIN message m ON m.id=um.message_id
            WHERE um.id=? AND um.user_id=? AND um.deleted=0
              AND m.message_type IN ('SYSTEM','AUDIT')
            """,
            Integer.class,
            id,
            user.getUserId());
    if (owned == null || owned == 0) throw new IllegalArgumentException("消息不存在或无权查看");
  }
}
