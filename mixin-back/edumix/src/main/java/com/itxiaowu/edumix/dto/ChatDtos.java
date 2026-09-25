package com.itxiaowu.edumix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** 教师主页、会话列表和私信消息相关传输对象的集合。 */
public final class ChatDtos {
  private ChatDtos() {}

  public record PublicTeacher(
      Long id, String realName, String avatarUrl, String college, String title) {}

  public record TeacherPage(PublicTeacher teacher, List<CaseResponse> cases) {}

  public record Conversation(
      Long id,
      PublicTeacher other,
      String lastMessage,
      LocalDateTime lastMessageAt,
      long unreadCount) {}

  public record ChatMessage(
      Long id, Long conversationId, Long senderId, String content, LocalDateTime createTime) {}

  public record MessagePage(List<ChatMessage> items, Long nextBeforeId) {}

  public record SendMessage(
      @NotBlank(message = "消息不能为空") @Size(max = 2000, message = "消息不能超过2000字") String content) {}
}
