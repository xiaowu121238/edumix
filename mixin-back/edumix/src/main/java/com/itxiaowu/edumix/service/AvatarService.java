package com.itxiaowu.edumix.service;

import com.itxiaowu.edumix.dto.UserProfileResponse;
import com.itxiaowu.edumix.entity.SysUser;
import com.itxiaowu.edumix.mapper.SysUserMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AvatarService {
  private static final Map<String, MediaType> TYPES =
      Map.of(
          "jpg",
          MediaType.IMAGE_JPEG,
          "png",
          MediaType.IMAGE_PNG,
          "webp",
          MediaType.parseMediaType("image/webp"));
  private final SysUserMapper userMapper;
  private final UserService userService;

  @Value("${edumix.storage.location:uploads}")
  private String storageLocation;

  public UserProfileResponse upload(Long userId, MultipartFile file) {
    if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择头像图片");
    if (file.getSize() > 2 * 1024 * 1024) throw new IllegalArgumentException("头像不能超过2MB");
    String original =
        file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
    String extension = original.substring(original.lastIndexOf('.') + 1);
    if ("jpeg".equals(extension)) extension = "jpg";
    if (!TYPES.containsKey(extension)) throw new IllegalArgumentException("头像只支持 JPG、PNG 或 WebP");
    try {
      byte[] bytes = file.getBytes();
      if (!validSignature(extension, bytes)) throw new IllegalArgumentException("头像文件格式不正确");
      Path directory = avatarDirectory();
      Files.createDirectories(directory);
      String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
      Path target = directory.resolve(filename);
      Files.write(target, bytes);
      try {
        SysUser user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus()))
          throw new IllegalArgumentException("用户不存在或已禁用");
        user.setAvatarUrl("/api/profile/avatars/" + filename);
        userMapper.updateById(user);
        return userService.profile(userId);
      } catch (RuntimeException exception) {
        Files.deleteIfExists(target);
        throw exception;
      }
    } catch (IOException exception) {
      throw new IllegalArgumentException("头像保存失败");
    }
  }

  public AvatarFile read(String filename) {
    if (!filename.matches("[0-9a-f]{32}\\.(jpg|png|webp)"))
      throw new IllegalArgumentException("头像不存在");
    Path path = avatarDirectory().resolve(filename);
    if (!Files.isRegularFile(path)) throw new IllegalArgumentException("头像不存在");
    return new AvatarFile(
        new FileSystemResource(path), TYPES.get(filename.substring(filename.lastIndexOf('.') + 1)));
  }

  private Path avatarDirectory() {
    return Paths.get(storageLocation).toAbsolutePath().normalize().resolve("avatars");
  }

  private boolean validSignature(String extension, byte[] bytes) {
    if ("jpg".equals(extension))
      return bytes.length >= 3
          && (bytes[0] & 255) == 255
          && (bytes[1] & 255) == 216
          && (bytes[2] & 255) == 255;
    if ("png".equals(extension))
      return bytes.length >= 8
          && Arrays.equals(
              Arrays.copyOf(bytes, 8), new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
    return bytes.length >= 12
        && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
        && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
  }

  public record AvatarFile(FileSystemResource resource, MediaType contentType) {}
}
