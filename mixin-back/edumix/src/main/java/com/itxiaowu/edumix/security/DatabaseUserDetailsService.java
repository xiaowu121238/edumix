package com.itxiaowu.edumix.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** 从数据库读取启用账号及其角色，并转换为 Spring Security 登录用户。 */
@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {
  private final SysUserMapper userMapper;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    SysUser user =
        userMapper.selectOne(
            Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getStatus, 1));
    if (user == null) {
      throw new UsernameNotFoundException("账号或密码错误");
    }
    return new UserPrincipal(user, userMapper.selectRoleCodes(user.getId()));
  }
}
