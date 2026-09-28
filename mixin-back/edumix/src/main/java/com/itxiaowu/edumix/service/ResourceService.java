package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.*;
import com.itxiaowu.edumix.entity.ResourceCategory;
import com.itxiaowu.edumix.security.UserPrincipal;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/** 教学资源业务接口，定义上传、检索、审核、收藏、纠错和文件读取能力。 */
public interface ResourceService {
  List<ResourceCategory> categories();

  List<ResourceResponse> list(
      String keyword, String type, Long categoryId, Boolean favoriteOnly, UserPrincipal user);

  List<ResourceResponse> mine(UserPrincipal user);

  ResourceResponse upload(
      MultipartFile file,
      String resourceName,
      String description,
      Long categoryId,
      String visibility,
      UserPrincipal user);

  ResourceResponse update(Long id, ResourceUpdateRequest request, UserPrincipal user);

  ResourceResponse update(
      Long id, ResourceUpdateRequest request, MultipartFile file, UserPrincipal user);

  List<ResourceReviewHistoryResponse> reviewHistory(Long id, UserPrincipal user);

  ResourceResponse review(Long id, ResourceReviewRequest request, UserPrincipal reviewer);

  void delete(Long id, UserPrincipal user);

  boolean toggleFavorite(Long id, UserPrincipal user);

  void submitCorrection(Long id, CorrectionRequest request, UserPrincipal user);

  List<CorrectionResponse> pendingCorrections();

  CorrectionResponse handleCorrection(
      Long correctionId, CorrectionHandleRequest request, UserPrincipal handler);

  FilePayload file(Long id, boolean download, UserPrincipal user);

  record FilePayload(Resource resource, String contentType, String filename, boolean download) {}
}
