package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.UserProfileResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

/** 用户头像业务接口，定义头像上传和读取能力。 */
public interface AvatarService {
  UserProfileResponse upload(Long userId, MultipartFile file);

  AvatarFile read(String filename);

  record AvatarFile(FileSystemResource resource, MediaType contentType) {}
}
