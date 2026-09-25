package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.*;

import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.CourseService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CourseResourceLinkTests {
  @Autowired CourseService courses;
  @Autowired JdbcTemplate jdbc;
  @Autowired SqlSessionTemplate sqlSession;

  @Test
  void linksOnlyOwnApprovedFavoritesAndKeepsLinkAfterUnfavorite() {
    UserPrincipal owner = user(), other = user();
    Long point = point(owner.getUserId());
    Long approved = resource(owner.getUserId(), "APPROVED");
    Long pending = resource(owner.getUserId(), "PENDING");
    Long notFavorite = resource(owner.getUserId(), "APPROVED");
    jdbc.update(
        "INSERT INTO resource_favorite(user_id,resource_id) VALUES(?,?)",
        owner.getUserId(),
        approved);
    jdbc.update(
        "INSERT INTO resource_favorite(user_id,resource_id) VALUES(?,?)",
        owner.getUserId(),
        pending);

    assertEquals(
        List.of(approved),
        courses.favoriteCandidates(point, owner).stream().map(r -> r.id()).toList());
    assertThrows(
        IllegalArgumentException.class,
        () -> courses.linkResources(point, List.of(pending), owner));
    assertThrows(
        IllegalArgumentException.class,
        () -> courses.linkResources(point, List.of(notFavorite), owner));
    assertThrows(
        AccessDeniedException.class, () -> courses.linkResources(point, List.of(approved), other));

    courses.linkResources(point, List.of(approved, approved), owner);
    assertEquals(1, courses.linkedResources(point, owner).size());
    assertEquals(
        1,
        courses
            .tree(courseId(point), owner)
            .chapters()
            .get(0)
            .knowledgePoints()
            .get(0)
            .resourceCount());
    jdbc.update(
        "DELETE FROM resource_favorite WHERE user_id=? AND resource_id=?",
        owner.getUserId(),
        approved);
    assertTrue(courses.favoriteCandidates(point, owner).isEmpty());
    assertEquals(approved, courses.linkedResources(point, owner).get(0).id());

    jdbc.update("UPDATE teaching_resource SET audit_status='REJECTED' WHERE id=?", approved);
    sqlSession.clearCache();
    assertFalse(courses.linkedResources(point, owner).get(0).available());
    courses.unlinkResource(point, approved, owner);
    assertTrue(courses.linkedResources(point, owner).isEmpty());
  }

  private UserPrincipal user() {
    String name = "course_link_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
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
    return new UserPrincipal(record, List.of("TEACHER"));
  }

  private Long point(Long ownerId) {
    String code = "COURSE-LINK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    jdbc.update(
        "INSERT INTO course(course_code,course_name,owner_id) VALUES(?,'关联测试课程',?)", code, ownerId);
    Long courseId =
        jdbc.queryForObject("SELECT id FROM course WHERE course_code=?", Long.class, code);
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

  private Long courseId(Long pointId) {
    return jdbc.queryForObject(
        "SELECT course_id FROM knowledge_point WHERE id=?", Long.class, pointId);
  }

  private Long resource(Long uploaderId, String status) {
    String number = "RES-LINK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    jdbc.update(
        "INSERT INTO teaching_resource(resource_no,resource_name,resource_type,file_url,file_size,uploader_id,visibility,audit_status) VALUES(?,'关联测试资源','PDF','test.pdf',1,?,'PUBLIC',?)",
        number,
        uploaderId,
        status);
    return jdbc.queryForObject(
        "SELECT id FROM teaching_resource WHERE resource_no=?", Long.class, number);
  }
}
