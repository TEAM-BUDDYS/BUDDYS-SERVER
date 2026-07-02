package org.sopt.buddys.domain.user.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

  USER_NOT_FOUND("USER-E001", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
