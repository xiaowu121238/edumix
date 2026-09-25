package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.dto.*;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.TeachingCaseService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 教学案例接口：管理本地草稿、发布审核、详情查看、收藏及积分解锁。 */
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class TeachingCaseController {
  private final TeachingCaseService service;

  @GetMapping
  public ApiResponse<List<CaseResponse>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long courseId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false, defaultValue = "false") Boolean favoriteOnly,
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.list(keyword, courseId, status, favoriteOnly, user));
  }

  @GetMapping("/published")
  public ApiResponse<List<CaseResponse>> published(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long courseId,
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.published(keyword, courseId, user));
  }

  @GetMapping("/mine")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<List<CaseResponse>> mine(@AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.mine(user));
  }

  @PostMapping("/drafts")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<CaseResponse> createDraft(
      @Valid @RequestBody CaseDraftRequest request, @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.createDraft(request, user));
  }

  @PutMapping("/{id}/draft")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<CaseResponse> updateDraft(
      @PathVariable Long id,
      @Valid @RequestBody CaseDraftRequest request,
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.updateDraft(id, request, user));
  }

  @GetMapping("/{id}")
  public ApiResponse<CaseResponse> get(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.get(id, user));
  }

  @PostMapping("/{id}/unlock")
  public ApiResponse<CaseResponse> unlock(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.unlock(id, user));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<CaseResponse> create(
      @Valid @RequestBody CaseRequest request, @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.create(request, user));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<CaseResponse> update(
      @PathVariable Long id,
      @Valid @RequestBody CaseRequest request,
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.update(id, request, user));
  }

  @PostMapping("/{id}/submit")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<CaseResponse> submit(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.submit(id, user));
  }

  @PostMapping("/{id}/review")
  @PreAuthorize("hasAnyRole('RESOURCE_MANAGER','ADMIN')")
  public ApiResponse<CaseResponse> review(
      @PathVariable Long id,
      @Valid @RequestBody CaseReviewRequest request,
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.review(id, request, user));
  }

  @PostMapping("/{id}/favorite")
  public ApiResponse<Boolean> favorite(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(service.toggleFavorite(id, user));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<Void> delete(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
    service.delete(id, user);
    return ApiResponse.success(null);
  }
}
