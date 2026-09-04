package org.sopt.buddys.domain.verification.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExchangeVerificationErrorCode implements ErrorCode {

  UNSUPPORTED_DOCUMENT_TYPE("EXCH-E001", HttpStatus.BAD_REQUEST, "지원하지 않는 서류 형식입니다."),
  DOCUMENT_FILE_TOO_LARGE("EXCH-E002", HttpStatus.BAD_REQUEST, "허용된 서류 파일 크기를 초과했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
