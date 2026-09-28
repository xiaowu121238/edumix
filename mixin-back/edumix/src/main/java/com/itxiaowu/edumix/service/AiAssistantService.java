package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.*;
import com.itxiaowu.edumix.security.UserPrincipal;
import java.util.List;

/** AI 教学助手业务接口，定义推荐和案例草稿整合能力。 */
public interface AiAssistantService {
  AiStatusResponse status();

  AiRecommendationResponse recommend(AiRecommendRequest request, UserPrincipal user);

  AiChatResponse chat(AiChatRequest request, UserPrincipal user);

  RecommendationCatalog recommendationCatalog(Long courseId, UserPrincipal user);

  record RecommendationCatalog(List<ResourceResponse> resources, List<CaseResponse> cases) {}
}
