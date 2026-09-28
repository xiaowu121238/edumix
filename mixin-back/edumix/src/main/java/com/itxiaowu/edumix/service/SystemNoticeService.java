package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.SystemNoticeRequest;
import com.itxiaowu.edumix.security.UserPrincipal;

/** 系统公告业务接口，定义管理员全站通知发布能力。 */
public interface SystemNoticeService {
  long publish(SystemNoticeRequest request, UserPrincipal admin);
}
