package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.ChatDtos;
import com.itxiaowu.edumix.security.UserPrincipal;
import java.util.List;

/** 私信业务接口，定义会话创建、消息查询、发送和已读处理能力。 */
public interface ChatService {
  ChatDtos.Conversation start(Long otherId, UserPrincipal me);

  List<ChatDtos.Conversation> conversations(UserPrincipal me);

  ChatDtos.MessagePage messages(Long conversationId, Long beforeId, int size, UserPrincipal me);

  ChatDtos.ChatMessage send(Long conversationId, String raw, UserPrincipal me);

  void markRead(Long conversationId, UserPrincipal me);
}
