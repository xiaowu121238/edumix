package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.PointService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 积分接口：处理余额查询、每日签到、积分流水、案例解锁和管理员发放。 */
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {
  private final PointService points;

  public record GrantRequest(
      @NotNull Long recipientId,
      @NotNull @Min(1) @Max(1000) Integer amount,
      @NotBlank @Size(max = 500) String reason,
      @NotBlank String password) {}

  @GetMapping("/wallet")
  public ApiResponse<PointService.Wallet> wallet(@AuthenticationPrincipal UserPrincipal me) {
    return ApiResponse.success(points.wallet(me.getUserId()));
  }

  @GetMapping("/history")
  public ApiResponse<List<PointService.Entry>> history(@AuthenticationPrincipal UserPrincipal me) {
    return ApiResponse.success(points.history(me.getUserId()));
  }

  @GetMapping("/check-ins")
  public ApiResponse<List<LocalDate>> checkIns(
      @RequestParam int year, @RequestParam int month, @AuthenticationPrincipal UserPrincipal me) {
    return ApiResponse.success(points.checkInDates(me.getUserId(), year, month));
  }

  @PostMapping("/check-in")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<PointService.Wallet> checkIn(@AuthenticationPrincipal UserPrincipal me) {
    return ApiResponse.success(points.checkIn(me.getUserId()));
  }

  @PostMapping("/grant")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<PointService.Wallet> grant(
      @Valid @RequestBody GrantRequest request, @AuthenticationPrincipal UserPrincipal me) {
    return ApiResponse.success(
        points.grant(
            me.getUserId(),
            request.recipientId(),
            request.amount(),
            request.reason(),
            request.password()));
  }
}
