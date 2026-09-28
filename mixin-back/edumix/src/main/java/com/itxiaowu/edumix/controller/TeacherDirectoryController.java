package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.dto.ChatDtos;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.TeacherDirectoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 教师目录接口：提供教师检索、公开主页和发起私信所需的用户信息。 */
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','RESOURCE_MANAGER','ADMIN')")
public class TeacherDirectoryController {
  private final TeacherDirectoryService teachers;

  @GetMapping
  public ApiResponse<List<ChatDtos.PublicTeacher>> search(
      @RequestParam(required = false) String keyword) {
    return ApiResponse.success(teachers.search(keyword));
  }

  @GetMapping("/{id}")
  public ApiResponse<ChatDtos.TeacherPage> detail(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal viewer) {
    return ApiResponse.success(teachers.detail(id, viewer));
  }
}
