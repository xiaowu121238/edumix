package com.itxiaowu.edumix.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itxiaowu.edumix.common.PageResponse;
import com.itxiaowu.edumix.dto.*;
import com.itxiaowu.edumix.entity.*;
import com.itxiaowu.edumix.mapper.*;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * 教学资源业务服务。
 *
 * <p>统一处理本地文件存储、元数据维护、公开审核、预览下载、收藏、纠错和驳回后重新提交。 未审核通过的公共资源不会出现在其他用户的资源库中。
 */
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {
  private static final Set<String> IMAGE = Set.of("jpg", "jpeg", "png", "gif", "webp");
  private static final Set<String> VIDEO = Set.of("mp4");
  private static final Set<String> PDF = Set.of("pdf");
  private static final Set<String> DOCUMENT =
      Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip", "rar", "7z");
  private static final Set<String> VISIBILITY = Set.of("PRIVATE", "PUBLIC");

  private final TeachingResourceMapper resourceMapper;
  private final ResourceCategoryMapper categoryMapper;
  private final ResourceFavoriteMapper favoriteMapper;
  private final ResourceCorrectionMapper correctionMapper;
  private final ContentReviewMapper reviewMapper;
  private final SysUserMapper userMapper;
  private final AuditNotificationService notifications;
  private final CorrectionNotificationService correctionNotifications;

  @Value("${edumix.storage.location:uploads}")
  private String storageLocation;

  @Override
  public List<ResourceCategory> categories() {
    return categoryMapper.selectList(
        Wrappers.<ResourceCategory>lambdaQuery()
            .eq(ResourceCategory::getStatus, 1)
            .orderByAsc(ResourceCategory::getSortOrder)
            .orderByAsc(ResourceCategory::getId));
  }

  @Override
  public List<ResourceResponse> list(
      String keyword, String type, Long categoryId, Boolean favoriteOnly, UserPrincipal user) {
    LambdaQueryWrapper<TeachingResource> query =
        resourceQuery(keyword, type, categoryId, favoriteOnly, user);
    Set<Long> favorites =
        favoriteMapper.findByUser(user.getUserId()).stream()
            .map(ResourceFavorite::getResourceId)
            .collect(java.util.stream.Collectors.toSet());
    return resourceMapper.selectList(query).stream()
        .map(r -> response(r, favorites.contains(r.getId())))
        .toList();
  }

  @Override
  public PageResponse<ResourceResponse> page(
      String keyword,
      String type,
      Long categoryId,
      Boolean favoriteOnly,
      long page,
      long size,
      UserPrincipal user) {
    Page<TeachingResource> result =
        resourceMapper.selectPage(
            pageRequest(page, size), resourceQuery(keyword, type, categoryId, favoriteOnly, user));
    Set<Long> favorites = favoriteIds(result.getRecords(), user.getUserId());
    return PageResponse.from(
        result, resource -> response(resource, favorites.contains(resource.getId())));
  }

  @Override
  public List<ResourceResponse> mine(UserPrincipal user) {
    Set<Long> favorites =
        favoriteMapper.findByUser(user.getUserId()).stream()
            .map(ResourceFavorite::getResourceId)
            .collect(java.util.stream.Collectors.toSet());
    return resourceMapper
        .selectList(
            Wrappers.<TeachingResource>lambdaQuery()
                .eq(TeachingResource::getUploaderId, user.getUserId())
                .orderByDesc(TeachingResource::getCreateTime))
        .stream()
        .map(resource -> response(resource, favorites.contains(resource.getId())))
        .toList();
  }

  @Override
  public PageResponse<ResourceResponse> minePage(
      String keyword,
      String type,
      Long categoryId,
      Boolean favoriteOnly,
      long page,
      long size,
      UserPrincipal user) {
    LambdaQueryWrapper<TeachingResource> query =
        resourceQuery(keyword, type, categoryId, favoriteOnly, user)
            .eq(TeachingResource::getUploaderId, user.getUserId());
    Page<TeachingResource> result = resourceMapper.selectPage(pageRequest(page, size), query);
    Set<Long> favorites = favoriteIds(result.getRecords(), user.getUserId());
    return PageResponse.from(
        result, resource -> response(resource, favorites.contains(resource.getId())));
  }

  @Transactional
  @Override
  public ResourceResponse upload(
      MultipartFile file,
      String resourceName,
      String description,
      Long categoryId,
      String visibility,
      UserPrincipal user) {
    if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择需要上传的文件");
    String original = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
    String extension = extension(original);
    String type = resourceType(extension);
    com.itxiaowu.edumix.service.ResourceFileValidator.validate(file, extension);
    String scope = normalizeVisibility(visibility);
    requireCategory(categoryId);
    String month = LocalDate.now().toString().substring(0, 7);
    String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
    Path root = Paths.get(storageLocation).toAbsolutePath().normalize();
    Path target = root.resolve(month).resolve(storedName).normalize();
    if (!target.startsWith(root)) throw new IllegalArgumentException("文件保存路径不合法");
    try {
      Files.createDirectories(target.getParent());
      file.transferTo(target);
    } catch (IOException e) {
      throw new IllegalArgumentException("文件保存失败");
    }

    TeachingResource entity = new TeachingResource();
    entity.setResourceNo("RES-" + user.getUserId() + "-" + System.currentTimeMillis());
    entity.setResourceName(
        resourceName == null || resourceName.isBlank() ? original : resourceName.trim());
    entity.setResourceType(type);
    entity.setDescription(clean(description));
    entity.setFileUrl(month + "/" + storedName);
    entity.setFileSize(file.getSize());
    entity.setUploaderId(user.getUserId());
    entity.setCategoryId(categoryId);
    entity.setVisibility(scope);
    entity.setAuditStatus(scope.equals("PRIVATE") ? "APPROVED" : "PENDING");
    entity.setDownloadCount(0);
    entity.setReferenceCount(0);
    try {
      resourceMapper.insert(entity);
    } catch (RuntimeException e) {
      try {
        Files.deleteIfExists(target);
      } catch (IOException ignored) {
      }
      throw e;
    }
    return response(resourceMapper.selectById(entity.getId()), false);
  }

  @Transactional
  @Override
  public ResourceResponse update(Long id, ResourceUpdateRequest request, UserPrincipal user) {
    return update(id, request, null, user);
  }

  @Transactional
  @Override
  public ResourceResponse update(
      Long id, ResourceUpdateRequest request, MultipartFile file, UserPrincipal user) {
    TeachingResource entity = requireWritable(id, user);
    if ("PENDING".equals(entity.getAuditStatus()) && "PUBLIC".equals(entity.getVisibility()))
      throw new IllegalArgumentException("资源正在审核中，请等待审核结果后再修改");
    requireCategory(request.categoryId());
    String scope = normalizeVisibility(request.visibility());
    Path root = Paths.get(storageLocation).toAbsolutePath().normalize();
    Path replacement = null;
    Path previous = null;
    String replacementUrl = null;
    String replacementType = null;
    if (file != null) {
      if (file.isEmpty()) throw new IllegalArgumentException("替换文件不能为空");
      String original = Optional.ofNullable(file.getOriginalFilename()).orElse("");
      String ext = extension(original);
      replacementType = resourceType(ext);
      com.itxiaowu.edumix.service.ResourceFileValidator.validate(file, ext);
      String month = LocalDate.now().toString().substring(0, 7);
      replacementUrl = month + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
      replacement = root.resolve(replacementUrl).normalize();
      previous = root.resolve(entity.getFileUrl()).normalize();
      if (!replacement.startsWith(root) || !previous.startsWith(root))
        throw new IllegalArgumentException("文件保存路径不合法");
      try {
        Files.createDirectories(replacement.getParent());
        file.transferTo(replacement);
      } catch (IOException exception) {
        try {
          Files.deleteIfExists(replacement);
        } catch (IOException ignored) {
        }
        throw new IllegalArgumentException("替换文件保存失败");
      }
    }
    Path newFile = replacement;
    Path oldFile = previous;
    try {
      entity.setResourceName(request.resourceName().trim());
      entity.setDescription(clean(request.description()));
      entity.setCategoryId(request.categoryId());
      entity.setVisibility(scope);
      // 公共资源一旦被修改就立即失去已通过状态，旧版本在重新审核期间也不再公开。
      entity.setAuditStatus(scope.equals("PRIVATE") ? "APPROVED" : "PENDING");
      if (newFile != null) {
        entity.setFileUrl(replacementUrl);
        entity.setResourceType(replacementType);
        entity.setFileSize(file.getSize());
      }
      resourceMapper.updateById(entity);
      if (newFile != null)
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
              @Override
              public void afterCommit() {
                try {
                  Files.deleteIfExists(oldFile);
                } catch (IOException ignored) {
                }
              }

              @Override
              public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED)
                  try {
                    Files.deleteIfExists(newFile);
                  } catch (IOException ignored) {
                  }
              }
            });
    } catch (RuntimeException exception) {
      if (newFile != null)
        try {
          Files.deleteIfExists(newFile);
        } catch (IOException ignored) {
        }
      throw exception;
    }
    return response(resourceMapper.selectById(id), favoriteMapper.exists(user.getUserId(), id) > 0);
  }

  @Override
  public List<ResourceReviewHistoryResponse> reviewHistory(Long id, UserPrincipal user) {
    TeachingResource resource = resourceMapper.selectById(id);
    if (resource == null) throw new IllegalArgumentException("资源不存在");
    if (!resource.getUploaderId().equals(user.getUserId())
        && !user.roles().contains("RESOURCE_MANAGER")
        && !user.roles().contains("ADMIN")) throw new AccessDeniedException("无权查看该资源的审核记录");
    return reviewMapper
        .selectList(
            Wrappers.<ContentReview>lambdaQuery()
                .eq(ContentReview::getTargetType, "RESOURCE")
                .eq(ContentReview::getTargetId, id)
                .orderByDesc(ContentReview::getSubmissionNo))
        .stream()
        .map(
            review -> {
              SysUser reviewer = userMapper.selectById(review.getReviewerId());
              return new ResourceReviewHistoryResponse(
                  review.getSubmissionNo(),
                  review.getReviewResult(),
                  review.getReviewComment(),
                  reviewer == null ? "审核负责人" : reviewer.getRealName(),
                  review.getReviewedAt());
            })
        .toList();
  }

  @Transactional
  @Override
  public ResourceResponse review(Long id, ResourceReviewRequest request, UserPrincipal reviewer) {
    // 负责人不能审核自己上传的资源，避免上传与审核由同一账号完成。
    TeachingResource entity = resourceMapper.selectById(id);
    if (entity == null) throw new IllegalArgumentException("资源不存在");
    if (entity.getUploaderId().equals(reviewer.getUserId()))
      throw new IllegalArgumentException("不能审核自己上传的资源");
    if (!"PENDING".equals(entity.getAuditStatus()) || !"PUBLIC".equals(entity.getVisibility()))
      throw new IllegalArgumentException("只能审核待审核的共享资源");
    if ("REJECTED".equals(request.result())
        && (request.comment() == null || request.comment().isBlank()))
      throw new IllegalArgumentException("驳回时请填写审核意见");
    int submission =
        Math.toIntExact(
                reviewMapper.selectCount(
                    Wrappers.<ContentReview>lambdaQuery()
                        .eq(ContentReview::getTargetType, "RESOURCE")
                        .eq(ContentReview::getTargetId, id)))
            + 1;
    ContentReview record = new ContentReview();
    record.setTargetType("RESOURCE");
    record.setTargetId(id);
    record.setReviewerId(reviewer.getUserId());
    record.setSubmissionNo(submission);
    record.setReviewResult(request.result());
    record.setReviewComment(clean(request.comment()));
    record.setReviewedAt(java.time.LocalDateTime.now());
    reviewMapper.insert(record);
    entity.setAuditStatus(request.result());
    resourceMapper.updateById(entity);
    notifications.result(
        entity.getUploaderId(),
        reviewer.getUserId(),
        "RESOURCE",
        id,
        entity.getResourceName(),
        "APPROVED".equals(request.result()),
        request.comment());
    if ("APPROVED".equals(request.result()))
      resolveAcceptedCorrections(entity, reviewer.getUserId());
    return response(resourceMapper.selectById(id), false);
  }

  @Transactional
  @Override
  public void delete(Long id, UserPrincipal user) {
    TeachingResource entity = requireWritable(id, user);
    resourceMapper.deleteById(entity);
    Path root = Paths.get(storageLocation).toAbsolutePath().normalize();
    Path path = root.resolve(entity.getFileUrl()).normalize();
    if (!path.startsWith(root)) throw new IllegalArgumentException("文件保存路径不合法");
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      throw new IllegalArgumentException("资源记录已删除，但文件清理失败");
    }
  }

  @Transactional
  @Override
  public boolean toggleFavorite(Long id, UserPrincipal user) {
    requireReadable(id, user);
    if (favoriteMapper.exists(user.getUserId(), id) > 0) {
      favoriteMapper.remove(user.getUserId(), id);
      return false;
    }
    favoriteMapper.add(user.getUserId(), id);
    return true;
  }

  @Transactional
  @Override
  public void submitCorrection(Long id, CorrectionRequest request, UserPrincipal user) {
    // 同一用户对同一资源只能存在一条尚未处理完成的纠错，防止重复通知。
    TeachingResource resource = requireReadable(id, user);
    if (resource.getUploaderId().equals(user.getUserId()))
      throw new IllegalArgumentException("不能纠错自己上传的资源");
    long active =
        correctionMapper.selectCount(
            Wrappers.<ResourceCorrection>lambdaQuery()
                .eq(ResourceCorrection::getResourceId, id)
                .eq(ResourceCorrection::getSubmitterId, user.getUserId())
                .in(ResourceCorrection::getStatus, "PENDING", "ACCEPTED"));
    if (active > 0) throw new IllegalArgumentException("您已提交过尚未处理完成的纠错，请勿重复提交");
    ResourceCorrection correction = new ResourceCorrection();
    correction.setResourceId(id);
    correction.setSubmitterId(user.getUserId());
    correction.setCorrectionContent(request.content().trim());
    correction.setStatus("PENDING");
    correctionMapper.insert(correction);
    SysUser submitter = userMapper.selectById(user.getUserId());
    correctionNotifications.submitted(
        correction.getId(),
        id,
        resource.getResourceName(),
        resource.getUploaderId(),
        user.getUserId(),
        submitter == null ? user.getUsername() : submitter.getRealName(),
        correction.getCorrectionContent());
  }

  @Override
  public List<CorrectionResponse> pendingCorrections() {
    return correctionMapper
        .selectList(
            Wrappers.<ResourceCorrection>lambdaQuery()
                .eq(ResourceCorrection::getStatus, "PENDING")
                .orderByAsc(ResourceCorrection::getCreateTime))
        .stream()
        .map(this::correctionResponse)
        .toList();
  }

  @Transactional
  @Override
  public CorrectionResponse handleCorrection(
      Long correctionId, CorrectionHandleRequest request, UserPrincipal handler) {
    if (!handler.roles().contains("RESOURCE_MANAGER") && !handler.roles().contains("ADMIN"))
      throw new AccessDeniedException("只有资源负责人或管理员可以处理纠错");
    ResourceCorrection correction = correctionMapper.selectById(correctionId);
    if (correction == null) throw new IllegalArgumentException("纠错记录不存在");
    if (!"PENDING".equals(correction.getStatus()))
      throw new IllegalArgumentException("该纠错已经处理，不能重复操作");
    if (correction.getSubmitterId().equals(handler.getUserId()))
      throw new IllegalArgumentException("不能处理自己提交的纠错");
    TeachingResource resource = resourceMapper.selectById(correction.getResourceId());
    if (resource == null) throw new IllegalArgumentException("关联资源不存在");
    correction.setStatus(request.result());
    correction.setHandlerId(handler.getUserId());
    correction.setHandleComment(request.comment().trim());
    correction.setHandledAt(java.time.LocalDateTime.now());
    correctionMapper.updateById(correction);
    correctionNotifications.handled(
        correction.getId(),
        resource.getResourceName(),
        correction.getSubmitterId(),
        resource.getUploaderId(),
        handler.getUserId(),
        "ACCEPTED".equals(request.result()),
        request.comment());
    return correctionResponse(correctionMapper.selectById(correctionId));
  }

  @Transactional
  @Override
  public FilePayload file(Long id, boolean download, UserPrincipal user) {
    TeachingResource entity = requireReadable(id, user);
    Path root = Paths.get(storageLocation).toAbsolutePath().normalize();
    Path path = root.resolve(entity.getFileUrl()).normalize();
    if (!path.startsWith(root) || !Files.isRegularFile(path))
      throw new IllegalArgumentException("资源文件不存在");
    if (download) {
      entity.setDownloadCount(Optional.ofNullable(entity.getDownloadCount()).orElse(0) + 1);
      resourceMapper.updateById(entity);
    }
    String contentType;
    try {
      contentType = Files.probeContentType(path);
    } catch (IOException e) {
      contentType = null;
    }
    if (contentType == null) contentType = "application/octet-stream";
    return new FilePayload(
        new FileSystemResource(path), contentType, entity.getResourceName(), download);
  }

  @Override
  public Map<String, Long> mineStats(UserPrincipal user) {
    long total =
        resourceMapper.selectCount(
            Wrappers.<TeachingResource>lambdaQuery()
                .eq(TeachingResource::getUploaderId, user.getUserId()));
    long approved =
        resourceMapper.selectCount(
            Wrappers.<TeachingResource>lambdaQuery()
                .eq(TeachingResource::getUploaderId, user.getUserId())
                .eq(TeachingResource::getVisibility, "PUBLIC")
                .eq(TeachingResource::getAuditStatus, "APPROVED"));
    return Map.of("total", total, "approved", approved);
  }

  @Override
  public List<ResourceResponse> byIds(List<Long> ids, UserPrincipal user) {
    if (ids == null || ids.size() > 100) throw new IllegalArgumentException("每次最多查询100个资源");
    List<Long> unique = ids.stream().filter(Objects::nonNull).distinct().toList();
    if (unique.isEmpty()) return List.of();
    List<TeachingResource> readable =
        resourceMapper
            .selectList(
                Wrappers.<TeachingResource>lambdaQuery().in(TeachingResource::getId, unique))
            .stream()
            .filter(r -> canRead(r, user))
            .toList();
    Set<Long> favorites = favoriteIds(readable, user.getUserId());
    return readable.stream().map(r -> response(r, favorites.contains(r.getId()))).toList();
  }

  private TeachingResource requireReadable(Long id, UserPrincipal user) {
    TeachingResource entity = resourceMapper.selectById(id);
    if (entity == null) throw new IllegalArgumentException("资源不存在");
    if (!canRead(entity, user)) throw new AccessDeniedException("无权访问该资源");
    return entity;
  }

  private TeachingResource requireWritable(Long id, UserPrincipal user) {
    TeachingResource entity = resourceMapper.selectById(id);
    if (entity == null) throw new IllegalArgumentException("资源不存在");
    if (!user.roles().contains("ADMIN") && !entity.getUploaderId().equals(user.getUserId()))
      throw new AccessDeniedException("无权修改该资源");
    return entity;
  }

  private boolean canRead(TeachingResource r, UserPrincipal principal) {
    if (principal.roles().contains("ADMIN")
        || principal.roles().contains("RESOURCE_MANAGER")
        || r.getUploaderId().equals(principal.getUserId())) return true;
    return "PUBLIC".equals(r.getVisibility()) && "APPROVED".equals(r.getAuditStatus());
  }

  /** 构造资源检索条件。关键词仍按“名称或简介包含关键词”匹配，但权限、收藏等条件在分页前由数据库完成。 */
  private LambdaQueryWrapper<TeachingResource> resourceQuery(
      String keyword, String type, Long categoryId, Boolean favoriteOnly, UserPrincipal user) {
    LambdaQueryWrapper<TeachingResource> query = Wrappers.lambdaQuery();
    if (keyword != null && !keyword.isBlank()) {
      String normalized = keyword.trim();
      query.and(
          q ->
              q.like(TeachingResource::getResourceName, normalized)
                  .or()
                  .like(TeachingResource::getDescription, normalized));
    }
    if (type != null && !type.isBlank())
      query.eq(TeachingResource::getResourceType, type.toUpperCase());
    if (categoryId != null) query.eq(TeachingResource::getCategoryId, categoryId);
    if (!user.roles().contains("ADMIN") && !user.roles().contains("RESOURCE_MANAGER"))
      query.and(
          q ->
              q.eq(TeachingResource::getUploaderId, user.getUserId())
                  .or(
                      visible ->
                          visible
                              .eq(TeachingResource::getVisibility, "PUBLIC")
                              .eq(TeachingResource::getAuditStatus, "APPROVED")));
    if (Boolean.TRUE.equals(favoriteOnly))
      query.inSql(
          TeachingResource::getId,
          "SELECT resource_id FROM resource_favorite WHERE user_id = " + user.getUserId());
    return query.orderByDesc(TeachingResource::getCreateTime).orderByDesc(TeachingResource::getId);
  }

  private Page<TeachingResource> pageRequest(long page, long size) {
    return new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size)));
  }

  private Set<Long> favoriteIds(List<TeachingResource> records, Long userId) {
    if (records.isEmpty()) return Set.of();
    return new HashSet<>(
        favoriteMapper.findIdsInPage(
            userId, records.stream().map(TeachingResource::getId).toList()));
  }

  private CorrectionResponse correctionResponse(ResourceCorrection correction) {
    TeachingResource resource = resourceMapper.selectById(correction.getResourceId());
    SysUser uploader = resource == null ? null : userMapper.selectById(resource.getUploaderId());
    SysUser submitter = userMapper.selectById(correction.getSubmitterId());
    SysUser handler =
        correction.getHandlerId() == null ? null : userMapper.selectById(correction.getHandlerId());
    return new CorrectionResponse(
        correction.getId(),
        correction.getResourceId(),
        resource == null ? "资源已删除" : resource.getResourceName(),
        resource == null ? null : resource.getResourceType(),
        resource == null ? null : resource.getUploaderId(),
        uploader == null ? "未知上传者" : uploader.getRealName(),
        correction.getSubmitterId(),
        submitter == null ? "未知用户" : submitter.getRealName(),
        correction.getCorrectionContent(),
        correction.getStatus(),
        correction.getHandlerId(),
        handler == null ? null : handler.getRealName(),
        correction.getHandleComment(),
        correction.getHandledAt(),
        correction.getCreateTime());
  }

  private void resolveAcceptedCorrections(TeachingResource resource, Long reviewerId) {
    // 资源修改并重新审核通过后，自动关闭此前已经确认属实的纠错记录。
    List<ResourceCorrection> accepted =
        correctionMapper.selectList(
            Wrappers.<ResourceCorrection>lambdaQuery()
                .eq(ResourceCorrection::getResourceId, resource.getId())
                .eq(ResourceCorrection::getStatus, "ACCEPTED"));
    for (ResourceCorrection correction : accepted) {
      correction.setStatus("RESOLVED");
      correction.setHandledAt(java.time.LocalDateTime.now());
      correctionMapper.updateById(correction);
      correctionNotifications.resolved(
          correction.getId(),
          resource.getResourceName(),
          correction.getSubmitterId(),
          resource.getUploaderId(),
          reviewerId);
    }
  }

  private ResourceResponse response(TeachingResource r, boolean favorite) {
    ResourceCategory category =
        r.getCategoryId() == null ? null : categoryMapper.selectById(r.getCategoryId());
    boolean previewable = Set.of("IMAGE", "PDF", "VIDEO").contains(r.getResourceType());
    ContentReview last =
        reviewMapper.selectOne(
            Wrappers.<ContentReview>lambdaQuery()
                .eq(ContentReview::getTargetType, "RESOURCE")
                .eq(ContentReview::getTargetId, r.getId())
                .orderByDesc(ContentReview::getSubmissionNo)
                .last("LIMIT 1"));
    return new ResourceResponse(
        r.getId(),
        r.getResourceNo(),
        r.getResourceName(),
        r.getResourceType(),
        r.getDescription(),
        r.getFileSize(),
        r.getUploaderId(),
        r.getCategoryId(),
        category == null ? null : category.getCategoryName(),
        r.getVisibility(),
        r.getAuditStatus(),
        r.getDownloadCount(),
        r.getReferenceCount(),
        favorite,
        previewable,
        "/api/resources/" + r.getId() + "/file",
        last == null ? null : last.getReviewComment(),
        r.getCreateTime(),
        r.getUpdateTime());
  }

  private void requireCategory(Long id) {
    if (id != null && categoryMapper.selectById(id) == null)
      throw new IllegalArgumentException("资源分类不存在");
  }

  private String normalizeVisibility(String value) {
    String result = value == null || value.isBlank() ? "PRIVATE" : value.toUpperCase();
    if (!VISIBILITY.contains(result)) throw new IllegalArgumentException("可见范围只能是仅自己或公开");
    return result;
  }

  private String extension(String name) {
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1) throw new IllegalArgumentException("文件缺少扩展名");
    return name.substring(dot + 1).toLowerCase();
  }

  private String resourceType(String ext) {
    if (IMAGE.contains(ext)) return "IMAGE";
    if (PDF.contains(ext)) return "PDF";
    if (VIDEO.contains(ext)) return "VIDEO";
    if (DOCUMENT.contains(ext)) return "DOCUMENT";
    throw new IllegalArgumentException("暂不支持该文件格式");
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
