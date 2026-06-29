package org.sopt.buddys.domain.auth.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

  KAKAO_AUTH_FAILED("AUTH-E001", HttpStatus.BAD_REQUEST, "카카오 인증에 실패했습니다."),
  KAKAO_EMAIL_REQUIRED("AUTH-E002", HttpStatus.UNAUTHORIZED, "카카오 이메일 제공 동의가 필요합니다."),
  DUPLICATE_NICKNAME("AUTH-E003", HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
  REFRESH_TOKEN_NOT_FOUND("AUTH-E004", HttpStatus.UNAUTHORIZED, "리프레쉬 토큰을 찾을 수 없습니다."),
  REFRESH_TOKEN_EXPIRED("AUTH-E005", HttpStatus.UNAUTHORIZED, "리프레쉬 토큰이 만료되었습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
