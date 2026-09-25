package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.ChangePasswordRequest;
import com.itxiaowu.edumix.dto.UpdateProfileRequest;
import com.itxiaowu.edumix.dto.UserProfileResponse;
import com.itxiaowu.edumix.entity.PointAccount;
import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.mapper.PointAccountMapper;
import com.itxiaowu.edumix.mapper.SysUserMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户资料服务：读取当前账号、更新个人资料、修改密码并组装角色与积分信息。 */
@Service
@RequiredArgsConstructor
public class UserService {
  private final SysUserMapper userMapper;
  private final PointAccountMapper pointAccountMapper;
  private final PasswordEncoder passwordEncoder;

  public UserProfileResponse profile(Long userId) {
    SysUser user = requireUser(userId);
    List<String> roles = userMapper.selectRoleCodes(userId);
    PointAccount account = pointAccountMapper.selectById(userId);
    return UserProfileResponse.from(user, roles, account);
  }

  @Transactional
  public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
    SysUser user = requireUser(userId);
    user.setRealName(request.realName());
    user.setEmail(blankToNull(request.email()));
    user.setPhone(blankToNull(request.phone()));
    user.setAvatarUrl(blankToNull(request.avatarUrl()));
    user.setCollege(blankToNull(request.college()));
    user.setTitle(blankToNull(request.title()));
    userMapper.updateById(user);
    return profile(userId);
  }

  @Transactional
  public void changePassword(Long userId, ChangePasswordRequest request) {
    SysUser user = requireUser(userId);
    if (!passwordEncoder.matches(request.oldPassword(), user.getPassword()))
      throw new IllegalArgumentException("原密码不正确");
    if (passwordEncoder.matches(request.newPassword(), user.getPassword()))
      throw new IllegalArgumentException("新密码不能与原密码相同");
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    userMapper.updateById(user);
  }

  public void recordLogin(Long userId) {
    SysUser user = new SysUser();
    user.setId(userId);
    user.setLastLoginTime(LocalDateTime.now());
    userMapper.updateById(user);
  }

  private SysUser requireUser(Long userId) {
    SysUser user = userMapper.selectById(userId);
    if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
      throw new IllegalArgumentException("用户不存在或已被禁用");
    }
    return user;
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
