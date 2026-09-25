package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.*;

import com.itxiaowu.edumix.dto.AccountDtos;
import com.itxiaowu.edumix.dto.ChangePasswordRequest;
import com.itxiaowu.edumix.mapper.SysUserMapper;
import com.itxiaowu.edumix.security.JwtService;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.AccountService;
import com.itxiaowu.edumix.service.AvatarService;
import com.itxiaowu.edumix.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProfileWorkflowTests {
  @Autowired AccountService accounts;
  @Autowired UserService users;
  @Autowired SysUserMapper userMapper;
  @Autowired PasswordEncoder passwords;
  @Autowired JwtService jwt;
  @Autowired AvatarService avatars;

  @Test
  void passwordChangeRequiresOldPasswordAndInvalidatesPreviousTokens() {
    var account =
        accounts.register(
            new AccountDtos.RegisterRequest(
                "profile_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20),
                "secure123",
                "资料测试",
                null,
                "计算机学院"));
    var original = userMapper.selectById(account.id());
    String oldToken = jwt.generate(new UserPrincipal(original, account.roles()));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            users.changePassword(
                account.id(), new ChangePasswordRequest("wrong123", "changed123")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            users.changePassword(
                account.id(), new ChangePasswordRequest("secure123", "secure123")));
    users.changePassword(account.id(), new ChangePasswordRequest("secure123", "changed123"));

    var updated = userMapper.selectById(account.id());
    assertTrue(passwords.matches("changed123", updated.getPassword()));
    assertFalse(
        jwt.passwordVersion(updated.getPassword())
            .equals(jwt.parse(oldToken).get("pv", String.class)));
  }

  @Test
  void avatarUploadRejectsSpoofedFile() {
    var account =
        accounts.register(
            new AccountDtos.RegisterRequest(
                "avatar_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20),
                "secure123",
                "头像测试",
                null,
                "计算机学院"));
    var fake = new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes());
    assertThrows(IllegalArgumentException.class, () -> avatars.upload(account.id(), fake));
    assertNull(userMapper.selectById(account.id()).getAvatarUrl());
  }
}
