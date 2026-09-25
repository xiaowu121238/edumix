package com.itxiaowu.edumix.dto;

import com.itxiaowu.edumix.entity.PointAccount;
import com.itxiaowu.edumix.entity.SysUser;
import java.util.List;

public record UserProfileResponse(
    Long id,
    String username,
    String realName,
    String email,
    String phone,
    String avatarUrl,
    String college,
    String title,
    List<String> roles,
    Integer pointBalance) {
  public static UserProfileResponse from(SysUser user, List<String> roles, PointAccount account) {
    return new UserProfileResponse(
        user.getId(),
        user.getUsername(),
        user.getRealName(),
        user.getEmail(),
        user.getPhone(),
        user.getAvatarUrl(),
        user.getCollege(),
        user.getTitle(),
        roles,
        account == null ? 0 : account.getBalance());
  }
}
