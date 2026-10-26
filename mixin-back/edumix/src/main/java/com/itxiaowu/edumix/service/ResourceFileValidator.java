package com.itxiaowu.edumix.service;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;
import org.springframework.web.multipart.MultipartFile;

/** 文件容器及签名校验。限制读取量，不信任客户端声明的 MIME 类型。 */
public final class ResourceFileValidator {
  private ResourceFileValidator() {}

  public static void validate(MultipartFile file, String ext) {
    try (InputStream in = file.getInputStream()) {
      byte[] h = in.readNBytes(32);
      boolean valid =
          switch (ext) {
            case "jpg", "jpeg" -> starts(h, 0xff, 0xd8, 0xff);
            case "png" -> starts(h, 137, 80, 78, 71, 13, 10, 26, 10);
            case "gif" -> ascii(h, 0, "GIF87a") || ascii(h, 0, "GIF89a");
            case "webp" -> ascii(h, 0, "RIFF") && ascii(h, 8, "WEBP");
            case "pdf" -> ascii(h, 0, "%PDF-");
            case "mp4" ->
                h.length >= 16
                    && ascii(h, 4, "ftyp")
                    && Set.of(
                            "isom", "iso2", "iso3", "iso4", "iso5", "iso6", "mp41", "mp42", "avc1",
                            "M4V ", "MSNV", "dash")
                        .contains(new String(h, 8, 4, StandardCharsets.US_ASCII));
            case "doc", "xls", "ppt" -> starts(h, 0xd0, 0xcf, 0x11, 0xe0, 0xa1, 0xb1, 0x1a, 0xe1);
            case "docx", "xlsx", "pptx", "zip" -> validZip(file, ext);
            case "rar" ->
                starts(h, 82, 97, 114, 33, 26, 7, 0) || starts(h, 82, 97, 114, 33, 26, 7, 1, 0);
            case "7z" -> starts(h, 55, 122, 188, 175, 39, 28);
            case "txt" -> validText(file);
            default -> false;
          };
      if (!valid) throw new IllegalArgumentException("文件内容与扩展名不匹配或文件格式损坏");
    } catch (IOException e) {
      throw new IllegalArgumentException("无法校验文件格式，请重新选择文件", e);
    }
  }

  private static boolean starts(byte[] data, int... expected) {
    if (data.length < expected.length) return false;
    for (int i = 0; i < expected.length; i++) if ((data[i] & 255) != expected[i]) return false;
    return true;
  }

  private static boolean ascii(byte[] data, int offset, String value) {
    return data.length >= offset + value.length()
        && new String(data, offset, value.length(), StandardCharsets.US_ASCII).equals(value);
  }

  private static boolean validZip(MultipartFile file, String ext) throws IOException {
    // 先限制容器目录/解压规模，避免通过伪造 OOXML 或压缩炸弹绕过检查。
    Set<String> entries = new HashSet<>();
    long expanded = 0;
    int count = 0;
    try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
      ZipEntry entry;
      byte[] buffer = new byte[8192];
      while ((entry = zip.getNextEntry()) != null) {
        if (++count > 4096) throw new IllegalArgumentException("压缩文件条目过多，最多4096项");
        entries.add(entry.getName());
        int n;
        while ((n = zip.read(buffer)) != -1) {
          expanded += n;
          if (expanded > 256L * 1024 * 1024) throw new IllegalArgumentException("压缩内容展开后不能超过256MB");
        }
        zip.closeEntry();
      }
    }
    if (ext.equals("zip")) {
      try (InputStream in = file.getInputStream()) {
        byte[] h = in.readNBytes(4);
        return count > 0 || starts(h, 80, 75, 5, 6);
      }
    }
    String main =
        switch (ext) {
          case "docx" -> "word/document.xml";
          case "xlsx" -> "xl/workbook.xml";
          default -> "ppt/presentation.xml";
        };
    return entries.contains("[Content_Types].xml")
        && entries.contains("_rels/.rels")
        && entries.contains(main);
  }

  private static boolean validText(MultipartFile file) throws IOException {
    if (file.getSize() > 20L * 1024 * 1024) throw new IllegalArgumentException("文本文件不能超过20MB");
    byte[] data = file.getBytes();
    Charset charset = StandardCharsets.UTF_8;
    int offset = 0;
    if (starts(data, 255, 254)) {
      charset = StandardCharsets.UTF_16LE;
      offset = 2;
    } else if (starts(data, 254, 255)) {
      charset = StandardCharsets.UTF_16BE;
      offset = 2;
    }
    try {
      String text =
          charset
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(data, offset, data.length - offset))
              .toString();
      return text.chars()
          .noneMatch(c -> c == 0 || (c < 32 && c != 9 && c != 10 && c != 13 && c != 12));
    } catch (CharacterCodingException e) {
      return false;
    }
  }
}
