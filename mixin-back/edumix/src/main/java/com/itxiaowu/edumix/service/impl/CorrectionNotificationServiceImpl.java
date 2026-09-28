package com.itxiaowu.edumix.service.impl;

import com.itxiaowu.edumix.mapper.SqlQueryMapper;
import com.itxiaowu.edumix.service.*;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 资源纠错通知服务：同时通知资源上传者和负责人，并跟踪纠错处理结果。 */
@Service
@RequiredArgsConstructor
public class CorrectionNotificationServiceImpl implements CorrectionNotificationService {
  private final SqlQueryMapper jdbc;
  private final NotificationDeliveryService delivery;

  @Override
  public void submitted(
      Long correctionId,
      Long resourceId,
      String resourceName,
      Long uploaderId,
      Long submitterId,
      String submitterName,
      String content) {
    Set<Long> recipients = new LinkedHashSet<>();
    recipients.add(uploaderId);
    recipients.addAll(
        jdbc.query(
            """
                SELECT DISTINCT ur.user_id FROM sys_user_role ur
                JOIN sys_role r ON r.id=ur.role_id JOIN sys_user u ON u.id=ur.user_id
                WHERE r.role_code IN ('RESOURCE_MANAGER','ADMIN') AND u.status=1 AND u.deleted=0
                """,
            (rs, row) -> rs.getLong(1)));
    String body =
        "用户“"
            + submitterName
            + "”对资源《"
            + resourceName
            + "》提交了纠错："
            + content.trim()
            + "。当前状态：等待资源负责人核实。";
    send(recipients, submitterId, "收到新的资源纠错", body, correctionId);
  }

  @Override
  public void handled(
      Long correctionId,
      String resourceName,
      Long submitterId,
      Long uploaderId,
      Long handlerId,
      boolean accepted,
      String comment) {
    String result = accepted ? "纠错已确认属实" : "纠错未被采纳";
    send(
        Set.of(submitterId),
        handlerId,
        result,
        "您对资源《"
            + resourceName
            + "》提交的纠错"
            + (accepted ? "已确认属实。" : "经核实未被采纳。")
            + "处理说明："
            + comment.trim(),
        correctionId);
    if (accepted)
      send(
          Set.of(uploaderId),
          handlerId,
          "资源需要修改",
          "资源《" + resourceName + "》的纠错已由负责人确认属实，请根据处理说明修改并重新提交审核。处理说明：" + comment.trim(),
          correctionId);
  }

  @Override
  public void resolved(
      Long correctionId, String resourceName, Long submitterId, Long uploaderId, Long reviewerId) {
    send(
        Set.of(submitterId, uploaderId),
        reviewerId,
        "资源纠错已完成",
        "资源《" + resourceName + "》已完成修改并通过重新审核，本次纠错已关闭。",
        correctionId);
  }

  private void send(
      Set<Long> recipients, Long senderId, String title, String content, Long correctionId) {
    delivery.send("AUDIT", title, content, senderId, "CORRECTION", correctionId, recipients, true);
  }
}
