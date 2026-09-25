package com.itxiaowu.edumix.exception;

import com.itxiaowu.edumix.common.ApiResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(AiUnavailableException.class)
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public ApiResponse<Void> aiUnavailable(AiUnavailableException exception) {
    return ApiResponse.error(503, exception.getMessage());
  }

  @ExceptionHandler(BadCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ApiResponse<Void> badCredentials() {
    return ApiResponse.error(401, "账号或密码错误");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> validation(MethodArgumentNotValidException exception) {
    String message =
        exception.getBindingResult().getFieldErrors().isEmpty()
            ? "请求参数不正确"
            : exception.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
    return ApiResponse.error(400, message);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> illegalArgument(IllegalArgumentException exception) {
    return ApiResponse.error(400, exception.getMessage());
  }

  @ExceptionHandler(DuplicateKeyException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiResponse<Void> duplicateKey() {
    return ApiResponse.error(409, "数据已存在，请检查编码等唯一字段");
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiResponse<Void> accessDenied(AccessDeniedException exception) {
    return ApiResponse.error(403, exception.getMessage());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
  public ApiResponse<Void> fileTooLarge() {
    return ApiResponse.error(413, "文件不能超过200MB");
  }
}
