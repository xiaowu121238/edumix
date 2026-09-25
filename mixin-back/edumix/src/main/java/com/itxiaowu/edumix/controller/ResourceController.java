package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.dto.*;
import com.itxiaowu.edumix.entity.ResourceCategory;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.ResourceService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 教学资源接口：负责上传、检索、预览、下载、收藏、纠错和重新提交审核。 */
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {
  private final ResourceService resourceService;

  @GetMapping
  public ApiResponse<List<ResourceResponse>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false, defaultValue = "false") Boolean favoriteOnly,
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(resourceService.list(keyword, type, categoryId, favoriteOnly, user));
  }

  @GetMapping("/mine")
  public ApiResponse<List<ResourceResponse>> mine(@AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(resourceService.mine(user));
  }

  @GetMapping("/categories")
  public ApiResponse<List<ResourceCategory>> categories() {
    return ApiResponse.success(resourceService.categories());
  }

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<ResourceResponse> upload(
      @RequestPart("file") MultipartFile file,
      @RequestParam(required = false) String resourceName,
      @RequestParam(required = false) String description,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) String visibility,
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(
        resourceService.upload(file, resourceName, description, categoryId, visibility, user));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<ResourceResponse> update(
      @PathVariable Long id,
      @Valid @RequestBody ResourceUpdateRequest request,
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(resourceService.update(id, request, user));
  }

  @PutMapping(value = "/{id}/resubmit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<ResourceResponse> resubmit(
      @PathVariable Long id,
      @Valid @RequestPart("metadata") ResourceUpdateRequest request,
      @RequestPart(value = "file", required = false) MultipartFile file,
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(resourceService.update(id, request, file, user));
  }

  @GetMapping("/{id}/reviews")
  public ApiResponse<List<ResourceReviewHistoryResponse>> reviewHistory(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(resourceService.reviewHistory(id, user));
  }

  @PostMapping("/{id}/review")
  @PreAuthorize("hasAnyRole('RESOURCE_MANAGER','ADMIN')")
  public ApiResponse<ResourceResponse> review(
      @PathVariable Long id,
      @Valid @RequestBody ResourceReviewRequest request,
      @AuthenticationPrincipal UserPrincipal reviewer) {
    return ApiResponse.success(resourceService.review(id, request, reviewer));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<Void> delete(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
    resourceService.delete(id, user);
    return ApiResponse.success(null);
  }

  @PostMapping("/{id}/favorite")
  public ApiResponse<Boolean> favorite(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(resourceService.toggleFavorite(id, user));
  }

  @PostMapping("/{id}/corrections")
  public ApiResponse<Void> correction(
      @PathVariable Long id,
      @Valid @RequestBody CorrectionRequest request,
      @AuthenticationPrincipal UserPrincipal user) {
    resourceService.submitCorrection(id, request, user);
    return ApiResponse.success(null);
  }

  @GetMapping("/corrections/pending")
  @PreAuthorize("hasAnyRole('RESOURCE_MANAGER','ADMIN')")
  public ApiResponse<List<CorrectionResponse>> pendingCorrections() {
    return ApiResponse.success(resourceService.pendingCorrections());
  }

  @PostMapping("/corrections/{correctionId}/handle")
  @PreAuthorize("hasAnyRole('RESOURCE_MANAGER','ADMIN')")
  public ApiResponse<CorrectionResponse> handleCorrection(
      @PathVariable Long correctionId,
      @Valid @RequestBody CorrectionHandleRequest request,
      @AuthenticationPrincipal UserPrincipal handler) {
    return ApiResponse.success(resourceService.handleCorrection(correctionId, request, handler));
  }

  @GetMapping("/{id}/file")
  public ResponseEntity<org.springframework.core.io.Resource> file(
      @PathVariable Long id,
      @RequestParam(defaultValue = "false") boolean download,
      @AuthenticationPrincipal UserPrincipal user) {
    var file = resourceService.file(id, download, user);
    ContentDisposition disposition =
        (file.download() ? ContentDisposition.attachment() : ContentDisposition.inline())
            .filename(file.filename(), StandardCharsets.UTF_8)
            .build();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .body(file.resource());
  }
}
