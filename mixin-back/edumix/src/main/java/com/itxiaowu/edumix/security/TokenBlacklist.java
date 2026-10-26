package com.itxiaowu.edumix.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 登录令牌版本管理。
 *
 * <p>注销时递增用户表中的版本号，使该账号此前签发的全部 JWT 立即失效。状态保存在用户表中，重启和多实例部署后仍然有效，同时不再需要单独的令牌黑名单表。
 */
@Component
@RequiredArgsConstructor
public class TokenBlacklist {
  private final JdbcTemplate jdbc;

  public void revoke(Long userId) {
    jdbc.update("UPDATE sys_user SET token_version=token_version+1 WHERE id=?", userId);
  }

  public int currentVersion(Long userId) {
    Integer version =
        jdbc.queryForObject("SELECT token_version FROM sys_user WHERE id=?", Integer.class, userId);
    return version == null ? 0 : version;
  }

  public boolean isRevoked(Long userId, Integer tokenVersion) {
    return tokenVersion == null || currentVersion(userId) != tokenVersion;
  }
}
