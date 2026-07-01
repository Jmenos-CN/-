package com.jmens.advisor.common.api;

public record Result<T>(int code, boolean success, String message, T data) {

  public static <T> Result<T> ok(T data) {
    return new Result<>(200, true, "success", data);
  }
}
