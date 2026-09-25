package com.itxiaowu.edumix.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.mapper.SysUserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 启动迁移：首次运行时将本地演示账号的明文密码转换为 BCrypt 密文。 */
@Component
@RequiredArgsConstructor
public class DemoPasswordMigration implements ApplicationRunner {
  private final SysUserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(ApplicationArguments args) {
    List<SysUser> users =
        userMapper.selectList(
            Wrappers.<SysUser>lambdaQuery()
                .in(SysUser::getUsername, List.of("teacher", "reviewer", "admin")));
    users.stream()
        .filter(user -> user.getPassword() != null && user.getPassword().startsWith("{noop}"))
        .forEach(
            user -> {
              SysUser update = new SysUser();
              update.setId(user.getId());
              update.setPassword(passwordEncoder.encode(user.getPassword().substring(6)));
              userMapper.updateById(update);
            });
  }
}
