package org.sopt.buddys.domain.image.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {

  UNSUPPORTED_CONTENT_TYPE("IMG-E001", HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다."),
  FILE_TOO_LARGE("IMG-E002", HttpStatus.BAD_REQUEST, "허용된 파일 크기를 초과했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}