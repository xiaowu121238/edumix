package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.dto.ChatDtos;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 用户私信接口：管理会话列表、历史消息、未读状态以及消息发送。 */
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class ChatController {
  private final ChatService service;

  @GetMapping
  public ApiResponse<List<ChatDtos.Conversation>> list(@AuthenticationPrincipal UserPrincipal me) {
    return ApiResponse.success(service.conversations(me));
  }

  @PostMapping("/with/{teacherId}")
  public ApiResponse<ChatDtos.Conversation> start(
      @PathVariable Long teacherId, @AuthenticationPrincipal UserPrincipal me) {
    return ApiResponse.success(service.start(teacherId, me));
  }

  @GetMapping("/{id}/messages")
  public ApiResponse<ChatDtos.MessagePage> messages(
      @PathVariable Long id,
      @RequestParam(required = false) Long beforeId,
      @RequestParam(defaultValue = "30") int size,
      @AuthenticationPrincipal UserPrincipal me) {
    return ApiResponse.success(service.messages(id, beforeId, size, me));
  }

  @PostMapping("/{id}/messages")
  public ApiResponse<ChatDtos.ChatMessage> send(
      @PathVariable Long id,
      @Valid @RequestBody ChatDtos.SendMessage request,
      @AuthenticationPrincipal UserPrincipal me) {
    return ApiResponse.success(service.send(id, request.content(), me));
  }

  @PostMapping("/{id}/read")
  public ApiResponse<Void> read(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal me) {
    service.markRead(id, me);
    return ApiResponse.success();
  }
}
