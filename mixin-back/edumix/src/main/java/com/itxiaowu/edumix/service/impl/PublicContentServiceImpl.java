package com.itxiaowu.edumix.service.impl;

import com.itxiaowu.edumix.dto.PublicHomeResponse;
import com.itxiaowu.edumix.dto.PublicSearchResponse;
import com.itxiaowu.edumix.mapper.PublicContentMapper;
import com.itxiaowu.edumix.service.PublicContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 公共首页业务实现，统一控制首页数量、搜索上限及关键词校验。 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicContentServiceImpl implements PublicContentService {
  private static final int FEATURED_LIMIT = 6;
  private static final int SEARCH_LIMIT = 8;

  private final PublicContentMapper mapper;

  @Override
  public PublicHomeResponse home() {
    return new PublicHomeResponse(
        mapper.countActiveTeachers(),
        mapper.countPublicResources(),
        mapper.countPublicCases(),
        mapper.findFeaturedCases(FEATURED_LIMIT));
  }

  @Override
  public PublicSearchResponse search(String keyword) {
    String normalized = keyword == null ? "" : keyword.trim();
    if (normalized.isEmpty()) throw new IllegalArgumentException("请输入搜索关键词");
    if (normalized.length() > 50) throw new IllegalArgumentException("搜索关键词不能超过50个字符");
    return new PublicSearchResponse(
        normalized,
        mapper.searchResources(normalized, SEARCH_LIMIT),
        mapper.searchCases(normalized, SEARCH_LIMIT));
  }
}
