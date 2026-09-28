package com.itxiaowu.edumix.service;

/** 审核通知业务接口，负责向资源或案例提交者发送审核结果。 */
public interface AuditNotificationService {
  void result(
      Long recipientId,
      Long reviewerId,
      String kind,
      Long businessId,
      String name,
      boolean approved,
      String comment);
}
