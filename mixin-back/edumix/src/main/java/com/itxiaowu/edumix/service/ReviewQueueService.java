package com.itxiaowu.edumix.service;

import java.time.LocalDateTime;
import java.util.List;

/** 审核队列业务接口，统一汇总待审核资源和待审核案例。 */
public interface ReviewQueueService {
  List<PendingItem> pending();

  record PendingItem(
      String kind,
      Long id,
      String title,
      String summary,
      String author,
      Long authorId,
      String visibility,
      String resourceType,
      LocalDateTime submittedAt) {}
}
