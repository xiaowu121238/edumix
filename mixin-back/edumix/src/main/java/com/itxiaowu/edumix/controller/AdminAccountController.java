package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.dto.AccountDtos;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 管理员账号接口：创建负责人或管理员、调整角色、停用账号以及重置密码。 */
@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountController {
  private final AccountService accounts;

  @GetMapping
  public ApiResponse<List<AccountDtos.Account>> list() {
    return ApiResponse.success(accounts.list());
  }

  @PostMapping
  public ApiResponse<AccountDtos.Account> create(
      @Valid @RequestBody AccountDtos.CreateUserRequest request) {
    return ApiResponse.success(accounts.createManaged(request));
  }

  @PutMapping("/{id}/roles")
  public ApiResponse<AccountDtos.Account> roles(
      @PathVariable Long id,
      @RequestBody AccountDtos.RolesRequest request,
      @AuthenticationPrincipal UserPrincipal admin) {
    return ApiResponse.success(accounts.setRoles(id, request.roles(), admin.getUserId()));
  }

  @PutMapping("/{id}/status")
  public ApiResponse<AccountDtos.Account> status(
      @PathVariable Long id,
      @RequestBody AccountDtos.StatusRequest request,
      @AuthenticationPrincipal UserPrincipal admin) {
    return ApiResponse.success(accounts.setStatus(id, request.status(), admin.getUserId()));
  }

  @PostMapping("/{id}/reset-password")
  public ApiResponse<Void> resetPassword(
      @PathVariable Long id, @RequestBody AccountDtos.ResetPasswordRequest request) {
    accounts.resetPassword(id, request.password());
    return ApiResponse.success();
  }
}
