package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.dto.SystemNoticeRequest;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.SystemNoticeService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 消息中心接口：统一提供系统通知、审核通知、私信入口和已读状态管理。 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
  private final JdbcTemplate jdbc;
  private final SystemNoticeService systemNotices;

  public record Notice(
      Long id,
      String category,
      String title,
      String content,
      boolean read,
      LocalDateTime createTime) {}

  public record UnreadCount(long system, long audit, long privateMessages) {}

  @GetMapping
  public ApiResponse<List<Notice>> list(@AuthenticationPrincipal UserPrincipal me) {
    return ApiResponse.success(
        jdbc.query(
            """
            SELECT um.id,m.message_type,m.title,m.content,um.read_status,um.create_time
            FROM user_message um JOIN message m ON m.id=um.message_id
            WHERE um.user_id=? AND um.deleted=0 AND m.message_type IN ('SYSTEM','AUDIT')
            ORDER BY um.create_time DESC LIMIT 100
            """,
            (rs, n) ->
                new Notice(
                    rs.getLong("id"),
                    rs.getString("message_type"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getInt("read_status") == 1,
                    rs.getTimestamp("create_time").toLocalDateTime()),
            me.getUserId()));
  }

  @GetMapping("/unread-count")
  public ApiResponse<UnreadCount> unreadCount(@AuthenticationPrincipal UserPrincipal me) {
    List<Long> counts =
        jdbc
            .query(
                """
            SELECT m.message_type,COUNT(*) FROM user_message um JOIN message m ON m.id=um.message_id
            WHERE um.user_id=? AND um.deleted=0 AND um.read_status=0 AND m.message_type IN ('SYSTEM','AUDIT')
            GROUP BY m.message_type
            """,
                (rs, n) ->
                    List.of(
                        "SYSTEM".equals(rs.getString(1)) ? rs.getLong(2) : 0L,
                        "AUDIT".equals(rs.getString(1)) ? rs.getLong(2) : 0L),
                me.getUserId())
            .stream()
            .reduce(List.of(0L, 0L), (a, b) -> List.of(a.get(0) + b.get(0), a.get(1) + b.get(1)));
    Long privateCount =
        me.roles().contains("TEACHER")
            ? jdbc.queryForObject(
                """
            SELECT COUNT(*) FROM chat_message cm
            JOIN chat_conversation c ON c.id=cm.conversation_id
            LEFT JOIN chat_read_cursor rc ON rc.conversation_id=c.id AND rc.user_id=?
            WHERE (c.user_low_id=? OR c.user_high_id=?) AND cm.sender_id<>?
              AND cm.id>COALESCE(rc.last_read_message_id,0)
            """,
                Long.class,
                me.getUserId(),
                me.getUserId(),
                me.getUserId(),
                me.getUserId())
            : 0L;
    return ApiResponse.success(
        new UnreadCount(counts.get(0), counts.get(1), privateCount == null ? 0 : privateCount));
  }

  @PostMapping("/read-all")
  public ApiResponse<Void> readAll(
      @RequestParam(required = false) String category, @AuthenticationPrincipal UserPrincipal me) {
    if (category != null && !category.equals("SYSTEM") && !category.equals("AUDIT"))
      throw new IllegalArgumentException("通知分类不正确");
    jdbc.update(
        """
            UPDATE user_message um JOIN message m ON m.id=um.message_id
            SET um.read_status=1,um.read_time=COALESCE(um.read_time,NOW())
            WHERE um.user_id=? AND um.deleted=0 AND m.message_type IN ('SYSTEM','AUDIT')
              AND (? IS NULL OR m.message_type=?)
            """,
        me.getUserId(),
        category,
        category);
    return ApiResponse.success();
  }

  @PostMapping("/{id}/read")
  public ApiResponse<Void> readOne(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal me) {
    int updated =
        jdbc.update(
            """
            UPDATE user_message um JOIN message m ON m.id=um.message_id
            SET um.read_status=1,um.read_time=COALESCE(um.read_time,NOW())
            WHERE um.id=? AND um.user_id=? AND um.deleted=0 AND m.message_type IN ('SYSTEM','AUDIT')
            """,
            id,
            me.getUserId());
    if (updated == 0) {
      Integer owned =
          jdbc.queryForObject(
              """
                SELECT COUNT(*) FROM user_message um JOIN message m ON m.id=um.message_id
                WHERE um.id=? AND um.user_id=? AND um.deleted=0 AND m.message_type IN ('SYSTEM','AUDIT')
                """,
              Integer.class,
              id,
              me.getUserId());
      if (owned == null || owned == 0) throw new IllegalArgumentException("消息不存在或无权查看");
    }
    return ApiResponse.success();
  }

  @PostMapping("/system")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Long> publish(
      @Valid @RequestBody SystemNoticeRequest request,
      @AuthenticationPrincipal UserPrincipal admin) {
    return ApiResponse.success(systemNotices.publish(request, admin));
  }
}
