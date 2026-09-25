package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.dto.AccountDtos;
import com.itxiaowu.edumix.dto.LoginRequest;
import com.itxiaowu.edumix.dto.LoginResponse;
import com.itxiaowu.edumix.dto.UserProfileResponse;
import com.itxiaowu.edumix.security.JwtService;
import com.itxiaowu.edumix.security.TokenBlacklist;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.AccountService;
import com.itxiaowu.edumix.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final TokenBlacklist tokenBlacklist;
  private final UserService userService;
  private final AccountService accountService;

  @PostMapping("/register")
  public ApiResponse<AccountDtos.Account> register(
      @Valid @RequestBody AccountDtos.RegisterRequest request) {
    return ApiResponse.success(accountService.register(request));
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    String token = jwtService.generate(principal);
    userService.recordLogin(principal.getUserId());
    UserProfileResponse profile = userService.profile(principal.getUserId());
    return ApiResponse.success(
        new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), profile));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    String token = authorization.substring(7);
    Claims claims = jwtService.parse(token);
    tokenBlacklist.revoke(claims.getId(), claims.getExpiration().toInstant());
    return ApiResponse.success();
  }

  @GetMapping("/me")
  public ApiResponse<UserProfileResponse> me(Authentication authentication) {
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    return ApiResponse.success(userService.profile(principal.getUserId()));
  }
}
