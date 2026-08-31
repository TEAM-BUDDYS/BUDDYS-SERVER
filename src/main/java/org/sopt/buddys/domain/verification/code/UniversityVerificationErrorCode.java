package org.sopt.buddys.domain.verification.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UniversityVerificationErrorCode implements ErrorCode {

  VERIFICATION_CODE_INVALID("UNIV-E001", HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않거나 만료되었습니다."),
  MAIL_SEND_FAILED("UNIV-E002", HttpStatus.INTERNAL_SERVER_ERROR, "인증 메일 발송에 실패했습니다."),
  VERIFICATION_ATTEMPT_LIMIT_EXCEEDED("UNIV-E003", HttpStatus.TOO_MANY_REQUESTS, "인증번호 입력 가능 횟수를 초과했습니다. 인증번호를 다시 발급해주세요.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
