package org.sopt.buddys.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.sopt.buddys.global.common.code.SuccessCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BaseResponse<T>(
    boolean success,
    String code,
    String message,
    T data
) {

  public static <T> BaseResponse<T> success(
      SuccessCode successCode,
      T data
  ) {
    return new BaseResponse<>(
        true,
        successCode.getCode(),
        successCode.getMessage(),
        data
    );
  }

  public static BaseResponse<Void> success(
      SuccessCode successCode
  ) {
    return new BaseResponse<>(
        true,
        successCode.getCode(),
        successCode.getMessage(),
        null
    );
  }

  public static BaseResponse<Void> failure(
      ErrorCode errorCode
  ) {
    return new BaseResponse<>(
        false,
        errorCode.getCode(),
        errorCode.getMessage(),
        null
    );
  }

  public static <T> BaseResponse<T> failure(
      ErrorCode errorCode,
      T data
  ) {
    return new BaseResponse<>(
        false,
        errorCode.getCode(),
        errorCode.getMessage(),
        data
    );
  }
}