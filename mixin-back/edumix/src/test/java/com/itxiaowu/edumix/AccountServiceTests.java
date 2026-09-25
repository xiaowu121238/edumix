package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.*;

import com.itxiaowu.edumix.dto.AccountDtos;
import com.itxiaowu.edumix.mapper.SysUserMapper;
import com.itxiaowu.edumix.service.AccountService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AccountServiceTests {
  @Autowired AccountService accounts;
  @Autowired SysUserMapper users;
  @Autowired PasswordEncoder passwords;
  @Autowired JdbcTemplate jdbc;

  @Test
  void registrationAlwaysCreatesActiveTeacherWithHashedPassword() {
    String name = name();
    var teacher =
        accounts.register(
            new AccountDtos.RegisterRequest(name, "secure123", "测试教师", null, "计算机学院"));
    assertEquals(List.of("TEACHER"), teacher.roles());
    assertEquals(1, teacher.status());
    assertTrue(passwords.matches("secure123", users.selectById(teacher.id()).getPassword()));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM point_account WHERE user_id=?", Integer.class, teacher.id()));
    assertEquals(
        20,
        jdbc.queryForObject(
            "SELECT balance FROM point_account WHERE user_id=?", Integer.class, teacher.id()));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM point_log WHERE user_id=? AND business_type='REGISTER'",
            Integer.class,
            teacher.id()));
    assertThrows(
        DuplicateKeyException.class,
        () ->
            accounts.register(
                new AccountDtos.RegisterRequest(name, "secure123", "重复", null, null)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            accounts.register(
                new AccountDtos.RegisterRequest(name(), "123456", "弱密码", null, null)));
  }

  @Test
  void managerHasBothRolesAndAdminCanManageAccountsSafely() {
    var manager =
        accounts.createManaged(
            new AccountDtos.CreateUserRequest(
                name(), "secure123", "负责人", null, null, null, "RESOURCE_MANAGER"));
    assertEquals(List.of("TEACHER", "RESOURCE_MANAGER"), manager.roles());
    var admin =
        accounts.createManaged(
            new AccountDtos.CreateUserRequest(
                name(), "secure123", "管理员", null, null, null, "ADMIN"));
    assertEquals(List.of("ADMIN"), admin.roles());
    assertThrows(
        IllegalArgumentException.class,
        () -> accounts.setRoles(admin.id(), List.of("TEACHER"), admin.id()));
    assertThrows(
        IllegalArgumentException.class, () -> accounts.setStatus(admin.id(), 0, admin.id()));
    assertEquals(
        List.of("TEACHER"),
        accounts.setRoles(manager.id(), List.of("TEACHER"), admin.id()).roles());
    assertEquals(0, accounts.setStatus(manager.id(), 0, admin.id()).status());
    accounts.resetPassword(manager.id(), "changed123");
    assertTrue(passwords.matches("changed123", users.selectById(manager.id()).getPassword()));
    assertThrows(
        IllegalArgumentException.class,
        () -> accounts.setRoles(manager.id(), List.of("ADMIN", "RESOURCE_MANAGER"), admin.id()));
  }

  private String name() {
    return "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
  }
}
