package com.itxiaowu.edumix.dto;

import java.util.List;

/** AI 推荐响应，分别返回推荐资源、推荐案例及整体推荐说明。 */
public record AiRecommendationResponse(String summary, List<Item> resources, List<Item> cases) {
  public record Item(
      Long id, String type, String name, String metadata, String reason, String useSuggestion) {}
}
