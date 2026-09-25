package com.itxiaowu.edumix.dto;

import java.util.List;

public record AiChatResponse(String answer, Draft draft) {
  public record Draft(
      String title,
      String summary,
      String content,
      Long courseId,
      List<Long> knowledgePointIds,
      List<Long> resourceIds) {}
}
