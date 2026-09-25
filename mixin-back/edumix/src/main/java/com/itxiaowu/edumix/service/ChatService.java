package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.ChatDtos;
import com.itxiaowu.edumix.security.UserPrincipal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 私信业务服务：校验收发双方、保存消息、统计未读数并维护会话已读状态。 */
@Service
@RequiredArgsConstructor
public class ChatService {
  private final JdbcTemplate jdbc;
  private final SimpMessagingTemplate messaging;

  @Transactional
  public ChatDtos.Conversation start(Long otherId, UserPrincipal me) {
    if (otherId.equals(me.getUserId())) throw new IllegalArgumentException("不能给自己发私信");
    ChatDtos.PublicTeacher other = teacher(otherId);
    long low = Math.min(me.getUserId(), otherId), high = Math.max(me.getUserId(), otherId);
    jdbc.update(
        "INSERT IGNORE INTO chat_conversation(user_low_id,user_high_id) VALUES(?,?)", low, high);
    Long id =
        jdbc.queryForObject(
            "SELECT id FROM chat_conversation WHERE user_low_id=? AND user_high_id=?",
            Long.class,
            low,
            high);
    return new ChatDtos.Conversation(id, other, null, null, 0);
  }

  public List<ChatDtos.Conversation> conversations(UserPrincipal me) {
    return jdbc.query(
        """
            SELECT c.id, u.id AS other_id, u.real_name, u.avatar_url, u.college, u.title,
                   m.content AS last_content, c.last_message_at,
                   (SELECT COUNT(*) FROM chat_message unread
                    WHERE unread.conversation_id=c.id AND unread.sender_id<>?
                      AND unread.id>COALESCE(rc.last_read_message_id,0)) AS unread_count
            FROM chat_conversation c
            JOIN sys_user u ON u.id=IF(c.user_low_id=?,c.user_high_id,c.user_low_id) AND u.deleted=0
            LEFT JOIN chat_message m ON m.id=c.last_message_id
            LEFT JOIN chat_read_cursor rc ON rc.conversation_id=c.id AND rc.user_id=?
            WHERE (c.user_low_id=? OR c.user_high_id=?)
            ORDER BY c.last_message_at DESC,c.id DESC
            """,
        (rs, n) ->
            new ChatDtos.Conversation(
                rs.getLong("id"),
                new ChatDtos.PublicTeacher(
                    rs.getLong("other_id"),
                    rs.getString("real_name"),
                    rs.getString("avatar_url"),
                    rs.getString("college"),
                    rs.getString("title")),
                rs.getString("last_content"),
                toTime(rs.getTimestamp("last_message_at")),
                rs.getLong("unread_count")),
        me.getUserId(),
        me.getUserId(),
        me.getUserId(),
        me.getUserId(),
        me.getUserId());
  }

  public ChatDtos.MessagePage messages(
      Long conversationId, Long beforeId, int size, UserPrincipal me) {
    requireMember(conversationId, me.getUserId());
    int limit = Math.max(1, Math.min(size, 50));
    List<ChatDtos.ChatMessage> newest =
        jdbc.query(
            """
            SELECT id,conversation_id,sender_id,content,create_time FROM chat_message
            WHERE conversation_id=? AND (? IS NULL OR id<?) ORDER BY id DESC LIMIT ?
            """,
            (rs, n) -> mapMessage(rs),
            conversationId,
            beforeId,
            beforeId,
            limit + 1);
    boolean more = newest.size() > limit;
    List<ChatDtos.ChatMessage> page = newest.subList(0, Math.min(limit, newest.size()));
    List<ChatDtos.ChatMessage> chronological = new ArrayList<>(page);
    Collections.reverse(chronological);
    return new ChatDtos.MessagePage(chronological, more ? page.get(page.size() - 1).id() : null);
  }

  @Transactional
  public ChatDtos.ChatMessage send(Long conversationId, String raw, UserPrincipal me) {
    long recipient = requireMember(conversationId, me.getUserId());
    String content = raw == null ? "" : raw.strip();
    if (content.isBlank() || content.length() > 2000)
      throw new IllegalArgumentException("消息长度须为1到2000字");
    GeneratedKeyHolder key = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  "INSERT INTO chat_message(conversation_id,sender_id,content) VALUES(?,?,?)",
                  Statement.RETURN_GENERATED_KEYS);
          statement.setLong(1, conversationId);
          statement.setLong(2, me.getUserId());
          statement.setString(3, content);
          return statement;
        },
        key);
    long id = key.getKey().longValue();
    jdbc.update(
        "UPDATE chat_conversation SET last_message_id=?,last_message_at=(SELECT create_time FROM chat_message WHERE id=?) WHERE id=?",
        id,
        id,
        conversationId);
    ChatDtos.ChatMessage result =
        jdbc.queryForObject(
            "SELECT id,conversation_id,sender_id,content,create_time FROM chat_message WHERE id=?",
            (rs, n) -> mapMessage(rs),
            id);
    String recipientName =
        jdbc.queryForObject(
            "SELECT username FROM sys_user WHERE id=? AND deleted=0", String.class, recipient);
    String senderName = me.getUsername();
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            messaging.convertAndSendToUser(recipientName, "/queue/chat", result);
            messaging.convertAndSendToUser(senderName, "/queue/chat", result);
          }
        });
    return result;
  }

  @Transactional
  public void markRead(Long conversationId, UserPrincipal me) {
    requireMember(conversationId, me.getUserId());
    Long latest =
        jdbc.queryForObject(
            "SELECT MAX(id) FROM chat_message WHERE conversation_id=?", Long.class, conversationId);
    if (latest == null) return;
    jdbc.update(
        """
            INSERT INTO chat_read_cursor(conversation_id,user_id,last_read_message_id) VALUES(?,?,?)
            ON DUPLICATE KEY UPDATE last_read_message_id=GREATEST(COALESCE(last_read_message_id,0),VALUES(last_read_message_id))
            """,
        conversationId,
        me.getUserId(),
        latest);
  }

  private long requireMember(Long conversationId, Long userId) {
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT user_low_id,user_high_id FROM chat_conversation WHERE id=?", conversationId);
    if (rows.isEmpty()) throw new IllegalArgumentException("会话不存在");
    long low = ((Number) rows.get(0).get("user_low_id")).longValue(),
        high = ((Number) rows.get(0).get("user_high_id")).longValue();
    if (userId != low && userId != high) throw new AccessDeniedException("无权访问此会话");
    return userId == low ? high : low;
  }

  private ChatDtos.PublicTeacher teacher(Long id) {
    List<ChatDtos.PublicTeacher> users =
        jdbc.query(
            """
            SELECT u.id,u.real_name,u.avatar_url,u.college,u.title FROM sys_user u
            WHERE u.id=? AND u.status=1 AND u.deleted=0 AND EXISTS(
                SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id
                WHERE ur.user_id=u.id AND r.role_code='TEACHER')
            """,
            (rs, n) ->
                new ChatDtos.PublicTeacher(
                    rs.getLong("id"),
                    rs.getString("real_name"),
                    rs.getString("avatar_url"),
                    rs.getString("college"),
                    rs.getString("title")),
            id);
    if (users.isEmpty()) throw new IllegalArgumentException("该教师不存在或不可私信");
    return users.get(0);
  }

  private ChatDtos.ChatMessage mapMessage(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new ChatDtos.ChatMessage(
        rs.getLong("id"),
        rs.getLong("conversation_id"),
        rs.getLong("sender_id"),
        rs.getString("content"),
        toTime(rs.getTimestamp("create_time")));
  }

  private LocalDateTime toTime(Timestamp value) {
    return value == null ? null : value.toLocalDateTime();
  }
}
