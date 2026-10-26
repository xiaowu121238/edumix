package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.*;

import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.ResourceService;
import com.itxiaowu.edumix.service.TeachingCaseService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** 验证资源和案例检索确实经过 MyBatis-Plus 物理分页。 */
@SpringBootTest
class PaginationQueryTests {
  @Autowired ResourceService resources;
  @Autowired TeachingCaseService cases;
  @Autowired JdbcTemplate jdbc;

  @Test
  void resourceAndCasePagesRespectRequestedSize() {
    UserPrincipal user = teacher();

    var resourcePage = resources.page(null, null, null, false, 1, 1, user);
    var casePage = cases.publishedPage(null, null, 1, 1, user);

    assertEquals(1, resourcePage.page());
    assertEquals(1, resourcePage.size());
    assertTrue(resourcePage.records().size() <= 1);
    assertEquals(1, casePage.page());
    assertEquals(1, casePage.size());
    assertTrue(casePage.records().size() <= 1);
  }

  private UserPrincipal teacher() {
    Long id = jdbc.queryForObject("SELECT MIN(id) FROM sys_user WHERE deleted=0", Long.class);
    assertNotNull(id, "测试库至少需要一个有效用户");
    SysUser user = new SysUser();
    user.setId(id);
    user.setUsername("pagination-test");
    user.setPassword("test-only");
    user.setRealName("分页测试用户");
    user.setStatus(1);
    return new UserPrincipal(user, List.of("TEACHER"));
  }
}
