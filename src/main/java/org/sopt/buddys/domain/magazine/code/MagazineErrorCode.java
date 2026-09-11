package org.sopt.buddys.domain.magazine.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MagazineErrorCode implements ErrorCode {

  MAGAZINE_NOT_FOUND("MAGAZINE-E001", HttpStatus.NOT_FOUND, "매거진을 찾을 수 없습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
