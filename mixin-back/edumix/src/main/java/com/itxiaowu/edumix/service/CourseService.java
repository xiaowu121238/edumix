package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.*;
import com.itxiaowu.edumix.security.UserPrincipal;
import java.util.List;

/** 课程体系业务接口，定义课程、章节、知识点及关联资源操作。 */
public interface CourseService {
  List<KnowledgePointResourceResponse> linkedResources(Long pointId, UserPrincipal user);

  List<ResourceResponse> favoriteCandidates(Long pointId, UserPrincipal user);

  void linkResources(Long pointId, List<Long> resourceIds, UserPrincipal user);

  void unlinkResource(Long pointId, Long resourceId, UserPrincipal user);

  List<CourseSummaryResponse> list(UserPrincipal user);

  CourseTreeResponse tree(Long courseId, UserPrincipal user);

  CourseSummaryResponse create(CourseRequest request, UserPrincipal user);

  CourseSummaryResponse update(Long id, CourseRequest request, UserPrincipal user);

  void delete(Long id, UserPrincipal user);

  ChapterTreeResponse createChapter(Long courseId, ChapterRequest request, UserPrincipal user);

  ChapterTreeResponse updateChapter(Long id, ChapterRequest request, UserPrincipal user);

  void deleteChapter(Long id, UserPrincipal user);

  KnowledgePointResponse createPoint(
      Long chapterId, KnowledgePointRequest request, UserPrincipal user);

  KnowledgePointResponse updatePoint(Long id, KnowledgePointRequest request, UserPrincipal user);

  void deletePoint(Long id, UserPrincipal user);
}
