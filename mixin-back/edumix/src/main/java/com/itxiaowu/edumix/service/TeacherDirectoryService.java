package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.ChatDtos;
import com.itxiaowu.edumix.security.UserPrincipal;
import java.util.List;

/** 教师目录业务接口，定义教师检索和公开主页查询能力。 */
public interface TeacherDirectoryService {
  List<ChatDtos.PublicTeacher> search(String keyword);

  ChatDtos.TeacherPage detail(Long id, UserPrincipal viewer);
}
