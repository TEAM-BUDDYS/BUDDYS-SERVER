package org.sopt.buddys.domain.location.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LocationErrorCode implements ErrorCode {
  COUNTRY_NOT_FOUND("LOC-E001", HttpStatus.NOT_FOUND, "국가를 찾을 수 없습니다."),
  CITY_NOT_FOUND("LOC-E002", HttpStatus.NOT_FOUND, "도시를 찾을 수 없습니다."),
  UNIVERSITY_NOT_FOUND("LOC-E003", HttpStatus.NOT_FOUND, "대학교를 찾을 수 없습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
