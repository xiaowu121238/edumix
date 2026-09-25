package com.itxiaowu.edumix.dto;

import java.util.List;

public record AiRecommendationResponse(String summary, List<Item> resources, List<Item> cases) {
  public record Item(
      Long id, String type, String name, String metadata, String reason, String useSuggestion) {}
}
