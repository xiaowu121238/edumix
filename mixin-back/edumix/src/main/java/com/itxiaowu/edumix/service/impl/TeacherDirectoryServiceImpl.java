package com.itxiaowu.edumix.service.impl;

import com.itxiaowu.edumix.dto.CaseResponse;
import com.itxiaowu.edumix.dto.ChatDtos;
import com.itxiaowu.edumix.mapper.SqlQueryMapper;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.TeacherDirectoryService;
import com.itxiaowu.edumix.service.TeachingCaseService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 教师目录业务实现，查询启用教师及其公开案例。 */
@Service
@RequiredArgsConstructor
public class TeacherDirectoryServiceImpl implements TeacherDirectoryService {
  private final SqlQueryMapper sql;
  private final TeachingCaseService cases;

  @Override
  public List<ChatDtos.PublicTeacher> search(String keyword) {
    String query = keyword == null ? "" : keyword.trim();
    if (query.length() > 50) throw new IllegalArgumentException("检索词不能超过50字");
    return sql.query(
        """
        SELECT u.id,u.real_name,u.avatar_url,u.college,u.title FROM sys_user u
        WHERE u.status=1 AND u.deleted=0 AND EXISTS(
          SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id
          WHERE ur.user_id=u.id AND r.role_code='TEACHER')
          AND (?='' OR u.real_name LIKE CONCAT('%',?,'%') OR u.college LIKE CONCAT('%',?,'%'))
        ORDER BY u.id LIMIT 30
        """,
        (rs, rowNum) -> teacher(rs), query, query, query);
  }

  @Override
  public ChatDtos.TeacherPage detail(Long id, UserPrincipal viewer) {
    ChatDtos.PublicTeacher teacher =
        sql
            .query(
                """
                SELECT u.id,u.real_name,u.avatar_url,u.college,u.title FROM sys_user u
                WHERE u.id=? AND u.status=1 AND u.deleted=0 AND EXISTS(
                  SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id
                  WHERE ur.user_id=u.id AND r.role_code='TEACHER')
                """,
                (rs, rowNum) -> teacher(rs),
                id)
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("教师不存在"));
    List<CaseResponse> publicCases =
        cases.list(null, null, "APPROVED", false, viewer).stream()
            .filter(item -> id.equals(item.creatorId()) && "PUBLIC".equals(item.visibility()))
            .toList();
    return new ChatDtos.TeacherPage(teacher, publicCases);
  }

  private ChatDtos.PublicTeacher teacher(java.sql.ResultSet result) throws java.sql.SQLException {
    return new ChatDtos.PublicTeacher(
        result.getLong("id"),
        result.getString("real_name"),
        result.getString("avatar_url"),
        result.getString("college"),
        result.getString("title"));
  }
}
