package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.*;

import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.DashboardService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DashboardServiceTests {
  @Autowired DashboardService dashboard;
  @Autowired JdbcTemplate jdbc;
  @Autowired SqlSessionTemplate sqlSession;

  @Test
  void showsPersonalCountsAndOnlyActionableReviewTasks() {
    UserPrincipal teacher = user(List.of("TEACHER"));
    UserPrincipal reviewer = user(List.of("TEACHER", "RESOURCE_MANAGER"));
    var reviewBaseline = dashboard.summary(reviewer);
    Long courseId = course(teacher.getUserId());
    jdbc.update("INSERT INTO course_chapter(course_id,chapter_name) VALUES(?,'第一章')", courseId);
    Long chapterId =
        jdbc.queryForObject(
            "SELECT id FROM course_chapter WHERE course_id=? ORDER BY id DESC LIMIT 1",
            Long.class,
            courseId);
    jdbc.update(
        "INSERT INTO knowledge_point(course_id,chapter_id,point_name) VALUES(?,?,'知识点')",
        courseId,
        chapterId);
    Long resourceId = resource(teacher.getUserId(), "APPROVED");
    resource(reviewer.getUserId(), "PENDING");
    Long pendingResource = resource(teacher.getUserId(), "PENDING");
    Long caseId = teachingCase(teacher.getUserId(), "APPROVED");
    teachingCase(teacher.getUserId(), "PENDING");
    jdbc.update(
        "INSERT INTO resource_favorite(user_id,resource_id) VALUES(?,?)",
        teacher.getUserId(),
        resourceId);
    jdbc.update(
        "INSERT INTO case_favorite(user_id,case_id) VALUES(?,?)", teacher.getUserId(), caseId);
    sqlSession.clearCache();

    var mine = dashboard.summary(teacher);
    assertEquals(1, mine.courseCount());
    assertEquals(2, mine.resourceCount());
    assertEquals(2, mine.caseCount());
    assertEquals(1, mine.resourceFavoriteCount());
    assertEquals(1, mine.caseFavoriteCount());
    assertEquals(0, mine.pendingResourceCount());
    assertEquals(courseId, mine.recentCourses().get(0).id());
    assertEquals(1, mine.recentCourses().get(0).chapterCount());
    assertEquals(1, mine.recentCourses().get(0).knowledgePointCount());

    var reviews = dashboard.summary(reviewer);
    assertEquals(0, reviews.courseCount());
    assertEquals(1, reviews.resourceCount());
    assertEquals(reviewBaseline.pendingResourceCount() + 1, reviews.pendingResourceCount());
    assertEquals(reviewBaseline.pendingCaseCount() + 1, reviews.pendingCaseCount());
    assertNotEquals(pendingResource, resourceId);
  }

  private UserPrincipal user(List<String> roles) {
    String name = "dash_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
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
    return new UserPrincipal(record, roles);
  }

  private Long course(Long ownerId) {
    String code = unique("COURSE");
    jdbc.update(
        "INSERT INTO course(course_code,course_name,owner_id) VALUES(?,'教师课程',?)", code, ownerId);
    return jdbc.queryForObject("SELECT id FROM course WHERE course_code=?", Long.class, code);
  }

  private Long resource(Long uploaderId, String status) {
    String no = unique("RES");
    jdbc.update(
        "INSERT INTO teaching_resource(resource_no,resource_name,resource_type,file_url,file_size,uploader_id,visibility,audit_status) VALUES(?,'测试资源','PDF','test.pdf',1,?,'PUBLIC',?)",
        no,
        uploaderId,
        status);
    return jdbc.queryForObject(
        "SELECT id FROM teaching_resource WHERE resource_no=?", Long.class, no);
  }

  private Long teachingCase(Long creatorId, String status) {
    String no = unique("CASE");
    jdbc.update(
        "INSERT INTO teaching_case(case_no,case_name,creator_id,visibility,access_type,audit_status) VALUES(?,'测试案例',?,'PUBLIC','FREE',?)",
        no,
        creatorId,
        status);
    return jdbc.queryForObject("SELECT id FROM teaching_case WHERE case_no=?", Long.class, no);
  }

  private String unique(String type) {
    return type + "-DASH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
  }
}
