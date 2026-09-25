package com.itxiaowu.edumix.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditNotificationService {
  private final NotificationDeliveryService delivery;

  public void result(
      Long recipientId,
      Long reviewerId,
      String kind,
      Long businessId,
      String name,
      boolean approved,
      String comment) {
    String label = "CASE".equals(kind) ? "案例" : "资源";
    String title = label + (approved ? "审核通过" : "审核未通过");
    String content =
        "您提交的"
            + label
            + "《"
            + name
            + "》"
            + (approved ? "已通过审核。" : "未通过审核。")
            + (comment == null || comment.isBlank() ? "" : "审核意见：" + comment.trim());
    delivery.send(
        "AUDIT", title, content, reviewerId, kind, businessId, Set.of(recipientId), false);
  }
}
