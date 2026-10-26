package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.*;

import com.itxiaowu.edumix.dto.ResourceUpdateRequest;
import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.security.*;
import com.itxiaowu.edumix.service.ResourceService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FinalizationRegressionTests {
  @Autowired JdbcTemplate jdbc;
  @Autowired ResourceService resources;

  private UserPrincipal user() {
    String name = "final_" + UUID.randomUUID().toString().substring(0, 12);
    jdbc.update(
        "INSERT INTO sys_user(username,password,real_name,status,deleted) VALUES(?,'test-only',?,1,0)",
        name,
        name);
    Long id = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, name);
    SysUser u = new SysUser();
    u.setId(id);
    u.setUsername(name);
    u.setPassword("test-only");
    u.setStatus(1);
    return new UserPrincipal(u, List.of("TEACHER"));
  }

  private Long resource(UserPrincipal owner, String scope, String status) {
    String no = UUID.randomUUID().toString();
    jdbc.update(
        "INSERT INTO teaching_resource(resource_no,resource_name,resource_type,file_url,file_size,uploader_id,visibility,audit_status) VALUES(?,'older resource','PDF','test-only.pdf',1,?,?,?)",
        no,
        owner.getUserId(),
        scope,
        status);
    return jdbc.queryForObject(
        "SELECT id FROM teaching_resource WHERE resource_no=?", Long.class, no);
  }

  @Test
  void laterPagesAndReferencedIdsRespectVisibilityAndTotals() {
    var owner = user();
    var viewer = user();
    Long old = resource(owner, "PUBLIC", "APPROVED");
    for (int i = 0; i < 105; i++) resource(owner, "PUBLIC", "APPROVED");
    Long hidden = resource(owner, "PRIVATE", "APPROVED");
    Long pending = resource(owner, "PUBLIC", "PENDING");
    Long deleted = resource(owner, "PUBLIC", "APPROVED");
    jdbc.update("UPDATE teaching_resource SET deleted=1 WHERE id=?", deleted);
    assertEquals(108L, resources.mineStats(owner).get("total"));
    assertEquals(106L, resources.mineStats(owner).get("approved"));
    var first = resources.minePage(null, null, null, false, 1, 100, owner);
    var second = resources.minePage(null, null, null, false, 2, 100, owner);
    assertEquals(108, first.total());
    assertEquals(8, second.records().size());
    assertTrue(second.records().stream().anyMatch(r -> r.id().equals(old)));
    var selected = resources.byIds(List.of(old, hidden, pending, deleted, old), viewer);
    assertEquals(List.of(old), selected.stream().map(r -> r.id()).toList());
    assertEquals(3, resources.byIds(List.of(old, hidden, pending), owner).size());
    assertThrows(
        IllegalArgumentException.class,
        () -> resources.byIds(Collections.nCopies(101, old), viewer));
    jdbc.update(
        "INSERT INTO resource_favorite(user_id,resource_id) VALUES(?,?)", viewer.getUserId(), old);
    var favorites = resources.page(null, null, null, true, 1, 12, viewer);
    assertEquals(1, favorites.total());
    assertEquals(old, favorites.records().get(0).id());
  }

  @Test
  void invalidUploadAndReplacementDoNotChangeDatabase() {
    var owner = user();
    Long id = resource(owner, "PRIVATE", "APPROVED");
    var fake = new MockMultipartFile("file", "fake.pdf", "application/pdf", "not a PDF".getBytes());
    long before = resources.mineStats(owner).get("total");
    assertThrows(
        IllegalArgumentException.class,
        () -> resources.upload(fake, "fake", null, null, "PRIVATE", owner));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            resources.update(
                id, new ResourceUpdateRequest("changed", null, null, "PRIVATE"), fake, owner));
    assertEquals(before, resources.mineStats(owner).get("total"));
    assertEquals("older resource", resources.byIds(List.of(id), owner).get(0).resourceName());
  }

  @Test
  void logoutVersionSurvivesRecreatingService() {
    UserPrincipal principal = user();
    var first = new TokenBlacklist(jdbc);
    int issuedVersion = first.currentVersion(principal.getUserId());
    first.revoke(principal.getUserId());
    var restarted = new TokenBlacklist(jdbc);
    assertTrue(restarted.isRevoked(principal.getUserId(), issuedVersion));
    assertFalse(
        restarted.isRevoked(
            principal.getUserId(), restarted.currentVersion(principal.getUserId())));
  }
}
