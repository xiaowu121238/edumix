package com.itxiaowu.edumix.service;

import java.util.Collection;

/** 站内消息投递接口，统一维护消息主体和用户收件记录。 */
public interface NotificationDeliveryService {
  long send(
      String messageType,
      String title,
      String content,
      Long senderId,
      String businessType,
      Long businessId,
      Collection<Long> recipientIds,
      boolean excludeSender);

  long broadcastSystem(String title, String content, Long senderId);
}
