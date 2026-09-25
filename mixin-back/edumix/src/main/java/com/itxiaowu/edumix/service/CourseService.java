package com.itxiaowu.edumix.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itxiaowu.edumix.dto.*;
import com.itxiaowu.edumix.entity.Course;
import com.itxiaowu.edumix.entity.CourseChapter;
import com.itxiaowu.edumix.entity.KnowledgePoint;
import com.itxiaowu.edumix.mapper.CourseChapterMapper;
import com.itxiaowu.edumix.mapper.CourseMapper;
import com.itxiaowu.edumix.mapper.KnowledgePointMapper;
import com.itxiaowu.edumix.security.UserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {
  private final CourseMapper courseMapper;
  private final CourseChapterMapper chapterMapper;
  private final KnowledgePointMapper pointMapper;
  private final ResourceService resourceService;
  private final JdbcTemplate jdbc;

  public List<KnowledgePointResourceResponse> linkedResources(Long pointId, UserPrincipal user) {
    KnowledgePoint point = requirePoint(pointId);
    requireReadable(point.getCourseId(), user);
    Map<Long, ResourceResponse> readable =
        resourceService.list(null, null, null, false, user).stream()
            .filter(r -> "APPROVED".equals(r.auditStatus()))
            .collect(Collectors.toMap(ResourceResponse::id, r -> r));
    return jdbc.query(
        "SELECT resource_id FROM resource_knowledge_point WHERE knowledge_point_id = ? ORDER BY create_time, resource_id",
        (rs, row) -> {
          long id = rs.getLong(1);
          ResourceResponse resource = readable.get(id);
          return resource == null
              ? new KnowledgePointResourceResponse(id, "资源暂不可用", null, null, false)
              : new KnowledgePointResourceResponse(
                  id,
                  resource.resourceName(),
                  resource.resourceType(),
                  resource.categoryName(),
                  true);
        },
        pointId);
  }

  public List<ResourceResponse> favoriteCandidates(Long pointId, UserPrincipal user) {
    KnowledgePoint point = requirePoint(pointId);
    requireWritable(point.getCourseId(), user);
    Set<Long> linked =
        jdbc
            .query(
                "SELECT resource_id FROM resource_knowledge_point WHERE knowledge_point_id = ?",
                (rs, row) -> rs.getLong(1),
                pointId)
            .stream()
            .collect(Collectors.toSet());
    return resourceService.list(null, null, null, true, user).stream()
        .filter(r -> "APPROVED".equals(r.auditStatus()) && !linked.contains(r.id()))
        .toList();
  }

  @Transactional
  public void linkResources(Long pointId, List<Long> resourceIds, UserPrincipal user) {
    KnowledgePoint point = requirePoint(pointId);
    requireWritable(point.getCourseId(), user);
    if (resourceIds == null
        || resourceIds.isEmpty()
        || resourceIds.size() > 50
        || resourceIds.stream().anyMatch(id -> id == null || id <= 0))
      throw new IllegalArgumentException("请选择1至50个有效资源");
    Set<Long> allowed =
        resourceService.list(null, null, null, true, user).stream()
            .filter(r -> "APPROVED".equals(r.auditStatus()))
            .map(ResourceResponse::id)
            .collect(Collectors.toSet());
    if (!allowed.containsAll(resourceIds))
      throw new IllegalArgumentException("只能关联当前账号收藏且已审核通过的可访问资源");
    for (Long resourceId : Set.copyOf(resourceIds))
      jdbc.update(
          "INSERT IGNORE INTO resource_knowledge_point (resource_id, knowledge_point_id, source) VALUES (?, ?, 'MANUAL')",
          resourceId,
          pointId);
  }

  @Transactional
  public void unlinkResource(Long pointId, Long resourceId, UserPrincipal user) {
    KnowledgePoint point = requirePoint(pointId);
    requireWritable(point.getCourseId(), user);
    jdbc.update(
        "DELETE FROM resource_knowledge_point WHERE knowledge_point_id = ? AND resource_id = ?",
        pointId,
        resourceId);
  }

  public List<CourseSummaryResponse> list(UserPrincipal user) {
    var query = Wrappers.<Course>lambdaQuery().orderByDesc(Course::getUpdateTime);
    if (!isAdminOrReviewer(user)) query.eq(Course::getOwnerId, user.getUserId());
    return courseMapper.selectList(query).stream().map(this::summary).toList();
  }

  public CourseTreeResponse tree(Long courseId, UserPrincipal user) {
    Course course = requireReadable(courseId, user);
    List<CourseChapter> chapters =
        chapterMapper.selectList(
            Wrappers.<CourseChapter>lambdaQuery()
                .eq(CourseChapter::getCourseId, courseId)
                .orderByAsc(CourseChapter::getSortOrder)
                .orderByAsc(CourseChapter::getId));
    List<KnowledgePoint> points =
        pointMapper.selectList(
            Wrappers.<KnowledgePoint>lambdaQuery()
                .eq(KnowledgePoint::getCourseId, courseId)
                .orderByAsc(KnowledgePoint::getSortOrder)
                .orderByAsc(KnowledgePoint::getId));
    List<ChapterTreeResponse> nodes =
        chapters.stream()
            .map(
                chapter ->
                    new ChapterTreeResponse(
                        chapter.getId(),
                        chapter.getCourseId(),
                        chapter.getChapterName(),
                        chapter.getChapterNo(),
                        chapter.getDescription(),
                        chapter.getSortOrder(),
                        chapter.getUpdateTime(),
                        points.stream()
                            .filter(point -> chapter.getId().equals(point.getChapterId()))
                            .map(this::pointResponse)
                            .toList()))
            .toList();
    return new CourseTreeResponse(summary(course), nodes);
  }

  @Transactional
  public CourseSummaryResponse create(CourseRequest request, UserPrincipal user) {
    Course course = new Course();
    course.setOwnerId(user.getUserId());
    apply(course, request);
    if (blank(course.getCourseCode()))
      course.setCourseCode("COURSE-" + user.getUserId() + "-" + System.currentTimeMillis());
    courseMapper.insert(course);
    return summary(courseMapper.selectById(course.getId()));
  }

  @Transactional
  public CourseSummaryResponse update(Long id, CourseRequest request, UserPrincipal user) {
    Course course = requireWritable(id, user);
    apply(course, request);
    courseMapper.updateById(course);
    return summary(courseMapper.selectById(id));
  }

  @Transactional
  public void delete(Long id, UserPrincipal user) {
    requireWritable(id, user);
    pointMapper.delete(Wrappers.<KnowledgePoint>lambdaQuery().eq(KnowledgePoint::getCourseId, id));
    chapterMapper.delete(Wrappers.<CourseChapter>lambdaQuery().eq(CourseChapter::getCourseId, id));
    courseMapper.deleteById(id);
  }

  @Transactional
  public ChapterTreeResponse createChapter(
      Long courseId, ChapterRequest request, UserPrincipal user) {
    requireWritable(courseId, user);
    CourseChapter chapter = new CourseChapter();
    chapter.setCourseId(courseId);
    apply(chapter, request);
    chapterMapper.insert(chapter);
    return chapterResponse(chapterMapper.selectById(chapter.getId()));
  }

  @Transactional
  public ChapterTreeResponse updateChapter(Long id, ChapterRequest request, UserPrincipal user) {
    CourseChapter chapter = requireChapter(id);
    requireWritable(chapter.getCourseId(), user);
    apply(chapter, request);
    chapterMapper.updateById(chapter);
    return chapterResponse(chapterMapper.selectById(id));
  }

  @Transactional
  public void deleteChapter(Long id, UserPrincipal user) {
    CourseChapter chapter = requireChapter(id);
    requireWritable(chapter.getCourseId(), user);
    pointMapper.delete(Wrappers.<KnowledgePoint>lambdaQuery().eq(KnowledgePoint::getChapterId, id));
    chapterMapper.deleteById(id);
  }

  @Transactional
  public KnowledgePointResponse createPoint(
      Long chapterId, KnowledgePointRequest request, UserPrincipal user) {
    CourseChapter chapter = requireChapter(chapterId);
    requireWritable(chapter.getCourseId(), user);
    KnowledgePoint point = new KnowledgePoint();
    point.setCourseId(chapter.getCourseId());
    point.setChapterId(chapterId);
    apply(point, request);
    pointMapper.insert(point);
    return pointResponse(pointMapper.selectById(point.getId()));
  }

  @Transactional
  public KnowledgePointResponse updatePoint(
      Long id, KnowledgePointRequest request, UserPrincipal user) {
    KnowledgePoint point = requirePoint(id);
    requireWritable(point.getCourseId(), user);
    apply(point, request);
    pointMapper.updateById(point);
    return pointResponse(pointMapper.selectById(id));
  }

  @Transactional
  public void deletePoint(Long id, UserPrincipal user) {
    KnowledgePoint point = requirePoint(id);
    requireWritable(point.getCourseId(), user);
    pointMapper.deleteById(id);
  }

  private Course requireReadable(Long id, UserPrincipal user) {
    Course course = courseMapper.selectById(id);
    if (course == null) throw new IllegalArgumentException("课程不存在");
    if (!isAdminOrReviewer(user) && !course.getOwnerId().equals(user.getUserId()))
      throw new AccessDeniedException("无权访问该课程");
    return course;
  }

  private Course requireWritable(Long id, UserPrincipal user) {
    Course course = courseMapper.selectById(id);
    if (course == null) throw new IllegalArgumentException("课程不存在");
    if (!user.roles().contains("ADMIN") && !course.getOwnerId().equals(user.getUserId()))
      throw new AccessDeniedException("无权修改该课程");
    return course;
  }

  private CourseChapter requireChapter(Long id) {
    CourseChapter chapter = chapterMapper.selectById(id);
    if (chapter == null) throw new IllegalArgumentException("章节不存在");
    return chapter;
  }

  private KnowledgePoint requirePoint(Long id) {
    KnowledgePoint point = pointMapper.selectById(id);
    if (point == null) throw new IllegalArgumentException("知识点不存在");
    return point;
  }

  private boolean isAdminOrReviewer(UserPrincipal user) {
    return user.roles().contains("ADMIN") || user.roles().contains("RESOURCE_MANAGER");
  }

  private CourseSummaryResponse summary(Course course) {
    int chapters =
        Math.toIntExact(
            chapterMapper.selectCount(
                Wrappers.<CourseChapter>lambdaQuery()
                    .eq(CourseChapter::getCourseId, course.getId())));
    int points =
        Math.toIntExact(
            pointMapper.selectCount(
                Wrappers.<KnowledgePoint>lambdaQuery()
                    .eq(KnowledgePoint::getCourseId, course.getId())));
    return new CourseSummaryResponse(
        course.getId(),
        course.getCourseCode(),
        course.getCourseName(),
        course.getDescription(),
        course.getKnowledgeObjective(),
        course.getAbilityObjective(),
        course.getQualityObjective(),
        course.getCoverUrl(),
        course.getCollege(),
        course.getSemester(),
        course.getStatus(),
        chapters,
        points,
        course.getUpdateTime());
  }

  private ChapterTreeResponse chapterResponse(CourseChapter chapter) {
    return new ChapterTreeResponse(
        chapter.getId(),
        chapter.getCourseId(),
        chapter.getChapterName(),
        chapter.getChapterNo(),
        chapter.getDescription(),
        chapter.getSortOrder(),
        chapter.getUpdateTime(),
        List.of());
  }

  private KnowledgePointResponse pointResponse(KnowledgePoint point) {
    int resourceCount =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM resource_knowledge_point WHERE knowledge_point_id = ?",
            Integer.class,
            point.getId());
    int caseCount =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM case_knowledge_point WHERE knowledge_point_id = ?",
            Integer.class,
            point.getId());
    return new KnowledgePointResponse(
        point.getId(),
        point.getCourseId(),
        point.getChapterId(),
        point.getPointName(),
        point.getPointCode(),
        point.getDescription(),
        point.getDifficulty(),
        point.getTeachingHours(),
        point.getSortOrder(),
        resourceCount,
        caseCount,
        point.getUpdateTime());
  }

  private void apply(Course target, CourseRequest source) {
    target.setCourseCode(clean(source.courseCode()));
    target.setCourseName(source.courseName().trim());
    target.setDescription(clean(source.description()));
    target.setKnowledgeObjective(clean(source.knowledgeObjective()));
    target.setAbilityObjective(clean(source.abilityObjective()));
    target.setQualityObjective(clean(source.qualityObjective()));
    target.setCoverUrl(clean(source.coverUrl()));
    target.setCollege(clean(source.college()));
    target.setSemester(clean(source.semester()));
    int status = source.status() == null ? 1 : source.status();
    if (status < 0 || status > 2) throw new IllegalArgumentException("课程状态只能是0、1或2");
    target.setStatus(status);
  }

  private void apply(CourseChapter target, ChapterRequest source) {
    target.setChapterName(source.chapterName().trim());
    target.setChapterNo(clean(source.chapterNo()));
    target.setDescription(clean(source.description()));
    target.setSortOrder(source.sortOrder() == null ? 0 : source.sortOrder());
  }

  private void apply(KnowledgePoint target, KnowledgePointRequest source) {
    target.setPointName(source.pointName().trim());
    target.setPointCode(clean(source.pointCode()));
    target.setDescription(clean(source.description()));
    target.setDifficulty(source.difficulty() == null ? 2 : source.difficulty());
    target.setTeachingHours(
        source.teachingHours() == null
            ? BigDecimal.ONE
            : BigDecimal.valueOf(source.teachingHours()));
    target.setSortOrder(source.sortOrder() == null ? 0 : source.sortOrder());
  }

  private String clean(String value) {
    return blank(value) ? null : value.trim();
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
