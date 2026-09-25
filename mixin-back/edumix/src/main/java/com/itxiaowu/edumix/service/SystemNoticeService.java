package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.SystemNoticeRequest;
import com.itxiaowu.edumix.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemNoticeService {
  private final NotificationDeliveryService delivery;

  @Transactional
  public long publish(SystemNoticeRequest request, UserPrincipal admin) {
    if (!admin.roles().contains("ADMIN")) throw new AccessDeniedException("只有管理员可以发布全站消息");
    String title = request.title() == null ? "" : request.title().strip();
    String content = request.content() == null ? "" : request.content().strip();
    if (title.isEmpty() || content.isEmpty() || title.length() > 255 || content.length() > 2000)
      throw new IllegalArgumentException("请填写标题和内容，标题不超过255字，内容不超过2000字");
    return delivery.broadcastSystem(title, content, admin.getUserId());
  }
}
