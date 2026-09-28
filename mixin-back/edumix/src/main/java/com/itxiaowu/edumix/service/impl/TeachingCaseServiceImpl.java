package com.itxiaowu.edumix.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itxiaowu.edumix.dto.*;
import com.itxiaowu.edumix.entity.*;
import com.itxiaowu.edumix.mapper.*;
import com.itxiaowu.edumix.security.UserPrincipal;
import com.itxiaowu.edumix.service.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教学案例业务服务。
 *
 * <p>区分个人草稿与公共案例，负责富文本内容、资源引用、提交审核、公开查询、收藏和积分解锁。 只有审核通过的公共案例才能被其他用户查看。
 */
@Service
@RequiredArgsConstructor
public class TeachingCaseServiceImpl implements TeachingCaseService {
  private static final Set<String> VISIBILITY = Set.of("PRIVATE", "PUBLIC");
  private static final Set<String> ACCESS = Set.of("FREE", "POINTS");
  private final TeachingCaseMapper caseMapper;
  private final CaseFavoriteMapper favoriteMapper;
  private final CaseRelationMapper relationMapper;
  private final CaseReviewMapper reviewMapper;
  private final CourseMapper courseMapper;
  private final KnowledgePointMapper pointMapper;
  private final TeachingResourceMapper resourceMapper;
  private final SysUserMapper userMapper;
  private final AuditNotificationService notifications;
  private final PointService points;

  @Override
  public List<CaseResponse> list(
      String keyword, Long courseId, String status, Boolean favoriteOnly, UserPrincipal user) {
    var query =
        Wrappers.<TeachingCase>lambdaQuery()
            // 先完成课程、关键词和审核状态筛选，再让优质案例获得更高展示优先级。
            .orderByDesc(TeachingCase::getExcellent)
            .orderByDesc(TeachingCase::getUpdateTime)
            .orderByDesc(TeachingCase::getId);
    if (keyword != null && !keyword.isBlank())
      query.like(TeachingCase::getCaseName, keyword.trim());
    if (courseId != null) query.eq(TeachingCase::getCourseId, courseId);
    if (status != null && !status.isBlank())
      query.eq(TeachingCase::getAuditStatus, status.toUpperCase());
    Set<Long> favorites =
        favoriteMapper.findByUser(user.getUserId()).stream()
            .map(CaseFavorite::getCaseId)
            .collect(java.util.stream.Collectors.toSet());
    return caseMapper.selectList(query).stream()
        .filter(c -> canRead(c, user))
        .filter(
            c ->
                !Boolean.TRUE.equals(favoriteOnly)
                    || (favorites.contains(c.getId()) && contentUnlocked(c, user)))
        .map(c -> response(c, favorites.contains(c.getId()) && contentUnlocked(c, user), user))
        .toList();
  }

  @Override
  public List<CaseResponse> published(String keyword, Long courseId, UserPrincipal user) {
    return list(keyword, courseId, "APPROVED", false, user).stream()
        .filter(c -> "PUBLIC".equals(c.visibility()))
        .toList();
  }

  @Override
  public List<CaseResponse> mine(UserPrincipal user) {
    return caseMapper
        .selectList(
            Wrappers.<TeachingCase>lambdaQuery()
                .eq(TeachingCase::getCreatorId, user.getUserId())
                .orderByDesc(TeachingCase::getUpdateTime)
                .orderByDesc(TeachingCase::getId))
        .stream()
        .map(c -> response(c, favoriteMapper.exists(user.getUserId(), c.getId()) > 0, user))
        .toList();
  }

  @Transactional
  @Override
  public CaseResponse createDraft(CaseDraftRequest request, UserPrincipal user) {
    TeachingCase entity = new TeachingCase();
    entity.setCreatorId(user.getUserId());
    entity.setCaseNo("CASE-DRAFT-" + UUID.randomUUID().toString().replace("-", ""));
    entity.setAuditStatus("DRAFT");
    entity.setExcellent(0);
    entity.setReferenceCount(0);
    applyDraft(entity, request, user);
    caseMapper.insert(entity);
    replaceDraftRelations(entity.getId(), request);
    return response(caseMapper.selectById(entity.getId()), false, user);
  }

  @Transactional
  @Override
  public CaseResponse updateDraft(Long id, CaseDraftRequest request, UserPrincipal user) {
    TeachingCase entity = requireWritable(id, user);
    if ("PENDING".equals(entity.getAuditStatus()) && "PUBLIC".equals(entity.getVisibility()))
      throw new IllegalArgumentException("审核中的案例不能编辑，请等待审核结果");
    if ("APPROVED".equals(entity.getAuditStatus()) && "PUBLIC".equals(entity.getVisibility()))
      throw new IllegalArgumentException("已发布案例不能直接修改，请联系管理员");
    applyDraft(entity, request, user);
    entity.setAuditStatus("DRAFT");
    entity.setExcellent(0);
    caseMapper.updateById(entity);
    replaceDraftRelations(id, request);
    return response(
        caseMapper.selectById(id), favoriteMapper.exists(user.getUserId(), id) > 0, user);
  }

  @Override
  public CaseResponse get(Long id, UserPrincipal user) {
    return response(
        requireReadable(id, user), favoriteMapper.exists(user.getUserId(), id) > 0, user);
  }

  @Transactional
  @Override
  public CaseResponse unlock(Long id, UserPrincipal user) {
    TeachingCase entity = requireReadable(id, user);
    if (!"APPROVED".equals(entity.getAuditStatus()) || !"POINTS".equals(entity.getAccessType()))
      throw new IllegalArgumentException("该案例无需积分解锁");
    if (!contentUnlocked(entity, user))
      points.unlock(id, user.getUserId(), entity.getCreatorId(), entity.getPointsPrice());
    return response(entity, favoriteMapper.exists(user.getUserId(), id) > 0, user);
  }

  @Transactional
  @Override
  public CaseResponse create(CaseRequest request, UserPrincipal user) {
    validate(request, user);
    TeachingCase entity = new TeachingCase();
    entity.setCreatorId(user.getUserId());
    entity.setCaseNo("CASE-" + user.getUserId() + "-" + System.currentTimeMillis());
    apply(entity, request);
    entity.setAuditStatus("PRIVATE".equals(entity.getVisibility()) ? "APPROVED" : "DRAFT");
    entity.setExcellent(0);
    entity.setReferenceCount(0);
    caseMapper.insert(entity);
    replaceRelations(entity.getId(), request);
    return response(caseMapper.selectById(entity.getId()), false, user);
  }

  @Transactional
  @Override
  public CaseResponse update(Long id, CaseRequest request, UserPrincipal user) {
    TeachingCase entity = requireWritable(id, user);
    if (("PENDING".equals(entity.getAuditStatus()) || "APPROVED".equals(entity.getAuditStatus()))
        && "PUBLIC".equals(entity.getVisibility()))
      throw new IllegalArgumentException("审核中或已发布的案例不能直接编辑");
    validate(request, user);
    apply(entity, request);
    entity.setAuditStatus("PRIVATE".equals(entity.getVisibility()) ? "APPROVED" : "DRAFT");
    entity.setExcellent(0);
    caseMapper.updateById(entity);
    replaceRelations(id, request);
    return response(
        caseMapper.selectById(id), favoriteMapper.exists(user.getUserId(), id) > 0, user);
  }

  @Transactional
  @Override
  public CaseResponse submit(Long id, UserPrincipal user) {
    // 草稿提交时才执行发布完整性校验，允许教师在本地草稿阶段逐步补充内容。
    TeachingCase entity = requireWritable(id, user);
    if (!"PUBLIC".equals(entity.getVisibility()))
      throw new IllegalArgumentException("只有公开案例可以提交审核");
    if (!Set.of("DRAFT", "REJECTED").contains(entity.getAuditStatus()))
      throw new IllegalArgumentException("只有草稿或被驳回的案例可以提交审核");
    if (entity.getCaseName() == null
        || entity.getCaseName().isBlank()
        || entity.getCourseId() == null
        || relationMapper.pointIds(id).isEmpty()
        || entity.getContent() == null
        || (Jsoup.parseBodyFragment(entity.getContent()).body().text().isBlank()
            && !entity.getContent().contains("data-resource-id")))
      throw new IllegalArgumentException("发布前请填写标题、课程、知识点和正文");
    if ("POINTS".equals(entity.getAccessType())
        && (entity.getPointsPrice() == null || entity.getPointsPrice() <= 0))
      throw new IllegalArgumentException("积分访问案例须设置大于0的积分价格");
    if ("POINTS".equals(entity.getAccessType()) && entity.getPointsPrice() > 3)
      throw new IllegalArgumentException("积分案例定价不能超过3分");
    entity.setAuditStatus("PENDING");
    caseMapper.updateById(entity);
    return response(
        caseMapper.selectById(id), favoriteMapper.exists(user.getUserId(), id) > 0, user);
  }

  private void applyDraft(TeachingCase entity, CaseDraftRequest request, UserPrincipal user) {
    Long courseId = request.courseId();
    if (courseId != null) {
      Course course = courseMapper.selectById(courseId);
      if (course == null) throw new IllegalArgumentException("所属课程不存在");
      if (!user.roles().contains("ADMIN") && !course.getOwnerId().equals(user.getUserId()))
        throw new AccessDeniedException("只能为自己负责的课程创建案例");
    }
    List<Long> pointIds = Optional.ofNullable(request.knowledgePointIds()).orElse(List.of());
    if (!pointIds.isEmpty() && courseId == null) throw new IllegalArgumentException("选择知识点前请先选择课程");
    for (Long pointId : new LinkedHashSet<>(pointIds)) {
      KnowledgePoint point = pointId == null ? null : pointMapper.selectById(pointId);
      if (point == null || !courseId.equals(point.getCourseId()))
        throw new IllegalArgumentException("知识点不属于所选课程");
    }
    for (Long resourceId :
        new LinkedHashSet<>(Optional.ofNullable(request.resourceIds()).orElse(List.of()))) {
      TeachingResource resource = resourceId == null ? null : resourceMapper.selectById(resourceId);
      if (resource == null
          || (!resource.getUploaderId().equals(user.getUserId())
              && !"APPROVED".equals(resource.getAuditStatus())))
        throw new IllegalArgumentException("使用资源不存在或当前不可用");
    }
    String visibility =
        request.visibility() == null ? "PUBLIC" : request.visibility().toUpperCase();
    if ("PRIVATE".equals(visibility)) visibility = "PUBLIC"; // 个人案例在正式提交前始终保留为草稿，提交后才进入公共审核流程。
    if (!VISIBILITY.contains(visibility)) throw new IllegalArgumentException("可见范围不合法");
    String access = request.accessType() == null ? "FREE" : request.accessType().toUpperCase();
    if (!ACCESS.contains(access)) throw new IllegalArgumentException("访问类型不合法");
    entity.setCaseName(request.caseName() == null ? "" : request.caseName().trim());
    entity.setCourseId(courseId);
    entity.setSummary(clean(request.summary()));
    String content = request.content();
    entity.setContent(
        content == null
                || (Jsoup.parseBodyFragment(content).body().text().isBlank()
                    && !content.contains("data-resource-id"))
            ? null
            : sanitizeContent(content));
    entity.setVisibility(visibility);
    entity.setAccessType(access);
    entity.setPointsPrice(
        "POINTS".equals(access) ? Optional.ofNullable(request.pointsPrice()).orElse(0) : 0);
  }

  private void replaceDraftRelations(Long id, CaseDraftRequest request) {
    relationMapper.deletePoints(id);
    relationMapper.deleteResources(id);
    for (Long pointId :
        new LinkedHashSet<>(Optional.ofNullable(request.knowledgePointIds()).orElse(List.of())))
      relationMapper.addPoint(id, pointId);
    int sort = 0;
    for (Long resourceId :
        new LinkedHashSet<>(Optional.ofNullable(request.resourceIds()).orElse(List.of())))
      relationMapper.addResource(id, resourceId, ++sort);
  }

  @Transactional
  @Override
  public CaseResponse review(Long id, CaseReviewRequest request, UserPrincipal user) {
    // 审核状态是公共案例可见性的唯一来源，前端传入的可见状态不能绕过审核。
    TeachingCase entity = caseMapper.selectById(id);
    if (entity == null) throw new IllegalArgumentException("案例不存在");
    if (entity.getCreatorId().equals(user.getUserId()))
      throw new IllegalArgumentException("不能审核自己提交的案例");
    if (!"PENDING".equals(entity.getAuditStatus())) throw new IllegalArgumentException("只能审核待审核案例");
    if (!"PUBLIC".equals(entity.getVisibility()))
      throw new IllegalArgumentException("旧可见范围已停用，请作者重新设置为公开后提交");
    if ("REJECTED".equals(request.result())
        && (request.comment() == null || request.comment().isBlank()))
      throw new IllegalArgumentException("驳回时请填写审核意见");
    if ("APPROVED".equals(request.result())
        && "POINTS".equals(entity.getAccessType())
        && (entity.getPointsPrice() == null
            || entity.getPointsPrice() < 1
            || entity.getPointsPrice() > 3))
      throw new IllegalArgumentException("积分案例定价须为1到3分，请先退回修改");
    int submission =
        Math.toIntExact(
                reviewMapper.selectCount(
                    Wrappers.<CaseReview>lambdaQuery().eq(CaseReview::getCaseId, id)))
            + 1;
    CaseReview review = new CaseReview();
    review.setCaseId(id);
    review.setReviewerId(user.getUserId());
    review.setSubmissionNo(submission);
    review.setReviewResult(request.result());
    review.setReviewComment(clean(request.comment()));
    review.setReviewedAt(LocalDateTime.now());
    reviewMapper.insert(review);
    entity.setAuditStatus(request.result());
    entity.setExcellent(
        "APPROVED".equals(request.result()) && Boolean.TRUE.equals(request.excellent()) ? 1 : 0);
    caseMapper.updateById(entity);
    if ("APPROVED".equals(request.result()))
      points.caseApproved(entity.getCreatorId(), id, "POINTS".equals(entity.getAccessType()));
    notifications.result(
        entity.getCreatorId(),
        user.getUserId(),
        "CASE",
        id,
        entity.getCaseName(),
        "APPROVED".equals(request.result()),
        request.comment());
    return response(
        caseMapper.selectById(id), favoriteMapper.exists(user.getUserId(), id) > 0, user);
  }

  @Transactional
  @Override
  public boolean toggleFavorite(Long id, UserPrincipal user) {
    TeachingCase entity = requireReadable(id, user);
    if (favoriteMapper.exists(user.getUserId(), id) > 0) {
      favoriteMapper.remove(user.getUserId(), id);
      return false;
    }
    if (!contentUnlocked(entity, user)) throw new IllegalArgumentException("请先解锁案例，再添加到个人收藏");
    favoriteMapper.add(user.getUserId(), id);
    return true;
  }

  @Transactional
  @Override
  public void delete(Long id, UserPrincipal user) {
    TeachingCase entity = requireWritable(id, user);
    relationMapper.deletePoints(id);
    relationMapper.deleteResources(id);
    caseMapper.deleteById(entity);
  }

  private void validate(CaseRequest request, UserPrincipal user) {
    Course course = courseMapper.selectById(request.courseId());
    if (course == null) throw new IllegalArgumentException("所属课程不存在");
    if (!user.roles().contains("ADMIN") && !course.getOwnerId().equals(user.getUserId()))
      throw new AccessDeniedException("只能为自己负责的课程创建案例");
    for (Long pointId : new LinkedHashSet<>(request.knowledgePointIds())) {
      KnowledgePoint point = pointMapper.selectById(pointId);
      if (point == null || !request.courseId().equals(point.getCourseId()))
        throw new IllegalArgumentException("知识点不属于所选课程");
    }
    for (Long resourceId :
        new LinkedHashSet<>(Optional.ofNullable(request.resourceIds()).orElse(List.of()))) {
      TeachingResource resource = resourceMapper.selectById(resourceId);
      if (resource == null
          || (!resource.getUploaderId().equals(user.getUserId())
              && !"APPROVED".equals(resource.getAuditStatus())))
        throw new IllegalArgumentException("使用资源不存在或当前不可用");
    }
    String visibility =
        normalize(
            request.visibility() == null ? "PUBLIC" : request.visibility(), VISIBILITY, "可见范围");
    if ("PRIVATE".equals(visibility))
      throw new IllegalArgumentException("新案例请保存到草稿箱，不再支持仅自己可见的已发布案例");
    String access =
        normalize(request.accessType() == null ? "FREE" : request.accessType(), ACCESS, "访问类型");
    if ("POINTS".equals(access) && Optional.ofNullable(request.pointsPrice()).orElse(0) <= 0)
      throw new IllegalArgumentException("积分访问案例必须设置大于0的积分价格");
    if ("POINTS".equals(access) && request.pointsPrice() > 3)
      throw new IllegalArgumentException("积分案例定价不能超过3分");
    if ("PRIVATE".equals(visibility) && "POINTS".equals(access))
      throw new IllegalArgumentException("私有案例不能设置积分访问");
  }

  private void apply(TeachingCase e, CaseRequest r) {
    e.setCaseName(r.caseName().trim());
    e.setCourseId(r.courseId());
    e.setSummary(clean(r.summary()));
    e.setTeachingObjective(clean(r.teachingObjective()));
    e.setKeyPoints(clean(r.keyPoints()));
    e.setDifficultPoints(clean(r.difficultPoints()));
    e.setTeachingProcess(clean(r.teachingProcess()));
    e.setTeachingReflection(clean(r.teachingReflection()));
    e.setContent(sanitizeContent(r.content()));
    e.setVisibility(
        normalize(r.visibility() == null ? "PUBLIC" : r.visibility(), VISIBILITY, "可见范围"));
    e.setAccessType(normalize(r.accessType() == null ? "FREE" : r.accessType(), ACCESS, "访问类型"));
    e.setPointsPrice("POINTS".equals(e.getAccessType()) ? r.pointsPrice() : 0);
  }

  private void replaceRelations(Long id, CaseRequest request) {
    relationMapper.deletePoints(id);
    relationMapper.deleteResources(id);
    new LinkedHashSet<>(request.knowledgePointIds())
        .forEach(pointId -> relationMapper.addPoint(id, pointId));
    int sort = 0;
    for (Long resourceId :
        new LinkedHashSet<>(Optional.ofNullable(request.resourceIds()).orElse(List.of())))
      relationMapper.addResource(id, resourceId, ++sort);
  }

  private TeachingCase requireReadable(Long id, UserPrincipal user) {
    TeachingCase e = caseMapper.selectById(id);
    if (e == null) throw new IllegalArgumentException("案例不存在");
    if (!canRead(e, user)) throw new AccessDeniedException("无权访问该案例");
    return e;
  }

  private TeachingCase requireWritable(Long id, UserPrincipal user) {
    TeachingCase e = caseMapper.selectById(id);
    if (e == null) throw new IllegalArgumentException("案例不存在");
    if (!user.roles().contains("ADMIN") && !e.getCreatorId().equals(user.getUserId()))
      throw new AccessDeniedException("无权修改该案例");
    return e;
  }

  private boolean canRead(TeachingCase c, UserPrincipal p) {
    if (p.roles().contains("ADMIN")
        || p.roles().contains("RESOURCE_MANAGER")
        || c.getCreatorId().equals(p.getUserId())) return true;
    if (!"APPROVED".equals(c.getAuditStatus())) return false;
    return "PUBLIC".equals(c.getVisibility());
  }

  private boolean contentUnlocked(TeachingCase c, UserPrincipal user) {
    return !"APPROVED".equals(c.getAuditStatus())
        || !"POINTS".equals(c.getAccessType())
        || c.getCreatorId().equals(user.getUserId())
        || user.roles().contains("ADMIN")
        || user.roles().contains("RESOURCE_MANAGER")
        || points.unlocked(user.getUserId(), c.getId());
  }

  private CaseResponse response(TeachingCase c, boolean favorite, UserPrincipal user) {
    boolean unlocked = contentUnlocked(c, user);
    Course course = c.getCourseId() == null ? null : courseMapper.selectById(c.getCourseId());
    SysUser creator = userMapper.selectById(c.getCreatorId());
    List<Long> pointIds = relationMapper.pointIds(c.getId()),
        resourceIds = relationMapper.resourceIds(c.getId());
    List<String> pointNames =
        pointIds.stream()
            .map(pointMapper::selectById)
            .filter(Objects::nonNull)
            .map(KnowledgePoint::getPointName)
            .toList();
    List<String> resourceNames =
        resourceIds.stream()
            .map(resourceMapper::selectById)
            .filter(Objects::nonNull)
            .map(TeachingResource::getResourceName)
            .toList();
    CaseReview last =
        reviewMapper.selectOne(
            Wrappers.<CaseReview>lambdaQuery()
                .eq(CaseReview::getCaseId, c.getId())
                .orderByDesc(CaseReview::getSubmissionNo)
                .last("LIMIT 1"));
    return new CaseResponse(
        c.getId(),
        c.getCaseNo(),
        c.getCaseName(),
        c.getSummary(),
        unlocked ? c.getTeachingObjective() : null,
        unlocked ? c.getKeyPoints() : null,
        unlocked ? c.getDifficultPoints() : null,
        unlocked ? c.getTeachingProcess() : null,
        unlocked ? c.getTeachingReflection() : null,
        unlocked ? c.getContent() : null,
        c.getCreatorId(),
        creator == null ? "未知教师" : creator.getRealName(),
        creator == null ? null : creator.getCollege(),
        c.getCourseId(),
        course == null ? null : course.getCourseName(),
        pointIds,
        pointNames,
        unlocked ? resourceIds : List.of(),
        unlocked ? resourceNames : List.of(),
        c.getVisibility(),
        c.getAccessType(),
        c.getPointsPrice(),
        c.getAuditStatus(),
        c.getExcellent(),
        c.getReferenceCount(),
        favorite,
        last == null ? null : last.getReviewComment(),
        c.getCreateTime(),
        c.getUpdateTime(),
        unlocked);
  }

  private String normalize(String value, Set<String> allowed, String field) {
    String v = value.toUpperCase();
    if (!allowed.contains(v)) throw new IllegalArgumentException(field + "不合法");
    return v;
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String sanitizeContent(String content) {
    Safelist safe =
        Safelist.relaxed()
            .addTags("figure", "figcaption", "video", "source")
            .addAttributes(":all", "class")
            .addAttributes("img", "data-resource-id", "data-resource-name")
            .addAttributes(
                "video", "data-resource-id", "data-resource-name", "controls", "preload", "poster")
            .addAttributes("source", "src", "type")
            .addAttributes("a", "data-resource-id", "data-resource-name")
            .addProtocols("a", "href", "http", "https")
            .addProtocols("img", "src", "http", "https");
    String cleaned = Jsoup.clean(content, safe);
    if (Jsoup.parseBodyFragment(cleaned).body().text().isBlank()
        && !cleaned.contains("data-resource-id")) throw new IllegalArgumentException("帖子正文不能为空");
    return cleaned;
  }
}
