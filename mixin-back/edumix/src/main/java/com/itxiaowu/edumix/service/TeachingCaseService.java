package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.*;
import com.itxiaowu.edumix.security.UserPrincipal;
import java.util.List;

/** 教学案例业务接口，定义草稿、发布、审核、收藏和积分解锁能力。 */
public interface TeachingCaseService {
  List<CaseResponse> list(
      String keyword, Long courseId, String status, Boolean favoriteOnly, UserPrincipal user);

  List<CaseResponse> published(String keyword, Long courseId, UserPrincipal user);

  List<CaseResponse> mine(UserPrincipal user);

  CaseResponse createDraft(CaseDraftRequest request, UserPrincipal user);

  CaseResponse updateDraft(Long id, CaseDraftRequest request, UserPrincipal user);

  CaseResponse get(Long id, UserPrincipal user);

  CaseResponse unlock(Long id, UserPrincipal user);

  CaseResponse create(CaseRequest request, UserPrincipal user);

  CaseResponse update(Long id, CaseRequest request, UserPrincipal user);

  CaseResponse submit(Long id, UserPrincipal user);

  CaseResponse review(Long id, CaseReviewRequest request, UserPrincipal user);

  boolean toggleFavorite(Long id, UserPrincipal user);

  void delete(Long id, UserPrincipal user);
}
