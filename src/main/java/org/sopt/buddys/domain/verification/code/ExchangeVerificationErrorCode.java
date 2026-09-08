package org.sopt.buddys.domain.verification.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExchangeVerificationErrorCode implements ErrorCode {

  UNSUPPORTED_DOCUMENT_TYPE("EXCH-E001", HttpStatus.BAD_REQUEST, "지원하지 않는 서류 형식입니다."),
  DOCUMENT_FILE_TOO_LARGE("EXCH-E002", HttpStatus.BAD_REQUEST, "허용된 서류 파일 크기를 초과했습니다."),
  INVALID_DOCUMENT_KEY("EXCH-E003", HttpStatus.BAD_REQUEST, "유효하지 않은 서류 키입니다."),
  DOCUMENT_NOT_UPLOADED("EXCH-E004", HttpStatus.BAD_REQUEST, "업로드된 서류를 찾을 수 없습니다."),
  DOCUMENT_METADATA_MISMATCH("EXCH-E005", HttpStatus.BAD_REQUEST, "업로드된 서류 정보가 일치하지 않습니다."),
  PENDING_VERIFICATION_ALREADY_EXISTS("EXCH-E006", HttpStatus.CONFLICT, "이미 처리 대기 중인 파견교 인증 신청이 있습니다."),
  DOCUMENT_ALREADY_SUBMITTED("EXCH-E007", HttpStatus.CONFLICT, "이미 제출된 서류입니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
