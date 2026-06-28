package org.sopt.buddys.domain.auth.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

  KAKAO_AUTH_FAILED(
      "AUTH-E001",
      HttpStatus.BAD_REQUEST,
      "카카오 인증에 실패했습니다."
  );

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
