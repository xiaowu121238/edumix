package com.itxiaowu.edumix.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itxiaowu.edumix.dto.AccountDtos;
import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.mapper.SysUserMapper;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {
  private final SysUserMapper users;
  private final JdbcTemplate jdbc;
  private final PasswordEncoder passwords;
  private final PointService points;

  @Transactional
  public AccountDtos.Account register(AccountDtos.RegisterRequest request) {
    AccountDtos.Account account =
        create(
            request.username(),
            request.password(),
            request.realName(),
            request.email(),
            request.college(),
            null,
            List.of("TEACHER"));
    points.registration(account.id());
    return account;
  }

  @Transactional
  public AccountDtos.Account createManaged(AccountDtos.CreateUserRequest request) {
    List<String> roles =
        switch (request.accountType()) {
          case "TEACHER" -> List.of("TEACHER");
          case "RESOURCE_MANAGER" -> List.of("TEACHER", "RESOURCE_MANAGER");
          case "ADMIN" -> List.of("ADMIN");
          default -> throw new IllegalArgumentException("账号类型不正确");
        };
    return create(
        request.username(),
        request.password(),
        request.realName(),
        request.email(),
        request.college(),
        request.title(),
        roles);
  }

  public List<AccountDtos.Account> list() {
    return users.selectList(Wrappers.<SysUser>lambdaQuery().orderByDesc(SysUser::getId)).stream()
        .map(this::view)
        .toList();
  }

  @Transactional
  public AccountDtos.Account setRoles(Long id, List<String> requested, Long operatorId) {
    SysUser user = requireUser(id);
    List<String> roles = normalizeRoles(requested);
    if (id.equals(operatorId) && !roles.contains("ADMIN"))
      throw new IllegalArgumentException("不能移除自己的管理员权限");
    if (users.selectRoleCodes(id).contains("ADMIN") && !roles.contains("ADMIN"))
      requireAnotherAdmin(id);
    jdbc.update("DELETE FROM sys_user_role WHERE user_id=?", id);
    bindRoles(id, roles);
    return view(user);
  }

  @Transactional
  public AccountDtos.Account setStatus(Long id, Integer status, Long operatorId) {
    if (status == null || (status != 0 && status != 1))
      throw new IllegalArgumentException("状态只能是启用或停用");
    SysUser user = requireUser(id);
    if (id.equals(operatorId) && status == 0) throw new IllegalArgumentException("不能停用自己的账号");
    if (status == 0 && users.selectRoleCodes(id).contains("ADMIN")) requireAnotherAdmin(id);
    user.setStatus(status);
    users.updateById(user);
    return view(user);
  }

  @Transactional
  public void resetPassword(Long id, String password) {
    SysUser user = requireUser(id);
    user.setPassword(passwords.encode(checkPassword(password)));
    users.updateById(user);
  }

  private AccountDtos.Account create(
      String username,
      String password,
      String realName,
      String email,
      String college,
      String title,
      List<String> roles) {
    String name = username == null ? "" : username.trim();
    if (!name.matches("[A-Za-z0-9_]{3,50}"))
      throw new IllegalArgumentException("用户名须为3到50位字母、数字或下划线");
    String display = realName == null ? "" : realName.trim();
    if (display.isBlank() || display.length() > 50)
      throw new IllegalArgumentException("姓名须为1到50个字符");
    if (email != null
        && !email.isBlank()
        && (email.length() > 100 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")))
      throw new IllegalArgumentException("邮箱格式不正确");
    if (college != null && college.length() > 100)
      throw new IllegalArgumentException("学院不能超过100个字符");
    if (title != null && title.length() > 50) throw new IllegalArgumentException("职称不能超过50个字符");
    if (users.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, name)) > 0)
      throw new DuplicateKeyException("用户名已存在");
    SysUser user = new SysUser();
    user.setUsername(name);
    user.setPassword(passwords.encode(checkPassword(password)));
    user.setRealName(display);
    user.setEmail(blank(email));
    user.setCollege(blank(college));
    user.setTitle(blank(title));
    user.setStatus(1);
    user.setDeleted(0);
    users.insert(user);
    bindRoles(user.getId(), roles);
    jdbc.update("INSERT INTO point_account(user_id) VALUES(?)", user.getId());
    return view(user);
  }

  private String checkPassword(String password) {
    if (password == null || password.length() < 8 || password.length() > 72)
      throw new IllegalArgumentException("密码须为8到72个字符");
    return password;
  }

  private List<String> normalizeRoles(List<String> roles) {
    if (roles == null) throw new IllegalArgumentException("必须指定角色");
    Set<String> set = Set.copyOf(roles);
    if (set.equals(Set.of("TEACHER"))) return List.of("TEACHER");
    if (set.equals(Set.of("TEACHER", "RESOURCE_MANAGER")))
      return List.of("TEACHER", "RESOURCE_MANAGER");
    if (set.equals(Set.of("ADMIN"))) return List.of("ADMIN");
    throw new IllegalArgumentException("角色组合仅支持教师、负责人或管理员");
  }

  private void bindRoles(Long id, List<String> roles) {
    for (String role : roles) {
      int count =
          jdbc.update(
              "INSERT INTO sys_user_role(user_id,role_id) SELECT ?,id FROM sys_role WHERE role_code=?",
              id,
              role);
      if (count != 1) throw new IllegalStateException("缺少系统角色：" + role);
    }
  }

  private void requireAnotherAdmin(Long id) {
    Long others =
        jdbc.queryForObject(
            """
            SELECT COUNT(*) FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id
            JOIN sys_role r ON r.id=ur.role_id
            WHERE u.id<>? AND u.status=1 AND u.deleted=0 AND r.role_code='ADMIN'
            """,
            Long.class,
            id);
    if (others == null || others == 0) throw new IllegalArgumentException("不能移除最后一个启用的管理员");
  }

  private SysUser requireUser(Long id) {
    SysUser user = users.selectById(id);
    if (user == null) throw new IllegalArgumentException("账号不存在");
    return user;
  }

  private AccountDtos.Account view(SysUser user) {
    List<String> roles =
        jdbc.queryForList(
            """
            SELECT r.role_code FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id
            WHERE ur.user_id=? ORDER BY r.id
            """,
            String.class,
            user.getId());
    return new AccountDtos.Account(
        user.getId(),
        user.getUsername(),
        user.getRealName(),
        user.getEmail(),
        user.getCollege(),
        user.getTitle(),
        user.getStatus(),
        roles);
  }

  private String blank(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
