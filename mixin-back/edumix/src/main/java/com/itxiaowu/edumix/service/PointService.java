package com.itxiaowu.edumix.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 积分业务接口，定义账户、签到、奖励、解锁和管理员发放能力。 */
public interface PointService {
  Wallet wallet(Long userId);

  List<Entry> history(Long userId);

  List<LocalDate> checkInDates(Long userId, int year, int month);

  Wallet checkIn(Long userId);

  void registration(Long userId);

  void caseApproved(Long authorId, Long caseId, boolean paid);

  boolean unlock(Long caseId, Long buyerId, Long authorId, int price);

  boolean unlocked(Long userId, Long caseId);

  Wallet grant(Long operatorId, Long recipientId, int amount, String reason, String password);

  record Wallet(int balance, int totalIncome, int totalExpense, boolean checkedInToday) {}

  record Entry(
      Long id,
      String businessType,
      int changeAmount,
      int balanceAfter,
      String remark,
      LocalDateTime createTime) {}
}
