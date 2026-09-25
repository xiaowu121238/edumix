package com.itxiaowu.edumix.security;

import com.itxiaowu.edumix.entity.SysUser;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record UserPrincipal(SysUser user, List<String> roles) implements UserDetails {
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
  }

  public Long getUserId() {
    return user.getId();
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getUsername();
  }

  @Override
  public boolean isEnabled() {
    return Integer.valueOf(1).equals(user.getStatus());
  }
}
