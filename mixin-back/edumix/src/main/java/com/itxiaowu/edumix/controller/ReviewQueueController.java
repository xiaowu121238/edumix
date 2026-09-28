package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.service.ReviewQueueService;
import com.itxiaowu.edumix.service.ReviewQueueService.PendingItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 负责人审核接口：查询待审资源与案例，并执行通过或驳回操作。 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESOURCE_MANAGER','ADMIN')")
public class ReviewQueueController {
  private final ReviewQueueService reviews;

  @GetMapping("/pending")
  public ApiResponse<List<PendingItem>> pending() {
    return ApiResponse.success(reviews.pending());
  }
}
