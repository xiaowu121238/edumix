package com.itxiaowu.edumix.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final long expirationSeconds;

  public JwtService(
      @Value("${edumix.jwt.secret}") String secret,
      @Value("${edumix.jwt.expiration-seconds}") long expirationSeconds) {
    this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    this.expirationSeconds = expirationSeconds;
  }

  public String generate(UserPrincipal principal) {
    Instant now = Instant.now();
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(principal.getUsername())
        .claim("uid", principal.getUserId())
        .claim("roles", principal.roles())
        .claim("pv", passwordVersion(principal.getPassword()))
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(expirationSeconds)))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public long getExpirationSeconds() {
    return expirationSeconds;
  }

  public String passwordVersion(String passwordHash) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(passwordHash.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 不可用", exception);
    }
  }
}
