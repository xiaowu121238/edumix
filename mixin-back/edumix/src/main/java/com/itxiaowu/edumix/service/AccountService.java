package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.AccountDtos;
import java.util.List;

/** 账号业务接口，定义教师注册和管理员账号管理能力。 */
public interface AccountService {
  AccountDtos.Account register(AccountDtos.RegisterRequest request);

  AccountDtos.Account createManaged(AccountDtos.CreateUserRequest request);

  List<AccountDtos.Account> list();

  AccountDtos.Account setRoles(Long id, List<String> requested, Long operatorId);

  AccountDtos.Account setStatus(Long id, Integer status, Long operatorId);

  void resetPassword(Long id, String password);
}
