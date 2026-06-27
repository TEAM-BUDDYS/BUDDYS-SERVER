package org.sopt.buddys.global.exception;

import lombok.Getter;
import org.sopt.buddys.global.common.code.ErrorCode;

@Getter
public class BaseException extends RuntimeException {

  private final ErrorCode errorCode;

  public BaseException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public BaseException(ErrorCode errorCode, String detail) {
    super(detail);
    this.errorCode = errorCode;
  }
}