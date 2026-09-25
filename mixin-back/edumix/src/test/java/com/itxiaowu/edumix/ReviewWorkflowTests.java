package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.*;

import com.itxiaowu.edumix.controller.ReviewQueueController;
import com.itxiaowu.edumix.dto.CaseReviewRequest;
import com.itxiaowu.edumix.dto.ResourceReviewRequest;
import com.itxiaowu.edumix.dto.ResourceUpdateRequest;
import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.ResourceService;
import com.itxiaowu.edumix.service.TeachingCaseService;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ReviewWorkflowTests {
  @Autowired ResourceService resources;
  @Autowired TeachingCaseService cases;
  @Autowired ReviewQueueController queue;
  @Autowired JdbcTemplate jdbc;

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void pendingResourceIsHiddenUntilAnotherReviewerApprovesIt() {
    UserPrincipal author = user("TEACHER"),
        reviewer = user("TEACHER", "RESOURCE_MANAGER"),
        viewer = user("TEACHER");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(reviewer, null, reviewer.getAuthorities()));
    Long id = resource(author.getUserId());
    assertTrue(
        queue.pending().data().stream()
            .anyMatch(item -> item.kind().equals("RESOURCE") && item.id().equals(id)));
    assertTrue(
        resources.list(null, null, null, false, viewer).stream()
            .noneMatch(item -> item.id().equals(id)));
    assertThrows(
        IllegalArgumentException.class,
        () -> resources.review(id, new ResourceReviewRequest("APPROVED", null), author));
    var approved = resources.review(id, new ResourceReviewRequest("APPROVED", "适合共享"), reviewer);
    assertEquals("APPROVED", approved.auditStatus());
    assertTrue(
        resources.list(null, null, null, false, viewer).stream()
            .anyMatch(item -> item.id().equals(id)));
    assertTrue(
        queue.pending().data().stream()
            .noneMatch(item -> item.kind().equals("RESOURCE") && item.id().equals(id)));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM resource_review WHERE resource_id=?", Integer.class, id));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_message um JOIN message m ON m.id=um.message_id WHERE um.user_id=? AND m.business_type='RESOURCE' AND m.business_id=?",
            Integer.class,
            author.getUserId(),
            id));
  }

  @Test
  void rejectionRequiresReasonAndKeepsResourcePrivateToAuthorAndReviewer() {
    UserPrincipal author = user("TEACHER"),
        reviewer = user("TEACHER", "RESOURCE_MANAGER"),
        viewer = user("TEACHER");
    Long id = resource(author.getUserId());
    assertThrows(
        IllegalArgumentException.class,
        () -> resources.review(id, new ResourceReviewRequest("REJECTED", " "), reviewer));
    assertEquals(
        "REJECTED",
        resources
            .review(id, new ResourceReviewRequest("REJECTED", "内容需要修订"), reviewer)
            .auditStatus());
    assertTrue(
        resources.list(null, null, null, false, viewer).stream()
            .noneMatch(item -> item.id().equals(id)));
    assertTrue(
        resources.list(null, null, null, false, author).stream()
            .anyMatch(item -> item.id().equals(id)));
  }

  @Test
  void oldSchoolResourceIsPrivateToUploaderAndOldScopeCannotBeSubmitted() {
    UserPrincipal author = user("TEACHER"), viewer = user("TEACHER");
    Long id = resource(author.getUserId());
    jdbc.update(
        "UPDATE teaching_resource SET visibility='SCHOOL',audit_status='APPROVED' WHERE id=?", id);
    assertTrue(
        resources.list(null, null, null, false, viewer).stream()
            .noneMatch(item -> item.id().equals(id)));
    assertThrows(AccessDeniedException.class, () -> resources.file(id, false, viewer));
    assertTrue(resources.mine(author).stream().anyMatch(item -> item.id().equals(id)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            resources.update(
                id, new ResourceUpdateRequest("旧范围资源", null, null, "COLLEGE"), author));
    assertEquals(
        "PRIVATE",
        resources
            .update(id, new ResourceUpdateRequest("旧范围资源", null, null, "PRIVATE"), author)
            .visibility());
  }

  @Test
  void editingApprovedResourceHidesItUntilReplacementPassesReview(@TempDir Path uploadDirectory) {
    UserPrincipal author = user("TEACHER"),
        reviewer = user("TEACHER", "RESOURCE_MANAGER"),
        viewer = user("TEACHER");
    Long id = resource(author.getUserId());
    String originalStorage = (String) ReflectionTestUtils.getField(resources, "storageLocation");
    ReflectionTestUtils.setField(resources, "storageLocation", uploadDirectory.toString());
    try {
      resources.review(id, new ResourceReviewRequest("APPROVED", "初次通过"), reviewer);
      assertTrue(
          resources.list(null, null, null, false, viewer).stream()
              .anyMatch(item -> item.id().equals(id)));

      var metadata = new ResourceUpdateRequest("修订后的资源", "补充了说明", null, "PUBLIC");
      assertEquals("PENDING", resources.update(id, metadata, author).auditStatus());
      assertTrue(
          resources.list(null, null, null, false, viewer).stream()
              .noneMatch(item -> item.id().equals(id)));
      assertThrows(AccessDeniedException.class, () -> resources.file(id, false, viewer));
      assertThrows(IllegalArgumentException.class, () -> resources.update(id, metadata, author));

      resources.review(id, new ResourceReviewRequest("REJECTED", "请替换文件"), reviewer);
      var replacement =
          new MockMultipartFile(
              "file", "revised.pdf", "application/pdf", "new pdf content".getBytes());
      var submitted = resources.update(id, metadata, replacement, author);
      assertEquals("PENDING", submitted.auditStatus());
      assertEquals((long) replacement.getSize(), submitted.fileSize());
      assertTrue(
          resources.list(null, null, null, false, viewer).stream()
              .noneMatch(item -> item.id().equals(id)));
      assertEquals(2, resources.reviewHistory(id, author).size());
      assertEquals("请替换文件", resources.reviewHistory(id, author).get(0).comment());
      assertThrows(AccessDeniedException.class, () -> resources.reviewHistory(id, viewer));

      resources.review(id, new ResourceReviewRequest("APPROVED", "修订通过"), reviewer);
      assertTrue(
          resources.list(null, null, null, false, viewer).stream()
              .anyMatch(item -> item.id().equals(id)));
      assertEquals(3, resources.reviewHistory(id, author).size());
    } finally {
      ReflectionTestUtils.setField(resources, "storageLocation", originalStorage);
    }
  }

  @Test
  void pendingCaseBecomesVisibleOnlyAfterDifferentReviewerApprovesIt() {
    UserPrincipal author = user("TEACHER"),
        reviewer = user("TEACHER", "RESOURCE_MANAGER"),
        viewer = user("TEACHER");
    Long id = teachingCase(author.getUserId());
    assertTrue(
        cases.list(null, null, null, false, viewer).stream()
            .noneMatch(item -> item.id().equals(id)));
    assertThrows(
        IllegalArgumentException.class,
        () -> cases.review(id, new CaseReviewRequest("APPROVED", null, false), author));
    assertThrows(
        IllegalArgumentException.class,
        () -> cases.review(id, new CaseReviewRequest("REJECTED", " ", false), reviewer));
    assertEquals(
        "APPROVED",
        cases.review(id, new CaseReviewRequest("APPROVED", "内容合格", false), reviewer).auditStatus());
    assertTrue(
        cases.list(null, null, null, false, viewer).stream()
            .anyMatch(item -> item.id().equals(id)));
    assertEquals(
        1,
        jdbc.queryForObject("SELECT COUNT(*) FROM case_review WHERE case_id=?", Integer.class, id));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_message um JOIN message m ON m.id=um.message_id WHERE um.user_id=? AND m.business_type='CASE' AND m.business_id=?",
            Integer.class,
            author.getUserId(),
            id));
  }

  private UserPrincipal user(String... roles) {
    String username =
        "audit_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    jdbc.update(
        "INSERT INTO sys_user(username,password,real_name,status,deleted) VALUES(?,?,?,1,0)",
        username,
        "test-only",
        username);
    Long id = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, username);
    for (String role : roles)
      jdbc.update(
          "INSERT INTO sys_user_role(user_id,role_id) SELECT ?,id FROM sys_role WHERE role_code=?",
          id,
          role);
    SysUser record = new SysUser();
    record.setId(id);
    record.setUsername(username);
    record.setStatus(1);
    record.setRealName(username);
    return new UserPrincipal(record, List.of(roles));
  }

  private Long resource(Long authorId) {
    String number = "RES-TEST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    jdbc.update(
        """
            INSERT INTO teaching_resource(resource_no,resource_name,resource_type,file_url,file_size,uploader_id,visibility,audit_status)
            VALUES(?,'审核流程测试资源','PDF','test-only.pdf',1,?,'PUBLIC','PENDING')
            """,
        number,
        authorId);
    return jdbc.queryForObject(
        "SELECT id FROM teaching_resource WHERE resource_no=?", Long.class, number);
  }

  private Long teachingCase(Long authorId) {
    String number = "CASE-TEST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    jdbc.update(
        """
            INSERT INTO teaching_case(case_no,case_name,creator_id,visibility,access_type,audit_status)
            VALUES(?,'审核流程测试案例',?,'PUBLIC','FREE','PENDING')
            """,
        number,
        authorId);
    return jdbc.queryForObject("SELECT id FROM teaching_case WHERE case_no=?", Long.class, number);
  }
}
