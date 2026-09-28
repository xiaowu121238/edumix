package com.itxiaowu.edumix.service.impl;

import com.itxiaowu.edumix.mapper.SqlQueryMapper;
import com.itxiaowu.edumix.service.*;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

/**
 * 平台消息的统一投递服务。
 *
 * <p>业务服务只负责生成消息内容；本服务统一维护 message 与 user_message 两张表之间的写入规则，避免审核、纠错和系统公告各自复制相同的持久化代码。
 */
@Service
@RequiredArgsConstructor
public class NotificationDeliveryServiceImpl implements NotificationDeliveryService {
  private static final int MAX_CONTENT_LENGTH = 2000;

  private final SqlQueryMapper jdbc;

  /** 向指定用户投递一条消息；同一个接收人只会写入一次。 */
  @Override
  public long send(
      String messageType,
      String title,
      String content,
      Long senderId,
      String businessType,
      Long businessId,
      Collection<Long> recipientIds,
      boolean excludeSender) {
    Set<Long> recipients = new LinkedHashSet<>();
    if (recipientIds != null) {
      recipientIds.stream()
          .filter(java.util.Objects::nonNull)
          .filter(id -> !excludeSender || !id.equals(senderId))
          .forEach(recipients::add);
    }

    String body = normalizeContent(content);
    GeneratedKeyHolder key = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          var statement =
              connection.prepareStatement(
                  "INSERT INTO message(message_type,title,content,sender_id,business_type,business_id) VALUES(?,?,?,?,?,?)",
                  Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, messageType);
          statement.setString(2, title);
          statement.setString(3, body);
          if (senderId == null) statement.setNull(4, java.sql.Types.BIGINT);
          else statement.setLong(4, senderId);
          statement.setString(5, businessType);
          if (businessId == null) statement.setNull(6, java.sql.Types.BIGINT);
          else statement.setLong(6, businessId);
          return statement;
        },
        key);

    long messageId = key.getKey().longValue();
    for (Long recipientId : recipients) {
      jdbc.update(
          "INSERT IGNORE INTO user_message(user_id,message_id) VALUES(?,?)",
          recipientId,
          messageId);
    }
    return messageId;
  }

  /** 向当前所有启用账号广播系统消息。 */
  @Override
  public long broadcastSystem(String title, String content, Long senderId) {
    long messageId = send("SYSTEM", title, content, senderId, null, null, Set.of(), false);
    jdbc.update(
        """
                INSERT INTO user_message(user_id,message_id)
                SELECT id,? FROM sys_user WHERE status=1 AND deleted=0
                """,
        messageId);
    return messageId;
  }

  private String normalizeContent(String content) {
    String value = content == null ? "" : content.strip();
    return value.length() > MAX_CONTENT_LENGTH ? value.substring(0, MAX_CONTENT_LENGTH) : value;
  }
}
