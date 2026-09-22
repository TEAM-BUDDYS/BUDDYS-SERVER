package org.sopt.buddys.domain.verification.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.SuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExchangeVerificationSuccessCode implements SuccessCode {

  VERIFICATION_SUBMITTED("EXCH-S001", HttpStatus.CREATED, "파견교 인증 신청이 접수되었습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
