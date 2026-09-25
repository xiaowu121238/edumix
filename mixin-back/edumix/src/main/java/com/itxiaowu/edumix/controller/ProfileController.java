package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.dto.ChangePasswordRequest;
import com.itxiaowu.edumix.dto.UpdateProfileRequest;
import com.itxiaowu.edumix.dto.UserProfileResponse;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.AvatarService;
import com.itxiaowu.edumix.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 个人资料接口：查询和修改个人信息、上传头像以及修改登录密码。 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
  private final UserService userService;
  private final AvatarService avatarService;

  @GetMapping
  public ApiResponse<UserProfileResponse> get(Authentication authentication) {
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    return ApiResponse.success(userService.profile(principal.getUserId()));
  }

  @PutMapping
  public ApiResponse<UserProfileResponse> update(
      Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    return ApiResponse.success(userService.updateProfile(principal.getUserId(), request));
  }

  @PostMapping("/password")
  public ApiResponse<Void> changePassword(
      Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    userService.changePassword(principal.getUserId(), request);
    return ApiResponse.success(null);
  }

  @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<UserProfileResponse> uploadAvatar(
      Authentication authentication, @RequestPart("file") MultipartFile file) {
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    return ApiResponse.success(avatarService.upload(principal.getUserId(), file));
  }

  @GetMapping("/avatars/{filename}")
  public ResponseEntity<org.springframework.core.io.Resource> avatar(
      @PathVariable String filename) {
    var avatar = avatarService.read(filename);
    return ResponseEntity.ok()
        .contentType(avatar.contentType())
        .header("X-Content-Type-Options", "nosniff")
        .body(avatar.resource());
  }
}
