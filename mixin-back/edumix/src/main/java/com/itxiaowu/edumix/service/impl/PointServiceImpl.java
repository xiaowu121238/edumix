package com.itxiaowu.edumix.service.impl;

import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.mapper.SqlQueryMapper;
import com.itxiaowu.edumix.mapper.SysUserMapper;
import com.itxiaowu.edumix.service.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 积分业务服务。
 *
 * <p>负责注册奖励、每日签到、案例审核奖励、付费案例解锁、作者收益和管理员人工发放， 所有余额变化都会写入积分流水。
 */
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {
  private final SqlQueryMapper jdbc;
  private final SysUserMapper users;
  private final PasswordEncoder passwords;

  @Override
  public Wallet wallet(Long userId) {
    ensureAccount(userId);
    Map<String, Object> row =
        jdbc.queryForMap(
            "SELECT balance,total_income,total_expense FROM point_account WHERE user_id=?", userId);
    return new Wallet(
        ((Number) row.get("balance")).intValue(),
        ((Number) row.get("total_income")).intValue(),
        ((Number) row.get("total_expense")).intValue(),
        jdbc.queryForObject(
                "SELECT COUNT(*) FROM point_log WHERE user_id=? AND business_type='DAILY_CHECKIN' AND business_id=? AND deleted=0",
                Integer.class,
                userId,
                Long.parseLong(
                    LocalDate.now(ZoneId.of("Asia/Shanghai"))
                        .format(DateTimeFormatter.BASIC_ISO_DATE)))
            > 0);
  }

  @Override
  public List<Entry> history(Long userId) {
    return jdbc.query(
        """
            SELECT id,business_type,change_amount,balance_after,remark,create_time
            FROM point_log WHERE user_id=? AND deleted=0 ORDER BY id DESC LIMIT 100
            """,
        (rs, n) ->
            new Entry(
                rs.getLong("id"),
                rs.getString("business_type"),
                rs.getInt("change_amount"),
                rs.getInt("balance_after"),
                rs.getString("remark"),
                rs.getTimestamp("create_time").toLocalDateTime()),
        userId);
  }

  @Override
  public List<LocalDate> checkInDates(Long userId, int year, int month) {
    if (year < 2000 || year > 2100 || month < 1 || month > 12)
      throw new IllegalArgumentException("月份不正确");
    YearMonth selected = YearMonth.of(year, month);
    return jdbc.query(
        """
            SELECT STR_TO_DATE(CAST(business_id AS CHAR), '%Y%m%d') AS checkin_date
            FROM point_log
            WHERE user_id=? AND deleted=0 AND business_type='DAILY_CHECKIN'
              AND business_id>=? AND business_id<?
            ORDER BY business_id
            """,
        (rs, n) -> rs.getDate("checkin_date").toLocalDate(),
        userId,
        Long.parseLong(selected.atDay(1).format(DateTimeFormatter.BASIC_ISO_DATE)),
        Long.parseLong(selected.plusMonths(1).atDay(1).format(DateTimeFormatter.BASIC_ISO_DATE)));
  }

  @Transactional
  @Override
  public Wallet checkIn(Long userId) {
    // 签到事实直接记录在积分流水中，唯一键保证同一用户同一天只能奖励一次。
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
    long businessId = Long.parseLong(today.format(DateTimeFormatter.BASIC_ISO_DATE));
    ensureAccount(userId);
    lockedBalance(userId);
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM point_log WHERE user_id=? AND business_type='DAILY_CHECKIN' AND business_id=? AND deleted=0",
            Integer.class,
            userId,
            businessId)
        > 0) throw new IllegalArgumentException("今天已经签到过了");
    creditLocked(userId, 5, "DAILY_CHECKIN", businessId, "每日签到奖励");
    return wallet(userId);
  }

  @Transactional
  @Override
  public void registration(Long userId) {
    credit(userId, 20, "REGISTER", userId, "注册教师账号奖励");
  }

  @Transactional
  @Override
  public void caseApproved(Long authorId, Long caseId, boolean paid) {
    ensureAccount(authorId);
    lockedBalance(authorId);
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM point_log WHERE user_id=? AND business_id=? AND business_type IN ('CASE_APPROVED','CASE_APPROVED_FREE','CASE_APPROVED_PAID')",
            Integer.class,
            authorId,
            caseId)
        > 0) return;
    credit(
        authorId,
        paid ? 10 : 20,
        paid ? "CASE_APPROVED_PAID" : "CASE_APPROVED_FREE",
        caseId,
        paid ? "积分案例审核通过奖励" : "免费案例审核通过奖励");
  }

  @Transactional
  @Override
  public boolean unlock(Long caseId, Long buyerId, Long authorId, int price) {
    // 扣除买方积分、增加作者积分和记录解锁关系必须在同一事务中完成。
    if (price < 1) throw new IllegalArgumentException("案例积分价格不正确");
    if (buyerId.equals(authorId)) throw new IllegalArgumentException("作者无需解锁自己的案例");
    ensureAccount(buyerId);
    ensureAccount(authorId);
    long first = Math.min(buyerId, authorId), second = Math.max(buyerId, authorId);
    lockedBalance(first);
    lockedBalance(second);
    if (unlocked(buyerId, caseId)) return false;
    debitLocked(buyerId, price, "CASE_UNLOCK_SPEND", caseId, "解锁积分案例");
    creditLocked(authorId, price, "CASE_UNLOCK_INCOME", caseId, "案例被解锁获得积分");
    return true;
  }

  @Override
  public boolean unlocked(Long userId, Long caseId) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM point_log WHERE user_id=? AND business_id=? AND business_type IN ('CASE_UNLOCK','CASE_UNLOCK_SPEND') AND deleted=0",
            Integer.class,
            userId,
            caseId)
        > 0;
  }

  @Transactional
  @Override
  public Wallet grant(
      Long operatorId, Long recipientId, int amount, String reason, String password) {
    if (operatorId.equals(recipientId)) throw new IllegalArgumentException("管理员不能给自己发放积分");
    if (amount < 1 || amount > 1000) throw new IllegalArgumentException("单次发放积分须为1到1000分");
    if (reason == null || reason.isBlank() || reason.length() > 500)
      throw new IllegalArgumentException("请填写500字以内的发放原因");
    SysUser admin = users.selectById(operatorId), recipient = users.selectById(recipientId);
    if (!users.selectRoleCodes(operatorId).contains("ADMIN"))
      throw new IllegalArgumentException("只有管理员可以发放积分");
    if (admin == null
        || !Integer.valueOf(1).equals(admin.getStatus())
        || password == null
        || !passwords.matches(password, admin.getPassword()))
      throw new IllegalArgumentException("管理员密码不正确");
    if (recipient == null || !Integer.valueOf(1).equals(recipient.getStatus()))
      throw new IllegalArgumentException("接收账号不存在或已停用");
    // 管理员、接收者、原因和金额全部保存在积分流水中，无需单独维护重复的发放表。
    long grantId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    credit(recipientId, amount, "ADMIN_GRANT", grantId, "管理员发放：" + reason.trim());
    jdbc.update(
        "UPDATE point_log SET operator_id=? WHERE user_id=? AND business_type='ADMIN_GRANT' AND business_id=?",
        operatorId,
        recipientId,
        grantId);
    return wallet(recipientId);
  }

  private void credit(Long userId, int amount, String type, Long businessId, String remark) {
    ensureAccount(userId);
    lockedBalance(userId);
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM point_log WHERE user_id=? AND business_type=? AND business_id=?",
            Integer.class,
            userId,
            type,
            businessId)
        > 0) return;
    creditLocked(userId, amount, type, businessId, remark);
  }

  private void creditLocked(Long userId, int amount, String type, Long businessId, String remark) {
    jdbc.update(
        "UPDATE point_account SET balance=balance+?,total_income=total_income+?,version=version+1 WHERE user_id=?",
        amount,
        amount,
        userId);
    log(userId, type, businessId, amount, remark);
  }

  private void debitLocked(Long userId, int amount, String type, Long businessId, String remark) {
    int changed =
        jdbc.update(
            "UPDATE point_account SET balance=balance-?,total_expense=total_expense+?,version=version+1 WHERE user_id=? AND balance>=?",
            amount,
            amount,
            userId,
            amount);
    if (changed != 1) throw new IllegalArgumentException("积分不足，无法解锁案例");
    log(userId, type, businessId, -amount, remark);
  }

  private void log(Long userId, String type, Long businessId, int amount, String remark) {
    Integer balance =
        jdbc.queryForObject(
            "SELECT balance FROM point_account WHERE user_id=?", Integer.class, userId);
    jdbc.update(
        "INSERT INTO point_log(user_id,business_type,business_id,change_amount,balance_after,remark) VALUES(?,?,?,?,?,?)",
        userId,
        type,
        businessId,
        amount,
        balance,
        remark);
  }

  private void ensureAccount(Long userId) {
    jdbc.update("INSERT IGNORE INTO point_account(user_id) VALUES(?)", userId);
  }

  private int lockedBalance(Long userId) {
    return jdbc.queryForObject(
        "SELECT balance FROM point_account WHERE user_id=? FOR UPDATE", Integer.class, userId);
  }
}
