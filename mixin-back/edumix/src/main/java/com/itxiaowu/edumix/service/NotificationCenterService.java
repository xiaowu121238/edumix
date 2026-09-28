package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.security.UserPrincipal;
import java.time.LocalDateTime;
import java.util.List;

/** 消息中心业务接口，定义通知查询、未读统计和已读状态维护能力。 */
public interface NotificationCenterService {
  List<Notice> list(UserPrincipal user);

  UnreadCount unreadCount(UserPrincipal user);

  void readAll(String category, UserPrincipal user);

  void readOne(Long id, UserPrincipal user);

  record Notice(
      Long id,
      String category,
      String title,
      String content,
      boolean read,
      LocalDateTime createTime) {}

  record UnreadCount(long system, long audit, long privateMessages) {}
}
