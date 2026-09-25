package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.*;

import com.itxiaowu.edumix.dto.CaseDraftRequest;
import com.itxiaowu.edumix.dto.CaseReviewRequest;
import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.TeachingCaseService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CaseDraftWorkflowTests {
  @Autowired TeachingCaseService cases;
  @Autowired JdbcTemplate jdbc;

  @Test
  void incompleteDraftStaysPersonalUntilReviewApproval() {
    UserPrincipal author = user("TEACHER"),
        viewer = user("TEACHER"),
        reviewer = user("TEACHER", "RESOURCE_MANAGER");
    Long id = cases.createDraft(draft("", null, List.of(), ""), author).id();
    assertEquals("DRAFT", cases.get(id, author).auditStatus());
    assertTrue(cases.mine(author).stream().anyMatch(c -> c.id().equals(id)));
    assertTrue(cases.mine(viewer).stream().noneMatch(c -> c.id().equals(id)));
    assertTrue(cases.published(null, null, viewer).stream().noneMatch(c -> c.id().equals(id)));
    assertThrows(IllegalArgumentException.class, () -> cases.submit(id, author));
    assertThrows(
        AccessDeniedException.class,
        () -> cases.updateDraft(id, draft("越权", null, List.of(), ""), viewer));

    Long course = course(author.getUserId()), point = point(course);
    cases.updateDraft(
        id, draft("完整案例", course, List.of(point), "<h2>教学过程</h2><p>详细内容</p>"), author);
    assertEquals("PENDING", cases.submit(id, author).auditStatus());
    assertTrue(cases.published(null, null, viewer).stream().noneMatch(c -> c.id().equals(id)));
    assertEquals(
        "APPROVED",
        cases.review(id, new CaseReviewRequest("APPROVED", "通过", false), reviewer).auditStatus());
    assertTrue(cases.published(null, null, viewer).stream().anyMatch(c -> c.id().equals(id)));
  }

  @Test
  void legacyPrivateCaseIsPreservedButNotPublic() {
    UserPrincipal author = user("TEACHER"), viewer = user("TEACHER");
    String number = "CASE-OLD-" + UUID.randomUUID().toString().replace("-", "");
    jdbc.update(
        "INSERT INTO teaching_case(case_no,case_name,creator_id,visibility,access_type,audit_status) VALUES(?,'旧私有案例',?,'PRIVATE','FREE','APPROVED')",
        number,
        author.getUserId());
    Long id =
        jdbc.queryForObject("SELECT id FROM teaching_case WHERE case_no=?", Long.class, number);
    assertTrue(cases.mine(author).stream().anyMatch(c -> c.id().equals(id)));
    assertTrue(cases.published(null, null, viewer).stream().noneMatch(c -> c.id().equals(id)));
    assertThrows(AccessDeniedException.class, () -> cases.get(id, viewer));
    assertEquals(
        "DRAFT", cases.updateDraft(id, draft("旧私有案例", null, List.of(), ""), author).auditStatus());
  }

  @Test
  void oldSchoolCaseIsNotPublicAndCanReturnToDraft() {
    UserPrincipal author = user("TEACHER"), viewer = user("TEACHER");
    String number = "CASE-OLD-SCOPE-" + UUID.randomUUID().toString().replace("-", "");
    jdbc.update(
        "INSERT INTO teaching_case(case_no,case_name,creator_id,visibility,access_type,audit_status) VALUES(?,'旧范围案例',?,'SCHOOL','FREE','APPROVED')",
        number,
        author.getUserId());
    Long id =
        jdbc.queryForObject("SELECT id FROM teaching_case WHERE case_no=?", Long.class, number);
    assertThrows(AccessDeniedException.class, () -> cases.get(id, viewer));
    assertTrue(cases.published(null, null, viewer).stream().noneMatch(c -> c.id().equals(id)));
    assertEquals(
        "DRAFT", cases.updateDraft(id, draft("旧范围案例", null, List.of(), ""), author).auditStatus());
    assertEquals("PUBLIC", cases.get(id, author).visibility());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            cases.createDraft(
                new CaseDraftRequest(
                    "错误范围", null, List.of(), null, null, List.of(), "COLLEGE", "FREE", 0),
                author));
  }

  @Test
  void paidCasePriceMustBeOneToThreeBeforeSubmission() {
    UserPrincipal author = user("TEACHER");
    Long course = course(author.getUserId()), knowledgePoint = point(course);
    CaseDraftRequest expensive =
        new CaseDraftRequest(
            "收费案例",
            course,
            List.of(knowledgePoint),
            null,
            "<p>正文</p>",
            List.of(),
            "PUBLIC",
            "POINTS",
            4);
    Long id = cases.createDraft(expensive, author).id();
    assertThrows(IllegalArgumentException.class, () -> cases.submit(id, author));
    CaseDraftRequest valid =
        new CaseDraftRequest(
            "收费案例",
            course,
            List.of(knowledgePoint),
            null,
            "<p>正文</p>",
            List.of(),
            "PUBLIC",
            "POINTS",
            3);
    cases.updateDraft(id, valid, author);
    assertEquals("PENDING", cases.submit(id, author).auditStatus());
  }

  private CaseDraftRequest draft(String title, Long course, List<Long> points, String content) {
    return new CaseDraftRequest(
        title, course, points, null, content, List.of(), "PUBLIC", "FREE", 0);
  }

  private UserPrincipal user(String... roles) {
    String name = "case_draft_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    jdbc.update(
        "INSERT INTO sys_user(username,password,real_name,status,deleted) VALUES(?,?,?,1,0)",
        name,
        "test-only",
        name);
    Long id = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, name);
    SysUser record = new SysUser();
    record.setId(id);
    record.setUsername(name);
    record.setRealName(name);
    record.setStatus(1);
    return new UserPrincipal(record, List.of(roles));
  }

  private Long course(Long ownerId) {
    String code = "CASE-DRAFT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    jdbc.update(
        "INSERT INTO course(course_code,course_name,owner_id) VALUES(?,'草稿测试课程',?)", code, ownerId);
    return jdbc.queryForObject("SELECT id FROM course WHERE course_code=?", Long.class, code);
  }

  private Long point(Long courseId) {
    jdbc.update("INSERT INTO course_chapter(course_id,chapter_name) VALUES(?,'测试章节')", courseId);
    Long chapterId =
        jdbc.queryForObject(
            "SELECT id FROM course_chapter WHERE course_id=? ORDER BY id DESC LIMIT 1",
            Long.class,
            courseId);
    jdbc.update(
        "INSERT INTO knowledge_point(course_id,chapter_id,point_name) VALUES(?,?,'测试知识点')",
        courseId,
        chapterId);
    return jdbc.queryForObject(
        "SELECT id FROM knowledge_point WHERE chapter_id=? ORDER BY id DESC LIMIT 1",
        Long.class,
        chapterId);
  }
}
