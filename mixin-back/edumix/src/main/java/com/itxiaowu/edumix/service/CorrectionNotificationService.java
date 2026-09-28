package com.itxiaowu.edumix.service;

/** 资源纠错通知业务接口，定义纠错提交、处理和关闭通知。 */
public interface CorrectionNotificationService {
  void submitted(
      Long correctionId,
      Long resourceId,
      String resourceName,
      Long uploaderId,
      Long submitterId,
      String submitterName,
      String content);

  void handled(
      Long correctionId,
      String resourceName,
      Long submitterId,
      Long uploaderId,
      Long handlerId,
      boolean accepted,
      String comment);

  void resolved(
      Long correctionId, String resourceName, Long submitterId, Long uploaderId, Long reviewerId);
}
