package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.dto.SystemNoticeRequest;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.NotificationCenterService;
import com.itxiaowu.edumix.service.NotificationCenterService.Notice;
import com.itxiaowu.edumix.service.NotificationCenterService.UnreadCount;
import com.itxiaowu.edumix.service.SystemNoticeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 消息中心接口：统一提供系统通知、审核通知、私信入口和已读状态管理。 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
  private final NotificationCenterService notifications;
  private final SystemNoticeService systemNotices;

  @GetMapping
  public ApiResponse<List<Notice>> list(@AuthenticationPrincipal UserPrincipal me) {
    return ApiResponse.success(notifications.list(me));
  }

  @GetMapping("/unread-count")
  public ApiResponse<UnreadCount> unreadCount(@AuthenticationPrincipal UserPrincipal me) {
    return ApiResponse.success(notifications.unreadCount(me));
  }

  @PostMapping("/read-all")
  public ApiResponse<Void> readAll(
      @RequestParam(required = false) String category, @AuthenticationPrincipal UserPrincipal me) {
    notifications.readAll(category, me);
    return ApiResponse.success();
  }

  @PostMapping("/{id}/read")
  public ApiResponse<Void> readOne(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal me) {
    notifications.readOne(id, me);
    return ApiResponse.success();
  }

  @PostMapping("/system")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Long> publish(
      @Valid @RequestBody SystemNoticeRequest request,
      @AuthenticationPrincipal UserPrincipal admin) {
    return ApiResponse.success(systemNotices.publish(request, admin));
  }
}
