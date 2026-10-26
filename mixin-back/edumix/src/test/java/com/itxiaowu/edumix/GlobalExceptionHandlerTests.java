package com.itxiaowu.edumix;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itxiaowu.edumix.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** 验证异常始终转换为前端可展示的统一 JSON，而不是返回空响应或默认错误页。 */
class GlobalExceptionHandlerTests {
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new ErrorController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void oversizedUploadReturnsReadableMessage() throws Exception {
    mvc.perform(get("/test-error/upload"))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.code").value(413))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("200MB")));
  }

  @Test
  void invalidPathParameterReturnsParameterName() throws Exception {
    mvc.perform(get("/test-error/id/undefined"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("id")));
  }

  @Test
  void unknownErrorDoesNotExposeInternalDetails() throws Exception {
    mvc.perform(get("/test-error/unknown"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value(500))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("database-secret"))));
  }

  @RestController
  static class ErrorController {
    @GetMapping("/test-error/upload")
    void upload() {
      throw new MaxUploadSizeExceededException(200L * 1024 * 1024);
    }

    @GetMapping("/test-error/id/{id}")
    Long id(@PathVariable Long id) {
      return id;
    }

    @GetMapping("/test-error/unknown")
    void unknown() {
      throw new IllegalStateException("database-secret");
    }
  }
}
