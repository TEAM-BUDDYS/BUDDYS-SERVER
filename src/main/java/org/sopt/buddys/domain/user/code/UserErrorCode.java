package org.sopt.buddys.domain.user.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

  USER_NOT_FOUND("USER-E001", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
  DELETED_USER("USER-E002", HttpStatus.FORBIDDEN, "탈퇴한 사용자입니다."),
  UNIVERSITY_VERIFICATION_REQUIRED("USER-E003", HttpStatus.FORBIDDEN, "학교 인증이 필요합니다."),
  EXCHANGE_VERIFICATION_REQUIRED("USER-E004", HttpStatus.FORBIDDEN, "파견교 인증이 필요합니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
