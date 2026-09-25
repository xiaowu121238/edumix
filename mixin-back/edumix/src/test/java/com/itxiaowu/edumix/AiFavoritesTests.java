package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.*;

import com.itxiaowu.edumix.dto.AiRecommendRequest;
import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.AiAssistantService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AiFavoritesTests {
  @Autowired AiAssistantService ai;
  @Autowired JdbcTemplate jdbc;

  @Test
  void recommendationUsesWholePublicLibraryWithoutRequiringFavorites() {
    UserPrincipal first = user(), second = user();
    Long resourceId = resource(first.getUserId());
    Long caseId = teachingCase(first.getUserId());
    Long privateResourceId = resource(first.getUserId(), "PRIVATE", "APPROVED");
    Long pendingCaseId = teachingCase(first.getUserId(), "PUBLIC", "PENDING");
    var catalog = ai.recommendationCatalog(null, second);
    assertTrue(catalog.resources().stream().anyMatch(item -> item.id().equals(resourceId)));
    assertTrue(catalog.cases().stream().anyMatch(item -> item.id().equals(caseId)));
    assertTrue(catalog.resources().stream().noneMatch(item -> item.id().equals(privateResourceId)));
    assertTrue(catalog.cases().stream().noneMatch(item -> item.id().equals(pendingCaseId)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ai.recommend(
                new AiRecommendRequest(null, List.of(privateResourceId), List.of(), "参考该资源推荐", 6),
                second));
  }

  private UserPrincipal user() {
    String username =
        "ai_fav_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    jdbc.update(
        "INSERT INTO sys_user(username,password,real_name,status,deleted) VALUES(?,?,?,1,0)",
        username,
        "test-only",
        username);
    Long id = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, username);
    jdbc.update(
        "INSERT INTO sys_user_role(user_id,role_id) SELECT ?,id FROM sys_role WHERE role_code='TEACHER'",
        id);
    SysUser record = new SysUser();
    record.setId(id);
    record.setUsername(username);
    record.setStatus(1);
    record.setRealName(username);
    return new UserPrincipal(record, List.of("TEACHER"));
  }

  private Long resource(Long authorId) {
    return resource(authorId, "PUBLIC", "APPROVED");
  }

  private Long resource(Long authorId, String visibility, String status) {
    String number = "RES-AI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    jdbc.update(
        """
            INSERT INTO teaching_resource(resource_no,resource_name,resource_type,file_url,file_size,uploader_id,visibility,audit_status)
            VALUES(?,'AI推荐测试资源','PDF','test-only.pdf',1,?,?,?)
            """,
        number,
        authorId,
        visibility,
        status);
    return jdbc.queryForObject(
        "SELECT id FROM teaching_resource WHERE resource_no=?", Long.class, number);
  }

  private Long teachingCase(Long authorId) {
    return teachingCase(authorId, "PUBLIC", "APPROVED");
  }

  private Long teachingCase(Long authorId, String visibility, String status) {
    String number = "CASE-AI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    jdbc.update(
        """
            INSERT INTO teaching_case(case_no,case_name,creator_id,visibility,access_type,audit_status)
            VALUES(?,'AI推荐测试案例',?,?,'FREE',?)
            """,
        number,
        authorId,
        visibility,
        status);
    return jdbc.queryForObject("SELECT id FROM teaching_case WHERE case_no=?", Long.class, number);
  }
}
