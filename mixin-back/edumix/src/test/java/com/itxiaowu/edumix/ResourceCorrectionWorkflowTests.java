package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.*;

import com.itxiaowu.edumix.dto.*;
import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.mapper.SysUserMapper;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.AccountService;
import com.itxiaowu.edumix.service.ResourceService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ResourceCorrectionWorkflowTests {
  @Autowired ResourceService resources;
  @Autowired AccountService accounts;
  @Autowired SysUserMapper users;
  @Autowired JdbcTemplate jdbc;

  @Test
  void correctionNotifiesBothSidesAndClosesAfterApprovedRevision() {
    UserPrincipal uploader = register(), submitter = register(), manager = managedManager();
    Long resourceId = resource(uploader.getUserId());
    var request = new CorrectionRequest("资源第3页的继承关系标注有误，建议重新核对类图中的箭头方向。");
    assertThrows(
        IllegalArgumentException.class,
        () -> resources.submitCorrection(resourceId, request, uploader));

    resources.submitCorrection(resourceId, request, submitter);
    Long correctionId =
        jdbc.queryForObject(
            "SELECT id FROM resource_correction WHERE resource_id=? AND submitter_id=?",
            Long.class,
            resourceId,
            submitter.getUserId());
    assertNotNull(correctionId);
    assertThrows(
        IllegalArgumentException.class,
        () -> resources.submitCorrection(resourceId, request, submitter));
    assertTrue(
        resources.pendingCorrections().stream().anyMatch(item -> item.id().equals(correctionId)));
    assertTrue(notificationCount(uploader.getUserId(), correctionId) > 0);
    assertTrue(notificationCount(manager.getUserId(), correctionId) > 0);

    resources.handleCorrection(
        correctionId, new CorrectionHandleRequest("ACCEPTED", "问题属实，请修正类图后重新提交审核。"), manager);
    assertEquals(
        "ACCEPTED",
        jdbc.queryForObject(
            "SELECT status FROM resource_correction WHERE id=?", String.class, correctionId));
    resources.update(
        resourceId, new ResourceUpdateRequest("纠错测试资源", "已修正类图", null, "PUBLIC"), uploader);
    resources.review(resourceId, new ResourceReviewRequest("APPROVED", "修改内容正确"), manager);
    assertEquals(
        "RESOLVED",
        jdbc.queryForObject(
            "SELECT status FROM resource_correction WHERE id=?", String.class, correctionId));
    assertTrue(notificationCount(submitter.getUserId(), correctionId) >= 2);
  }

  private int notificationCount(Long userId, Long correctionId) {
    return jdbc.queryForObject(
        """
                SELECT COUNT(*) FROM user_message um JOIN message m ON m.id=um.message_id
                WHERE um.user_id=? AND m.business_type='CORRECTION' AND m.business_id=?
                """,
        Integer.class,
        userId,
        correctionId);
  }

  private Long resource(Long uploaderId) {
    String no = "CORRECTION-" + UUID.randomUUID().toString().replace("-", "");
    jdbc.update(
        "INSERT INTO teaching_resource(resource_no,resource_name,resource_type,description,file_url,uploader_id,visibility,audit_status) VALUES(?,'纠错测试资源','IMAGE','类图资源','test.png',?,'PUBLIC','APPROVED')",
        no,
        uploaderId);
    return jdbc.queryForObject(
        "SELECT id FROM teaching_resource WHERE resource_no=?", Long.class, no);
  }

  private UserPrincipal register() {
    String name = "correction_" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
    var account =
        accounts.register(new AccountDtos.RegisterRequest(name, "secure123", name, null, "计算机学院"));
    return principal(account.id());
  }

  private UserPrincipal managedManager() {
    String name = "manager_" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
    var account =
        accounts.createManaged(
            new AccountDtos.CreateUserRequest(
                name, "secure123", name, null, null, null, "RESOURCE_MANAGER"));
    return principal(account.id());
  }

  private UserPrincipal principal(Long id) {
    SysUser user = users.selectById(id);
    return new UserPrincipal(user, users.selectRoleCodes(id));
  }
}
