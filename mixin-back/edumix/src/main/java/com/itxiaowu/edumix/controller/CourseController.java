package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.dto.*;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.CourseService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {
  private final CourseService courseService;

  @GetMapping("/courses")
  public ApiResponse<List<CourseSummaryResponse>> list(
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(courseService.list(user));
  }

  @GetMapping("/courses/{courseId}/knowledge-tree")
  public ApiResponse<CourseTreeResponse> tree(
      @PathVariable Long courseId, @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(courseService.tree(courseId, user));
  }

  @PostMapping("/courses")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<CourseSummaryResponse> create(
      @Valid @RequestBody CourseRequest request, @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(courseService.create(request, user));
  }

  @PutMapping("/courses/{id}")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<CourseSummaryResponse> update(
      @PathVariable Long id,
      @Valid @RequestBody CourseRequest request,
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(courseService.update(id, request, user));
  }

  @DeleteMapping("/courses/{id}")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<Void> delete(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
    courseService.delete(id, user);
    return ApiResponse.success(null);
  }

  @PostMapping("/courses/{courseId}/chapters")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<ChapterTreeResponse> createChapter(
      @PathVariable Long courseId,
      @Valid @RequestBody ChapterRequest request,
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(courseService.createChapter(courseId, request, user));
  }

  @PutMapping("/chapters/{id}")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<ChapterTreeResponse> updateChapter(
      @PathVariable Long id,
      @Valid @RequestBody ChapterRequest request,
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(courseService.updateChapter(id, request, user));
  }

  @DeleteMapping("/chapters/{id}")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<Void> deleteChapter(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
    courseService.deleteChapter(id, user);
    return ApiResponse.success(null);
  }

  @PostMapping("/chapters/{chapterId}/knowledge-points")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<KnowledgePointResponse> createPoint(
      @PathVariable Long chapterId,
      @Valid @RequestBody KnowledgePointRequest request,
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(courseService.createPoint(chapterId, request, user));
  }

  @PutMapping("/knowledge-points/{id}")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<KnowledgePointResponse> updatePoint(
      @PathVariable Long id,
      @Valid @RequestBody KnowledgePointRequest request,
      @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(courseService.updatePoint(id, request, user));
  }

  @DeleteMapping("/knowledge-points/{id}")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<Void> deletePoint(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
    courseService.deletePoint(id, user);
    return ApiResponse.success(null);
  }

  @GetMapping("/knowledge-points/{id}/resources")
  public ApiResponse<List<KnowledgePointResourceResponse>> linkedResources(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(courseService.linkedResources(id, user));
  }

  @GetMapping("/knowledge-points/{id}/resource-candidates")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<List<ResourceResponse>> resourceCandidates(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
    return ApiResponse.success(courseService.favoriteCandidates(id, user));
  }

  @PostMapping("/knowledge-points/{id}/resources")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<Void> linkResources(
      @PathVariable Long id,
      @RequestBody List<Long> resourceIds,
      @AuthenticationPrincipal UserPrincipal user) {
    courseService.linkResources(id, resourceIds, user);
    return ApiResponse.success(null);
  }

  @DeleteMapping("/knowledge-points/{id}/resources/{resourceId}")
  @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
  public ApiResponse<Void> unlinkResource(
      @PathVariable Long id,
      @PathVariable Long resourceId,
      @AuthenticationPrincipal UserPrincipal user) {
    courseService.unlinkResource(id, resourceId, user);
    return ApiResponse.success(null);
  }
}
