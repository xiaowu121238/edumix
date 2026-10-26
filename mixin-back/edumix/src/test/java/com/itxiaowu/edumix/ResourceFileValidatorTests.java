package com.itxiaowu.edumix;

import static org.junit.jupiter.api.Assertions.*;

import com.itxiaowu.edumix.service.ResourceFileValidator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ResourceFileValidatorTests {
  private void validate(String ext, byte[] data) {
    ResourceFileValidator.validate(
        new MockMultipartFile("file", "file." + ext, "application/octet-stream", data), ext);
  }

  @Test
  void rejectsRenamedExecutableAcrossSupportedFormats() {
    for (String ext :
        new String[] {
          "jpg", "png", "gif", "webp", "pdf", "mp4", "doc", "xls", "ppt", "docx", "xlsx", "pptx",
          "zip", "rar", "7z", "txt"
        }) {
      assertThrows(
          IllegalArgumentException.class,
          () -> validate(ext, new byte[] {77, 90, 0, 0, 1, 2}),
          ext);
    }
  }

  @Test
  void acceptsPdfSignatureAndUnicodeText() {
    assertDoesNotThrow(() -> validate("pdf", "%PDF-1.7\n%%EOF".getBytes()));
    assertDoesNotThrow(() -> validate("txt", "中文教学内容\n第二行".getBytes(StandardCharsets.UTF_8)));
    assertThrows(
        IllegalArgumentException.class,
        () -> validate("txt", new byte[] {(byte) 0xff, (byte) 0xff}));
  }

  private byte[] zip(String... names) throws IOException {
    var out = new ByteArrayOutputStream();
    try (var z = new ZipOutputStream(out)) {
      for (String name : names) {
        z.putNextEntry(new ZipEntry(name));
        z.write("<xml/>".getBytes());
        z.closeEntry();
      }
    }
    return out.toByteArray();
  }

  @Test
  void checksOfficeContainerAgainstExtension() throws Exception {
    byte[] word = zip("[Content_Types].xml", "_rels/.rels", "word/document.xml");
    assertDoesNotThrow(() -> validate("docx", word));
    assertDoesNotThrow(() -> validate("zip", word));
    assertThrows(IllegalArgumentException.class, () -> validate("xlsx", word));
    assertThrows(IllegalArgumentException.class, () -> validate("docx", zip("arbitrary.txt")));
  }

  @Test
  void rejectsTooManyArchiveEntries() throws Exception {
    String[] entries = new String[4097];
    for (int i = 0; i < entries.length; i++) entries[i] = "entry" + i;
    byte[] archive = zip(entries);
    assertThrows(IllegalArgumentException.class, () -> validate("zip", archive));
  }
}
