package com.itxiaowu.edumix;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** 验证公共首页无需登录可访问，并且不会展示未审核或私有内容。 */
@SpringBootTest
@Transactional
class PublicContentTests {
  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  private MockMvc mvc;

  @BeforeEach
  void setUpMvc() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void anonymousSearchOnlyReturnsApprovedPublicSummaries() throws Exception {
    String marker = "公共搜索" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    Long authorId = createTeacher(marker);
    insertResource(authorId, marker + "资源", "PUBLIC", "APPROVED", "server-secret.pdf");
    insertResource(authorId, marker + "私有资源", "PRIVATE", "APPROVED", "private-secret.pdf");
    insertCase(authorId, marker + "优秀案例", "PUBLIC", "APPROVED", 1);
    insertCase(authorId, marker + "待审核案例", "PUBLIC", "PENDING", 0);

    mvc.perform(get("/api/public/search").param("keyword", marker))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.resources", hasSize(1)))
        .andExpect(jsonPath("$.data.resources[0].name").value(marker + "资源"))
        .andExpect(jsonPath("$.data.resources[0].fileUrl").doesNotExist())
        .andExpect(jsonPath("$.data.cases", hasSize(1)))
        .andExpect(jsonPath("$.data.cases[0].name").value(marker + "优秀案例"))
        .andExpect(jsonPath("$.data.cases[0].excellent").value(true))
        .andExpect(jsonPath("$.data.cases[0].content").doesNotExist());
  }

  @Test
  void anonymousCanReadRealHomeStatistics() throws Exception {
    mvc.perform(get("/api/public/home"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.teacherCount").isNumber())
        .andExpect(jsonPath("$.data.resourceCount").isNumber())
        .andExpect(jsonPath("$.data.caseCount").isNumber())
        .andExpect(jsonPath("$.data.featuredCases").isArray());
  }

  private Long createTeacher(String marker) {
    String username = "public_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    jdbc.update(
        "INSERT INTO sys_user(username,password,real_name,status,deleted) VALUES(?,?,?,1,0)",
        username,
        "test-only",
        marker + "教师");
    Long id = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, username);
    jdbc.update(
        "INSERT INTO sys_user_role(user_id,role_id) SELECT ?,id FROM sys_role WHERE role_code='TEACHER'",
        id);
    return id;
  }

  private void insertResource(
      Long authorId, String name, String visibility, String status, String fileUrl) {
    jdbc.update(
        """
        INSERT INTO teaching_resource
          (resource_no,resource_name,resource_type,file_url,file_size,uploader_id,visibility,audit_status)
        VALUES(?,?, 'PDF',?,1,?,?,?)
        """,
        "RES-PUBLIC-" + UUID.randomUUID().toString().replace("-", ""),
        name,
        fileUrl,
        authorId,
        visibility,
        status);
  }

  private void insertCase(
      Long authorId, String name, String visibility, String status, int excellent) {
    jdbc.update(
        """
        INSERT INTO teaching_case
          (case_no,case_name,summary,content,creator_id,visibility,access_type,audit_status,excellent)
        VALUES(?,?,?,'<p>不应出现在公共摘要中的正文</p>',?,?,'FREE',?,?)
        """,
        "CASE-PUBLIC-" + UUID.randomUUID().toString().replace("-", ""),
        name,
        "公开摘要",
        authorId,
        visibility,
        status,
        excellent);
  }
}
