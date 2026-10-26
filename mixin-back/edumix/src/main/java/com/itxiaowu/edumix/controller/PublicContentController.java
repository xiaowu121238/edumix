package com.itxiaowu.edumix.controller;

import com.itxiaowu.edumix.common.ApiResponse;
import com.itxiaowu.edumix.dto.PublicHomeResponse;
import com.itxiaowu.edumix.dto.PublicSearchResponse;
import com.itxiaowu.edumix.service.PublicContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 无需登录的公共首页接口，只提供经过审核的公开内容摘要。 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicContentController {
  private final PublicContentService service;

  @GetMapping("/home")
  public ApiResponse<PublicHomeResponse> home() {
    return ApiResponse.success(service.home());
  }

  @GetMapping("/search")
  public ApiResponse<PublicSearchResponse> search(@RequestParam String keyword) {
    return ApiResponse.success(service.search(keyword));
  }
}
