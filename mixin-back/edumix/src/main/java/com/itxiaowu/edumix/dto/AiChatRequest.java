package com.itxiaowu.edumix.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** AI 案例整合对话请求，包含本轮问题、历史消息和已选资源案例。 */
public record AiChatRequest(
    @NotBlank(message = "请输入要与AI讨论的内容") @Size(max = 4000, message = "单次消息不能超过4000字") String message,
    List<Long> resourceIds,
    List<Long> caseIds,
    @Size(max = 10, message = "最多携带10条历史消息") List<@Valid HistoryMessage> history,
    @Valid DraftContext currentDraft) {

  public record HistoryMessage(
      @NotBlank(message = "消息角色不能为空") String role,
      @NotBlank(message = "历史消息不能为空") @Size(max = 4000, message = "单条历史消息不能超过4000字")
          String content) {}

  public record DraftContext(
      @NotBlank(message = "草稿标题不能为空") @Size(max = 200, message = "草稿标题不能超过200字") String title,
      @Size(max = 1000, message = "草稿摘要不能超过1000字") String summary,
      @NotBlank(message = "草稿正文不能为空") @Size(max = 30000, message = "草稿正文不能超过30000字") String content,
      Long courseId,
      @Size(max = 100, message = "知识点数量不能超过100个") List<Long> knowledgePointIds,
      @Size(max = 100, message = "资源数量不能超过100个") List<Long> resourceIds) {}
}
