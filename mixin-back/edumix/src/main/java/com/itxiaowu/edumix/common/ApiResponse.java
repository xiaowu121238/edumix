package com.itxiaowu.edumix.common;

/**
 * 统一接口响应结构。
 *
 * @param code 业务状态码，0 表示成功
 * @param message 面向前端的结果说明
 * @param data 响应数据
 */
public record ApiResponse<T>(int code, String message, T data) {
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(200, "操作成功", data);
  }

  public static ApiResponse<Void> success() {
    return new ApiResponse<>(200, "操作成功", null);
  }

  public static ApiResponse<Void> error(int code, String message) {
    return new ApiResponse<>(code, message, null);
  }
}
