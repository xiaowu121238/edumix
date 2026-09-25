package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.dto.CaseResponse;
import com.itxiaowu.edumix.dto.ChatDtos;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.TeachingCaseService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 教师目录接口：提供教师检索、公开主页和发起私信所需的用户信息。 */
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','RESOURCE_MANAGER','ADMIN')")
public class TeacherDirectoryController {
  private final JdbcTemplate jdbc;
  private final TeachingCaseService cases;

  @GetMapping
  public ApiResponse<List<ChatDtos.PublicTeacher>> search(
      @RequestParam(required = false) String keyword) {
    String query = keyword == null ? "" : keyword.trim();
    if (query.length() > 50) throw new IllegalArgumentException("检索词不能超过50字");
    return ApiResponse.success(
        jdbc.query(
            """
            SELECT u.id,u.real_name,u.avatar_url,u.college,u.title FROM sys_user u
            WHERE u.status=1 AND u.deleted=0 AND EXISTS(
              SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id
              WHERE ur.user_id=u.id AND r.role_code='TEACHER')
              AND (?='' OR u.real_name LIKE CONCAT('%',?,'%') OR u.college LIKE CONCAT('%',?,'%'))
            ORDER BY u.id LIMIT 30
            """,
            (rs, n) ->
                new ChatDtos.PublicTeacher(
                    rs.getLong("id"),
                    rs.getString("real_name"),
                    rs.getString("avatar_url"),
                    rs.getString("college"),
                    rs.getString("title")),
            query,
            query,
            query));
  }

  @GetMapping("/{id}")
  public ApiResponse<ChatDtos.TeacherPage> detail(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal viewer) {
    ChatDtos.PublicTeacher teacher =
        jdbc
            .query(
                """
            SELECT u.id,u.real_name,u.avatar_url,u.college,u.title FROM sys_user u
            WHERE u.id=? AND u.status=1 AND u.deleted=0 AND EXISTS(
              SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id
              WHERE ur.user_id=u.id AND r.role_code='TEACHER')
            """,
                (rs, n) ->
                    new ChatDtos.PublicTeacher(
                        rs.getLong("id"),
                        rs.getString("real_name"),
                        rs.getString("avatar_url"),
                        rs.getString("college"),
                        rs.getString("title")),
                id)
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("教师不存在"));
    List<CaseResponse> publicCases =
        cases.list(null, null, "APPROVED", false, viewer).stream()
            .filter(c -> id.equals(c.creatorId()) && "PUBLIC".equals(c.visibility()))
            .toList();
    return ApiResponse.success(new ChatDtos.TeacherPage(teacher, publicCases));
  }
}
