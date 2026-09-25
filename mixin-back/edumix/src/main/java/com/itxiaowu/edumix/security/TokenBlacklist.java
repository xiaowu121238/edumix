package com.itxiaowu.edumix.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

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
