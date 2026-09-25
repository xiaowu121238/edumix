package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.dto.*;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.AiAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** AI 教学助手接口：提供公共资源推荐和基于已选资料的案例草稿整合对话。 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class AiAssistantController {
  private final AiAssistantService service;

  @GetMapping("/status")
  public ApiResponse<AiStatusResponse> status() {
    return ApiResponse.success(service.status());
  }

  @PostMapping("/recommend")
  public ApiResponse<AiRecommendationResponse> recommend(
      @Valid @RequestBody AiRecommendRequest request, @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.recommend(request, user));
  }

  @PostMapping("/chat")
  public ApiResponse<AiChatResponse> chat(
      @Valid @RequestBody AiChatRequest request, @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.chat(request, user));
  }
}
