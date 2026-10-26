package com.itxiaowu.edumix.exception;

import com.itxiaowu.edumix.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 全局异常处理器：将参数校验、权限、业务及未知异常转换为统一接口响应。 */
@Slf4j
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

  @ExceptionHandler(HandlerMethodValidationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> methodValidation(HandlerMethodValidationException exception) {
    String message =
        exception.getParameterValidationResults().stream()
            .flatMap(result -> result.getResolvableErrors().stream())
            .map(error -> error.getDefaultMessage())
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse("请求参数不正确");
    return ApiResponse.error(400, message);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> constraintViolation(ConstraintViolationException exception) {
    String message =
        exception.getConstraintViolations().stream()
            .map(violation -> violation.getMessage())
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse("请求参数不正确");
    return ApiResponse.error(400, message);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> typeMismatch(MethodArgumentTypeMismatchException exception) {
    return ApiResponse.error(400, "参数“" + exception.getName() + "”格式不正确");
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> missingParameter(MissingServletRequestParameterException exception) {
    return ApiResponse.error(400, "缺少必填参数：“" + exception.getParameterName() + "”");
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> missingPart(MissingServletRequestPartException exception) {
    return ApiResponse.error(400, "缺少上传内容：“" + exception.getRequestPartName() + "”");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> unreadableBody() {
    return ApiResponse.error(400, "请求内容无法解析，请检查填写格式");
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

  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiResponse<Void> dataIntegrity(DataIntegrityViolationException exception) {
    log.warn("数据库约束校验失败", exception);
    return ApiResponse.error(409, "当前数据仍被其他内容使用，或与已有数据冲突");
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiResponse<Void> accessDenied(AccessDeniedException exception) {
    return ApiResponse.error(403, exception.getMessage());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
  public ApiResponse<Void> fileTooLarge() {
    return ApiResponse.error(413, "文件不能超过200MB，请压缩文件或选择较小的文件后重新上传");
  }

  @ExceptionHandler(MultipartException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> multipart(MultipartException exception) {
    log.warn("上传请求解析失败", exception);
    return ApiResponse.error(400, "上传请求无法解析，请检查文件大小、格式或网络后重试");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
  public ApiResponse<Void> unsupportedMediaType() {
    return ApiResponse.error(415, "文件或请求格式不受支持");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  public ApiResponse<Void> methodNotAllowed() {
    return ApiResponse.error(405, "当前接口不支持此操作");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiResponse<Void> notFound() {
    return ApiResponse.error(404, "请求的内容不存在或已被删除");
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponse<Void> unknown(Exception exception, HttpServletRequest request) {
    log.error("未处理的接口异常：{} {}", request.getMethod(), request.getRequestURI(), exception);
    return ApiResponse.error(500, "服务器处理失败，请稍后重试；如果问题持续出现，请联系管理员");
  }
}
