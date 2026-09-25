package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.*;

import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.ChatService;
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
class ChatServiceTests {
  @Autowired ChatService chats;
  @Autowired JdbcTemplate jdbc;

  @Test
  void teachersCanExchangeStoredMessagesAndStrangersCannotReadThem() {
    UserPrincipal alice = teacher("Alice");
    UserPrincipal bob = teacher("Bob");
    UserPrincipal stranger = teacher("Stranger");

    var conversation = chats.start(bob.getUserId(), alice);
    assertEquals(conversation.id(), chats.start(alice.getUserId(), bob).id());
    assertThrows(IllegalArgumentException.class, () -> chats.start(alice.getUserId(), alice));
    assertThrows(
        AccessDeniedException.class, () -> chats.messages(conversation.id(), null, 30, stranger));

    var first = chats.send(conversation.id(), "  你好，Bob  ", alice);
    assertEquals("你好，Bob", first.content());
    assertEquals(1, chats.conversations(bob).get(0).unreadCount());
    assertEquals(first.id(), chats.messages(conversation.id(), null, 30, bob).items().get(0).id());
    chats.markRead(conversation.id(), bob);
    assertEquals(0, chats.conversations(bob).get(0).unreadCount());
    assertThrows(AccessDeniedException.class, () -> chats.send(conversation.id(), "偷窥", stranger));

    var second = chats.send(conversation.id(), "收到", bob);
    var newest = chats.messages(conversation.id(), null, 1, alice);
    assertEquals(second.id(), newest.items().get(0).id());
    assertEquals(
        first.id(),
        chats.messages(conversation.id(), newest.nextBeforeId(), 1, alice).items().get(0).id());
  }

  private UserPrincipal teacher(String display) {
    String username = "chat_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    jdbc.update(
        "INSERT INTO sys_user(username,password,real_name,status,deleted) VALUES(?,?,?,1,0)",
        username,
        "test-only",
        display);
    Long id = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, username);
    jdbc.update(
        "INSERT INTO sys_user_role(user_id,role_id) SELECT ?,id FROM sys_role WHERE role_code='TEACHER'",
        id);
    SysUser user = new SysUser();
    user.setId(id);
    user.setUsername(username);
    user.setRealName(display);
    user.setStatus(1);
    return new UserPrincipal(user, List.of("TEACHER"));
  }
}
