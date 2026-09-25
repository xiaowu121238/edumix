package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
  private final DashboardService service;

  @GetMapping("/summary")
  public ApiResponse<DashboardService.Summary> summary(
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.summary(user));
  }
}
