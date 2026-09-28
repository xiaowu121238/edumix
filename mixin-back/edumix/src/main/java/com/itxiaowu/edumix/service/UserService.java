package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.ChangePasswordRequest;
import com.itxiaowu.edumix.dto.UpdateProfileRequest;
import com.itxiaowu.edumix.dto.UserProfileResponse;

/** 用户资料业务接口，定义个人资料、密码和登录时间维护能力。 */
public interface UserService {
  UserProfileResponse profile(Long userId);

  UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

  void changePassword(Long userId, ChangePasswordRequest request);

  void recordLogin(Long userId);
}
