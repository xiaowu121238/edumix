package com.itxiaowu.edumix.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** 注销令牌黑名单：在令牌自然过期前阻止其再次访问受保护接口。 */
@Component
public class TokenBlacklist {
  private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

  public void revoke(String jwtId, Instant expiresAt) {
    cleanup();
    revokedTokens.put(jwtId, expiresAt);
  }

  public boolean isRevoked(String jwtId) {
    Instant expiresAt = revokedTokens.get(jwtId);
    if (expiresAt == null) return false;
    if (expiresAt.isBefore(Instant.now())) {
      revokedTokens.remove(jwtId);
      return false;
    }
    return true;
  }

  private void cleanup() {
    Instant now = Instant.now();
    revokedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
  }
}
