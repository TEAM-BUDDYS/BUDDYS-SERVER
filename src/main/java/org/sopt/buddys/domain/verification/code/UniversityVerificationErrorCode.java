package org.sopt.buddys.domain.verification.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UniversityVerificationErrorCode implements ErrorCode {

  VERIFICATION_TOKEN_NOT_FOUND("UNIV-E001", HttpStatus.BAD_REQUEST, "유효하지 않은 인증 링크입니다."),
  VERIFICATION_TOKEN_EXPIRED("UNIV-E002", HttpStatus.BAD_REQUEST, "만료된 인증 링크입니다."),
  VERIFICATION_TOKEN_OWNER_MISMATCH("UNIV-E003", HttpStatus.FORBIDDEN, "본인이 요청한 인증 링크가 아닙니다."),
  MAIL_SEND_FAILED("UNIV-E004", HttpStatus.INTERNAL_SERVER_ERROR, "인증 메일 발송에 실패했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
