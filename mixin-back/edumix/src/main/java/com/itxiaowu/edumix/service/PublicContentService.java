package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.PublicHomeResponse;
import com.itxiaowu.edumix.dto.PublicSearchResponse;

/** 公共首页业务接口。 */
public interface PublicContentService {
  PublicHomeResponse home();

  PublicSearchResponse search(String keyword);
}
