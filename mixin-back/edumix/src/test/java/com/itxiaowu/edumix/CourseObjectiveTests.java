package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.itxiaowu.edumix.dto.AccountDtos;
import com.itxiaowu.edumix.dto.CourseRequest;
import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.mapper.SysUserMapper;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.AccountService;
import com.itxiaowu.edumix.service.CourseService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CourseObjectiveTests {
  @Autowired CourseService courses;
  @Autowired AccountService accounts;
  @Autowired SysUserMapper users;

  @Test
  void createAndUpdatePreserveThreeObjectiveDimensions() {
    UserPrincipal teacher = register();
    String code = "OBJ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    var created =
        courses.create(
            new CourseRequest(
                code,
                "Java课程",
                "课程简介",
                "掌握面向对象知识",
                "能够完成程序设计与调试",
                "形成工程责任意识",
                null,
                "计算机学院",
                "2026秋",
                1),
            teacher);
    assertEquals("掌握面向对象知识", created.knowledgeObjective());
    assertEquals("能够完成程序设计与调试", created.abilityObjective());
    assertEquals("形成工程责任意识", created.qualityObjective());

    var updated =
        courses.update(
            created.id(),
            new CourseRequest(
                code,
                "Java课程",
                "课程简介",
                "掌握集合与并发知识",
                "能够解决复杂编程问题",
                "具备团队协作意识",
                null,
                "计算机学院",
                "2026秋",
                1),
            teacher);
    assertEquals("掌握集合与并发知识", updated.knowledgeObjective());
    assertEquals("能够解决复杂编程问题", updated.abilityObjective());
    assertEquals("具备团队协作意识", updated.qualityObjective());
  }

  private UserPrincipal register() {
    String username = "objective_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    var account =
        accounts.register(
            new AccountDtos.RegisterRequest(username, "secure123", username, null, "计算机学院"));
    SysUser user = users.selectById(account.id());
    return new UserPrincipal(user, users.selectRoleCodes(user.getId()));
  }
}
