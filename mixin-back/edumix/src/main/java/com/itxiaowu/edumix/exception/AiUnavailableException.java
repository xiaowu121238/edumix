package com.itxiaowu.edumix.exception;

/** AI 未配置、调用失败或服务暂不可用时抛出的业务异常。 */
public class AiUnavailableException extends RuntimeException {
  public AiUnavailableException(String message) {
    super(message);
  }

  public AiUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
