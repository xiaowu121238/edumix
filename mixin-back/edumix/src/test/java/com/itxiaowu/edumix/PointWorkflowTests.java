package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.*;

import com.itxiaowu.edumix.dto.AccountDtos;
import com.itxiaowu.edumix.dto.CaseReviewRequest;
import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.mapper.SysUserMapper;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.AccountService;
import com.itxiaowu.edumix.service.PointService;
import com.itxiaowu.edumix.service.TeachingCaseService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PointWorkflowTests {
  @Autowired AccountService accounts;
  @Autowired PointService points;
  @Autowired TeachingCaseService cases;
  @Autowired SysUserMapper users;
  @Autowired JdbcTemplate jdbc;

  @Test
  void registrationCheckInApprovalAndUnlockKeepBalancedLedger() {
    UserPrincipal author = register(), buyer = register();
    UserPrincipal reviewer = managed("RESOURCE_MANAGER");
    assertEquals(20, points.wallet(author.getUserId()).balance());
    assertEquals(25, points.checkIn(author.getUserId()).balance());
    assertThrows(IllegalArgumentException.class, () -> points.checkIn(author.getUserId()));
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
    assertEquals(
        List.of(today),
        points.checkInDates(author.getUserId(), today.getYear(), today.getMonthValue()));
    assertTrue(
        points.checkInDates(buyer.getUserId(), today.getYear(), today.getMonthValue()).isEmpty());
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM point_log WHERE user_id=? AND business_type='DAILY_CHECKIN'",
            Integer.class,
            author.getUserId()));

    Long free = caseRow(author.getUserId(), "FREE", 0),
        paid = caseRow(author.getUserId(), "POINTS", 3);
    cases.review(free, new CaseReviewRequest("APPROVED", "通过", false), reviewer);
    cases.review(paid, new CaseReviewRequest("APPROVED", "通过", false), reviewer);
    assertEquals(55, points.wallet(author.getUserId()).balance());
    assertThrows(
        IllegalArgumentException.class,
        () -> cases.review(paid, new CaseReviewRequest("APPROVED", "重复", false), reviewer));
    assertFalse(cases.get(paid, buyer).contentUnlocked());
    assertNull(cases.get(paid, buyer).content());
    assertTrue(cases.get(paid, author).contentUnlocked());
    assertThrows(IllegalArgumentException.class, () -> cases.toggleFavorite(paid, buyer));
    assertTrue(cases.list(null, null, null, true, buyer).isEmpty());

    assertTrue(cases.unlock(paid, buyer).contentUnlocked());
    assertTrue(cases.toggleFavorite(paid, buyer));
    assertEquals(
        List.of(paid),
        cases.list(null, null, null, true, buyer).stream().map(c -> c.id()).toList());
    assertEquals(17, points.wallet(buyer.getUserId()).balance());
    assertEquals(58, points.wallet(author.getUserId()).balance());
    cases.unlock(paid, buyer);
    assertEquals(17, points.wallet(buyer.getUserId()).balance());
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM case_unlock WHERE user_id=? AND case_id=?",
            Integer.class,
            buyer.getUserId(),
            paid));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM point_log WHERE user_id=? AND business_type='CASE_UNLOCK_INCOME' AND business_id=?",
            Integer.class,
            author.getUserId(),
            paid));
  }

  @Test
  void insufficientFundsRollBackUnlockAndAdminGrantRequiresOwnPassword() {
    UserPrincipal author = register(), buyer = register(), admin = managed("ADMIN");
    Long paid = caseRow(author.getUserId(), "POINTS", 3);
    cases.review(paid, new CaseReviewRequest("APPROVED", "通过", false), admin);
    jdbc.update("UPDATE point_account SET balance=0 WHERE user_id=?", buyer.getUserId());
    assertThrows(IllegalArgumentException.class, () -> cases.unlock(paid, buyer));
    assertEquals(0, points.wallet(buyer.getUserId()).balance());
    assertThrows(
        IllegalArgumentException.class,
        () -> points.grant(admin.getUserId(), buyer.getUserId(), 5, "测试", "wrong"));
    assertThrows(
        IllegalArgumentException.class,
        () -> points.grant(admin.getUserId(), admin.getUserId(), 5, "测试", "secure123"));
    assertThrows(
        IllegalArgumentException.class,
        () -> points.grant(author.getUserId(), buyer.getUserId(), 5, "测试", "secure123"));
    assertEquals(
        5, points.grant(admin.getUserId(), buyer.getUserId(), 5, "测试补分", "secure123").balance());
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM point_grant WHERE operator_id=? AND recipient_id=?",
            Integer.class,
            admin.getUserId(),
            buyer.getUserId()));
  }

  private UserPrincipal register() {
    String name = name();
    var account =
        accounts.register(new AccountDtos.RegisterRequest(name, "secure123", name, null, "计算机学院"));
    return principal(account.id());
  }

  private UserPrincipal managed(String type) {
    String name = name();
    var account =
        accounts.createManaged(
            new AccountDtos.CreateUserRequest(name, "secure123", name, null, null, null, type));
    return principal(account.id());
  }

  private UserPrincipal principal(Long id) {
    SysUser user = users.selectById(id);
    return new UserPrincipal(user, users.selectRoleCodes(id));
  }

  private Long caseRow(Long authorId, String access, int price) {
    String no = "POINT-TEST-" + UUID.randomUUID().toString().replace("-", "");
    jdbc.update(
        "INSERT INTO teaching_case(case_no,case_name,content,creator_id,visibility,access_type,points_price,audit_status) VALUES(?,'积分测试案例','<p>完整正文</p>',?,'PUBLIC',?,?,'PENDING')",
        no,
        authorId,
        access,
        price);
    return jdbc.queryForObject("SELECT id FROM teaching_case WHERE case_no=?", Long.class, no);
  }

  private String name() {
    return "points_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
  }
}
