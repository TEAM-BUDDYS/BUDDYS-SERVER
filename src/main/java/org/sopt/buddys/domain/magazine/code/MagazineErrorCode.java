package org.sopt.buddys.domain.magazine.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MagazineErrorCode implements ErrorCode {

  MAGAZINE_NOT_FOUND("MAGAZINE-E001", HttpStatus.NOT_FOUND, "매거진을 찾을 수 없습니다."),
  INVALID_YEAR_MONTH("MAGAZINE-E002", HttpStatus.BAD_REQUEST, "연도와 월을 올바르게 입력해주세요.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
