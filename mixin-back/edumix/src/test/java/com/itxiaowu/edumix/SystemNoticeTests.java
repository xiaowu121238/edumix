package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.*;

import com.itxiaowu.edumix.controller.NotificationController;
import com.itxiaowu.edumix.dto.SystemNoticeRequest;
import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.SystemNoticeService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SystemNoticeTests {
  @Autowired SystemNoticeService notices;
  @Autowired NotificationController controller;
  @Autowired JdbcTemplate jdbc;

  @Test
  void adminBroadcastsToActiveUsersAndReadStateIsPersonal() {
    UserPrincipal admin = user(1, List.of("ADMIN"));
    UserPrincipal teacher = user(1, List.of("TEACHER"));
    UserPrincipal disabled = user(0, List.of("TEACHER"));
    assertThrows(
        AccessDeniedException.class,
        () -> notices.publish(new SystemNoticeRequest("标题", "内容"), teacher));
    long before = controller.unreadCount(teacher).data().system();

    long id = notices.publish(new SystemNoticeRequest(" 维护公告 ", " 今晚维护 "), admin);
    assertEquals(
        1L,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_message WHERE user_id=? AND message_id=?",
            Long.class,
            teacher.getUserId(),
            id));
    assertEquals(
        0L,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_message WHERE user_id=? AND message_id=?",
            Long.class,
            disabled.getUserId(),
            id));
    assertEquals(
        "维护公告", jdbc.queryForObject("SELECT title FROM message WHERE id=?", String.class, id));
    assertEquals(before + 1, controller.unreadCount(teacher).data().system());

    jdbc.update("INSERT INTO message(message_type,title,content) VALUES('AUDIT','审核结果','已通过')");
    Long auditId =
        jdbc.queryForObject(
            "SELECT id FROM message WHERE message_type='AUDIT' AND title='审核结果' ORDER BY id DESC LIMIT 1",
            Long.class);
    jdbc.update(
        "INSERT INTO user_message(user_id,message_id) VALUES(?,?)", teacher.getUserId(), auditId);

    controller.readAll("SYSTEM", teacher);
    assertEquals(0, controller.unreadCount(teacher).data().system());
    assertEquals(1, controller.unreadCount(teacher).data().audit());
    assertEquals(
        0L,
        jdbc.queryForObject(
            "SELECT read_status FROM user_message WHERE user_id=? AND message_id=?",
            Long.class,
            admin.getUserId(),
            id));
  }

  @Test
  void publishEndpointRejectsTeacherRole() {
    UserPrincipal teacher = user(1, List.of("TEACHER"));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(teacher, null, teacher.getAuthorities()));
    try {
      assertThrows(
          AccessDeniedException.class,
          () -> controller.publish(new SystemNoticeRequest("标题", "内容"), teacher));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void openingOneNoticeMarksOnlyThatUsersNoticeRead() {
    UserPrincipal admin = user(1, List.of("ADMIN")), teacher = user(1, List.of("TEACHER"));
    long messageId = notices.publish(new SystemNoticeRequest("逐条阅读测试", "完整公告内容"), admin);
    Long teacherNoticeId =
        jdbc.queryForObject(
            "SELECT id FROM user_message WHERE user_id=? AND message_id=?",
            Long.class,
            teacher.getUserId(),
            messageId);
    Long adminNoticeId =
        jdbc.queryForObject(
            "SELECT id FROM user_message WHERE user_id=? AND message_id=?",
            Long.class,
            admin.getUserId(),
            messageId);
    assertThrows(IllegalArgumentException.class, () -> controller.readOne(adminNoticeId, teacher));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT read_status FROM user_message WHERE id=?", Integer.class, teacherNoticeId));
    controller.readOne(teacherNoticeId, teacher);
    controller.readOne(teacherNoticeId, teacher); // repeated opening remains safe
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT read_status FROM user_message WHERE id=?", Integer.class, teacherNoticeId));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT read_status FROM user_message WHERE id=?", Integer.class, adminNoticeId));
  }

  private UserPrincipal user(int status, List<String> roles) {
    String name = "notice_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    jdbc.update(
        "INSERT INTO sys_user(username,password,real_name,status,deleted) VALUES(?,?,?,?,0)",
        name,
        "test-only",
        name,
        status);
    Long id = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, name);
    SysUser record = new SysUser();
    record.setId(id);
    record.setUsername(name);
    record.setRealName(name);
    record.setStatus(status);
    return new UserPrincipal(record, roles);
  }
}
