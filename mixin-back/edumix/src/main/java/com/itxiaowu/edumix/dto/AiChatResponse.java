package com.itxiaowu.edumix.dto;

import java.util.List;

/** AI 案例整合响应，返回对话答复和可预览、可编辑的结构化草稿。 */
public record AiChatResponse(String answer, Draft draft) {
  public record Draft(
      String title,
      String summary,
      String content,
      Long courseId,
      List<Long> knowledgePointIds,
      List<Long> resourceIds) {}
}
